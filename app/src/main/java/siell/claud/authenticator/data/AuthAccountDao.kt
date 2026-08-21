package siell.claud.authenticator.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AuthAccountDao {
    @Query("SELECT * FROM accounts ORDER BY orderIndex ASC, timestamp DESC")
    fun getAllAccounts(): Flow<List<AuthAccount>>

    @Query("SELECT * FROM accounts ORDER BY orderIndex ASC, timestamp DESC")
    suspend fun getAllAccountsSync(): List<AuthAccount>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AuthAccount)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccounts(accounts: List<AuthAccount>)

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun deleteAccountById(id: String)
    
    @Query("DELETE FROM accounts")
    suspend fun deleteAllAccounts()
}
