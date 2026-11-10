package vn.sotietkiem.ui.screens

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import vn.sotietkiem.ui.AppViewModel

/** Màn chào ở lần mở đầu tiên: chọn lưu trên máy hoặc đăng nhập Google để đồng bộ. */
@Composable
fun WelcomeScreen(vm: AppViewModel) {
    val st by vm.cloud.state.collectAsState()
    val activity = LocalContext.current as Activity
    val scope = rememberCoroutineScope()

    // Đăng nhập xong (và đã có hoặc tạo mật khẩu đồng bộ) thì vào app
    LaunchedEffect(st.email) { if (st.enabled && st.email != null) vm.chooseStorage() }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(32.dp))
            Icon(Icons.Outlined.Savings, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(56.dp))
            Text("Sổ tiết kiệm", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Bạn muốn lưu dữ liệu ở đâu? Có thể đổi lại bất cứ lúc nào trong Cài đặt.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Choice(
                icon = Icons.Outlined.PhoneAndroid,
                title = "Lưu trên máy này",
                desc = "Không cần tài khoản. Dữ liệu được mã hóa và chỉ nằm trên điện thoại. Chuyển máy bằng file sao lưu.",
                enabled = !st.busy,
                onClick = { vm.chooseStorage() },
            )
            Choice(
                icon = Icons.Outlined.Cloud,
                title = "Đăng nhập Google để đồng bộ",
                desc = if (st.available) "Tự lưu lên mây, dùng trên nhiều điện thoại. Dữ liệu được mã hóa bằng mật khẩu riêng của bạn trước khi gửi lên."
                else "Chưa dùng được: bản app này chưa được cấu hình Firebase.",
                enabled = st.available && !st.busy,
                onClick = { scope.launch { vm.cloud.signIn(activity) } },
            )
            if (st.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            st.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Choice(icon: ImageVector, title: String, desc: String, enabled: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Icon(icon, null, tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
