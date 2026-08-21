package siell.claud.authenticator.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [AuthAccount::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun authAccountDao(): AuthAccountDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "auth_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
