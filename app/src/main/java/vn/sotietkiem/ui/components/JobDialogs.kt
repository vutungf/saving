package vn.sotietkiem.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import vn.sotietkiem.data.Job
import vn.sotietkiem.data.Owner
import androidx.compose.foundation.layout.FlowRow
import vn.sotietkiem.domain.JobCalc
import vn.sotietkiem.util.Fmt
import vn.sotietkiem.util.VnWords
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Nút chọn ngày dùng trong các hộp thoại công việc. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateButton(label: String, date: LocalDate, onPick: (LocalDate) -> Unit, modifier: Modifier = Modifier.fillMaxWidth()) {
    var open by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { open = true }, modifier = modifier) {
        Icon(Icons.Outlined.CalendarMonth, null); Text("  $label: ${Fmt.date(date)}")
    }
    if (open) {
        val st = rememberDatePickerState(initialSelectedDateMillis = date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    st.selectedDateMillis?.let { onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                    open = false
                }) { Text("Chọn") }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Hủy") } },
        ) { DatePicker(state = st) }
    }
}

@Composable
private fun DecimalField(label: String, value: String, suffix: String, onChange: (String) -> Unit, supporting: String? = null, isError: Boolean = false) {
    OutlinedTextField(
        value = value,
        onValueChange = { v -> onChange(v.filter { it.isDigit() || it == ',' || it == '.' }.take(8)) },
        label = { Text(label) }, singleLine = true, suffix = { Text(suffix) }, isError = isError,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        supportingText = supportText(supporting),
        modifier = Modifier.fillMaxWidth(),
    )
}

/**
 * Thêm hoặc sửa công việc: tên, giá mỗi giờ, ngày bắt đầu, mục tiêu giờ mỗi tháng, ghi chú.
 * Khi thêm mới có thêm ô "giờ chưa thanh toán hiện tại".
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun JobEditDialog(
    initial: Job?,
    owners: List<Owner>,
    onSave: (name: String, rate: Long, start: LocalDate, target: Double, note: String, initialUnpaid: Double, ownerId: String?) -> Unit,
    onDismiss: () -> Unit,
    defaultOwnerId: String? = null,
) {
    // Người làm công việc này; mặc định là người đang lọc ở tab Công việc (nếu có)
    var ownerId by remember { mutableStateOf(initial?.ownerId ?: defaultOwnerId?.takeIf { id -> owners.any { it.id == id } }) }
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var rate by remember { mutableStateOf(initial?.rate?.takeIf { it > 0 }?.toString() ?: "") }
    var start by remember { mutableStateOf(initial?.startDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: LocalDate.now()) }
    var target by remember { mutableStateOf(initial?.monthlyTarget?.takeIf { it > 0 }?.let { Fmt.hours(it) } ?: "") }
    var unpaid by remember { mutableStateOf("") }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var tried by remember { mutableStateOf(false) }

    val rateVal = rate.toLongOrNull()
    val targetVal = if (target.isBlank()) 0.0 else Fmt.parseHours(target)
    val unpaidVal = if (unpaid.isBlank()) 0.0 else Fmt.parseHours(unpaid)
    val firstEntry = initial?.entries?.mapNotNull { runCatching { LocalDate.parse(it.date) }.getOrNull() }?.minOrNull()
    val errors = buildMap {
        if (name.isBlank()) put("name", "Nhập tên công việc")
        if (rateVal == null) put("rate", "Nhập số tiền mỗi giờ")
        if (targetVal == null || targetVal < 0 || targetVal > 744) put("target", "Từ 0 đến 744 giờ")
        if (unpaidVal == null || unpaidVal < 0 || unpaidVal > 100_000) put("unpaid", "Số giờ không hợp lệ")
        if (firstEntry != null && start.isAfter(firstEntry)) put("start", "Đã có giờ ghi từ ${Fmt.date(firstEntry)}, ngày bắt đầu không được sau ngày đó")
    }
    fun err(k: String) = if (tried) errors[k] else null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Thêm công việc" else "Sửa công việc") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it.take(80) }, singleLine = true,
                    label = { Text("Tên công việc") }, placeholder = { Text("Ví dụ: Dạy kèm, Làm thêm") },
                    isError = err("name") != null, supportingText = supportText(err("name")),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = rate, onValueChange = { v -> rate = v.filter { it.isDigit() }.trimStart('0').take(12) },
                    singleLine = true, label = { Text("Số tiền mỗi giờ") }, suffix = { Text("₫/giờ") },
                    visualTransformation = ThousandsTransformation,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = err("rate") != null,
                    supportingText = { Text(err("rate") ?: rateVal?.let { VnWords.read(it) } ?: "") },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (owners.isNotEmpty()) {
                    Text("Người sở hữu", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = ownerId == null, onClick = { ownerId = null }, label = { Text("Chưa gán") })
                        owners.forEach { o ->
                            FilterChip(
                                selected = ownerId == o.id, onClick = { ownerId = o.id }, label = { Text(o.name) },
                                leadingIcon = { OwnerAvatar(o.name, ownerColor(owners, o.id), 20.dp) },
                            )
                        }
                    }
                }
                DateButton("Bắt đầu", start, { start = it })
                err("start")?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                DecimalField("Mục tiêu mỗi tháng (không bắt buộc)", target, "giờ", { target = it },
                    supporting = err("target") ?: "Ví dụ 40 giờ/tháng; để trống nếu không đặt", isError = err("target") != null)
                if (initial == null) {
                    DecimalField("Số giờ chưa thanh toán hiện tại", unpaid, "giờ", { unpaid = it },
                        supporting = err("unpaid") ?: "Giờ đã làm nhưng chưa được trả tiền (nếu có)", isError = err("unpaid") != null)
                }
                OutlinedTextField(
                    value = note, onValueChange = { note = it.take(500) }, label = { Text("Ghi chú (không bắt buộc)") },
                    minLines = 2, modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                tried = true
                if (errors.isEmpty()) { onSave(name, rateVal!!, start, targetVal!!, note, unpaidVal!!, ownerId); onDismiss() }
            }) { Text("Lưu") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } },
    )
}

/** Thêm giờ ([add] = true) hoặc bớt giờ: số giờ, ngày, ghi chú. */
@Composable
fun HoursDialog(add: Boolean, job: Job, initialDate: LocalDate, onSave: (hours: Double, date: LocalDate, note: String) -> Unit, onDismiss: () -> Unit) {
    var hours by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(initialDate) }
    var note by remember { mutableStateOf("") }
    val h = Fmt.parseHours(hours)
    val start = runCatching { LocalDate.parse(job.startDate) }.getOrNull()
    val error = when {
        hours.isNotBlank() && (h == null || h <= 0 || h > 1000) -> "Số giờ phải lớn hơn 0"
        start != null && date.isBefore(start) -> "Ngày trước ngày bắt đầu (${Fmt.date(start)})"
        else -> null
    }
    val ready = h != null && h > 0 && h <= 1000 && error == null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (add) "Thêm giờ" else "Bớt giờ") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("0,5", "1", "2", "4", "8").forEach { q ->
                        FilterChip(selected = hours == q, onClick = { hours = q }, label = { Text(q) })
                    }
                }
                DecimalField("Số giờ", hours, "giờ", { hours = it },
                    supporting = error ?: h?.let { "${if (add) "+" else "−"}${Fmt.vnd(JobCalc.amountFor(job.rate, it))} theo giá hiện tại" },
                    isError = error != null)
                DateButton("Ngày", date, { date = it })
                OutlinedTextField(value = note, onValueChange = { note = it.take(200) }, label = { Text("Ghi chú (không bắt buộc)") },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(enabled = ready, onClick = { onSave(if (add) h!! else -h!!, date, note); onDismiss() }) { Text(if (add) "Thêm" else "Bớt") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } },
    )
}

