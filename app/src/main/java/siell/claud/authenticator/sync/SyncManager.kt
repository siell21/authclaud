package siell.claud.authenticator.sync

import android.content.Context
import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.squareup.moshi.JsonClass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import siell.claud.authenticator.data.AuthAccount
import siell.claud.authenticator.data.AuthRepository
import siell.claud.authenticator.utils.CryptoUtils
import siell.claud.authenticator.utils.PrefsUtils
import java.text.SimpleDateFormat
import java.util.Locale

@JsonClass(generateAdapter = true)
data class SyncAccount(
    val id: String,
    val name: String,
    val issuer: String,
    val secretKeyPlain: String,
    val orderIndex: Int,
    val timestamp: Long
)

class SyncManager(private val context: Context, private val repository: AuthRepository) {

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val listAdapter = moshi.adapter<List<SyncAccount>>(
        Types.newParameterizedType(List::class.java, SyncAccount::class.java)
    )
    
    private val driveService: DriveService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://www.googleapis.com/")
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
        retrofit.create(DriveService::class.java)
    }

    suspend fun performAutoSync(token: String, userId: String) = withContext(Dispatchers.IO) {
        try {
            val authHeader = "Bearer $token"
            val listResponse = driveService.listFiles(authHeader)
            val driveFile = listResponse.files?.firstOrNull()

            val localAccounts = repository.getAllAccountsSync()
            val localEmpty = localAccounts.isEmpty()
            
            if (driveFile == null) {
                if (!localEmpty) {
                    // Drive empty, Local has data -> Backup
                    performBackup(token, userId)
                }
            } else {
                if (localEmpty) {
                    // Drive has data, Local empty -> Restore
                    performRestore(token, userId)
                } else {
                    // Compare timestamps. We'll use the most recently modified local account as local timestamp.
                    val lastLocalMod = localAccounts.maxOfOrNull { it.timestamp } ?: 0L
                    
                    val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
                    val driveTime = driveFile.modifiedTime?.let { format.parse(it)?.time } ?: 0L
                    
                    if (lastLocalMod > driveTime) {
                        performBackup(token, userId)
                    } else if (driveTime > lastLocalMod) {
                        performRestore(token, userId)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    suspend fun performBackup(token: String, userId: String) = withContext(Dispatchers.IO) {
        val authHeader = "Bearer $token"
        val accounts = repository.getAllAccountsSync()
        
        val plainAccounts = accounts.map { account ->
            SyncAccount(
                id = account.id,
                name = account.name,
                issuer = account.issuer,
                secretKeyPlain = CryptoUtils.decryptLocal(account.secretKeyEncrypted),
                orderIndex = account.orderIndex,
                timestamp = account.timestamp
            )
        }
        
        // Encrypt the accounts as a JSON string
        val json = listAdapter.toJson(plainAccounts)
        val encryptedData = CryptoUtils.encrypt(json, userId)
        
        val fileBody = encryptedData.toRequestBody("application/json".toMediaTypeOrNull())
        val filePart = MultipartBody.Part.createFormData("file", "backup.json", fileBody)
        
        val metadataJson = """{"name": "backup.json", "parents": ["appDataFolder"]}"""
        val metadataBody = metadataJson.toRequestBody("application/json".toMediaTypeOrNull())
        val metadataPart = MultipartBody.Part.createFormData("metadata", "metadata.json", metadataBody)
        
        val listResponse = driveService.listFiles(authHeader)
        val existingFile = listResponse.files?.firstOrNull()
        
        if (existingFile != null && existingFile.id != null) {
            driveService.updateFile(authHeader, existingFile.id, metadataPart, filePart)
        } else {
            driveService.uploadFile(authHeader, metadataPart, filePart)
        }
        PrefsUtils.setLastBackupTime(context, System.currentTimeMillis())
    }

    suspend fun performRestore(token: String, userId: String) = withContext(Dispatchers.IO) {
        val authHeader = "Bearer $token"
        val listResponse = driveService.listFiles(authHeader)
        val driveFile = listResponse.files?.firstOrNull()
            ?: throw Exception("No backup file found in Drive.")
            
        val fileId = driveFile.id ?: throw Exception("Invalid file ID")
        val responseBody = driveService.downloadFile(authHeader, fileId)
        val encryptedData = responseBody.string()
        
        val decryptedJson = CryptoUtils.decrypt(encryptedData, userId)
        val plainAccounts = listAdapter.fromJson(decryptedJson) ?: emptyList()
        
        val encryptedAccounts = plainAccounts.map { syncAccount ->
            AuthAccount(
                id = syncAccount.id,
                name = syncAccount.name,
                issuer = syncAccount.issuer,
                secretKeyEncrypted = CryptoUtils.encryptLocal(syncAccount.secretKeyPlain),
                orderIndex = syncAccount.orderIndex,
                timestamp = syncAccount.timestamp
            )
        }
        
        repository.overwriteAllAccounts(encryptedAccounts)
        PrefsUtils.setLastRestoreTime(context, System.currentTimeMillis())
    }
}
