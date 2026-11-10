package vn.sotietkiem.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Gesture
import androidx.compose.material.icons.outlined.Dialpad
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import vn.sotietkiem.ui.screens.SecretInput
import vn.sotietkiem.util.AppLock

private enum class Flow { NONE, ENABLE, CHANGE, DISABLE }

/** Thẻ "Khóa ứng dụng" trong tab Cài đặt: bật/tắt, đổi kiểu và mã khóa, vân tay, thời gian tự khóa. */
@Composable
fun LockSettingsCard(lock: AppLock, biometricAvailable: Boolean) {
    var enabled by remember { mutableStateOf(lock.enabled) }
    var type by remember { mutableStateOf(lock.type) }
    var bio by remember { mutableStateOf(lock.biometric) }
    var delay by remember { mutableIntStateOf(lock.delaySeconds) }
    var flow by remember { mutableStateOf(Flow.NONE) }

    SectionCard("Khóa ứng dụng") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Khóa ứng dụng", style = MaterialTheme.typography.bodyLarge)
                Text(
                    if (!enabled) "Đang tắt"
                    else if (type == AppLock.TYPE_PATTERN) "Đang dùng hình vẽ" else "Đang dùng mã PIN",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = enabled, onCheckedChange = { flow = if (it) Flow.ENABLE else Flow.DISABLE })
        }
        if (enabled) {
            TextButton(onClick = { flow = Flow.CHANGE }) { Text("Đổi kiểu hoặc mã khóa") }
            if (biometricAvailable) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Mở khóa bằng vân tay / khuôn mặt", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Switch(checked = bio, onCheckedChange = { bio = it; lock.biometric = it })
                }
            }
            Text("Tự khóa khi rời app", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0 to "Ngay lập tức", 60 to "Sau 1 phút", 300 to "Sau 5 phút").forEach { (sec, label) ->
                    FilterChip(selected = delay == sec, onClick = { delay = sec; lock.delaySeconds = sec }, label = { Text(label) })
                }
            }
        }
    }

    if (flow != Flow.NONE) {
        LockSetupDialog(
            lock = lock,
            verifyFirst = flow != Flow.ENABLE,
            setNew = flow != Flow.DISABLE,
            onDone = { newType, secret ->
                if (flow == Flow.DISABLE) {
                    lock.disable(); enabled = false; bio = false; delay = 0
                } else {
                    lock.setSecret(newType, secret); enabled = true; type = newType
                }
                flow = Flow.NONE
            },
            onCancel = { flow = Flow.NONE },
        )
    }
}

private enum class Step { VERIFY, CHOOSE, NEW1, NEW2 }

/**
 * Trình tự toàn màn hình: (xác nhận mã hiện tại) → (chọn kiểu PIN / hình vẽ) → nhập mã mới → nhập lại.
 * Tắt khóa thì chỉ cần bước xác nhận.
 */
@Composable
private fun LockSetupDialog(
    lock: AppLock,
    verifyFirst: Boolean,
    setNew: Boolean,
    onDone: (type: String, secret: String) -> Unit,
    onCancel: () -> Unit,
) {
    var step by remember { mutableStateOf(if (verifyFirst) Step.VERIFY else Step.CHOOSE) }
    var newType by remember { mutableStateOf(AppLock.TYPE_PIN) }
    var first by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var attempt by remember { mutableIntStateOf(0) } // đổi key để xóa ô nhập sau mỗi lần

    val pattern = if (step == Step.VERIFY) lock.type == AppLock.TYPE_PATTERN else newType == AppLock.TYPE_PATTERN

    Dialog(onDismissRequest = onCancel, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                Modifier.fillMaxSize().systemBarsPadding().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onCancel) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Hủy") }
                }
                Spacer(Modifier.height(24.dp))
                Text(
                    when (step) {
                        Step.VERIFY -> if (pattern) "Vẽ hình mở khóa hiện tại" else "Nhập mã PIN hiện tại"
                        Step.CHOOSE -> "Chọn kiểu khóa"
                        Step.NEW1 -> if (pattern) "Vẽ hình mở khóa mới" else "Nhập mã PIN mới"
                        Step.NEW2 -> if (pattern) "Vẽ lại hình để xác nhận" else "Nhập lại mã PIN để xác nhận"
                    },
                    style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center,
                )
                Text(
                    message ?: when (step) {
                        Step.NEW1 -> if (pattern) "Nối ít nhất ${AppLock.PATTERN_MIN} điểm" else "Từ 4 đến 6 chữ số"
                        Step.CHOOSE -> "Hãy nhớ kỹ. Nếu quên, chỉ có thể xóa toàn bộ dữ liệu để mở lại app."
                        else -> " "
                    },
                    style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center,
                    color = if (message != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                Spacer(Modifier.height(16.dp))

                if (step == Step.CHOOSE) {
                    ListItem(
                        headlineContent = { Text("Mã PIN") },
                        supportingContent = { Text("4 đến 6 chữ số") },
                        leadingContent = { Icon(Icons.Outlined.Dialpad, null) },
                        modifier = Modifier.clickable { newType = AppLock.TYPE_PIN; step = Step.NEW1; message = null },
                    )
                    ListItem(
                        headlineContent = { Text("Hình vẽ") },
                        supportingContent = { Text("Nối các điểm trên lưới 3×3") },
                        leadingContent = { Icon(Icons.Outlined.Gesture, null) },
                        modifier = Modifier.clickable { newType = AppLock.TYPE_PATTERN; step = Step.NEW1; message = null },
                    )
                } else {
                    val blocked = step == Step.VERIFY && lock.blockedUntil > 0
                    key(step, attempt) {
                        SecretInput(
                            pattern = pattern,
                            enabled = !blocked,
                            error = message != null,
                            onSubmit = { s ->
                                attempt++
                                when (step) {
                                    Step.VERIFY -> when {
                                        lock.blockedUntil > 0 -> message = "Sai quá nhiều lần, thử lại sau 30 giây"
                                        !lock.check(s) -> message = if (pattern) "Hình vẽ không đúng" else "Mã PIN không đúng"
                                        !setNew -> onDone(lock.type, "")
                                        else -> { step = Step.CHOOSE; message = null }
                                    }
                                    Step.NEW1 -> when {
                                        pattern && s.split('-').size < AppLock.PATTERN_MIN ->
                                            message = "Cần nối ít nhất ${AppLock.PATTERN_MIN} điểm, hãy vẽ lại"
                                        else -> { first = s; step = Step.NEW2; message = null }
                                    }
                                    Step.NEW2 -> if (s == first) onDone(newType, s) else {
                                        message = if (pattern) "Hình không khớp, hãy vẽ lại từ đầu" else "Mã không khớp, hãy nhập lại từ đầu"
                                        first = ""; step = Step.NEW1
                                    }
                                    Step.CHOOSE -> {}
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}
