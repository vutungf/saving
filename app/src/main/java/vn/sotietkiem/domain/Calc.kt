package vn.sotietkiem.domain

import vn.sotietkiem.data.Account
import vn.sotietkiem.data.InterestPayout
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.roundToLong

enum class Status(val label: String) {
    ACTIVE("Đang gửi"),
    SOON("Sắp đáo hạn"),
    MATURED("Đã đáo hạn"),
    CLOSED("Đã tất toán"),
}

/** Các phép tính theo mục 1 của spec. Lãi đơn, 365 ngày/năm. */
object Calc {
    const val SOON_DAYS = 30

    /** Ngày gửi + kỳ hạn. LocalDate.plusMonths tự lấy ngày cuối tháng nếu tháng đích không có ngày tương ứng. */
    fun maturityDate(start: LocalDate, termMonths: Int): LocalDate = start.plusMonths(termMonths.toLong())

    fun days(start: LocalDate, maturity: LocalDate): Long = ChronoUnit.DAYS.between(start, maturity)

    fun interest(amount: Long, ratePercent: Double, days: Long): Long =
        (amount.toDouble() * ratePercent / 100.0 * days / 365.0).roundToLong()

    fun start(a: Account): LocalDate = LocalDate.parse(a.startDate)
    fun maturity(a: Account): LocalDate = maturityDate(start(a), a.termMonths)
    fun days(a: Account): Long = days(start(a), maturity(a))
    fun interest(a: Account): Long = interest(a.amount, a.interestRate, days(a))
    fun total(a: Account): Long = a.amount + interest(a)

    /** Lãi mỗi tháng, chỉ có ý nghĩa với hình thức lĩnh lãi hàng tháng. */
    fun monthlyInterest(a: Account): Long? =
        if (a.interestPayout == InterestPayout.MONTHLY) (interest(a).toDouble() / a.termMonths).roundToLong() else null

    fun daysLeft(a: Account, today: LocalDate = LocalDate.now()): Long = ChronoUnit.DAYS.between(today, maturity(a))

    fun status(a: Account, today: LocalDate = LocalDate.now()): Status {
        if (a.closed) return Status.CLOSED
        val left = daysLeft(a, today)
        return when {
            left <= 0 -> Status.MATURED
            left <= SOON_DAYS -> Status.SOON
            else -> Status.ACTIVE
        }
    }

    /** Tiến độ từ 0 đến 1. */
    fun progress(a: Account, nowMillis: Long = System.currentTimeMillis()): Float {
        val s = startMillis(a)
        val e = maturityMillis(a)
        if (e <= s) return 1f
        return ((nowMillis - s).toDouble() / (e - s)).coerceIn(0.0, 1.0).toFloat()
    }

    /** Lãi tích lũy đến thời điểm [nowMillis], tính theo từng giây. Trả về Double để hiển thị con số chạy. */
    fun accrued(a: Account, nowMillis: Long = System.currentTimeMillis()): Double {
        if (a.closed) return 0.0
        val s = startMillis(a)
        val e = maturityMillis(a)
        if (nowMillis <= s) return 0.0
        val elapsed = (minOf(nowMillis, e) - s) / 1000.0
        return a.amount.toDouble() * a.interestRate / 100.0 * elapsed / (365.0 * 86400.0)
    }

    /** Tiền lãi sinh ra mỗi ngày của sổ đang chạy (chưa đáo hạn, chưa tất toán). */
    fun dailyInterest(a: Account, today: LocalDate = LocalDate.now()): Double {
        val st = status(a, today)
        if (st == Status.CLOSED || st == Status.MATURED) return 0.0
        if (today.isBefore(start(a))) return 0.0
        return a.amount.toDouble() * a.interestRate / 100.0 / 365.0
    }

    private val zone: ZoneId get() = ZoneId.systemDefault()
    private fun startMillis(a: Account) = ZonedDateTime.of(start(a).atStartOfDay(), zone).toInstant().toEpochMilli()
    private fun maturityMillis(a: Account) = ZonedDateTime.of(maturity(a).atStartOfDay(), zone).toInstant().toEpochMilli()
}

/** Số liệu cho màn hình Tổng quan (mục 3.1). */
data class Summary(
    val openAccounts: List<Account>,
    val principal: Long,
    val expectedInterest: Long,
    val totalAtMaturity: Long,
    val weightedRate: Double,
    val runningCount: Int,
    val dailyInterest: Double,
)

fun summarize(accounts: List<Account>, today: LocalDate = LocalDate.now()): Summary {
    val open = accounts.filter { !it.closed }
    val principal = open.sumOf { it.amount }
    val interest = open.sumOf { Calc.interest(it) }
    val rate = if (principal > 0) open.sumOf { it.amount.toDouble() * it.interestRate } / principal else 0.0
    return Summary(
        openAccounts = open,
        principal = principal,
        expectedInterest = interest,
        totalAtMaturity = principal + interest,
        weightedRate = rate,
        runningCount = open.count { Calc.status(it, today) != Status.MATURED },
        dailyInterest = open.sumOf { Calc.dailyInterest(it, today) },
    )
}

data class Slice(val label: String, val bankCode: String?, val amount: Long)

/** Phân bổ theo ngân hàng; quá 7 ngân hàng thì gộp phần còn lại thành "Khác". */
fun bankAllocation(accounts: List<Account>, maxSlices: Int = 7): List<Slice> {
    val groups = accounts.filter { !it.closed }
        .groupBy { if (it.bankCode == "OTHER") "OTHER:" + it.bankName.trim().lowercase() else it.bankCode }
        .map { (_, list) -> Slice(list.first().bankName, list.first().bankCode, list.sumOf { it.amount }) }
        .sortedByDescending { it.amount }
    if (groups.size <= maxSlices) return groups
    val head = groups.take(maxSlices - 1)
    val rest = groups.drop(maxSlices - 1)
    return head + Slice("Khác (${rest.size} ngân hàng)", null, rest.sumOf { it.amount })
}

data class MonthBucket(val year: Int, val month: Int, val principal: Long, val interest: Long) {
    val total get() = principal + interest
}

/** Tiền về theo tháng cho 12 tháng tới; sổ đã đáo hạn chưa tất toán tính vào tháng hiện tại. */
fun cashflow12(accounts: List<Account>, today: LocalDate = LocalDate.now()): List<MonthBucket> {
    val first = today.withDayOfMonth(1)
    val months = (0 until 12).map { first.plusMonths(it.toLong()) }
    val sums = Array(12) { longArrayOf(0, 0) }
    for (a in accounts) {
        if (a.closed) continue
        val m = Calc.maturity(a)
        val idx = if (m.isBefore(first)) 0 else ChronoUnit.MONTHS.between(first, m.withDayOfMonth(1)).toInt()
        if (idx in 0 until 12) {
            sums[idx][0] += a.amount
            sums[idx][1] += Calc.interest(a)
        }
    }
    return months.mapIndexed { i, d -> MonthBucket(d.year, d.monthValue, sums[i][0], sums[i][1]) }
}
