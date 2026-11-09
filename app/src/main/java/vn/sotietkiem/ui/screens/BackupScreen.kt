package vn.sotietkiem.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import vn.sotietkiem.data.Codec
import vn.sotietkiem.data.ImportResult
import vn.sotietkiem.ui.AppViewModel
import vn.sotietkiem.ui.components.ConfirmDialog
import vn.sotietkiem.ui.components.OwnersCard
import vn.sotietkiem.ui.components.SectionCard
import vn.sotietkiem.util.Fmt
import java.time.Instant
import java.time.ZoneId

@Composable
fun BackupScreen(vm: AppViewModel, count: Int, lastExport: Long, contentPadding: PaddingValues) {
    val ctx = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var pending by remember { mutableStateOf<ImportResult?>(null) }
    var confirmReplace by remember { mutableStateOf(false) }
    var showJson by remember { mutableStateOf<String?>(null) }
    var paste by remember { mutableStateOf(false) }
    var pasteText by remember { mutableStateOf("") }
    var clearStep by remember { mutableStateOf(0) }
    var importError by remember { mutableStateOf<String?>(null) }
    var reminders by remember { mutableStateOf(vm.repo.remindersEnabled) }

    fun toast(s: String) = Toast.makeText(ctx, s, Toast.LENGTH_SHORT).show()

    fun handleImport(text: String) {
        try {
            val r = Codec.parseImport(text)
            if (r.accounts.isEmpty()) importError = "Không tìm thấy sổ hợp lệ nào trong file" + if (r.skipped > 0) " (${r.skipped} mục bị bỏ qua)." else "."
            else pending = r
        } catch (e: IllegalArgumentException) {
            importError = e.message
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val json = Codec.export(vm.accounts.value, vm.owners.value)
        val ok = runCatching { ctx.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) } != null }.getOrDefault(false)
        if (ok) { vm.markExported(); toast("Đã xuất $count sổ") } else showJson = json
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val text = runCatching { ctx.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() } }.getOrNull()
        if (text == null) importError = "Không đọc được file." else handleImport(text)
    }
    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) toast("Chưa có quyền thông báo, app sẽ không nhắc được.")
    }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
            .padding(contentPadding).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        OwnersCard(vm)

        SectionCard("Xuất file JSON") {
            Text(
                "Lưu toàn bộ $count sổ ra file ${Codec.fileName()}. " +
                    if (lastExport == 0L) "Bạn chưa xuất file lần nào." else "Lần xuất gần nhất: ${Fmt.date(Instant.ofEpochMilli(lastExport).atZone(ZoneId.systemDefault()).toLocalDate())}.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val lastChange by vm.lastChange.collectAsState()
            if (count > 0 && lastChange > lastExport) {
                Text("Có thay đổi chưa được lưu ra file JSON.", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { exportLauncher.launch(Codec.fileName()) }, enabled = count > 0, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.FileDownload, null); Text(" Lưu file")
                }
                FilledTonalButton(
                    enabled = count > 0, modifier = Modifier.weight(1f),
                    onClick = {
                        val json = Codec.export(vm.accounts.value, vm.owners.value)
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "application/json"
                            putExtra(Intent.EXTRA_SUBJECT, Codec.fileName().removeSuffix(".json"))
                            putExtra(Intent.EXTRA_TEXT, json)
                        }
                        runCatching { ctx.startActivity(Intent.createChooser(send, "Gửi bản sao lưu")) }
                            .onSuccess { vm.markExported() }
                            .onFailure { showJson = json }
                    },
                ) { Icon(Icons.Outlined.Share, null); Text(" Chia sẻ") }
            }
            TextButton(enabled = count > 0, onClick = { showJson = Codec.export(vm.accounts.value, vm.owners.value) }) { Text("Xem nội dung JSON để sao chép") }
        }

        SectionCard("Nhập file JSON") {
            Text(
                "Chọn file đã xuất trước đó, hoặc dán nội dung JSON. Các mục không hợp lệ sẽ được bỏ qua.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { importLauncher.launch(arrayOf("application/json", "text/*", "application/octet-stream")) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.FileUpload, null); Text(" Chọn file")
                }
                FilledTonalButton(onClick = { pasteText = ""; paste = true }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.ContentPaste, null); Text(" Dán JSON")
                }
            }
        }

        SectionCard("Nhắc đáo hạn") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Gửi thông báo trước 3 ngày và vào ngày đáo hạn", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Switch(checked = reminders, onCheckedChange = { on ->
                    reminders = on; vm.repo.remindersEnabled = on
                    if (on && Build.VERSION.SDK_INT >= 33 &&
                        ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                    ) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                })
            }
        }

        SectionCard("Dữ liệu") {
            Text(
                "Dữ liệu chỉ nằm trên điện thoại này. Gỡ app hoặc xóa dữ liệu app thì các sổ sẽ mất, nên hãy xuất file định kỳ.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(onClick = { vm.loadSample(); toast("Đã thêm 5 sổ mẫu") }, modifier = Modifier.fillMaxWidth()) {
                Text("Xem thử với dữ liệu mẫu")
            }
            OutlinedButton(onClick = { clearStep = 1 }, enabled = count > 0, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.DeleteForever, null, tint = MaterialTheme.colorScheme.error)
                Text(" Xóa toàn bộ dữ liệu", color = MaterialTheme.colorScheme.error)
            }
        }
    }

    // ---- Hộp thoại ----
    pending?.let { r ->
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text("Nhập ${r.accounts.size} sổ") },
            text = {
                Text(
                    (if (r.skipped > 0) "${r.skipped} mục không hợp lệ đã bị bỏ qua.\n\n" else "") +
                        "Gộp: thêm vào dữ liệu hiện tại, sổ trùng ID sẽ được bỏ qua.\nThay thế: xóa $count sổ hiện có và dùng dữ liệu trong file."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val n = vm.importMerge(r.accounts, r.owners); pending = null
                    toast("Đã thêm $n sổ" + if (n < r.accounts.size) ", bỏ qua ${r.accounts.size - n} sổ trùng" else "")
                }) { Text("Gộp vào hiện tại") }
            },
            dismissButton = {
                TextButton(onClick = { confirmReplace = true }) { Text("Thay thế toàn bộ", color = MaterialTheme.colorScheme.error) }
            },
        )
    }
    if (confirmReplace) {
        val r = pending
        ConfirmDialog(
            title = "Thay thế toàn bộ?",
            text = "$count sổ hiện có sẽ bị xóa và thay bằng ${r?.accounts?.size ?: 0} sổ trong file.",
            confirm = "Thay thế", danger = true,
            onConfirm = { r?.let { vm.importReplace(it.accounts, it.owners); toast("Đã thay thế dữ liệu") }; pending = null },
            onDismiss = { confirmReplace = false },
        )
    }
    importError?.let { msg ->
        AlertDialog(
            onDismissRequest = { importError = null },
            title = { Text("Không nhập được") },
            text = { Text(msg) },
            confirmButton = { TextButton(onClick = { importError = null }) { Text("Đóng") } },
        )
    }
    if (paste) {
        AlertDialog(
            onDismissRequest = { paste = false },
            title = { Text("Dán nội dung JSON") },
            text = {
                OutlinedTextField(
                    value = pasteText, onValueChange = { pasteText = it },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp, max = 320.dp),
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    placeholder = { Text("{ \"accounts\": [ ... ] }") },
                )
            },
            confirmButton = {
                TextButton(enabled = pasteText.isNotBlank(), onClick = { paste = false; handleImport(pasteText) }) { Text("Kiểm tra") }
            },
            dismissButton = { TextButton(onClick = { paste = false }) { Text("Hủy") } },
        )
    }
    showJson?.let { json ->
        AlertDialog(
            onDismissRequest = { showJson = null },
            title = { Text("Nội dung JSON") },
            text = {
                SelectionContainer {
                    Text(
                        json, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        modifier = Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    clipboard.setText(AnnotatedString(json)); vm.markExported(); toast("Đã sao chép"); showJson = null
                }) { Text("Sao chép") }
            },
            dismissButton = { TextButton(onClick = { showJson = null }) { Text("Đóng") } },
        )
    }
    when (clearStep) {
        1 -> ConfirmDialog(
            title = "Xóa toàn bộ dữ liệu?", text = "Tất cả $count sổ sẽ bị xóa khỏi máy này.",
            confirm = "Tiếp tục", danger = true, onConfirm = { clearStep = 2 }, onDismiss = { if (clearStep == 1) clearStep = 0 },
        )
        2 -> ConfirmDialog(
            title = "Xác nhận lần cuối",
            text = "Thao tác này không thể hoàn tác. Bạn đã xuất file sao lưu chưa?",
            confirm = "Xóa vĩnh viễn", danger = true,
            onConfirm = { vm.clearAll(); toast("Đã xóa toàn bộ dữ liệu") }, onDismiss = { clearStep = 0 },
        )
    }
}
