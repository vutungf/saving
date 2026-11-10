package vn.sotietkiem.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * Lưới 3×3 để vẽ hình mở khóa. Điểm đánh số 0..8 từ trái sang phải, trên xuống dưới.
 * Khi nhấc ngón tay, gọi [onComplete] với danh sách điểm đã nối. [error] tô màu đỏ cho lần vẽ vừa rồi.
 */
@Composable
fun PatternPad(
    onComplete: (List<Int>) -> Unit,
    size: Dp = 280.dp,
    error: Boolean = false,
    enabled: Boolean = true,
) {
    // Lambda mới nhất của người gọi (pointerInput chỉ chạy lại khi [enabled] đổi)
    val complete by rememberUpdatedState(onComplete)
    val selected = remember { mutableStateListOf<Int>() }
    var finger by remember { mutableStateOf<Offset?>(null) }
    var box by remember { mutableStateOf(IntSize.Zero) }
    val primary = MaterialTheme.colorScheme.primary
    val danger = MaterialTheme.colorScheme.error
    val idle = MaterialTheme.colorScheme.outline
    val lineColor = if (error) danger else primary

    fun center(i: Int): Offset {
        val cell = box.width / 3f
        return Offset(cell * (i % 3) + cell / 2, cell * (i / 3) + cell / 2)
    }

    fun hit(p: Offset): Int? {
        if (box.width == 0) return null
        val cell = box.width / 3f
        val col = (p.x / cell).toInt()
        val row = (p.y / cell).toInt()
        if (col !in 0..2 || row !in 0..2) return null
        val c = center(row * 3 + col)
        // Chỉ nhận khi ngón tay khá gần tâm điểm, để vẽ chéo không bắt nhầm điểm bên cạnh
        return if ((p - c).getDistance() < cell * 0.32f) row * 3 + col else null
    }

    fun add(i: Int) {
        if (i in selected) return
        // Nối thẳng qua một điểm ở giữa (ví dụ 0 → 2 đi qua 1) thì tự thêm điểm giữa, giống Android
        selected.lastOrNull()?.let { last ->
            val r1 = last / 3; val c1 = last % 3; val r2 = i / 3; val c2 = i % 3
            if ((abs(r1 - r2) == 2 || r1 == r2) && (abs(c1 - c2) == 2 || c1 == c2)) {
                val mid = ((r1 + r2) / 2) * 3 + (c1 + c2) / 2
                if (mid != last && mid != i && mid !in selected) selected.add(mid)
            }
        }
        selected.add(i)
    }

    Canvas(
        Modifier.size(size)
            .onSizeChanged { box = it }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectDragGestures(
                    onDragStart = { p -> selected.clear(); finger = p; hit(p)?.let(::add) },
                    onDrag = { change, _ ->
                        finger = change.position
                        hit(change.position)?.let(::add)
                    },
                    onDragEnd = {
                        val result = selected.toList()
                        finger = null
                        selected.clear()
                        if (result.isNotEmpty()) complete(result)
                    },
                    onDragCancel = { finger = null; selected.clear() },
                )
            },
    ) {
        val cell = this.size.width / 3f
        // Đường nối
        for (k in 1 until selected.size) {
            drawLine(lineColor.copy(alpha = 0.6f), center(selected[k - 1]), center(selected[k]), 8.dp.toPx(), StrokeCap.Round)
        }
        val f = finger
        if (f != null && selected.isNotEmpty()) {
            drawLine(lineColor.copy(alpha = 0.4f), center(selected.last()), f, 8.dp.toPx(), StrokeCap.Round)
        }
        // Các điểm
        for (i in 0..8) {
            val on = i in selected
            val c = Offset(cell * (i % 3) + cell / 2, cell * (i / 3) + cell / 2)
            if (on) drawCircle(lineColor.copy(alpha = 0.18f), cell * 0.28f, c)
            drawCircle(if (on) lineColor else if (error) danger else idle, if (on) 9.dp.toPx() else 6.dp.toPx(), c)
        }
    }
}
