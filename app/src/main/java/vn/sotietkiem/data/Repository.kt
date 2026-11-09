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
import vn.sotietkiem.domain.Calc
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * Lưu toàn bộ sổ vào một file JSON trong bộ nhớ riêng của app (tương đương khóa localStorage "stk:v1"
 * ở bản web). Dữ liệu không rời khỏi máy, trừ khi người dùng tự xuất file.
 */
class Repository(context: Context) {
    private val file = File(context.filesDir, "stk_v1.json")
    private val prefs = context.getSharedPreferences("stk_prefs", Context.MODE_PRIVATE)

    fun load(): List<Account> = runCatching {
        if (!file.exists()) emptyList() else Codec.json.decodeFromString<List<Account>>(file.readText())
    }.getOrDefault(emptyList())

    @Synchronized
    fun save(list: List<Account>) {
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(Codec.json.encodeToString(list))
        if (!tmp.renameTo(file)) {
            file.writeText(tmp.readText()); tmp.delete()
        }
    }

    var lastExportAt: Long
        get() = prefs.getLong("lastExportAt", 0L)
        set(v) = prefs.edit().putLong("lastExportAt", v).apply()

    var remindersEnabled: Boolean
        get() = prefs.getBoolean("reminders", true)
        set(v) = prefs.edit().putBoolean("reminders", v).apply()

    /** Các khóa "id|loại" đã gửi thông báo, để không nhắc trùng. */
    var notified: Set<String>
        get() = prefs.getStringSet("notified", emptySet()) ?: emptySet()
        set(v) = prefs.edit().putStringSet("notified", v).apply()
}

data class ImportResult(val accounts: List<Account>, val skipped: Int)

object Codec {
    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    fun export(list: List<Account>): String =
        json.encodeToString(BackupFile(exportedAt = Instant.now().toString(), accounts = list))

    fun fileName(today: LocalDate = LocalDate.now()) = "so-tiet-kiem-$today.json"

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
        return ImportResult(out, skipped)
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
        )
    }
}
