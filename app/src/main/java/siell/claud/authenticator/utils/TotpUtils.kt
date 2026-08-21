package siell.claud.authenticator.utils

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.pow

object TotpUtils {

    fun generateTotp(secretKey: String, time: Long = System.currentTimeMillis()): String {
        try {
            val keyBytes = decodeBase32(secretKey.replace(" ", "").uppercase())
            if (keyBytes.isEmpty()) return "000000"

            val timeStep = time / 30000L
            val timeBytes = ByteArray(8)
            for (i in 7 downTo 0) {
                timeBytes[i] = (timeStep and 0xFFL).toByte()
                timeStep.shr(8) // wait, actually need a local var
            }
            var ts = timeStep
            for (i in 7 downTo 0) {
                timeBytes[i] = (ts and 0xFFL).toByte()
                ts = ts shr 8
            }

            val mac = Mac.getInstance("HmacSHA1")
            val signKey = SecretKeySpec(keyBytes, "HmacSHA1")
            mac.init(signKey)
            val hash = mac.doFinal(timeBytes)

            val offset = (hash.last().toInt() and 0xF)
            val binary = ((hash[offset].toInt() and 0x7F) shl 24) or
                    ((hash[offset + 1].toInt() and 0xFF) shl 16) or
                    ((hash[offset + 2].toInt() and 0xFF) shl 8) or
                    (hash[offset + 3].toInt() and 0xFF)

            val otp = binary % 10.0.pow(6).toInt()
            return otp.toString().padStart(6, '0')
        } catch (e: Exception) {
            e.printStackTrace()
            return "000000"
        }
    }

    private fun decodeBase32(base32: String): ByteArray {
        val charset = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
        val bytes = ByteArray(base32.length * 5 / 8)
        var buffer = 0
        var bitsLeft = 0
        var count = 0
        for (char in base32) {
            val value = charset.indexOf(char)
            if (value == -1) continue // Skip padding or invalid characters
            buffer = (buffer shl 5) or value
            bitsLeft += 5
            if (bitsLeft >= 8) {
                bytes[count++] = (buffer shr (bitsLeft - 8)).toByte()
                bitsLeft -= 8
            }
        }
        return bytes.copyOf(count)
    }
}
