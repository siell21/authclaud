package siell.claud.authenticator.sync

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface DriveService {
    @GET("drive/v3/files")
    suspend fun listFiles(
        @Header("Authorization") authHeader: String,
        @Query("spaces") spaces: String = "appDataFolder",
        @Query("q") query: String = "name='backup.json' and trashed=false",
        @Query("fields") fields: String = "files(id, name, modifiedTime)"
    ): DriveListResponse

    @POST("upload/drive/v3/files?uploadType=multipart")
    @Multipart
    suspend fun uploadFile(
        @Header("Authorization") authHeader: String,
        @Part metadata: MultipartBody.Part,
        @Part file: MultipartBody.Part
    ): DriveFile

    @PATCH("upload/drive/v3/files/{fileId}?uploadType=multipart")
    @Multipart
    suspend fun updateFile(
        @Header("Authorization") authHeader: String,
        @Path("fileId") fileId: String,
        @Part metadata: MultipartBody.Part,
        @Part file: MultipartBody.Part
    ): DriveFile

    @GET("drive/v3/files/{fileId}?alt=media")
    suspend fun downloadFile(
        @Header("Authorization") authHeader: String,
        @Path("fileId") fileId: String
    ): ResponseBody
}

data class DriveListResponse(
    val files: List<DriveFile>?
)

data class DriveFile(
    val id: String?,
    val name: String?,
    val modifiedTime: String?
)
