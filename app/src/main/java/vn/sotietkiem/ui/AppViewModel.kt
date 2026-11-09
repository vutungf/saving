package vn.sotietkiem.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import vn.sotietkiem.data.Account
import vn.sotietkiem.data.Banks
import vn.sotietkiem.data.InterestPayout
import vn.sotietkiem.data.Repository
import vn.sotietkiem.domain.Calc
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class AppViewModel(app: Application) : AndroidViewModel(app) {
    val repo = Repository(app)

    private val _accounts = MutableStateFlow(repo.load())
    val accounts: StateFlow<List<Account>> = _accounts.asStateFlow()

    private val _lastExport = MutableStateFlow(repo.lastExportAt)
    val lastExport: StateFlow<Long> = _lastExport.asStateFlow()

    private fun commit(list: List<Account>) {
        _accounts.value = list
        repo.save(list)
    }

    fun get(id: String): Account? = _accounts.value.firstOrNull { it.id == id }

    /** Thêm hoặc sửa. Nếu [renewFromId] khác null thì sổ cũ chuyển sang Đã tất toán. */
    fun upsert(a: Account, renewFromId: String? = null) {
        val now = Instant.now().toString()
        val fixed = a.copy(
            maturityDate = Calc.maturityDate(LocalDate.parse(a.startDate), a.termMonths).toString(),
            updatedAt = now,
        )
        var list = _accounts.value
        list = if (list.any { it.id == fixed.id }) list.map { if (it.id == fixed.id) fixed else it } else list + fixed
        if (renewFromId != null) {
            list = list.map {
                if (it.id == renewFromId) it.copy(closed = true, closedAt = Calc.maturity(it).toString(), updatedAt = now) else it
            }
        }
        commit(list)
    }

    fun delete(id: String) = commit(_accounts.value.filterNot { it.id == id })

    fun close(id: String) = commit(_accounts.value.map {
        if (it.id == id) it.copy(closed = true, closedAt = LocalDate.now().toString(), updatedAt = Instant.now().toString()) else it
    })

    fun reopen(id: String) = commit(_accounts.value.map {
        if (it.id == id) it.copy(closed = false, closedAt = null, updatedAt = Instant.now().toString()) else it
    })

    /** Gộp: sổ trùng ID bị bỏ qua. Trả về số sổ đã thêm. */
    fun importMerge(items: List<Account>): Int {
        val ids = _accounts.value.map { it.id }.toHashSet()
        val fresh = items.filter { ids.add(it.id) }
        commit(_accounts.value + fresh)
        return fresh.size
    }

    fun importReplace(items: List<Account>) = commit(items.distinctBy { it.id })

    fun clearAll() = commit(emptyList())

    fun markExported() {
        val t = System.currentTimeMillis()
        repo.lastExportAt = t
        _lastExport.value = t
    }

    fun loadSample() {
        val t = LocalDate.now()
        val now = Instant.now().toString()
        fun mk(name: String, code: String, amount: Long, start: LocalDate, term: Int, rate: Double, p: InterestPayout, note: String) =
            Account(
                id = UUID.randomUUID().toString(), name = name, amount = amount, startDate = start.toString(),
                termMonths = term, maturityDate = Calc.maturityDate(start, term).toString(), interestRate = rate,
                interestPayout = p, bankCode = code, bankName = Banks.find(code)?.name ?: code,
                note = "[Mẫu] $note", createdAt = now, updatedAt = now,
            )
        val sample = listOf(
            mk("Quỹ mua nhà", "VCB", 500_000_000, t.minusMonths(7), 12, 4.8, InterestPayout.MATURITY, "Sổ giấy, chi nhánh Bến Thành"),
            mk("Quỹ dự phòng", "TCB", 120_000_000, t.minusMonths(2).minusDays(20), 3, 4.3, InterestPayout.MATURITY, "Gửi online"),
            mk("Tiền học cho con", "BIDV", 200_000_000, t.minusMonths(6).minusDays(3), 6, 4.7, InterestPayout.MONTHLY, "Lĩnh lãi hàng tháng"),
            mk("Gửi thử Cake", "CAKE", 50_000_000, t.minusMonths(1).minusDays(5), 1, 4.1, InterestPayout.MATURITY, "Ngân hàng số"),
            mk("Quỹ hưu trí", "ACB", 300_000_000, t.minusMonths(3), 24, 5.6, InterestPayout.MATURITY, "Kỳ hạn dài"),
        )
        commit(_accounts.value + sample)
    }
}
