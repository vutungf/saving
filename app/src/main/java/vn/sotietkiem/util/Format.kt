package vn.sotietkiem.util

import java.text.Normalizer
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.roundToLong

object Fmt {
    private val dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    /** 150000000 -> "150.000.000" */
    fun group(n: Long): String {
        val s = abs(n).toString()
        val sb = StringBuilder()
        for ((i, c) in s.withIndex()) {
            if (i > 0 && (s.length - i) % 3 == 0) sb.append('.')
            sb.append(c)
        }
        return (if (n < 0) "-" else "") + sb
    }

    fun vnd(n: Long): String = group(n) + " ₫"
    fun vnd(n: Double): String = vnd(n.roundToLong())

    /** Rút gọn cho trục biểu đồ: 1,2 tỷ / 350 tr / 80 N */
    fun short(n: Long): String = when {
        n >= 1_000_000_000 -> trim(n / 1_000_000_000.0) + " tỷ"
        n >= 1_000_000 -> trim(n / 1_000_000.0) + " tr"
        n >= 1_000 -> trim(n / 1_000.0) + " N"
        else -> n.toString()
    }

    private fun trim(d: Double): String {
        val r = (d * 10).roundToLong() / 10.0
        return if (r == r.toLong().toDouble()) r.toLong().toString() else r.toString().replace('.', ',')
    }

    fun rate(r: Double): String {
        val s = "%.2f".format(java.util.Locale.US, r).trimEnd('0').trimEnd('.')
        return s.replace('.', ',') + "%"
    }

    fun date(d: LocalDate): String = d.format(dateFmt)
    fun date(iso: String): String = runCatching { date(LocalDate.parse(iso)) }.getOrDefault(iso)

    fun term(months: Int): String = "$months tháng"

    /** Chỉ giữ chữ số. "150.000.000 đ" -> 150000000 */
    fun parseAmount(s: String): Long? = s.filter { it.isDigit() }.takeIf { it.isNotEmpty() && it.length <= 15 }?.toLongOrNull()

    /** Nhận "5,2" hoặc "5.2". */
    fun parseRate(s: String): Double? = s.trim().replace(',', '.').takeIf { it.isNotEmpty() }?.toDoubleOrNull()

    /** Số giờ: 12.5 -> "12,5", 3.0 -> "3", 1.25 -> "1,25". */
    fun hours(h: Double): String {
        val r = Math.round(h * 100) / 100.0
        val txt = if (r == r.toLong().toDouble()) r.toLong().toString() else r.toString()
        return txt.replace('.', ',')
    }

    /** Nhận "1,5" hoặc "1.5"; null nếu không hợp lệ. */
    fun parseHours(s: String): Double? = s.trim().replace(',', '.').takeIf { it.isNotEmpty() }?.toDoubleOrNull()

    /** Bỏ dấu tiếng Việt, chữ thường, để tìm kiếm không dấu. */
    fun fold(s: String): String {
        val n = Normalizer.normalize(s, Normalizer.Form.NFD)
        return n.replace("\\p{Mn}+".toRegex(), "").replace('đ', 'd').replace('Đ', 'D').lowercase()
    }
}

/** Đọc số tiền bằng chữ tiếng Việt. 150000000 -> "Một trăm năm mươi triệu đồng" */
object VnWords {
    private val digits = arrayOf("không", "một", "hai", "ba", "bốn", "năm", "sáu", "bảy", "tám", "chín")
    private val units = arrayOf("", " nghìn", " triệu", " tỷ", " nghìn tỷ", " triệu tỷ")

    fun read(n: Long): String {
        if (n == 0L) return "Không đồng"
        val groups = mutableListOf<Int>()
        var x = abs(n)
        while (x > 0) { groups.add((x % 1000).toInt()); x /= 1000 }
        val parts = mutableListOf<String>()
        for (i in groups.indices.reversed()) {
            val g = groups[i]
            if (g == 0) continue
            val full = i != groups.lastIndex // nhóm không đứng đầu thì đọc đủ "không trăm", "lẻ"
            val unit = units[i]
            parts.add(readTriple(g, full) + unit)
        }
        val s = parts.joinToString(" ").trim().replace("\\s+".toRegex(), " ")
        return s.replaceFirstChar { it.uppercase() } + " đồng"
    }

    private fun readTriple(n: Int, full: Boolean): String {
        val h = n / 100
        val t = (n / 10) % 10
        val u = n % 10
        val sb = mutableListOf<String>()
        if (h > 0 || full) sb.add(digits[h] + " trăm")
        when {
            t == 0 && u == 0 -> {}
            t == 0 -> { if (h > 0 || full) sb.add("lẻ"); sb.add(digits[u]) }
            t == 1 -> {
                sb.add("mười")
                if (u == 5) sb.add("lăm") else if (u > 0) sb.add(digits[u])
            }
            else -> {
                sb.add(digits[t] + " mươi")
                when (u) {
                    0 -> {}
                    1 -> sb.add("mốt")
                    4 -> sb.add("tư")
                    5 -> sb.add("lăm")
                    else -> sb.add(digits[u])
                }
            }
        }
        return sb.joinToString(" ")
    }
}
