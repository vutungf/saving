package vn.sotietkiem.domain

import vn.sotietkiem.data.Account
import vn.sotietkiem.data.OwnerShare

/** Phạm vi thống kê: tất cả, một người sở hữu, hoặc các sổ chưa gán người sở hữu. */
object OwnerScope {
    const val ALL = "__all__"
    const val UNASSIGNED = "__none__"

    /**
     * Trả về danh sách sổ nhìn từ góc một người: mỗi sổ người đó có phần được thay bằng bản sao có
     * số tiền gốc = phần của người đó. Lãi đơn tỷ lệ thuận với gốc nên mọi phép tính khác giữ nguyên.
     */
    fun apply(accounts: List<Account>, scope: String): List<Account> = when (scope) {
        ALL -> accounts
        UNASSIGNED -> accounts.filter { it.owners.isEmpty() }
        else -> accounts.mapNotNull { a ->
            a.owners.firstOrNull { it.ownerId == scope }?.let { a.copy(amount = it.amount) }
        }
    }
}

/** Chia lại các phần theo tỷ lệ cũ khi số tiền gửi đổi (ví dụ tái tục gốc + lãi). Phần dư dồn vào người cuối. */
fun scaleShares(shares: List<OwnerShare>, oldAmount: Long, newAmount: Long): List<OwnerShare> {
    if (shares.isEmpty() || oldAmount <= 0) return emptyList()
    if (shares.size == 1) return listOf(shares[0].copy(amount = newAmount))
    val scaled = shares.dropLast(1).map { it.copy(amount = (it.amount.toBigInteger() * newAmount.toBigInteger() / oldAmount.toBigInteger()).toLong()) }
    val rest = newAmount - scaled.sumOf { it.amount }
    return scaled + shares.last().copy(amount = rest)
}

/** Chia đều [amount] cho [ownerIds]; phần dư (vài đồng) dồn vào người đầu. */
fun splitEven(ownerIds: List<String>, amount: Long): List<OwnerShare> {
    if (ownerIds.isEmpty()) return emptyList()
    val each = amount / ownerIds.size
    val extra = amount - each * ownerIds.size
    return ownerIds.mapIndexed { i, id -> OwnerShare(id, each + if (i == 0) extra else 0) }
}

/** Phân bổ tiền gốc đang gửi theo người sở hữu; sổ chưa gán gộp vào "Chưa gán". */
fun ownerAllocation(accounts: List<Account>, owners: List<vn.sotietkiem.data.Owner>): List<Slice> {
    val open = accounts.filter { !it.closed }
    val byOwner = owners.map { o ->
        Slice(o.name, o.id, open.sumOf { a -> a.owners.firstOrNull { it.ownerId == o.id }?.amount ?: 0L })
    }.filter { it.amount > 0 }
    val unassigned = open.filter { it.owners.isEmpty() }.sumOf { it.amount }
    return byOwner + if (unassigned > 0) listOf(Slice("Chưa gán", null, unassigned)) else emptyList()
}

/** Tiền gốc đang gửi của một người: cộng phần của người đó trong các sổ chưa tất toán. */
fun ownerPrincipal(accounts: List<Account>, ownerId: String): Long =
    accounts.filter { !it.closed }.sumOf { a -> a.owners.firstOrNull { it.ownerId == ownerId }?.amount ?: 0L }

/** Tiến độ mục tiêu (chỉ tính tiền gốc). */
data class GoalProgress(
    val goal: Long,
    val current: Long,
    val deadline: java.time.LocalDate?,
    val today: java.time.LocalDate,
) {
    val fraction: Float get() = if (goal <= 0) 0f else (current.toDouble() / goal).coerceIn(0.0, 1.0).toFloat()
    val percent: Int get() = if (goal <= 0) 0 else (current * 100 / goal).toInt()
    val missing: Long get() = (goal - current).coerceAtLeast(0)
    val reached: Boolean get() = current >= goal
    val daysLeft: Long? get() = deadline?.let { java.time.temporal.ChronoUnit.DAYS.between(today, it) }

    /** Số tiền cần gửi thêm mỗi tháng (làm tròn lên) để kịp hạn; null nếu không có hạn, đã đạt hoặc đã quá hạn. */
    val perMonth: Long?
        get() {
            val d = deadline ?: return null
            if (reached || !d.isAfter(today)) return null
            // Số tháng còn lại, tính cả tháng lẻ (12 tháng tròn = 12, 12 tháng 3 ngày = 13)
            var months = java.time.temporal.ChronoUnit.MONTHS.between(today, d)
            if (today.plusMonths(months).isBefore(d)) months++
            months = months.coerceAtLeast(1)
            return (missing + months - 1) / months
        }
}

fun goalOf(owner: vn.sotietkiem.data.Owner, accounts: List<Account>, today: java.time.LocalDate = java.time.LocalDate.now()): GoalProgress? {
    val g = owner.goalAmount?.takeIf { it > 0 } ?: return null
    val d = owner.goalDate?.let { runCatching { java.time.LocalDate.parse(it) }.getOrNull() }
    return GoalProgress(g, ownerPrincipal(accounts, owner.id), d, today)
}
