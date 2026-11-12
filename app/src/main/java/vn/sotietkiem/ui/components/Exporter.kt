package vn.sotietkiem.ui.components

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vn.sotietkiem.data.Codec
import vn.sotietkiem.ui.AppViewModel
import vn.sotietkiem.util.AppLock

enum class ExportTarget { FILE, SHARE, SHOW }

/** Tay cầm để mở luồng xuất file từ bất kỳ nút nào. */
class Exporter(private val open: (ExportTarget) -> Unit) {
    fun start(target: ExportTarget) = open(target)
}

const val EXPORT_PASSWORD_MIN = 6

/**
 * Luồng xuất sao lưu: hỏi có mã hóa bằng mật khẩu không (mặc định có), rồi lưu file / chia sẻ / hiện nội dung.
 * Gọi hàm này ở nơi muốn hiện các hộp thoại; dùng [Exporter.start] để bắt đầu.
 */
@Composable
fun rememberExporter(vm: AppViewModel, toast: (String) -> Unit): Exporter {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var target by remember { mutableStateOf<ExportTarget?>(null) }
    var pending by remember { mutableStateOf<String?>(null) }
    var shown by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        val content = pending
        pending = null
        if (uri == null || content == null) return@rememberLauncherForActivityResult
        val ok = runCatching { ctx.contentResolver.openOutputStream(uri)?.use { it.write(content.toByteArray()) } != null }
            .getOrDefault(false)
        if (ok) { vm.markExported(); toast("Đã xuất file sao lưu") } else shown = content
    }

    fun deliver(t: ExportTarget, content: String) {
        when (t) {
            ExportTarget.FILE -> {
                pending = content
                AppLock.skipNext = true
                fileLauncher.launch(Codec.fileName())
            }
            ExportTarget.SHARE -> {
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_SUBJECT, Codec.fileName().removeSuffix(".json"))
                    putExtra(Intent.EXTRA_TEXT, content)
                }
                AppLock.skipNext = true
                runCatching { ctx.startActivity(Intent.createChooser(send, "Gửi bản sao lưu")) }
                    .onSuccess { vm.markExported() }
                    .onFailure { shown = content }
            }
            ExportTarget.SHOW -> shown = content
        }
    }

    target?.let { t ->
        ExportPasswordDialog(
            busy = busy,
            onDismiss = { if (!busy) target = null },
            onConfirm = { password ->
                busy = true
                scope.launch {
                    val plain = Codec.export(vm.accounts.value, vm.owners.value, vm.jobs.value)
                    // Suy khóa từ mật khẩu mất khoảng 1 giây, chạy nền để không đứng màn hình
                    val content = if (password == null) plain else withContext(Dispatchers.Default) { Codec.encryptExport(plain, password) }
                    busy = false
                    target = null
                    deliver(t, content)
                }
            },
        )
    }

    shown?.let { content ->
        val clipboard = LocalClipboardManager.current
        AlertDialog(
            onDismissRequest = { shown = null },
            title = { Text("Nội dung sao lưu") },
            text = {
                SelectionContainer {
                    Text(
                        content, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        modifier = Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    clipboard.setText(AnnotatedString(content)); vm.markExported(); toast("Đã sao chép"); shown = null
                }) { Text("Sao chép") }
            },
            dismissButton = { TextButton(onClick = { shown = null }) { Text("Đóng") } },
        )
    }

    return remember { Exporter { target = it } }
}

/** Hỏi mật khẩu cho file xuất. Tắt mã hóa thì [onConfirm] nhận null. */
@Composable
private fun ExportPasswordDialog(busy: Boolean, onDismiss: () -> Unit, onConfirm: (String?) -> Unit) {
    var encrypt by remember { mutableStateOf(true) }
    var p1 by remember { mutableStateOf("") }
    var p2 by remember { mutableStateOf("") }
    val error = when {
        !encrypt -> null
        p1.isNotEmpty() && p1.length < EXPORT_PASSWORD_MIN -> "Mật khẩu cần ít nhất $EXPORT_PASSWORD_MIN ký tự"
        p2.isNotEmpty() && p1 != p2 -> "Hai lần nhập không khớp"
        else -> null
    }
    val ready = !encrypt || (p1.length >= EXPORT_PASSWORD_MIN && p1 == p2)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Xuất sao lưu") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Mã hóa bằng mật khẩu", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Switch(checked = encrypt, onCheckedChange = { encrypt = it }, enabled = !busy)
                }
                if (encrypt) {
                    PasswordField("Mật khẩu", p1, !busy) { p1 = it }
                    PasswordField("Nhập lại mật khẩu", p2, !busy) { p2 = it }
                    Text(
                        "Cần đúng mật khẩu này để nhập lại file. Quên mật khẩu thì không mở được file.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        "File không mã hóa: ai có file cũng đọc được toàn bộ số tiền và ghi chú.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error,
                    )
                }
                if (error != null) Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                if (busy) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
            }
        },
        confirmButton = {
            TextButton(enabled = ready && !busy, onClick = { onConfirm(if (encrypt) p1 else null) }) { Text("Xuất") }
        },
        dismissButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text("Hủy") } },
    )
}

@Composable
fun PasswordField(label: String, value: String, enabled: Boolean = true, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = { onChange(it.take(64)) }, singleLine = true, enabled = enabled,
        label = { Text(label) },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Hỏi mật khẩu để mở file sao lưu đã mã hóa. */
@Composable
fun ImportPasswordDialog(busy: Boolean, error: String?, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var p by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("File đã mã hóa") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Nhập mật khẩu đã dùng khi xuất file.", style = MaterialTheme.typography.bodyMedium)
                PasswordField("Mật khẩu", p, !busy) { p = it }
                if (error != null) Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                if (busy) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
            }
        },
        confirmButton = { TextButton(enabled = p.isNotEmpty() && !busy, onClick = { onConfirm(p) }) { Text("Mở") } },
        dismissButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text("Hủy") } },
    )
}
