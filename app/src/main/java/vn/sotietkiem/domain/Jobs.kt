package vn.sotietkiem.domain

import vn.sotietkiem.data.EntryKind
import vn.sotietkiem.data.Job
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.roundToLong

/** Các phép tính cho công việc tính theo giờ. */
object JobCalc {
    /** Tổng giờ đã làm (cộng các lần thêm, trừ các lần bớt). */
    fun workedHours(j: Job): Double = j.entries.filter { it.kind == EntryKind.WORK }.sumOf { it.hours }

    fun paidHours(j: Job): Double = j.entries.filter { it.kind == EntryKind.PAYMENT }.sumOf { it.hours }

    fun paidAmount(j: Job): Long = j.entries.filter { it.kind == EntryKind.PAYMENT }.sumOf { it.amount }

    /** Số giờ hiện chưa thanh toán (không âm). */
    fun unpaidHours(j: Job): Double = round2((workedHours(j) - paidHours(j)).coerceAtLeast(0.0))

    /** Số tiền chưa thanh toán, theo giá mỗi giờ hiện tại. */
    fun unpaidAmount(j: Job): Long = amountFor(j.rate, unpaidHours(j))

    fun amountFor(rate: Long, hours: Double): Long = (rate * hours).roundToLong()

    /** Số giờ làm trong tháng [ym] (theo ngày ghi nhận của các lần thêm/bớt giờ). */
    fun monthHours(j: Job, ym: YearMonth): Double = round2(
        j.entries.filter { it.kind == EntryKind.WORK && runCatching { YearMonth.from(LocalDate.parse(it.date)) == ym }.getOrDefault(false) }
            .sumOf { it.hours }
    )

    /** Tiền làm ra trong tháng theo giá hiện tại. */
    fun monthAmount(j: Job, ym: YearMonth): Long = amountFor(j.rate, monthHours(j, ym).coerceAtLeast(0.0))

    /** Phần trăm mục tiêu tháng (có thể vượt 100). Null nếu chưa đặt mục tiêu. */
    fun monthPercent(j: Job, ym: YearMonth): Int? =
        if (j.monthlyTarget <= 0) null else ((monthHours(j, ym) / j.monthlyTarget) * 100).coerceAtLeast(0.0).toInt()

    /**
     * Tiến độ "đúng nhịp" trong tháng đang chạy: tới hôm nay lẽ ra cần bao nhiêu giờ nếu chia đều mục tiêu
     * theo số ngày. Tháng đã qua trả về cả mục tiêu, tháng chưa tới trả về 0.
     */
    fun expectedByToday(j: Job, ym: YearMonth, today: LocalDate = LocalDate.now()): Double {
        if (j.monthlyTarget <= 0) return 0.0
        val cur = YearMonth.from(today)
        return when {
            ym < cur -> j.monthlyTarget
            ym > cur -> 0.0
            else -> round2(j.monthlyTarget * today.dayOfMonth / ym.lengthOfMonth())
        }
    }

    fun startMonth(j: Job): YearMonth = runCatching { YearMonth.from(LocalDate.parse(j.startDate)) }.getOrDefault(YearMonth.now())

    private fun round2(d: Double) = Math.round(d * 100) / 100.0
}
