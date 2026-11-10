package vn.sotietkiem.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import vn.sotietkiem.ui.components.ConfirmDialog
import vn.sotietkiem.ui.components.PatternPad
import vn.sotietkiem.util.AppLock

const val PIN_MIN = 4
const val PIN_MAX = 6

/**
 * Ô nhập mã khóa dùng chung: [pattern] = true thì vẽ hình, ngược lại là bàn phím PIN.
 * Mỗi lần nhập xong gọi [onSubmit]; ô tự xóa để nhập lại.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecretInput(
    pattern: Boolean,
    onSubmit: (String) -> Unit,
    enabled: Boolean = true,
    error: Boolean = false,
    leftKey: (@Composable () -> Unit)? = null,
    onLeft: () -> Unit = {},
) {
    if (pattern) {
        PatternPad(onComplete = { dots -> onSubmit(AppLock.encodePattern(dots)) }, enabled = enabled, error = error)
        if (leftKey != null) {
            Spacer(Modifier.height(8.dp))
            Surface(onClick = onLeft, shape = CircleShape, modifier = Modifier.size(56.dp),
                color = MaterialTheme.colorScheme.surfaceVariant) { Box(contentAlignment = Alignment.Center) { leftKey() } }
        }
        return
    }
    var pin by remember { mutableStateOf("") }
    fun send() { val p = pin; pin = ""; onSubmit(p) }
    PinDots(pin.length)
    Spacer(Modifier.height(20.dp))
    PinPad(
        onDigit = { d ->
            if (enabled && pin.length < PIN_MAX) {
                pin += d
                // Đủ 6 số thì tự gửi; mã 4–5 số bấm OK
                if (pin.length == PIN_MAX) send()
            }
        },
        onBack = { if (pin.isNotEmpty()) pin = pin.dropLast(1) },
        leftKey = leftKey,
        onLeft = onLeft,
        okEnabled = enabled && pin.length in PIN_MIN until PIN_MAX,
        onOk = { send() },
    )
}

/**
 * Màn hình khóa: nhập mã PIN hoặc vẽ hình, hoặc mở bằng vân tay/khuôn mặt nếu đã bật.
 * [onSecret] trả về true nếu đúng. [onForgot] xóa toàn bộ dữ liệu và tắt khóa.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LockScreen(
    pattern: Boolean,
    onSecret: (String) -> Boolean,
    blockedUntil: () -> Long,
    biometricAvailable: Boolean,
    onBiometric: () -> Unit,
    onForgot: () -> Unit,
) {
    var error by remember { mutableStateOf<String?>(null) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var forgot by remember { mutableStateOf(0) }
    val until = blockedUntil()
    val blocked = until > now

    LaunchedEffect(Unit) { if (biometricAvailable) onBiometric() }
    LaunchedEffect(blocked) { while (blocked) { delay(500); now = System.currentTimeMillis() } }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize().systemBarsPadding().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Outlined.Lock, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
            Spacer(Modifier.height(12.dp))
            Text("Sổ tiết kiệm", style = MaterialTheme.typography.titleLarge)
            Text(if (pattern) "Vẽ hình mở khóa" else "Nhập mã PIN để mở", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Text(
                if (blocked) "Thử lại sau ${((until - now) / 1000) + 1} giây" else error ?: " ",
                color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(12.dp))
            SecretInput(
                pattern = pattern,
                enabled = !blocked,
                error = error != null,
                leftKey = if (biometricAvailable) fingerprintKey() else null,
                onLeft = onBiometric,
                onSubmit = { secret ->
                    if (!onSecret(secret)) {
                        error = when {
                            blockedUntil() > System.currentTimeMillis() -> "Sai quá nhiều lần, thử lại sau 30 giây"
                            pattern -> "Hình vẽ không đúng"
                            else -> "Mã PIN không đúng"
                        }
                        now = System.currentTimeMillis()
                    }
                },
            )
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = { forgot = 1 }) { Text(if (pattern) "Quên hình mở khóa?" else "Quên mã PIN?") }
        }
    }

    when (forgot) {
        1 -> ConfirmDialog(
            title = "Quên mã khóa",
            text = "Không thể khôi phục mã khóa. Cách duy nhất là xóa toàn bộ dữ liệu trong app rồi nhập lại từ file JSON đã sao lưu.",
            confirm = "Xóa dữ liệu", danger = true,
            onConfirm = { forgot = 2 }, onDismiss = { if (forgot == 1) forgot = 0 },
        )
        2 -> ConfirmDialog(
            title = "Xác nhận lần cuối",
            text = "Tất cả sổ, người sở hữu và mục tiêu trên máy này sẽ bị xóa, khóa ứng dụng bị tắt và tài khoản Google (nếu có) bị đăng xuất. " +
                "Dữ liệu trên mây vẫn còn: đăng nhập lại và nhập mật khẩu đồng bộ để lấy về.",
            confirm = "Xóa vĩnh viễn", danger = true,
            onConfirm = onForgot, onDismiss = { forgot = 0 },
        )
    }
}

@Composable
fun PinDots(filled: Int, total: Int = PIN_MAX) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        repeat(total) { i ->
            val on = i < filled
            Box(
                Modifier.size(14.dp).clip(CircleShape)
                    .background(if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                    .border(1.5.dp, if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, CircleShape)
            )
        }
    }
}

/** Bàn phím số 3×4. Ô trái dưới là [leftKey] (ví dụ vân tay), ô phải dưới là xóa; nút OK nằm dưới cùng. */
@Composable
fun PinPad(
    onDigit: (String) -> Unit,
    onBack: () -> Unit,
    leftKey: (@Composable () -> Unit)?,
    onLeft: () -> Unit,
    okEnabled: Boolean,
    onOk: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9")).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) { row.forEach { d -> Key({ onDigit(d) }) { KeyText(d) } } }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            if (leftKey != null) Key(onLeft) { leftKey() } else Spacer(Modifier.size(72.dp))
            Key({ onDigit("0") }) { KeyText("0") }
            Key(onBack) { Icon(Icons.AutoMirrored.Outlined.Backspace, "Xóa") }
        }
        TextButton(onClick = onOk, enabled = okEnabled) { Text("OK", fontWeight = FontWeight.Bold) }
    }
}

private fun fingerprintKey(): @Composable () -> Unit = { Icon(Icons.Outlined.Fingerprint, "Vân tay / khuôn mặt") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Key(onClick: () -> Unit, content: @Composable () -> Unit) {
    Surface(
        onClick = onClick, shape = CircleShape, modifier = Modifier.size(72.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) { Box(contentAlignment = Alignment.Center) { content() } }
}

@Composable
private fun KeyText(d: String) = Text(d, fontSize = 26.sp, fontWeight = FontWeight.Medium)
