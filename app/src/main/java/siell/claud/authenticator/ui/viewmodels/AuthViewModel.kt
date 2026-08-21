package siell.claud.authenticator.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import siell.claud.authenticator.data.AppDatabase
import siell.claud.authenticator.data.AuthAccount
import siell.claud.authenticator.data.AuthRepository
import siell.claud.authenticator.sync.SyncManager
import siell.claud.authenticator.utils.PrefsUtils
import java.util.UUID

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: AuthRepository
    private val syncManager: SyncManager

    init {
        val database = AppDatabase.getDatabase(application)
        repository = AuthRepository(database.authAccountDao())
        syncManager = SyncManager(application, repository)
    }

    val allAccounts = repository.allAccounts

    private val _userEmail = MutableStateFlow<String?>(null)
    val userEmail = _userEmail.asStateFlow()

    private val _userName = MutableStateFlow<String?>(null)
    val userName = _userName.asStateFlow()
    
    private val _userProfilePicUrl = MutableStateFlow<String?>(null)
    val userProfilePicUrl = _userProfilePicUrl.asStateFlow()

    private val _userId = MutableStateFlow<String?>(null)
    val userId = _userId.asStateFlow()

    private val _accessToken = MutableStateFlow<String?>(null)
    
    private val _isAppLockEnabled = MutableStateFlow(false)
    val isAppLockEnabled = _isAppLockEnabled.asStateFlow()

    private val _appLockMethod = MutableStateFlow("")
    val appLockMethod = _appLockMethod.asStateFlow()

    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked = _isUnlocked.asStateFlow()
    
    private val _syncStatus = MutableStateFlow<String?>(null)
    val syncStatus = _syncStatus.asStateFlow()

    init {
        viewModelScope.launch {
            _isAppLockEnabled.value = PrefsUtils.isAppLockEnabled(application).first()
            _appLockMethod.value = PrefsUtils.getAppLockMethod(application).first()
        }
    }
    
    fun setLoginData(id: String, email: String, name: String, picUrl: String?, token: String) {
        _userId.value = id
        _userEmail.value = email
        _userName.value = name
        _userProfilePicUrl.value = picUrl
        _accessToken.value = token
        
        viewModelScope.launch {
            try {
                _syncStatus.value = "Syncing..."
                syncManager.performAutoSync(token, id)
                _syncStatus.value = "Synced successfully"
            } catch (e: Exception) {
                e.printStackTrace()
                _syncStatus.value = "Auto-sync failed"
            }
        }
    }

    fun logout() {
        _userId.value = null
        _userEmail.value = null
        _userName.value = null
        _userProfilePicUrl.value = null
        _accessToken.value = null
        _isUnlocked.value = false
    }

    fun unlockApp() {
        _isUnlocked.value = true
    }
    
    fun lockApp() {
        _isUnlocked.value = false
    }

    fun setAppLock(enabled: Boolean, method: String, pin: String) {
        viewModelScope.launch {
            PrefsUtils.setAppLockEnabled(getApplication(), enabled)
            PrefsUtils.setAppLockMethod(getApplication(), method)
            if (method == "pin") {
                PrefsUtils.setAppLockPin(getApplication(), pin)
            }
            _isAppLockEnabled.value = enabled
            _appLockMethod.value = method
        }
    }

    fun addAccount(name: String, issuer: String, secretKey: String) {
        viewModelScope.launch {
            val count = repository.getAllAccountsSync().size
            val encryptedSecret = siell.claud.authenticator.utils.CryptoUtils.encryptLocal(secretKey)
            val account = AuthAccount(
                id = UUID.randomUUID().toString(),
                name = name,
                issuer = issuer,
                secretKeyEncrypted = encryptedSecret,
                orderIndex = count
            )
            repository.insertAccount(account)
            triggerAutoSync()
        }
    }
    
    fun updateAccount(account: AuthAccount) {
        viewModelScope.launch {
            repository.insertAccount(account)
            triggerAutoSync()
        }
    }

    fun deleteAccount(id: String) {
        viewModelScope.launch {
            repository.deleteAccountById(id)
            triggerAutoSync()
        }
    }

    fun performManualBackup() {
        viewModelScope.launch {
            val token = _accessToken.value
            val uid = _userId.value
            if (token != null && uid != null) {
                try {
                    _syncStatus.value = "Backing up..."
                    syncManager.performBackup(token, uid)
                    _syncStatus.value = "Backup successful"
                } catch (e: Exception) {
                    _syncStatus.value = "Backup failed: ${e.message}"
                }
            } else {
                _syncStatus.value = "Backup failed: Not logged in"
            }
        }
    }

    fun performManualRestore() {
        viewModelScope.launch {
            val token = _accessToken.value
            val uid = _userId.value
            if (token != null && uid != null) {
                try {
                    _syncStatus.value = "Restoring..."
                    syncManager.performRestore(token, uid)
                    _syncStatus.value = "Restore successful"
                } catch (e: Exception) {
                    _syncStatus.value = "Restore failed: ${e.message}"
                }
            } else {
                _syncStatus.value = "Restore failed: Not logged in"
            }
        }
    }
    
    private fun triggerAutoSync() {
        val token = _accessToken.value
        val uid = _userId.value
        if (token != null && uid != null) {
            viewModelScope.launch {
                try {
                    syncManager.performBackup(token, uid)
                } catch (e: Exception) {
                    e.printStackTrace()
                    _syncStatus.value = "Auto-sync failed"
                }
            }
        }
    }
}
