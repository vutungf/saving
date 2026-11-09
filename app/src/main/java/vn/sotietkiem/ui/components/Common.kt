package vn.sotietkiem.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import vn.sotietkiem.data.Banks
import vn.sotietkiem.domain.Status
import vn.sotietkiem.ui.theme.LocalExtra
import vn.sotietkiem.util.Fmt

private val logoCache = HashMap<String, ImageBitmap?>()

/** Logo ngân hàng từ assets; ngân hàng không có logo hiện huy hiệu chữ cái có màu. */
@Composable
fun BankLogo(code: String, name: String, size: Dp = 40.dp) {
    val ctx = LocalContext.current
    val bank = Banks.find(code)
    val bmp = remember(code) {
        val b = bank
        if (b == null || !b.hasLogo) null else synchronized(logoCache) {
            logoCache.getOrPut(b.code) {
                runCatching {
                    ctx.assets.open("banks/${b.code}.webp").use { BitmapFactory.decodeStream(it)?.asImageBitmap() }
                }.getOrNull()
            }
        }
    }
    val shape = RoundedCornerShape(size * 0.28f)
    if (bmp != null) {
        Box(
            Modifier.size(size).clip(shape).background(Color.White)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape),
            contentAlignment = Alignment.Center,
        ) {
            Image(bmp, contentDescription = name, contentScale = ContentScale.Fit, modifier = Modifier.padding(size * 0.08f))
        }
    } else {
        val palette = LocalExtra.current.chart
        val c = palette[Math.floorMod(name.lowercase().hashCode(), palette.size)]
        Box(Modifier.size(size).clip(shape).background(c), contentAlignment = Alignment.Center) {
            Text(initials(name), color = Color.White, style = centeredLetter(size.value * 0.36f))
        }
    }
}

private fun initials(name: String): String {
    val words = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (words.isEmpty()) return "?"
    if (words.size == 1) return words[0].take(2).uppercase()
    return (words[0].take(1) + words[1].take(1)).uppercase()
}

@Composable
fun statusColor(s: Status): Color {
    val x = LocalExtra.current
    return when (s) {
        Status.ACTIVE -> x.positive
        Status.SOON -> x.warning
        Status.MATURED -> x.danger
        Status.CLOSED -> x.muted
    }
}

@Composable
fun StatusChip(s: Status) {
    val c = statusColor(s)
    Box(
        Modifier.clip(RoundedCornerShape(50)).background(c.copy(alpha = 0.14f)).padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(s.label, color = c, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun SectionCard(
    title: String? = null,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (title != null) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    trailing?.invoke()
                }
            }
            content()
        }
    }
}

@Composable
fun Dot(color: Color, size: Dp = 10.dp) = Box(Modifier.size(size).clip(CircleShape).background(color))

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirm: String,
    danger: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = { onConfirm(); onDismiss() }) {
                Text(confirm, color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } },
    )
}

/** Hiển thị chữ số dạng 150.000.000 khi gõ; giá trị thật chỉ gồm chữ số. */
object ThousandsTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        if (raw.isEmpty()) return TransformedText(text, OffsetMapping.Identity)
        val out = Fmt.group(raw.toLongOrNull() ?: 0L).let { if (raw.toLongOrNull() == null) raw else it }
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val dotsBefore = if (offset == 0) 0 else (raw.length - 1) / 3 - (raw.length - offset) / 3
                return (offset + dotsBefore).coerceIn(0, out.length)
            }
            override fun transformedToOriginal(offset: Int): Int {
                var digits = 0
                for (i in 0 until offset.coerceAtMost(out.length)) if (out[i] != '.') digits++
                return digits.coerceIn(0, raw.length)
            }
        }
        return TransformedText(AnnotatedString(out), mapping)
    }
}

/**
 * Kiểu chữ cho chữ cái nằm giữa hình tròn/vuông nhỏ. Be Vietnam Pro chừa nhiều khoảng phía trên cho dấu
 * tiếng Việt nên chữ bị lệch xuống; dùng font hệ thống, bỏ font padding và cắt dòng sát chữ để căn giữa.
 */
fun centeredLetter(sizeSp: Float) = androidx.compose.ui.text.TextStyle(
    fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
    fontWeight = FontWeight.Bold,
    fontSize = sizeSp.sp,
    lineHeight = sizeSp.sp,
    platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = androidx.compose.ui.text.style.LineHeightStyle(
        alignment = androidx.compose.ui.text.style.LineHeightStyle.Alignment.Center,
        trim = androidx.compose.ui.text.style.LineHeightStyle.Trim.Both,
    ),
)
