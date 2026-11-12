package vn.sotietkiem.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import vn.sotietkiem.data.EntryKind
import vn.sotietkiem.data.HourEntry
import vn.sotietkiem.data.Job
import vn.sotietkiem.data.Owner
import vn.sotietkiem.ui.components.OwnerAvatar
import vn.sotietkiem.ui.components.ownerColor
import vn.sotietkiem.domain.JobCalc
import vn.sotietkiem.ui.components.ConfirmDialog
import vn.sotietkiem.ui.components.HoursDialog
import vn.sotietkiem.ui.components.JobEditDialog
import vn.sotietkiem.ui.components.PayDialog
import vn.sotietkiem.ui.components.SectionCard
import vn.sotietkiem.ui.theme.LocalExtra
import vn.sotietkiem.util.Fmt
import java.time.LocalDate
import java.time.YearMonth

/** Thao tác trên công việc, do MainActivity nối vào ViewModel. */
interface JobActions {
    fun update(id: String, name: String, rate: Long, start: LocalDate, target: Double, note: String, ownerId: String?)
    fun addHours(id: String, hours: Double, date: LocalDate, note: String)
    fun pay(id: String, hours: Double, date: LocalDate, note: String)
    fun deleteEntry(id: String, entryId: String)
    fun setClosed(id: String, closed: Boolean)
    fun delete(id: String)
}

