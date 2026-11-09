package vn.sotietkiem.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import vn.sotietkiem.data.Owner
import vn.sotietkiem.ui.AppViewModel
import vn.sotietkiem.domain.GoalProgress
import vn.sotietkiem.domain.goalOf
import vn.sotietkiem.util.Fmt
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.ui.text.input.KeyboardType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import vn.sotietkiem.ui.theme.LocalExtra

/** Màu cố định cho từng người, theo thứ tự trong danh sách. */
@Composable
fun ownerColor(owners: List<Owner>, id: String): Color {
    val palette = LocalExtra.current.chart
    val i = owners.indexOfFirst { it.id == id }
    return if (i < 0) LocalExtra.current.muted else palette[i % palette.size]
}

@Composable
fun OwnerAvatar(name: String, color: Color, size: Dp = 28.dp) {
    Box(Modifier.size(size).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
        Text(name.trim().take(1).uppercase().ifEmpty { "?" }, color = Color.White, style = centeredLetter(size.value * 0.45f))
    }
}

/** Hộp thoại nhập tên người sở hữu (dùng cho thêm mới và đổi tên). */
@Composable
fun OwnerNameDialog(title: String, initial: String = "", onDone: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name, onValueChange = { name = it.take(40) }, singleLine = true,
                label = { Text("Tên") }, placeholder = { Text("Ví dụ: Bố, Mẹ, Con") },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = { onDone(name.trim()); onDismiss() }) { Text("Lưu") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } },
    )
}

/** Quản lý danh sách người sở hữu: thêm, đổi tên, xóa (chỉ khi không còn sổ nào). */
@Composable
fun OwnersCard(vm: AppViewModel) {
    val owners by vm.owners.collectAsState()
    val accounts by vm.accounts.collectAsState()
    var adding by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<Owner?>(null) }
    var deleting by remember { mutableStateOf<Owner?>(null) }

    SectionCard("Người sở hữu") {
        if (owners.isEmpty()) {
            Text(
                "Thêm người sở hữu (ví dụ Bố, Mẹ) để gán cho từng sổ và xem thống kê riêng từng người.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        owners.forEach { o ->
            val used = accounts.count { a -> a.owners.any { it.ownerId == o.id } }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OwnerAvatar(o.name, ownerColor(owners, o.id), 32.dp)
                Column(Modifier.weight(1f)) {
                    Text(o.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    Text(
                        (if (used == 0) "Chưa có sổ nào" else "$used sổ") +
                            (o.goalAmount?.let { " · mục tiêu ${Fmt.short(it)}" } ?: " · chưa đặt mục tiêu"),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = { renaming = o }) { Icon(Icons.Outlined.Edit, "Sửa tên và mục tiêu") }
                IconButton(onClick = { deleting = o }) { Icon(Icons.Outlined.Delete, "Xóa", tint = MaterialTheme.colorScheme.error) }
            }
            goalOf(o, accounts)?.let { GoalBar(it, ownerColor(owners, o.id)) }
        }
        FilledTonalButton(onClick = { adding = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.PersonAdd, null); Text("  Thêm người sở hữu")
        }
    }

    if (adding) OwnerNameDialog("Thêm người sở hữu", onDone = { vm.addOwner(it) }, onDismiss = { adding = false })
    renaming?.let { o ->
        OwnerEditDialog(o, onDone = { n, g, d -> vm.updateOwner(o.id, n, g, d) }, onDismiss = { renaming = null })
    }
    deleting?.let { o ->
        val used = vm.ownerUsage(o.id)
        if (used > 0) {
            AlertDialog(
                onDismissRequest = { deleting = null },
                title = { Text("Chưa xóa được") },
                text = { Text("${o.name} đang có phần trong $used sổ. Hãy sửa các sổ đó để bỏ ${o.name} ra trước.") },
                confirmButton = { TextButton(onClick = { deleting = null }) { Text("Đã hiểu") } },
            )
        } else {
            ConfirmDialog(
                title = "Xóa ${o.name}?", text = "Người này sẽ bị xóa khỏi danh sách người sở hữu.",
                confirm = "Xóa", danger = true,
                onConfirm = { vm.deleteOwner(o.id) }, onDismiss = { deleting = null },
            )
        }
    }
}

/** Thanh tiến độ mục tiêu kèm số còn thiếu và hạn. */
@Composable
fun GoalBar(g: GoalProgress, color: Color) {
    val x = LocalExtra.current
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("${Fmt.vnd(g.current)} / ${Fmt.vnd(g.goal)}", style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f))
            Text("${g.percent}%", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold,
                color = if (g.reached) x.positive else color)
        }
        LinearProgressIndicator(
            progress = { g.fraction },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
            color = if (g.reached) x.positive else color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            drawStopIndicator = {},
        )
        val left = g.daysLeft
        val line = when {
            g.reached -> "Đã đạt mục tiêu" + (g.deadline?.let { " · hạn ${Fmt.date(it)}" } ?: "")
            g.deadline == null -> "Còn thiếu ${Fmt.vnd(g.missing)}"
            left != null && left < 0 -> "Còn thiếu ${Fmt.vnd(g.missing)} · đã quá hạn ${-left} ngày (${Fmt.date(g.deadline!!)})"
            else -> "Còn thiếu ${Fmt.vnd(g.missing)} · còn $left ngày đến ${Fmt.date(g.deadline!!)}" +
                (g.perMonth?.let { " · cần gửi thêm ~${Fmt.vnd(it)}/tháng" } ?: "")
        }
        Text(line, style = MaterialTheme.typography.bodySmall,
            color = when {
                g.reached -> x.positive
                left != null && left < 0 -> x.danger
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            })
    }
}

