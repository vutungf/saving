package vn.sotietkiem.data

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import vn.sotietkiem.domain.Calc
import vn.sotietkiem.util.Crypto
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * Lưu sổ, người sở hữu và danh sách đã xóa vào các file trong bộ nhớ riêng của app.
 * Các file được mã hóa AES-256-GCM bằng khóa trong Android Keystore. File cũ chưa mã hóa
 * vẫn đọc được và được mã hóa lại ở lần lưu tiếp theo.
 */
class Repository(context: Context) {
    private val file = File(context.filesDir, "stk_v1.json")
    private val ownersFile = File(context.filesDir, "stk_owners.json")
    private val deletedFile = File(context.filesDir, "stk_deleted.json")
    private val prefs = context.getSharedPreferences("stk_prefs", Context.MODE_PRIVATE)

    /** Đọc file (tự giải mã nếu đã mã hóa). File không giải mã được thì đổi tên để giữ lại và trả về null. */
    private fun readText(f: File): String? {
        if (!f.exists()) return null
        val bytes = f.readBytes()
        if (!Crypto.isLocalEncrypted(bytes)) return bytes.decodeToString()
        return try {
            Crypto.decryptLocal(bytes).decodeToString()
        } catch (e: Exception) {
            // Khóa Keystore không còn (ví dụ dữ liệu được khôi phục sang máy khác)
            f.renameTo(File(f.parentFile, f.name + ".unreadable-" + System.currentTimeMillis()))
            null
        }
    }

    @Synchronized
    private fun writeText(f: File, text: String) {
        val tmp = File(f.parentFile, f.name + ".tmp")
        tmp.writeBytes(Crypto.encryptLocal(text.toByteArray()))
        if (!tmp.renameTo(f)) {
            f.writeBytes(tmp.readBytes()); tmp.delete()
        }
    }

    fun load(): List<Account> = runCatching {
        readText(file)?.let { Codec.json.decodeFromString<List<Account>>(it) } ?: emptyList()
    }.getOrDefault(emptyList())

    fun save(list: List<Account>) = writeText(file, Codec.json.encodeToString(list))

    fun loadOwners(): List<Owner> = runCatching {
        readText(ownersFile)?.let { Codec.json.decodeFromString<List<Owner>>(it).map { o -> o.normalized() } } ?: emptyList()
    }.getOrDefault(emptyList())

    fun saveOwners(list: List<Owner>) = writeText(ownersFile, Codec.json.encodeToString(list))

    /** Mã sổ / người sở hữu đã xóa → thời điểm xóa (ISO). Dùng khi đồng bộ để máy khác cũng xóa theo. */
    fun loadDeleted(): Map<String, String> = runCatching {
        readText(deletedFile)?.let { Codec.json.decodeFromString<Map<String, String>>(it) } ?: emptyMap()
    }.getOrDefault(emptyMap())

    fun saveDeleted(map: Map<String, String>) = writeText(deletedFile, Codec.json.encodeToString(map))

    /** Lần đầu mở app: người dùng đã chọn cách lưu (trên máy / tài khoản Google) chưa. */
    var storageChosen: Boolean
        get() = prefs.getBoolean("storageChosen", false)
        set(v) = prefs.edit().putBoolean("storageChosen", v).apply()

    /** Thời điểm đồng bộ thành công gần nhất với Firebase (0 = chưa). */
    var lastSyncAt: Long
        get() = prefs.getLong("lastSyncAt", 0L)
        set(v) = prefs.edit().putLong("lastSyncAt", v).apply()

    var lastExportAt: Long
        get() = prefs.getLong("lastExportAt", 0L)
        set(v) = prefs.edit().putLong("lastExportAt", v).apply()

    /** Thời điểm dữ liệu thay đổi gần nhất; lớn hơn lastExportAt nghĩa là có thay đổi chưa xuất file. */
    var lastChangeAt: Long
        get() = prefs.getLong("lastChangeAt", 0L)
        set(v) = prefs.edit().putLong("lastChangeAt", v).apply()

    var remindersEnabled: Boolean
        get() = prefs.getBoolean("reminders", true)
        set(v) = prefs.edit().putBoolean("reminders", v).apply()