/** Ghi nhận thanh toán: mặc định toàn bộ giờ chưa thanh toán, có thể sửa số giờ. */
@Composable
fun PayDialog(job: Job, onSave: (hours: Double, date: LocalDate, note: String) -> Unit, onDismiss: () -> Unit) {
    val unpaid = JobCalc.unpaidHours(job)
    var hours by remember { mutableStateOf(Fmt.hours(unpaid)) }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var note by remember { mutableStateOf("") }
    val h = Fmt.parseHours(hours)
    val error = when {
        h == null || h <= 0 -> "Số giờ phải lớn hơn 0"
        h > unpaid + 1e-9 -> "Nhiều hơn số giờ chưa thanh toán (${Fmt.hours(unpaid)} giờ)"
        else -> null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ghi nhận thanh toán") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Chưa thanh toán: ${Fmt.hours(unpaid)} giờ × ${Fmt.vnd(job.rate)}", style = MaterialTheme.typography.bodyMedium)
                DecimalField("Số giờ được thanh toán", hours, "giờ", { hours = it }, supporting = error, isError = error != null)
                HorizontalDivider()
                Row(Modifier.fillMaxWidth()) {
                    Text("Số tiền nhận", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Text(Fmt.vnd(JobCalc.amountFor(job.rate, h ?: 0.0)), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                DateButton("Ngày nhận", date, { date = it })
                OutlinedTextField(value = note, onValueChange = { note = it.take(200) }, label = { Text("Ghi chú (không bắt buộc)") },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { TextButton(enabled = error == null, onClick = { onSave(h!!, date, note); onDismiss() }) { Text("Đã nhận tiền") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } },
    )
}

private fun supportText(s: String?): (@Composable () -> Unit)? {
    if (s == null) return null
    return { Text(s) }
}