/** Sửa tên, số tiền mục tiêu và hạn (không bắt buộc) của một người sở hữu. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerEditDialog(owner: Owner, onDone: (String, Long?, String?) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(owner.name) }
    var goal by remember { mutableStateOf(owner.goalAmount?.toString().orEmpty()) }
    var date by remember { mutableStateOf(owner.goalDate) }
    var pick by remember { mutableStateOf(false) }
    val goalVal = goal.toLongOrNull()?.takeIf { it > 0 }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sửa ${owner.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it.take(40) }, singleLine = true,
                    label = { Text("Tên") }, modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = goal, onValueChange = { v -> goal = v.filter { it.isDigit() }.trimStart('0').take(15) },
                    singleLine = true, label = { Text("Mục tiêu tiền gốc (không bắt buộc)") },
                    visualTransformation = ThousandsTransformation,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    suffix = { Text("₫") },
                    supportingText = { Text(goalVal?.let { vn.sotietkiem.util.VnWords.read(it) } ?: "Để trống nếu không đặt mục tiêu") },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (goalVal != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(onClick = { pick = true }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Outlined.CalendarMonth, null)
                            Text("  " + (date?.let { "Hạn ${Fmt.date(it)}" } ?: "Đặt hạn (không bắt buộc)"))
                        }
                        if (date != null) IconButton(onClick = { date = null }) { Icon(Icons.Outlined.Close, "Bỏ hạn") }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = { onDone(name.trim(), goalVal, date); onDismiss() }) { Text("Lưu") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } },
    )

    if (pick) {
        val init = (date?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: LocalDate.now().plusYears(1))
            .atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        val st = rememberDatePickerState(initialSelectedDateMillis = init)
        DatePickerDialog(
            onDismissRequest = { pick = false },
            confirmButton = {
                TextButton(onClick = {
                    st.selectedDateMillis?.let { date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString() }
                    pick = false
                }) { Text("Chọn") }
            },
            dismissButton = { TextButton(onClick = { pick = false }) { Text("Hủy") } },
        ) { DatePicker(state = st) }
    }
}
