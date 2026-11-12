package vn.sotietkiem.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import vn.sotietkiem.data.Job
import vn.sotietkiem.data.Owner
import vn.sotietkiem.domain.OwnerScope
import vn.sotietkiem.ui.components.OwnerAvatar
import vn.sotietkiem.ui.components.ownerColor
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.FilterChip
import vn.sotietkiem.domain.JobCalc
import vn.sotietkiem.ui.components.SectionCard
import vn.sotietkiem.ui.theme.LocalExtra
import vn.sotietkiem.util.Fmt
import java.time.YearMonth

/** Tab Công việc: tổng tiền chưa thanh toán và danh sách công việc với tiến độ tháng này. */
@Composable
fun JobsScreen(
    allJobs: List<Job>,
    owners: List<Owner>,
    ownerFilter: String,
    onOwnerFilter: (String) -> Unit,
    contentPadding: PaddingValues,
    onOpen: (Job) -> Unit,
    onQuickHours: (Job, Double) -> Unit,
    onAdd: () -> Unit,
) {
    val ym = YearMonth.now()
    // Lọc theo người sở hữu (người đã bị xóa thì coi như Mọi người)
    val filter = if (ownerFilter == OwnerScope.ALL || ownerFilter == OwnerScope.UNASSIGNED || owners.any { it.id == ownerFilter })
        ownerFilter else OwnerScope.ALL
    val jobs = when (filter) {
        OwnerScope.ALL -> allJobs
        OwnerScope.UNASSIGNED -> allJobs.filter { it.ownerId == null || owners.none { o -> o.id == it.ownerId } }
        else -> allJobs.filter { it.ownerId == filter }
    }
    val active = jobs.filter { !it.closed }
    val sorted = active + jobs.filter { it.closed }

    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 96.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (owners.isNotEmpty() && allJobs.isNotEmpty()) {
            item {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = filter == OwnerScope.ALL, onClick = { onOwnerFilter(OwnerScope.ALL) },
                        label = { Text("Mọi người") }, leadingIcon = { Icon(Icons.Outlined.Person, null) },
                    )
                    owners.forEach { o ->
                        val n = allJobs.count { it.ownerId == o.id }
                        FilterChip(
                            selected = filter == o.id, onClick = { onOwnerFilter(o.id) }, label = { Text("${o.name} ($n)") },
                            leadingIcon = { OwnerAvatar(o.name, ownerColor(owners, o.id), 20.dp) },
                        )
                    }
                    val none = allJobs.count { it.ownerId == null || owners.none { o -> o.id == it.ownerId } }
                    if (none > 0) {
                        FilterChip(selected = filter == OwnerScope.UNASSIGNED, onClick = { onOwnerFilter(OwnerScope.UNASSIGNED) },
                            label = { Text("Chưa gán ($none)") })
                    }
                }
            }
        }

        if (allJobs.isEmpty()) {
            item {
                Column(Modifier.fillMaxWidth().padding(top = 48.dp), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Outlined.Work, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(56.dp))
                    Text("Chưa có công việc nào", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Thêm công việc tính tiền theo giờ để ghi giờ làm, theo dõi số giờ chưa được trả và tiến độ mục tiêu mỗi tháng.",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    AssistChip(onClick = onAdd, label = { Text("Thêm công việc") })
                }
            }
            return@LazyColumn
        }
        if (jobs.isEmpty()) {
            item {
                Text("Người này chưa có công việc nào.", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 16.dp))
            }
            return@LazyColumn
        }

        item {
            SectionCard {
                Text("Chưa thanh toán", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(Fmt.vnd(active.sumOf { JobCalc.unpaidAmount(it) }), style = MaterialTheme.typography.headlineSmall)
                Text(
                    "${Fmt.hours(active.sumOf { JobCalc.unpaidHours(it) })} giờ · ${active.size} công việc đang làm · " +
                        "tháng ${ym.monthValue}: ${Fmt.hours(active.sumOf { JobCalc.monthHours(it, ym) })} giờ",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(sorted, key = { it.id }) { j -> JobCard(j, owners, ym, onOpen, onQuickHours) }
    }
}

@Composable
private fun JobCard(j: Job, owners: List<Owner>, ym: YearMonth, onOpen: (Job) -> Unit, onQuickHours: (Job, Double) -> Unit) {
    val x = LocalExtra.current
    val month = JobCalc.monthHours(j, ym)
    val pct = JobCalc.monthPercent(j, ym)
    SectionCard(modifier = Modifier.clip(RoundedCornerShape(18.dp)).clickable { onOpen(j) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(j.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    owners.firstOrNull { it.id == j.ownerId }?.let { o ->
                        OwnerAvatar(o.name, ownerColor(owners, o.id), 16.dp)
                        Text(o.name + " ·", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("${Fmt.vnd(j.rate)}/giờ" + if (j.closed) " · đã tạm dừng" else "",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(Fmt.vnd(JobCalc.unpaidAmount(j)), style = MaterialTheme.typography.titleMedium,
                    color = if (JobCalc.unpaidHours(j) > 0) x.warning else MaterialTheme.colorScheme.onSurface)
                Text("${Fmt.hours(JobCalc.unpaidHours(j))} giờ chưa trả", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        MonthProgress(j, ym, month, pct)
        if (!j.closed) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(-1.0 to "−1 giờ", -0.5 to "−0,5", 0.5 to "+0,5", 1.0 to "+1 giờ").forEach { (d, label) ->
                    AssistChip(onClick = { onQuickHours(j, d) }, label = { Text(label) }, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/** Thanh tiến độ giờ trong tháng so với mục tiêu. */
@Composable
fun MonthProgress(j: Job, ym: YearMonth, month: Double = JobCalc.monthHours(j, ym), pct: Int? = JobCalc.monthPercent(j, ym)) {
    val x = LocalExtra.current
    if (pct == null) {
        Text("Tháng ${ym.monthValue}/${ym.year}: ${Fmt.hours(month)} giờ · ${Fmt.vnd(JobCalc.monthAmount(j, ym))}",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    val color: Color = when {
        pct >= 100 -> x.positive
        month + 1e-9 >= JobCalc.expectedByToday(j, ym) -> MaterialTheme.colorScheme.primary
        else -> x.warning
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Text("Tháng ${ym.monthValue}/${ym.year}: ${Fmt.hours(month)} / ${Fmt.hours(j.monthlyTarget)} giờ",
                style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            Text("$pct%", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = color)
        }
        LinearProgressIndicator(
            progress = { (pct / 100f).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
            color = color, trackColor = MaterialTheme.colorScheme.surfaceVariant, drawStopIndicator = {},
        )
    }
}
