package siell.claud.authenticator.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@Entity(tableName = "accounts")
@JsonClass(generateAdapter = true)
data class AuthAccount(
    @PrimaryKey val id: String, // UUID
    val name: String,
    val issuer: String,
    val secretKeyEncrypted: String,
    val orderIndex: Int,
    val timestamp: Long = System.currentTimeMillis()
)