    /** Các khóa "id|loại" đã gửi thông báo, để không nhắc trùng. */
    var notified: Set<String>
        get() = prefs.getStringSet("notified", emptySet()) ?: emptySet()
        set(v) = prefs.edit().putStringSet("notified", v).apply()
}

data class ImportResult(val accounts: List<Account>, val skipped: Int, val owners: List<Owner> = emptyList())

object Codec {
    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    fun export(list: List<Account>, owners: List<Owner> = emptyList()): String =
        json.encodeToString(BackupFile(exportedAt = Instant.now().toString(), owners = owners, accounts = list))

    fun fileName(today: LocalDate = LocalDate.now()) = "so-tiet-kiem-$today.json"

    /**
     * Bọc nội dung JSON trong một "phong bì" mã hóa bằng mật khẩu:
     * {"app":"so-tiet-kiem","encrypted":true,"kdf":"PBKDF2WithHmacSHA256","iter":..,"salt":..,"data":..}
     * data = base64(iv + ciphertext AES-256-GCM).
     */
    fun encryptExport(plainJson: String, password: String): String {
        val salt = Crypto.randomBytes(16)
        val key = Crypto.deriveKey(password, salt)
        val data = Crypto.encrypt(key, plainJson.toByteArray())
        val o = buildJsonObject {
            put("app", "so-tiet-kiem")
            put("encrypted", true)
            put("version", 1)
            put("cipher", "AES-256-GCM")
            put("kdf", "PBKDF2WithHmacSHA256")
            put("iter", Crypto.PBKDF2_ITER)
            put("salt", Crypto.b64(salt))
            put("data", Crypto.b64(data))
        }
        return json.encodeToString(JsonObject.serializer(), o)
    }

    fun isEncryptedExport(text: String): Boolean = runCatching {
        (json.parseToJsonElement(text.trim()) as? JsonObject)?.get("encrypted")?.let { (it as? JsonPrimitive)?.booleanOrNull } == true
    }.getOrDefault(false)

    /** Giải mã file xuất. Sai mật khẩu thì ném IllegalArgumentException. */
    fun decryptExport(text: String, password: String): String {
        val o = json.parseToJsonElement(text.trim()) as JsonObject
        val salt = Crypto.unb64(o.str("salt") ?: throw IllegalArgumentException("File mã hóa bị hỏng."))
        val data = Crypto.unb64(o.str("data") ?: throw IllegalArgumentException("File mã hóa bị hỏng."))
        val iter = o.num("iter")?.toInt() ?: Crypto.PBKDF2_ITER
        return try {
            Crypto.decrypt(Crypto.deriveKey(password, salt, iter), data).decodeToString()
        } catch (e: Exception) {
            throw IllegalArgumentException("Sai mật khẩu hoặc file bị hỏng.")
        }
    }

    /**
     * Nhận object có mảng "accounts", object có mảng "books", hoặc một mảng sổ trực tiếp.
     * Sổ thiếu hoặc sai bị bỏ qua. Ném IllegalArgumentException khi nội dung không phải JSON hợp lệ.
     */
    fun parseImport(text: String): ImportResult {
        val root = try { json.parseToJsonElement(text.trim()) } catch (e: Exception) {
            throw IllegalArgumentException("Nội dung không phải JSON hợp lệ.")
        }
        val arr: JsonArray = when (root) {
            is JsonArray -> root
            is JsonObject -> (root["accounts"] as? JsonArray) ?: (root["books"] as? JsonArray)
                ?: throw IllegalArgumentException("Không tìm thấy mảng \"accounts\" hoặc \"books\".")
            else -> throw IllegalArgumentException("Định dạng file không được hỗ trợ.")
        }
        val out = mutableListOf<Account>()
        var skipped = 0
        for (el in arr) {
            val a = (el as? JsonObject)?.let(::toAccount)
            if (a == null) skipped++ else out.add(a)
        }
        val owners = ((root as? JsonObject)?.get("owners") as? JsonArray).orEmpty().mapNotNull { el ->
            val o = el as? JsonObject ?: return@mapNotNull null
            val id = o.str("id")?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val name = o.str("name")?.trim()?.take(40)?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            val goal = o.num("goalAmount")?.toLong()?.takeIf { it > 0 }
            val goalDate = o.str("goalDate")?.let { runCatching { LocalDate.parse(it.take(10)).toString() }.getOrNull() }
            val goals = (o["goals"] as? JsonArray).orEmpty().mapNotNull { ge ->
                val g = ge as? JsonObject ?: return@mapNotNull null
                val amt = g.num("amount")?.toLong()?.takeIf { it > 0 } ?: return@mapNotNull null
                Goal(
                    id = g.str("id")?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString(),
                    name = g.str("name")?.trim()?.take(60)?.takeIf { it.isNotEmpty() } ?: "Mục tiêu",
                    amount = amt,
                    date = g.str("date")?.let { runCatching { LocalDate.parse(it.take(10)).toString() }.getOrNull() },
                )
            }.distinctBy { it.id }
            Owner(id, name, goal, if (goal != null) goalDate else null, goals).normalized()
        }.distinctBy { it.id }
        return ImportResult(out, skipped, owners)
    }

