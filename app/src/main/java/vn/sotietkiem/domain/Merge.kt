package vn.sotietkiem.domain

import kotlinx.serialization.Serializable
import vn.sotietkiem.data.Account
import vn.sotietkiem.data.Owner
import java.time.Instant

/** Toàn bộ dữ liệu được đồng bộ (trước khi mã hóa). */
@Serializable
data class SyncData(
    val accounts: List<Account> = emptyList(),
    val owners: List<Owner> = emptyList(),
    /** Mã đã xóa → thời điểm xóa (ISO). */
    val deleted: Map<String, String> = emptyMap(),
)

private fun ts(s: String?): Instant = s?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: Instant.EPOCH

/**
 * Gộp dữ liệu hai nơi (máy này và trên mây):
 * - mỗi sổ / người sở hữu lấy bản có updatedAt mới hơn;
 * - bản ghi đã bị xóa ở bất kỳ đâu (sau lần sửa cuối của nó) thì bị xóa;
 * - thứ tự giữ theo [local], bản ghi mới từ [remote] thêm vào cuối.
 * Mục xóa cũ hơn [keepDeletedDays] ngày được dọn khỏi danh sách.
 */
fun merge(local: SyncData, remote: SyncData, now: Instant = Instant.now(), keepDeletedDays: Long = 180): SyncData {
    // Lấy thời điểm xóa muộn nhất từ hai bên
    val deleted = HashMap<String, String>()
    for (m in listOf(local.deleted, remote.deleted)) for ((id, t) in m) if (ts(t) > ts(deleted[id])) deleted[id] = t

    fun alive(id: String, updatedAt: String?) = deleted[id]?.let { ts(it) < ts(updatedAt) } ?: true

    fun <T> pick(l: List<T>, r: List<T>, id: (T) -> String, upd: (T) -> String?): List<T> {
        val rById = r.associateBy(id)
        val out = l.map { a -> rById[id(a)]?.let { b -> if (ts(upd(b)) > ts(upd(a))) b else a } ?: a }
        val known = l.map(id).toHashSet()
        return (out + r.filter { id(it) !in known }).filter { alive(id(it), upd(it)) }
    }

    val accounts = pick(local.accounts, remote.accounts, { it.id }, { it.updatedAt })
    val owners = pick(local.owners, remote.owners, { it.id }, { it.updatedAt })
    // Bản ghi còn sống thì bỏ dấu xóa (đã được tạo lại hoặc sửa sau khi xóa)
    val aliveIds = (accounts.map { it.id } + owners.map { it.id }).toSet()
    val cutoff = now.minusSeconds(keepDeletedDays * 86_400)
    val keptDeleted = deleted.filter { (id, t) -> id !in aliveIds && ts(t) > cutoff }
    return SyncData(accounts, owners, keptDeleted)
}
