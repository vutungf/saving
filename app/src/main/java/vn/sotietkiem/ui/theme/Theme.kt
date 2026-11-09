package vn.sotietkiem.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import vn.sotietkiem.R

val BeVietnamPro = FontFamily(
    Font(R.font.be_vietnam_pro_regular, FontWeight.Normal),
    Font(R.font.be_vietnam_pro_medium, FontWeight.Medium),
    Font(R.font.be_vietnam_pro_semibold, FontWeight.SemiBold),
    Font(R.font.be_vietnam_pro_bold, FontWeight.Bold),
)

/** Màu ngữ nghĩa cho trạng thái sổ và biểu đồ. */
@Immutable
data class ExtraColors(
    val positive: Color,
    val warning: Color,
    val danger: Color,
    val muted: Color,
    val heroStart: Color,
    val heroEnd: Color,
    val chart: List<Color>,
)

private val LightExtra = ExtraColors(
    positive = Color(0xFF0E7C5A),
    warning = Color(0xFFB86E00),
    danger = Color(0xFFC0392B),
    muted = Color(0xFF6B7280),
    heroStart = Color(0xFF0E7C5A),
    heroEnd = Color(0xFF0A5C70),
    chart = listOf(
        Color(0xFF0E7C5A), Color(0xFF2F6FDB), Color(0xFFE08A00), Color(0xFF8E44AD),
        Color(0xFFD64561), Color(0xFF16A2B8), Color(0xFF7A8794),
    ),
)

private val DarkExtra = ExtraColors(
    positive = Color(0xFF3FC79A),
    warning = Color(0xFFF2B34B),
    danger = Color(0xFFFF7A6B),
    muted = Color(0xFF9AA3AE),
    heroStart = Color(0xFF115C46),
    heroEnd = Color(0xFF0D4352),
    chart = listOf(
        Color(0xFF3FC79A), Color(0xFF6E9BF0), Color(0xFFF2B34B), Color(0xFFB57EDC),
        Color(0xFFF07A92), Color(0xFF4CC9DB), Color(0xFF9AA3AE),
    ),
)

val LocalExtra = staticCompositionLocalOf { LightExtra }

private val LightScheme = lightColorScheme(
    primary = Color(0xFF0E7C5A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD3F0E4),
    onPrimaryContainer = Color(0xFF00382A),
    secondary = Color(0xFF2F6FDB),
    background = Color(0xFFF6F7F9),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEEF1F4),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFF1F3F5),
    onSurfaceVariant = Color(0xFF5B6470),
    outline = Color(0xFFD5DAE0),
    outlineVariant = Color(0xFFE4E8EC),
    error = Color(0xFFC0392B),
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF3FC79A),
    onPrimary = Color(0xFF00382A),
    primaryContainer = Color(0xFF0F4D3A),
    onPrimaryContainer = Color(0xFFBFEBDA),
    secondary = Color(0xFF6E9BF0),
    background = Color(0xFF0F1215),
    surface = Color(0xFF181C20),
    surfaceVariant = Color(0xFF22272C),
    surfaceContainer = Color(0xFF181C20),
    surfaceContainerHigh = Color(0xFF22272C),
    onSurfaceVariant = Color(0xFFA6AFB9),
    outline = Color(0xFF3A4148),
    outlineVariant = Color(0xFF2C3237),
    error = Color(0xFFFF7A6B),
)

private fun typography(): Typography {
    val base = Typography()
    fun TextStyle.f() = copy(fontFamily = BeVietnamPro)
    return Typography(
        displayLarge = base.displayLarge.f(), displayMedium = base.displayMedium.f(), displaySmall = base.displaySmall.f(),
        headlineLarge = base.headlineLarge.f(), headlineMedium = base.headlineMedium.f(),
        headlineSmall = base.headlineSmall.f().copy(fontWeight = FontWeight.Bold),
        titleLarge = base.titleLarge.f().copy(fontWeight = FontWeight.SemiBold),
        titleMedium = base.titleMedium.f().copy(fontWeight = FontWeight.SemiBold),
        titleSmall = base.titleSmall.f().copy(fontWeight = FontWeight.SemiBold),
        bodyLarge = base.bodyLarge.f(), bodyMedium = base.bodyMedium.f(), bodySmall = base.bodySmall.f(),
        labelLarge = base.labelLarge.f(), labelMedium = base.labelMedium.f(), labelSmall = base.labelSmall.f().copy(fontSize = 11.sp),
    )
}

@Composable
fun SoTietKiemTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    androidx.compose.runtime.CompositionLocalProvider(LocalExtra provides if (dark) DarkExtra else LightExtra) {
        MaterialTheme(
            colorScheme = if (dark) DarkScheme else LightScheme,
            typography = typography(),
            content = content,
        )
    }
}