    private fun JsonObject.str(k: String): String? {
        val p = this[k] as? JsonPrimitive ?: return null
        return if (p is JsonNull) null else p.content
    }

    private fun JsonObject.num(k: String): Double? {
        val p = this[k] as? JsonPrimitive ?: return null
        return p.doubleOrNull ?: p.content.replace(',', '.').toDoubleOrNull()
    }

    private fun toAccount(o: JsonObject): Account? {
        val name = o.str("name")?.trim()?.take(80)?.takeIf { it.isNotEmpty() } ?: return null
        val amount = o.num("amount")?.toLong()?.takeIf { it > 0 } ?: return null
        val start = o.str("startDate")?.let { runCatching { LocalDate.parse(it.take(10)) }.getOrNull() } ?: return null
        val term = o.num("termMonths")?.toInt()?.takeIf { it in 1..600 } ?: return null
        val rate = o.num("interestRate")?.takeIf { it in 0.0..100.0 } ?: return null
        val payout = when (o.str("interestPayout")) {
            "monthly" -> InterestPayout.MONTHLY
            "upfront" -> InterestPayout.UPFRONT
            else -> InterestPayout.MATURITY
        }
        val rawCode = o.str("bankCode")?.trim().orEmpty()
        val known = Banks.find(rawCode)
        val bankName = o.str("bankName")?.trim()?.takeIf { it.isNotEmpty() } ?: known?.name ?: "Ngân hàng khác"
        val now = Instant.now().toString()
        val closed = (o["closed"] as? JsonPrimitive)?.booleanOrNull ?: false
        return Account(
            id = o.str("id")?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString(),
            name = name,
            amount = amount,
            startDate = start.toString(),
            termMonths = term,
            maturityDate = Calc.maturityDate(start, term).toString(), // luôn tính lại
            interestRate = rate,
            interestPayout = payout,
            bankCode = known?.code ?: Banks.OTHER,
            bankName = known?.name ?: bankName,
            note = o.str("note")?.take(500).orEmpty(),
            closed = closed,
            closedAt = if (closed) o.str("closedAt")?.take(10) ?: LocalDate.now().toString() else null,
            createdAt = o.str("createdAt") ?: now,
            updatedAt = o.str("updatedAt") ?: now,
            owners = parseShares(o, amount),
        )
    }

    /** Phần tiền từng người; chỉ giữ khi hợp lệ (mỗi phần > 0, không trùng người, tổng bằng số tiền gửi). */
    private fun parseShares(o: JsonObject, amount: Long): List<OwnerShare> {
        val arr = o["owners"] as? JsonArray ?: return emptyList()
        val shares = arr.mapNotNull { el ->
            val s = el as? JsonObject ?: return@mapNotNull null
            val id = s.str("ownerId")?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val amt = s.num("amount")?.toLong()?.takeIf { it > 0 } ?: return@mapNotNull null
            OwnerShare(id, amt)
        }
        return if (shares.isNotEmpty() && shares.distinctBy { it.ownerId }.size == shares.size && shares.sumOf { it.amount } == amount)
            shares else emptyList()
    }
}
