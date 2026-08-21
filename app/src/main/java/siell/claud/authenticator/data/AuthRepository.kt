package siell.claud.authenticator.data

import kotlinx.coroutines.flow.Flow

class AuthRepository(private val authAccountDao: AuthAccountDao) {
    val allAccounts: Flow<List<AuthAccount>> = authAccountDao.getAllAccounts()

    suspend fun getAllAccountsSync(): List<AuthAccount> {
        return authAccountDao.getAllAccountsSync()
    }

    suspend fun insertAccount(account: AuthAccount) {
        authAccountDao.insertAccount(account)
    }
    
    suspend fun overwriteAllAccounts(accounts: List<AuthAccount>) {
        authAccountDao.deleteAllAccounts()
        authAccountDao.insertAccounts(accounts)
    }

    suspend fun deleteAccountById(id: String) {
        authAccountDao.deleteAccountById(id)
    }
    
    suspend fun deleteAllAccounts() {
        authAccountDao.deleteAllAccounts()
    }
}
