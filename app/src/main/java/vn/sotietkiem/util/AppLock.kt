package vn.sotietkiem.util

import android.content.Context
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Cài đặt khóa ứng dụng, lưu trong SharedPreferences riêng của app.
 * Mã PIN không lưu dạng rõ: chỉ lưu SHA-256(muối + PIN) với muối ngẫu nhiên.
 */
class AppLock(context: Context) {
    private val prefs = context.getSharedPreferences("stk_lock", Context.MODE_PRIVATE)

    val enabled: Boolean get() = prefs.getString("pinHash", null) != null

    /** Kiểu khóa: [TYPE_PIN] (mã số) hoặc [TYPE_PATTERN] (hình vẽ nối điểm). */
    val type: String get() = prefs.getString("type", TYPE_PIN) ?: TYPE_PIN

    /** Đặt mã khóa mới. Hình vẽ được lưu dưới dạng chuỗi chỉ số điểm, ví dụ "0-1-2-5-8". */
    fun setSecret(type: String, secret: String) {
        setPin(secret)
        prefs.edit().putString("type", type).apply()
    }

    var biometric: Boolean
        get() = prefs.getBoolean("biometric", false)
        set(v) = prefs.edit().putBoolean("biometric", v).apply()

    /** Tự khóa sau khi rời app bao nhiêu giây (0 = ngay lập tức). */
    var delaySeconds: Int
        get() = prefs.getInt("delay", 0)
        set(v) = prefs.edit().putInt("delay", v).apply()

    fun setPin(pin: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        prefs.edit()
            .putString("pinSalt", Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString("pinHash", hash(salt, pin))
            .putInt("fails", 0).putLong("lockedUntil", 0)
            .apply()
    }

    fun check(pin: String): Boolean {
        val saltStr = prefs.getString("pinSalt", null) ?: return false
        val expected = prefs.getString("pinHash", null) ?: return false
        val ok = MessageDigest.isEqual(hash(Base64.decode(saltStr, Base64.NO_WRAP), pin).toByteArray(), expected.toByteArray())
        if (ok) {
            prefs.edit().putInt("fails", 0).putLong("lockedUntil", 0).apply()
        } else {
            // Sai 5 lần liên tiếp thì phải chờ 30 giây
            val fails = prefs.getInt("fails", 0) + 1
            val e = prefs.edit().putInt("fails", fails)
            if (fails % 5 == 0) e.putLong("lockedUntil", System.currentTimeMillis() + 30_000)
            e.apply()
        }
        return ok
    }

    /** Thời điểm (ms) được thử lại nếu đang bị chặn do nhập sai nhiều lần; 0 nếu không bị chặn. */
    val blockedUntil: Long get() = prefs.getLong("lockedUntil", 0).takeIf { it > System.currentTimeMillis() } ?: 0L

    fun disable() = prefs.edit().clear().apply()

    private fun hash(salt: ByteArray, pin: String): String {
        val d = MessageDigest.getInstance("SHA-256").digest(salt + pin.toByteArray())
        return d.joinToString("") { "%02x".format(it) }
    }

    companion object {
        const val TYPE_PIN = "pin"
        const val TYPE_PATTERN = "pattern"
        /** Hình vẽ phải nối ít nhất từng này điểm. */
        const val PATTERN_MIN = 4

        fun encodePattern(dots: List<Int>) = dots.joinToString("-")

        /** Trạng thái trong tiến trình: đã mở khóa chưa, rời app lúc nào. Mất khi app bị tắt hẳn, nên lúc mở lại sẽ khóa. */
        @Volatile var unlocked = false
        @Volatile var leftAt = 0L
        /** Bỏ qua lần tự khóa kế tiếp, dùng khi mở trình chọn file hoặc chia sẻ (app tạm rời màn hình). */
        @Volatile var skipNext = false
    }
}
