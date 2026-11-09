package vn.sotietkiem.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class InterestPayout(val label: String) {
    @SerialName("maturity") MATURITY("Cuối kỳ"),
    @SerialName("monthly") MONTHLY("Hàng tháng"),
    @SerialName("upfront") UPFRONT("Đầu kỳ"),
}

/** Một sổ tiết kiệm. Các trường và tên khóa khớp định dạng file JSON ở mục 4 của spec. */
@Serializable
data class Account(
    val id: String,
    val name: String,
    val amount: Long,
    val startDate: String,          // YYYY-MM-DD
    val termMonths: Int,
    val maturityDate: String,       // YYYY-MM-DD, luôn được tính lại khi lưu/nhập
    val interestRate: Double,       // %/năm
    val interestPayout: InterestPayout = InterestPayout.MATURITY,
    val bankCode: String,
    val bankName: String,
    val note: String = "",
    val closed: Boolean = false,
    val closedAt: String? = null,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class BackupFile(
    val app: String = "so-tiet-kiem",
    val version: Int = 1,
    val currency: String = "VND",
    val exportedAt: String,
    val accounts: List<Account>,
)
