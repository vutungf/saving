package vn.sotietkiem.ui.components

import android.app.Activity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import vn.sotietkiem.sync.PassNeed
import vn.sotietkiem.ui.AppViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

const val SYNC_PASSWORD_MIN = 8

private val timeFmt = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")

/** Thẻ "Lưu trữ và đồng bộ": chọn lưu trên máy hoặc đồng bộ tài khoản Google. */
@Composable
fun CloudSettingsCard(vm: AppViewModel) {
    val st by vm.cloud.state.collectAsState()
    val activity = LocalContext.current as Activity
    val scope = rememberCoroutineScope()
    var askLogout by remember { mutableStateOf(false) }
    var askDelete by remember { mutableStateOf(false) }
    var changePass by remember { mutableStateOf(false) }

    SectionCard("Lưu trữ và đồng bộ") {
        StorageOption(
            selected = !st.enabled,
            title = "Chỉ trên máy này",
            desc = "Dữ liệu mã hóa và chỉ nằm trên điện thoại. Chuyển máy bằng file sao lưu.",
            onClick = { if (st.enabled) askLogout = true },
        )
        StorageOption(
            selected = st.enabled,
            title = "Đồng bộ với tài khoản Google",
            desc = if (st.available) "Tự lưu lên Firebase và dùng chung trên nhiều điện thoại."
            else "Chưa dùng được: bản app này chưa được cấu hình Firebase.",
            enabled = st.available && !st.busy,
            onClick = { if (!st.enabled) scope.launch { vm.cloud.signIn(activity) } },
        )

        if (st.enabled && st.email != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.CloudDone, null, tint = MaterialTheme.colorScheme.primary)
                Column(Modifier.weight(1f)) {
                    Text(st.email!!, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        when {
                            st.busy -> "Đang đồng bộ..."
                            st.pass != PassNeed.NONE -> "Cần mật khẩu đồng bộ"
                            st.lastSync > 0 -> "Đồng bộ lúc " + Instant.ofEpochMilli(st.lastSync).atZone(ZoneId.systemDefault()).format(timeFmt)
                            else -> "Chưa đồng bộ"
                        },
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = { vm.cloud.syncNow() }, enabled = st.ready && !st.busy, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.Sync, null); Text(" Đồng bộ ngay")
                }
                OutlinedButton(onClick = { askLogout = true }, enabled = !st.busy, modifier = Modifier.weight(1f)) { Text("Đăng xuất") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { changePass = true }, enabled = st.ready && !st.busy) { Text("Đổi mật khẩu đồng bộ") }
                TextButton(onClick = { askDelete = true }, enabled = !st.busy) {
                    Text("Xóa dữ liệu trên mây", color = MaterialTheme.colorScheme.error)
                }
            }
        }
        if (st.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        st.error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
        if (st.enabled) {
            Text(
                "Dữ liệu được mã hóa trên điện thoại bằng mật khẩu đồng bộ trước khi gửi lên, nên Google và Firebase cũng không đọc được. " +
                    "Quên mật khẩu đồng bộ thì không mở được dữ liệu trên mây, nhưng dữ liệu trên máy vẫn còn.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (askLogout) {
        ConfirmDialog(
            title = "Ngừng đồng bộ?",
            text = "App sẽ đăng xuất tài khoản Google và chỉ lưu trên máy này. Dữ liệu trên máy và trên mây đều được giữ nguyên.",
            confirm = "Đăng xuất",
            onConfirm = { scope.launch { vm.cloud.signOut() } }, onDismiss = { askLogout = false },
        )
    }
    if (askDelete) {
        ConfirmDialog(
            title = "Xóa dữ liệu trên mây?",
            text = "Bản dữ liệu trên Firebase của tài khoản này sẽ bị xóa và app đăng xuất. Dữ liệu trên máy này vẫn giữ nguyên. " +
                "Các máy khác đang đồng bộ sẽ không còn nhận được dữ liệu.",
            confirm = "Xóa trên mây", danger = true,
            onConfirm = { scope.launch { vm.cloud.deleteCloudData() } }, onDismiss = { askDelete = false },
        )
    }
    if (changePass) {
        PassphraseDialog(
            create = true, title = "Đổi mật khẩu đồng bộ", busy = st.busy, error = null,
            onConfirm = { p -> scope.launch { vm.cloud.changePassphrase(p); changePass = false } },
            onCancel = { changePass = false },
        )
    }
}

@Composable
private fun StorageOption(selected: Boolean, title: String, desc: String, enabled: Boolean = true, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick, enabled = enabled)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge,
                color = if (enabled || selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/**
 * Hỏi mật khẩu đồng bộ sau khi đăng nhập: tạo mới (lần đầu) hoặc nhập lại (máy mới / đã đổi ở máy khác).
 * Gọi ở gốc giao diện để hiện ở bất kỳ màn nào.
 */
@Composable
fun CloudDialogs(vm: AppViewModel) {
    val st by vm.cloud.state.collectAsState()
    val scope = rememberCoroutineScope()
    if (!st.enabled || st.email == null || st.pass == PassNeed.NONE) return
    PassphraseDialog(
        create = st.pass == PassNeed.CREATE,
        title = if (st.pass == PassNeed.CREATE) "Tạo mật khẩu đồng bộ" else "Nhập mật khẩu đồng bộ",
        busy = st.busy,
        error = st.error,
        onConfirm = { p ->
            scope.launch {
                if (st.pass == PassNeed.CREATE) vm.cloud.createPassphrase(p) else vm.cloud.enterPassphrase(p)
            }
        },
        // Không muốn tiếp tục thì quay về chỉ lưu trên máy
        onCancel = { scope.launch { vm.cloud.signOut() } },
        cancelLabel = "Chỉ lưu trên máy",
    )
}

@Composable
fun PassphraseDialog(
    create: Boolean,
    title: String,
    busy: Boolean,
    error: String?,
    onConfirm: (String) -> Unit,
    onCancel: () -> Unit,
    cancelLabel: String = "Hủy",
) {
    var p1 by remember { mutableStateOf("") }
    var p2 by remember { mutableStateOf("") }
    val localError = when {
        !create -> null
        p1.isNotEmpty() && p1.length < SYNC_PASSWORD_MIN -> "Cần ít nhất $SYNC_PASSWORD_MIN ký tự"
        p2.isNotEmpty() && p1 != p2 -> "Hai lần nhập không khớp"
        else -> null
    }
    val ready = if (create) p1.length >= SYNC_PASSWORD_MIN && p1 == p2 else p1.isNotEmpty()

    AlertDialog(
        onDismissRequest = {},
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    if (create) "Mật khẩu này dùng để mã hóa dữ liệu trước khi gửi lên mây. Máy khác đăng nhập cùng tài khoản sẽ cần mật khẩu này. " +
                        "Không thể lấy lại nếu quên, hãy ghi lại ở nơi an toàn."
                    else "Dữ liệu trên mây đã được mã hóa. Nhập mật khẩu đồng bộ bạn đã tạo trước đây.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                PasswordField("Mật khẩu đồng bộ", p1, !busy) { p1 = it }
                if (create) PasswordField("Nhập lại", p2, !busy) { p2 = it }
                (localError ?: error)?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
                if (busy) {
                    Row(Modifier.fillMaxWidth().height(40.dp), horizontalArrangement = Arrangement.Center) { CircularProgressIndicator() }
                }
            }
        },
        confirmButton = { TextButton(enabled = ready && !busy, onClick = { onConfirm(p1) }) { Text(if (create) "Tạo" else "Mở") } },
        dismissButton = { TextButton(enabled = !busy, onClick = onCancel) { Text(cancelLabel) } },
    )
}
