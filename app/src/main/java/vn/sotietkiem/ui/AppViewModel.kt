package vn.sotietkiem.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import vn.sotietkiem.data.Account
import vn.sotietkiem.data.Banks
import vn.sotietkiem.data.InterestPayout
import vn.sotietkiem.data.Owner
import vn.sotietkiem.data.Goal
import vn.sotietkiem.data.OwnerShare
import vn.sotietkiem.data.Job
import vn.sotietkiem.data.HourEntry
import vn.sotietkiem.data.EntryKind
import vn.sotietkiem.data.Repository
import vn.sotietkiem.domain.Calc
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class AppViewModel(app: Application) : AndroidViewModel(app) {
    val repo = Repository(app)

    private val _accounts = MutableStateFlow(repo.load())
    val accounts: StateFlow<List<Account>> = _accounts.asStateFlow()

    private val _owners = MutableStateFlow(repo.loadOwners())
    val owners: StateFlow<List<Owner>> = _owners.asStateFlow()

    private val _jobs = MutableStateFlow(repo.loadJobs())
    val jobs: StateFlow<List<Job>> = _jobs.asStateFlow()

    private val _lastExport = MutableStateFlow(repo.lastExportAt)
    val lastExport: StateFlow<Long> = _lastExport.asStateFlow()

    private val _lastChange = MutableStateFlow(repo.lastChangeAt)
    val lastChange: StateFlow<Long> = _lastChange.asStateFlow()

    /** Ghi nhận mỗi lần dữ liệu đổi, để nhắc người dùng xuất file JSON. */
    private fun touch() {
        val t = System.currentTimeMillis()
        repo.lastChangeAt = t
        _lastChange.value = t
    }

    /** Mã đã xóa → thời điểm xóa; gửi kèm khi đồng bộ để máy khác xóa theo. */
    private var deleted: Map<String, String> = repo.loadDeleted()

    private val _lastSync = MutableStateFlow(repo.lastSyncAt)
    val lastSync: StateFlow<Long> = _lastSync.asStateFlow()

    /** Gọi sau mỗi thay đổi do người dùng (để đồng bộ lên mây nếu đang bật). */
    var onChanged: (() -> Unit)? = null

    private fun markDeleted(ids: Collection<String>) {
        if (ids.isEmpty()) return
        val now = Instant.now().toString()
        deleted = deleted + ids.associateWith { now }
        repo.saveDeleted(deleted)
    }

    private fun commit(list: List<Account>) {
        val newIds = list.map { it.id }.toHashSet()
        markDeleted(_accounts.value.map { it.id }.filterNot { it in newIds })
        _accounts.value = list
        repo.save(list)
        touch()
        onChanged?.invoke()
    }

    /** Dữ liệu để đồng bộ. */
    fun syncData() = vn.sotietkiem.domain.SyncData(_accounts.value, _owners.value, deleted, _jobs.value)

    /** Ghi dữ liệu đã gộp từ mây về máy (không tính là thay đổi mới của người dùng). */
    fun applySynced(d: vn.sotietkiem.domain.SyncData) {
        _accounts.value = d.accounts; repo.save(d.accounts)
        _owners.value = d.owners.map { it.normalized() }; repo.saveOwners(_owners.value)
        deleted = d.deleted; repo.saveDeleted(deleted)
        _jobs.value = d.jobs; repo.saveJobs(d.jobs)
    }

    fun markSynced() {
        val t = System.currentTimeMillis()
        repo.lastSyncAt = t
        _lastSync.value = t
    }

    fun get(id: String): Account? = _accounts.value.firstOrNull { it.id == id }

    // ---- Người sở hữu ----
    private fun commitOwners(list: List<Owner>) {
        val old = _owners.value.associateBy { it.id }
        val now = Instant.now().toString()
        // Đánh dấu thời điểm sửa cho người sở hữu mới hoặc vừa đổi (tên, mục tiêu...)
        val stamped = list.map { o ->
            val before = old[o.id]
            if (before == null || before.copy(updatedAt = null) != o.copy(updatedAt = null)) o.copy(updatedAt = now) else o
        }
        val newIds = stamped.map { it.id }.toHashSet()
        markDeleted(old.keys.filterNot { it in newIds })
        _owners.value = stamped
        repo.saveOwners(stamped)
        touch()
        onChanged?.invoke()
    }

    /** Thêm người sở hữu mới; trùng tên (không phân biệt hoa thường) thì trả về người đã có. */
    fun addOwner(name: String): Owner {
        val n = name.trim().take(40)
        _owners.value.firstOrNull { it.name.equals(n, ignoreCase = true) }?.let { return it }
        val o = Owner(UUID.randomUUID().toString(), n)
        commitOwners(_owners.value + o)
        return o
    }

    fun renameOwner(id: String, name: String) =
        commitOwners(_owners.value.map { if (it.id == id) it.copy(name = name.trim().take(40)) else it })

    // ---- Mục tiêu của từng người (thứ tự = mức ưu tiên) ----
    private fun editGoals(ownerId: String, f: (List<Goal>) -> List<Goal>) =
        commitOwners(_owners.value.map { if (it.id == ownerId) it.copy(goals = f(it.goals)) else it })

    fun addGoal(ownerId: String, name: String, amount: Long, date: String?) =
        editGoals(ownerId) { it + Goal(UUID.randomUUID().toString(), name.trim().take(60), amount, date) }

    fun updateGoal(ownerId: String, goal: Goal) =
        editGoals(ownerId) { list -> list.map { if (it.id == goal.id) goal.copy(name = goal.name.trim().take(60)) else it } }

    fun deleteGoal(ownerId: String, goalId: String) = editGoals(ownerId) { list -> list.filterNot { it.id == goalId } }

    /** Đổi chỗ mục tiêu: delta = -1 lên trên, +1 xuống dưới. */
    fun moveGoal(ownerId: String, goalId: String, delta: Int) = editGoals(ownerId) { list ->
        val i = list.indexOfFirst { it.id == goalId }
        val j = i + delta
        if (i < 0 || j !in list.indices) list
        else list.toMutableList().also { m -> val t = m[i]; m[i] = m[j]; m[j] = t }
    }

    /** Số sổ đang có phần của người này. Chỉ xóa được người không còn sổ nào. */
    fun ownerUsage(id: String): Int = _accounts.value.count { a -> a.owners.any { it.ownerId == id } }

    /** Số công việc đang gán cho người này. */
    fun ownerJobCount(id: String): Int = _jobs.value.count { it.ownerId == id }

    fun deleteOwner(id: String): Boolean {
        if (ownerUsage(id) > 0 || ownerJobCount(id) > 0) return false
        commitOwners(_owners.value.filterNot { it.id == id })
        return true
    }

    /** Bỏ phần của người không có trong danh sách; nếu vì thế tổng lệch thì coi như chưa gán. */
    private fun sanitize(a: Account, ids: Set<String>): Account {
        if (a.owners.all { it.ownerId in ids }) return a
        return a.copy(owners = emptyList())
    }

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
    fun importMerge(items: List<Account>, owners: List<Owner> = emptyList(), jobs: List<Job> = emptyList()): Int {
        val jobIds = _jobs.value.map { it.id }.toHashSet()
        val ownerIds = _owners.value.map { it.id }.toSet() + owners.map { it.id }
        val newJobs = jobs.filter { jobIds.add(it.id) }.map { j -> if (j.ownerId != null && j.ownerId !in ownerIds) j.copy(ownerId = null) else j }
        if (newJobs.isNotEmpty()) commitJobs(_jobs.value + newJobs)
        val known = _owners.value.map { it.id }.toHashSet()
        val merged = _owners.value + owners.filter { known.add(it.id) }.map { it.normalized() }
        commitOwners(merged)
        val ids = _accounts.value.map { it.id }.toHashSet()
        val fresh = items.filter { ids.add(it.id) }.map { sanitize(it, known) }
        commit(_accounts.value + fresh)
        return fresh.size
    }

    fun importReplace(items: List<Account>, owners: List<Owner> = emptyList(), jobs: List<Job> = emptyList()) {
        val ownerIdsR = owners.map { it.id }.toSet()
        commitJobs(jobs.distinctBy { it.id }.map { j -> if (j.ownerId != null && j.ownerId !in ownerIdsR) j.copy(ownerId = null) else j })
        val list = owners.distinctBy { it.id }.map { it.normalized() }
        commitOwners(list)
        val ids = list.map { it.id }.toSet()
        commit(items.distinctBy { it.id }.map { sanitize(it, ids) })
    }

    fun clearAll() {
        commit(emptyList())
        commitOwners(emptyList())
        commitJobs(emptyList())
    }

    fun markExported() {
        val t = System.currentTimeMillis()
        repo.lastExportAt = t
        _lastExport.value = t
    }

    /**
     * Dữ liệu mẫu để xem thử: 2 người sở hữu (Bố, Mẹ) kèm mục tiêu, và 5 sổ chia phần cho từng người
     * (một sổ để "Chưa gán"). Bấm nhiều lần không tạo trùng người hay mục tiêu.
     */
    fun loadSample() {
        val t = LocalDate.now()
        val now = Instant.now().toString()

        // ---- Người sở hữu và mục tiêu mẫu (thứ tự = mức ưu tiên) ----
        val bo = addOwner("Bố")
        val me = addOwner("Mẹ")
        fun ensureGoals(ownerId: String, goals: List<Triple<String, Long, LocalDate?>>) {
            val have = _owners.value.firstOrNull { it.id == ownerId }?.goals?.map { it.name }?.toSet() ?: emptySet()
            goals.filter { it.first !in have }.forEach { (n, amt, d) -> addGoal(ownerId, n, amt, d?.toString()) }
        }
        ensureGoals(bo.id, listOf(
            Triple("Quỹ khẩn cấp", 100_000_000L, null),
            Triple("Mua nhà", 1_500_000_000L, t.plusYears(2)),
            Triple("Nghỉ hưu", 2_000_000_000L, t.plusYears(10)),
        ))
        ensureGoals(me.id, listOf(
            Triple("Học phí đại học cho con", 500_000_000L, t.plusYears(3)),
            Triple("Du lịch gia đình", 80_000_000L, t.plusMonths(8)),
        ))

        // ---- Sổ mẫu ----
        fun mk(
            name: String, code: String, amount: Long, start: LocalDate, term: Int, rate: Double, p: InterestPayout,
            note: String, shares: List<OwnerShare> = emptyList(),
        ) = Account(
            id = UUID.randomUUID().toString(), name = name, amount = amount, startDate = start.toString(),
            termMonths = term, maturityDate = Calc.maturityDate(start, term).toString(), interestRate = rate,
            interestPayout = p, bankCode = code, bankName = Banks.find(code)?.name ?: code,
            note = "[Mẫu] $note", createdAt = now, updatedAt = now, owners = shares,
        )
        val sample = listOf(
            mk("Quỹ mua nhà", "VCB", 500_000_000, t.minusMonths(7), 12, 4.8, InterestPayout.MATURITY, "Sổ giấy, chi nhánh Bến Thành",
                listOf(OwnerShare(bo.id, 300_000_000), OwnerShare(me.id, 200_000_000))),
            mk("Quỹ dự phòng", "TCB", 120_000_000, t.minusMonths(2).minusDays(20), 3, 4.3, InterestPayout.MATURITY, "Gửi online",
                listOf(OwnerShare(me.id, 120_000_000))),
            mk("Tiền học cho con", "BIDV", 200_000_000, t.minusMonths(6).minusDays(3), 6, 4.7, InterestPayout.MONTHLY, "Lĩnh lãi hàng tháng",
                listOf(OwnerShare(bo.id, 100_000_000), OwnerShare(me.id, 100_000_000))),
            mk("Gửi thử Cake", "CAKE", 50_000_000, t.minusMonths(1).minusDays(5), 1, 4.1, InterestPayout.MATURITY, "Ngân hàng số, chưa gán người sở hữu"),
            mk("Quỹ hưu trí", "ACB", 300_000_000, t.minusMonths(3), 24, 5.6, InterestPayout.MATURITY, "Kỳ hạn dài",
                listOf(OwnerShare(bo.id, 300_000_000))),
        )
        commit(_accounts.value + sample)

        // ---- Công việc mẫu: dạy kèm 200.000 ₫/giờ, mục tiêu 30 giờ/tháng, đã có tháng trước và tháng này ----
        if (_jobs.value.none { it.name == "Dạy kèm Toán (mẫu)" }) {
            val start = t.minusMonths(2).withDayOfMonth(1)
            val lastMonth = t.minusMonths(1)
            fun e(kind: EntryKind, d: LocalDate, h: Double, amount: Long = 0, note: String = "") =
                HourEntry(UUID.randomUUID().toString(), kind, d.toString(), h, amount, note, now)
            val entries = mutableListOf(
                e(EntryKind.WORK, start.plusDays(3), 12.0, note = "Tuần đầu"),
                e(EntryKind.WORK, start.plusDays(17), 14.0),
                e(EntryKind.PAYMENT, start.plusMonths(1).withDayOfMonth(1), 26.0, 5_200_000, "Trả tháng ${start.monthValue}"),
                e(EntryKind.WORK, lastMonth.withDayOfMonth(6), 10.0),
                e(EntryKind.WORK, lastMonth.withDayOfMonth(20), 16.5),
                e(EntryKind.WORK, lastMonth.withDayOfMonth(25), -1.5, note = "Ghi nhầm"),
            )
            // Tháng này: đã làm một ít tùy theo hôm nay là ngày mấy
            if (t.dayOfMonth >= 2) entries += e(EntryKind.WORK, t.withDayOfMonth(minOf(2, t.dayOfMonth)), 6.0)
            if (t.dayOfMonth >= 10) entries += e(EntryKind.WORK, t.withDayOfMonth(10), 5.5)
            commitJobs(_jobs.value + Job(
                id = UUID.randomUUID().toString(), name = "Dạy kèm Toán (mẫu)", rate = 200_000,
                startDate = start.toString(), monthlyTarget = 30.0, note = "[Mẫu] Học sinh lớp 9, 3 buổi/tuần",
                entries = entries, createdAt = now, updatedAt = now, ownerId = me.id,
            ))
        }
    }

    // ---- Công việc tính theo giờ ----

    /** Lưu danh sách công việc: ghi nhận công việc / dòng giờ bị xóa và đánh dấu thời điểm sửa. */
    private fun commitJobs(list: List<Job>) {
        val old = _jobs.value.associateBy { it.id }
        val now = Instant.now().toString()
        val stamped = list.map { j ->
            val before = old[j.id]
            if (before == null || before.copy(updatedAt = "") != j.copy(updatedAt = "")) j.copy(updatedAt = now) else j
        }
        val newIds = stamped.map { it.id }.toHashSet()
        val newEntryIds = stamped.flatMap { j -> j.entries.map { it.id } }.toHashSet()
        markDeleted(old.keys.filterNot { it in newIds } +
            old.values.flatMap { j -> j.entries.map { it.id } }.filterNot { it in newEntryIds })
        _jobs.value = stamped
        repo.saveJobs(stamped)
        touch()
        onChanged?.invoke()
    }

    private fun editJob(id: String, f: (Job) -> Job) = commitJobs(_jobs.value.map { if (it.id == id) f(it) else it })

    fun getJob(id: String): Job? = _jobs.value.firstOrNull { it.id == id }

    /** Thêm công việc; [initialUnpaid] > 0 thì ghi sẵn số giờ chưa thanh toán vào ngày bắt đầu. */
    fun addJob(name: String, rate: Long, startDate: String, monthlyTarget: Double, note: String, initialUnpaid: Double, ownerId: String? = null): Job {
        val now = Instant.now().toString()
        val entries = if (initialUnpaid > 0) listOf(
            HourEntry(UUID.randomUUID().toString(), EntryKind.WORK, startDate, initialUnpaid, note = "Giờ chưa thanh toán ban đầu", createdAt = now)
        ) else emptyList()
        val j = Job(UUID.randomUUID().toString(), name.trim().take(80), rate, startDate, monthlyTarget, note.trim().take(500), entries,
            createdAt = now, updatedAt = now, ownerId = ownerId)
        commitJobs(_jobs.value + j)
        return j
    }

    fun updateJob(id: String, name: String, rate: Long, startDate: String, monthlyTarget: Double, note: String, ownerId: String?) =
        editJob(id) {
            it.copy(name = name.trim().take(80), rate = rate, startDate = startDate, monthlyTarget = monthlyTarget,
                note = note.trim().take(500), ownerId = ownerId)
        }

    fun setJobClosed(id: String, closed: Boolean) = editJob(id) { it.copy(closed = closed) }

    fun deleteJob(id: String) = commitJobs(_jobs.value.filterNot { it.id == id })

    /** Thêm giờ (hours > 0) hoặc bớt giờ (hours < 0) vào ngày [date]. */
    fun addHours(jobId: String, hours: Double, date: String, note: String = "") = editJob(jobId) {
        it.copy(entries = it.entries + HourEntry(UUID.randomUUID().toString(), EntryKind.WORK, date, hours, note = note.trim().take(200),
            createdAt = Instant.now().toString()))
    }

    /** Ghi nhận đã thanh toán [hours] giờ với giá mỗi giờ hiện tại. */
    fun pay(jobId: String, hours: Double, date: String, note: String = "") = editJob(jobId) {
        it.copy(entries = it.entries + HourEntry(UUID.randomUUID().toString(), EntryKind.PAYMENT, date, hours,
            amount = vn.sotietkiem.domain.JobCalc.amountFor(it.rate, hours), note = note.trim().take(200), createdAt = Instant.now().toString()))
    }

    fun deleteEntry(jobId: String, entryId: String) = editJob(jobId) { j -> j.copy(entries = j.entries.filterNot { it.id == entryId }) }

    /**
     * Xóa dữ liệu chỉ trên máy này (không ghi nhận là "đã xóa" nên không lan lên mây).
     * Dùng khi quên mã khóa ứng dụng.
     */
    fun clearLocal() {
        _accounts.value = emptyList(); repo.save(emptyList())
        _owners.value = emptyList(); repo.saveOwners(emptyList())
        deleted = emptyMap(); repo.saveDeleted(deleted)
        _jobs.value = emptyList(); repo.saveJobs(emptyList())
    }

    // ---- Đồng bộ tài khoản Google (Firebase), mã hóa đầu-cuối ----
    val cloud = vn.sotietkiem.sync.CloudSync(
        app,
        viewModelScope,
        local = { syncData() },
        applyData = { applySynced(it) },
        onSynced = { markSynced() },
    )

    /** Đã chọn cách lưu ở lần mở đầu tiên chưa (chưa thì hiện màn chào). */
    private val _storageChosen = MutableStateFlow(repo.storageChosen)
    val storageChosen: StateFlow<Boolean> = _storageChosen.asStateFlow()

    fun chooseStorage() {
        repo.storageChosen = true
        _storageChosen.value = true
    }

    init {
        // Người dùng cũ đã có dữ liệu thì coi như đã chọn lưu trên máy, không hiện màn chọn
        if (!repo.storageChosen && (_accounts.value.isNotEmpty() || _owners.value.isNotEmpty())) chooseStorage()
        onChanged = { cloud.scheduleSync() }
        cloud.start()
    }
}
