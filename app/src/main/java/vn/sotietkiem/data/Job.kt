package vn.sotietkiem.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Loại dòng trong sổ giờ của một công việc. */
@Serializable
enum class EntryKind {
    /** Thêm giờ (hours > 0) hoặc bớt giờ (hours < 0). */
    @SerialName("work") WORK,
    /** Đã thanh toán [HourEntry.hours] giờ, nhận [HourEntry.amount] đồng. */
    @SerialName("payment") PAYMENT,
}

@Serializable
data class HourEntry(
    val id: String,
    val kind: EntryKind = EntryKind.WORK,
    /** Ngày ghi nhận, YYYY-MM-DD. */
    val date: String,
    /** Số giờ. WORK: dương là thêm, âm là bớt. PAYMENT: số giờ được thanh toán (dương). */
    val hours: Double,
    /** Số tiền đã nhận (chỉ có ở PAYMENT), theo giá mỗi giờ tại lúc thanh toán. */
    val amount: Long = 0,
    val note: String = "",
    val createdAt: String,
)

/**
 * Một công việc tính tiền theo giờ, ví dụ dạy kèm, làm thêm.
 * Giờ chưa thanh toán = tổng giờ đã làm − tổng giờ đã thanh toán, tính tiền theo [rate] hiện tại.
 */
@Serializable
data class Job(
    val id: String,
    val name: String,
    /** Số tiền thanh toán mỗi giờ (VND). */
    val rate: Long,
    /** Ngày bắt đầu, YYYY-MM-DD. */
    val startDate: String,
    /** Mục tiêu số giờ mỗi tháng (0 = không đặt). */
    val monthlyTarget: Double = 0.0,
    val note: String = "",
    val entries: List<HourEntry> = emptyList(),
    val closed: Boolean = false,
    val createdAt: String,
    val updatedAt: String,
    /** Người sở hữu (người làm công việc này); null = chưa gán. */
    val ownerId: String? = null,
)
