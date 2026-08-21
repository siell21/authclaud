package siell.claud.authenticator.utils

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "settings")

object PrefsUtils {
    private val LAST_BACKUP_TIME = longPreferencesKey("last_backup_time")
    private val LAST_RESTORE_TIME = longPreferencesKey("last_restore_time")
    private val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
    private val APP_LOCK_METHOD = stringPreferencesKey("app_lock_method") // "biometric" or "pin"
    private val APP_LOCK_PIN = stringPreferencesKey("app_lock_pin")

    suspend fun setLastBackupTime(context: Context, time: Long) {
        context.dataStore.edit { it[LAST_BACKUP_TIME] = time }
    }

    fun getLastBackupTime(context: Context): Flow<Long> {
        return context.dataStore.data.map { it[LAST_BACKUP_TIME] ?: 0L }
    }

    suspend fun setLastRestoreTime(context: Context, time: Long) {
        context.dataStore.edit { it[LAST_RESTORE_TIME] = time }
    }

    fun getLastRestoreTime(context: Context): Flow<Long> {
        return context.dataStore.data.map { it[LAST_RESTORE_TIME] ?: 0L }
    }

    suspend fun setAppLockEnabled(context: Context, enabled: Boolean) {
        context.dataStore.edit { it[APP_LOCK_ENABLED] = enabled }
    }

    fun isAppLockEnabled(context: Context): Flow<Boolean> {
        return context.dataStore.data.map { it[APP_LOCK_ENABLED] ?: false }
    }
    
    suspend fun setAppLockMethod(context: Context, method: String) {
        context.dataStore.edit { it[APP_LOCK_METHOD] = method }
    }
    
    fun getAppLockMethod(context: Context): Flow<String> {
        return context.dataStore.data.map { it[APP_LOCK_METHOD] ?: "" }
    }
    
    suspend fun setAppLockPin(context: Context, pin: String) {
        context.dataStore.edit { it[APP_LOCK_PIN] = pin }
    }
    
    fun getAppLockPin(context: Context): Flow<String> {
        return context.dataStore.data.map { it[APP_LOCK_PIN] ?: "" }
    }
}
