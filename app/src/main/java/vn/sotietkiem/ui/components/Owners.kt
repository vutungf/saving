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
                    Text(if (used == 0) "Chưa có sổ nào" else "$used sổ", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { renaming = o }) { Icon(Icons.Outlined.Edit, "Đổi tên") }
                IconButton(onClick = { deleting = o }) { Icon(Icons.Outlined.Delete, "Xóa", tint = MaterialTheme.colorScheme.error) }
            }
        }
        FilledTonalButton(onClick = { adding = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.PersonAdd, null); Text("  Thêm người sở hữu")
        }
    }

    if (adding) OwnerNameDialog("Thêm người sở hữu", onDone = { vm.addOwner(it) }, onDismiss = { adding = false })
    renaming?.let { o ->
        OwnerNameDialog("Đổi tên", o.name, onDone = { vm.renameOwner(o.id, it) }, onDismiss = { renaming = null })
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