/** Chi tiết một công việc: giờ chưa thanh toán, tiến độ từng tháng, thêm/bớt giờ, thanh toán, lịch sử. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobDetailScreen(j: Job, owners: List<Owner>, actions: JobActions, onBack: () -> Unit) {
    val x = LocalExtra.current
    val startMonth = JobCalc.startMonth(j)
    val nowMonth = YearMonth.now()
    var ymText by rememberSaveable(j.id) { mutableStateOf(nowMonth.toString()) }
    val ym = YearMonth.parse(ymText).let { if (it < startMonth) startMonth else it }
    var menu by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    var hoursDialog by remember { mutableStateOf<Boolean?>(null) } // true = thêm, false = bớt
    var paying by remember { mutableStateOf(false) }
    var askDelete by remember { mutableStateOf(false) }
    var askDeleteEntry by remember { mutableStateOf<HourEntry?>(null) }
    val unpaid = JobCalc.unpaidHours(j)
    // Ngày mặc định khi thêm giờ: hôm nay nếu đang xem tháng này, ngược lại là ngày cuối tháng đang xem
    val defaultDate = if (ym == nowMonth) LocalDate.now() else ym.atEndOfMonth()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(j.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại") } },
                actions = {
                    IconButton(onClick = { editing = true }) { Icon(Icons.Outlined.Edit, "Sửa") }
                    Box {
                        IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, "Thao tác khác") }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(text = { Text(if (j.closed) "Tiếp tục công việc" else "Tạm dừng công việc") },
                                onClick = { menu = false; actions.setClosed(j.id, !j.closed) })
                            HorizontalDivider()
                            DropdownMenuItem(text = { Text("Xóa công việc", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error) },
                                onClick = { menu = false; askDelete = true })
                        }
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // ---- Chưa thanh toán ----
            SectionCard {
                Text("Chưa thanh toán", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(Fmt.vnd(JobCalc.unpaidAmount(j)), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
                    color = if (unpaid > 0) x.warning else MaterialTheme.colorScheme.onSurface)
                Text("${Fmt.hours(unpaid)} giờ × ${Fmt.vnd(j.rate)}/giờ", style = MaterialTheme.typography.bodyMedium)
                Text("Đổi số tiền mỗi giờ (nút bút chì) thì số tiền chưa thanh toán tính lại theo giá mới.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = { paying = true }, enabled = unpaid > 0, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.Payments, null); Text("  Ghi nhận thanh toán")
                }
            }

            // ---- Tháng ----
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(enabled = ym > startMonth, onClick = { ymText = ym.minusMonths(1).toString() }) {
                        Icon(Icons.Outlined.ChevronLeft, "Tháng trước")
                    }
                    Text("Tháng ${ym.monthValue}/${ym.year}", style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    IconButton(enabled = ym < nowMonth, onClick = { ymText = ym.plusMonths(1).toString() }) {
                        Icon(Icons.Outlined.ChevronRight, "Tháng sau")
                    }
                }
                MonthProgress(j, ym)
                val expected = JobCalc.expectedByToday(j, ym)
                if (j.monthlyTarget > 0 && ym == nowMonth) {
                    val month = JobCalc.monthHours(j, ym)
                    Text(
                        if (month + 1e-9 >= expected) "Đúng nhịp: tới hôm nay cần khoảng ${Fmt.hours(expected)} giờ."
                        else "Chậm ${Fmt.hours(expected - month)} giờ so với nhịp (cần khoảng ${Fmt.hours(expected)} giờ tới hôm nay). " +
                            "Còn ${Fmt.hours((j.monthlyTarget - month).coerceAtLeast(0.0))} giờ để đạt mục tiêu.",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (month + 1e-9 >= expected) MaterialTheme.colorScheme.onSurfaceVariant else x.warning,
                    )
                }
                Text("Tiền làm ra trong tháng: ${Fmt.vnd(JobCalc.monthAmount(j, ym))} (theo giá hiện tại)",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (!j.closed) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(onClick = { hoursDialog = true }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Outlined.Add, null); Text(" Thêm giờ")
                        }
                        OutlinedButton(onClick = { hoursDialog = false }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Outlined.Remove, null); Text(" Bớt giờ")
                        }
                    }
                }
                val monthEntries = j.entries.filter { runCatching { YearMonth.from(LocalDate.parse(it.date)) == ym }.getOrDefault(false) }
                    .sortedWith(compareByDescending<HourEntry> { it.date }.thenByDescending { it.createdAt })
                if (monthEntries.isEmpty()) {
                    Text("Chưa có ghi nhận nào trong tháng này.", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    monthEntries.forEach { e -> EntryRow(e) { askDeleteEntry = e } }
                }
            }

            // ---- Thông tin ----
            SectionCard("Thông tin") {
                val owner = owners.firstOrNull { it.id == j.ownerId }
                if (owner != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Người sở hữu", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                        OwnerAvatar(owner.name, ownerColor(owners, owner.id), 22.dp)
                        Text(owner.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    }
                } else if (owners.isNotEmpty()) {
                    Info("Người sở hữu", "Chưa gán")
                }
                Info("Bắt đầu", Fmt.date(j.startDate))
                Info("Số tiền mỗi giờ", Fmt.vnd(j.rate))
                Info("Mục tiêu mỗi tháng", if (j.monthlyTarget > 0) "${Fmt.hours(j.monthlyTarget)} giờ" else "Chưa đặt")
                Info("Tổng giờ đã làm", "${Fmt.hours(JobCalc.workedHours(j))} giờ")
                Info("Đã thanh toán", "${Fmt.hours(JobCalc.paidHours(j))} giờ · ${Fmt.vnd(JobCalc.paidAmount(j))}")
                if (j.note.isNotBlank()) Text(j.note, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (editing) {
        JobEditDialog(j, owners, onSave = { n, r, s, t, note, _, oid -> actions.update(j.id, n, r, s, t, note, oid) }, onDismiss = { editing = false })
    }
    hoursDialog?.let { add ->
        HoursDialog(add, j, defaultDate, onSave = { h, d, note -> actions.addHours(j.id, h, d, note) }, onDismiss = { hoursDialog = null })
    }
    if (paying) {
        PayDialog(j, onSave = { h, d, note -> actions.pay(j.id, h, d, note) }, onDismiss = { paying = false })
    }
    if (askDelete) {
        ConfirmDialog(
            title = "Xóa công việc?", text = "Công việc \"${j.name}\" và toàn bộ giờ đã ghi sẽ bị xóa.", confirm = "Xóa", danger = true,
            onConfirm = { actions.delete(j.id) }, onDismiss = { askDelete = false },
        )
    }
    askDeleteEntry?.let { e ->
        ConfirmDialog(
            title = "Xóa ghi nhận?",
            text = "${Fmt.date(e.date)}: " + entryLabel(e) + ". Thao tác này không hoàn tác được.",
            confirm = "Xóa", danger = true,
            onConfirm = { actions.deleteEntry(j.id, e.id) }, onDismiss = { askDeleteEntry = null },
        )
    }
}

private fun entryLabel(e: HourEntry): String = when {
    e.kind == EntryKind.PAYMENT -> "đã thanh toán ${Fmt.hours(e.hours)} giờ (${Fmt.vnd(e.amount)})"
    e.hours >= 0 -> "thêm ${Fmt.hours(e.hours)} giờ"
    else -> "bớt ${Fmt.hours(-e.hours)} giờ"
}

@Composable
private fun EntryRow(e: HourEntry, onDelete: () -> Unit) {
    val x = LocalExtra.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                when {
                    e.kind == EntryKind.PAYMENT -> "Thanh toán ${Fmt.hours(e.hours)} giờ"
                    e.hours >= 0 -> "+${Fmt.hours(e.hours)} giờ"
                    else -> "−${Fmt.hours(-e.hours)} giờ"
                },
                style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
                color = when {
                    e.kind == EntryKind.PAYMENT -> x.positive
                    e.hours < 0 -> x.danger
                    else -> MaterialTheme.colorScheme.onSurface
                },
            )
            Text(
                Fmt.date(e.date) + (if (e.kind == EntryKind.PAYMENT) " · nhận ${Fmt.vnd(e.amount)}" else "") +
                    (if (e.note.isNotBlank()) " · ${e.note}" else ""),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onDelete) { Icon(Icons.Outlined.Delete, "Xóa", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun Info(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
