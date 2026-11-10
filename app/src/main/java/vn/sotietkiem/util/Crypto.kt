package vn.sotietkiem.util

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Mã hóa AES-256-GCM.
 * - Dữ liệu trên máy: khóa sinh trong Android Keystore, không bao giờ rời khỏi chip bảo mật của máy.
 * - File xuất và dữ liệu trên Firebase: khóa suy ra từ mật khẩu bằng PBKDF2-HMAC-SHA256.
 */
object Crypto {
    private const val KEYSTORE = "AndroidKeyStore"
    private const val LOCAL_ALIAS = "stk_local_key"
    private const val GCM_TAG_BITS = 128
    private const val IV_LEN = 12
    const val PBKDF2_ITER = 120_000

    /** Đầu file đã mã hóa trên máy, để phân biệt với file JSON cũ chưa mã hóa. */
    private val MAGIC = "STKENC1".toByteArray()

    private val random = SecureRandom()

    fun randomBytes(n: Int) = ByteArray(n).also { random.nextBytes(it) }

    // ---- Khóa trong Android Keystore (cho file trên máy) ----

    private fun localKey(): SecretKey {
        val ks = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (ks.getKey(LOCAL_ALIAS, null) as? SecretKey)?.let { return it }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        gen.init(
            KeyGenParameterSpec.Builder(LOCAL_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return gen.generateKey()
    }

    fun isLocalEncrypted(bytes: ByteArray) =
        bytes.size > MAGIC.size && bytes.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)

    /** MAGIC + iv + ciphertext. IV do Keystore tự sinh. */
    fun encryptLocal(plain: ByteArray): ByteArray {
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.ENCRYPT_MODE, localKey())
        return MAGIC + c.iv + c.doFinal(plain)
    }

    fun decryptLocal(data: ByteArray): ByteArray {
        val iv = data.copyOfRange(MAGIC.size, MAGIC.size + IV_LEN)
        val ct = data.copyOfRange(MAGIC.size + IV_LEN, data.size)
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.DECRYPT_MODE, localKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
        return c.doFinal(ct)
    }

    // ---- Khóa từ mật khẩu (file xuất, Firebase) ----

    fun deriveKey(password: String, salt: ByteArray, iterations: Int = PBKDF2_ITER): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, 256)
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    /** Trả về iv + ciphertext (đã gồm thẻ xác thực GCM). Sai khóa thì giải mã ném lỗi. */
    fun encrypt(key: ByteArray, plain: ByteArray): ByteArray {
        val iv = randomBytes(IV_LEN)
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(GCM_TAG_BITS, iv))
        return iv + c.doFinal(plain)
    }

    fun decrypt(key: ByteArray, data: ByteArray): ByteArray {
        val iv = data.copyOfRange(0, IV_LEN)
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(GCM_TAG_BITS, iv))
        return c.doFinal(data, IV_LEN, data.size - IV_LEN)
    }

    fun b64(b: ByteArray): String = java.util.Base64.getEncoder().encodeToString(b)
    fun unb64(s: String): ByteArray = java.util.Base64.getDecoder().decode(s)
}
