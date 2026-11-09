package vn.sotietkiem.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import vn.sotietkiem.domain.MonthBucket
import vn.sotietkiem.domain.Slice
import vn.sotietkiem.ui.theme.LocalExtra
import vn.sotietkiem.util.Fmt
import kotlin.math.roundToInt

/** Biểu đồ tròn (dạng vành khuyên) kèm chú thích số tiền và tỷ lệ. */
@Composable
fun BankDonut(slices: List<Slice>) {
    val palette = LocalExtra.current.chart
    val total = slices.sumOf { it.amount }.coerceAtLeast(1)
    val track = MaterialTheme.colorScheme.surfaceVariant
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(Modifier.size(128.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(128.dp)) {
                val stroke = 22.dp.toPx()
                val inset = stroke / 2
                val arcSize = Size(size.width - stroke, size.height - stroke)
                drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
                var startAngle = -90f
                val gap = if (slices.size > 1) 1.5f else 0f
                slices.forEachIndexed { i, s ->
                    val sweep = 360f * s.amount / total
                    drawArc(
                        palette[i % palette.size], startAngle + gap / 2, (sweep - gap).coerceAtLeast(0.5f), false,
                        Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Butt),
                    )
                    startAngle += sweep
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${slices.size}", style = MaterialTheme.typography.titleLarge)
                Text("ngân hàng", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            slices.forEachIndexed { i, s ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Dot(palette[i % palette.size])
                    Column(Modifier.weight(1f)) {
                        Text(s.label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(Fmt.vnd(s.amount), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(pct(s.amount, total), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

private fun pct(a: Long, total: Long): String {
    val p = a * 1000.0 / total
    val r = p.roundToInt() / 10.0
    return (if (r == r.toInt().toDouble()) r.toInt().toString() else r.toString().replace('.', ',')) + "%"
}

/** Biểu đồ cột tiền về 12 tháng tới. Chạm vào cột để xem chi tiết. */
@Composable
fun CashflowBars(buckets: List<MonthBucket>) {
    val x = LocalExtra.current
    val primary = MaterialTheme.colorScheme.primary
    val interestColor = x.chart[2]
    val max = buckets.maxOf { it.total }.coerceAtLeast(1)
    var selected by remember(buckets) { mutableIntStateOf(buckets.indexOfFirst { it.total > 0 }.coerceAtLeast(0)) }
    val sel = buckets[selected]

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Tháng ${sel.month}/${sel.year}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(Fmt.vnd(sel.total), style = MaterialTheme.typography.titleMedium)
            }
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Dot(primary, 8.dp); Text("Gốc ${Fmt.short(sel.principal)}", style = MaterialTheme.typography.labelSmall)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Dot(interestColor, 8.dp); Text("Lãi ${Fmt.short(sel.interest)}", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().height(140.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            buckets.forEachIndexed { i, b ->
                val frac = b.total.toFloat() / max
                val pFrac = if (b.total > 0) b.principal.toFloat() / b.total else 0f
                Column(
                    Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(6.dp))
                        .background(if (i == selected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
                        .clickable { selected = i }
                        .padding(vertical = 2.dp),
                    verticalArrangement = Arrangement.Bottom,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val barH = 112.dp * frac
                    if (b.total > 0) {
                        Column(Modifier.width(14.dp).height(barH.coerceAtLeast(3.dp)).clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))) {
                            Box(Modifier.fillMaxWidth().weight((1f - pFrac).coerceAtLeast(0.001f)).background(interestColor))
                            Box(Modifier.fillMaxWidth().weight(pFrac.coerceAtLeast(0.001f)).background(primary))
                        }
                    } else {
                        Box(Modifier.width(14.dp).height(2.dp).background(MaterialTheme.colorScheme.outlineVariant))
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "T${b.month}", style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, maxLines = 1,
                        color = if (i == selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
