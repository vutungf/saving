package vn.sotietkiem.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import vn.sotietkiem.data.Account
import vn.sotietkiem.domain.Calc
import vn.sotietkiem.domain.Status
import vn.sotietkiem.domain.bankAllocation
import vn.sotietkiem.domain.cashflow12
import vn.sotietkiem.domain.summarize
import vn.sotietkiem.ui.components.AccountActions
import vn.sotietkiem.ui.components.BankDonut
import vn.sotietkiem.ui.components.BankLogo
import vn.sotietkiem.ui.components.CashflowBars
import vn.sotietkiem.ui.components.MaturedButtons
import vn.sotietkiem.ui.components.MaturityCalculator
import vn.sotietkiem.ui.components.OwnerAvatar
import vn.sotietkiem.ui.components.ownerColor
import vn.sotietkiem.data.Owner
import vn.sotietkiem.domain.OwnerScope
import vn.sotietkiem.domain.ownerAllocation
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import vn.sotietkiem.ui.components.SectionCard
import vn.sotietkiem.ui.components.StatusChip
import vn.sotietkiem.ui.theme.LocalExtra
import vn.sotietkiem.util.Fmt
import vn.sotietkiem.util.VnWords
import java.text.NumberFormat
import java.util.Locale

@Composable
fun OverviewScreen(
    allAccounts: List<Account>,
    owners: List<Owner>,
    scope: String,
    onScope: (String) -> Unit,
    nowMillis: Long,
    lastExport: Long,
    contentPadding: PaddingValues,
    actions: AccountActions,
    onAdd: () -> Unit,
    onSample: () -> Unit,
    onBackup: () -> Unit,
    unsaved: Boolean = false,
    onExport: () -> Unit = onBackup,
) {
    // Thống kê theo người sở hữu: mỗi sổ chỉ tính phần tiền của người được chọn
    val accounts = OwnerScope.apply(allAccounts, scope)
    val sum = summarize(accounts)
    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 96.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (allAccounts.isEmpty()) {
            item { EmptyState(onAdd, onSample) }
            return@LazyColumn
        }

        val needBackup = unsaved || lastExport == 0L || nowMillis - lastExport > 30L * 86_400_000
        if (needBackup) item { BackupReminder(lastExport, unsaved, onExport) }

        if (owners.isNotEmpty()) {
            item { ScopeChips(owners, allAccounts.any { it.owners.isEmpty() }, scope, onScope) }
            if (accounts.isEmpty()) {
                item {
                    Text("Chưa có sổ nào trong phạm vi này.", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                return@LazyColumn
            }
        }

        item { Hero(sum.principal, sum.openAccounts.size, sum.openAccounts.sumOf { Calc.accrued(it, nowMillis) }, sum.dailyInterest) }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Stat("Lãi dự kiến cả kỳ", "+" + Fmt.vnd(sum.expectedInterest), Modifier.weight(1f))
                Stat("Tổng nhận khi đáo hạn", Fmt.vnd(sum.totalAtMaturity), Modifier.weight(1f))
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Stat("Lãi suất bình quân", Fmt.rate(sum.weightedRate) + "/năm", Modifier.weight(1f))
                Stat("Sổ đang gửi", "${sum.runningCount} sổ", Modifier.weight(1f))
            }
        }

        val due = sum.openAccounts
            .filter { Calc.status(it) == Status.SOON || Calc.status(it) == Status.MATURED }
            .sortedBy { Calc.maturity(it) }
        item {
            SectionCard("Đến hạn trong 30 ngày tới") {
                if (due.isEmpty()) {
                    Text("Không có sổ nào sắp đáo hạn.", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                due.forEachIndexed { i, a ->
                    if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    DueRow(a, actions)
                }
            }
        }

        item { MaturityCalculator(accounts, onOpen = { actions.open(it) }) }

        if (scope == OwnerScope.ALL && owners.isNotEmpty()) {
            val bySlices = ownerAllocation(allAccounts, owners)
            if (bySlices.isNotEmpty()) item {
                val extra = LocalExtra.current
                val cols = bySlices.map { s -> if (s.bankCode == null) extra.muted else ownerColor(owners, s.bankCode) }
                SectionCard("Phân bổ theo người sở hữu") { BankDonut(bySlices, unit = "người", colors = cols) }
            }
        }

        val slices = bankAllocation(accounts)
        if (slices.isNotEmpty()) item { SectionCard("Phân bổ theo ngân hàng") { BankDonut(slices) } }

        item {
            SectionCard("Tiền về theo tháng") {
                Text("Gốc và lãi đáo hạn trong 12 tháng tới", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                CashflowBars(cashflow12(accounts))
            }
        }
    }
}

/** Chọn phạm vi thống kê: Tất cả, từng người, hoặc Chưa gán. */
@Composable
private fun ScopeChips(owners: List<Owner>, hasUnassigned: Boolean, scope: String, onScope: (String) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected = scope == OwnerScope.ALL, onClick = { onScope(OwnerScope.ALL) }, label = { Text("Tất cả") })
        owners.forEach { o ->
            FilterChip(
                selected = scope == o.id, onClick = { onScope(o.id) }, label = { Text(o.name) },
                leadingIcon = { OwnerAvatar(o.name, ownerColor(owners, o.id), 20.dp) },
            )
        }
        if (hasUnassigned) {
            FilterChip(selected = scope == OwnerScope.UNASSIGNED, onClick = { onScope(OwnerScope.UNASSIGNED) }, label = { Text("Chưa gán") })
        }
    }
}

@Composable
private fun Hero(principal: Long, count: Int, accrued: Double, daily: Double) {
    val x = LocalExtra.current
    val accruedFmt = NumberFormat.getNumberInstance(Locale("vi", "VN")).apply {
        minimumFractionDigits = 2; maximumFractionDigits = 2
    }
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(listOf(x.heroStart, x.heroEnd)))
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Tổng tiền gốc đang gửi", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelLarge)
            Text(Fmt.vnd(principal), color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("${VnWords.read(principal)} · $count sổ", color = Color.White.copy(alpha = 0.75f),
                style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1.3f)) {
                    Text("Lãi đã tích lũy đến lúc này", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall)
                    Text("+" + accruedFmt.format(accrued) + " ₫", color = Color.White, style = MaterialTheme.typography.titleMedium)
                }
                Column(Modifier.weight(1f)) {
                    Text("Lãi mỗi ngày", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall)
                    Text("+" + Fmt.vnd(daily), color = Color.White, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier, shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun DueRow(a: Account, actions: AccountActions) {
    val st = Calc.status(a)
    val left = Calc.daysLeft(a)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { actions.open(a) }.padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            BankLogo(a.bankCode, a.bankName, 36.dp)
            Column(Modifier.weight(1f)) {
                Text(a.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${Fmt.date(Calc.maturity(a))} · " + when {
                        left > 0 -> "còn $left ngày"
                        left == 0L -> "hôm nay"
                        else -> "quá ${-left} ngày"
                    },
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(Fmt.vnd(Calc.total(a)), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                StatusChip(st)
            }
        }
        if (st == Status.MATURED) MaturedButtons(a, actions)
    }
}

@Composable
private fun BackupReminder(lastExport: Long, unsaved: Boolean, onExport: () -> Unit) {
    val x = LocalExtra.current
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = x.warning.copy(alpha = 0.12f)),
    ) {
        Row(Modifier.padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.CloudUpload, null, tint = x.warning)
            Text(
                when {
                    lastExport == 0L -> "Bạn chưa sao lưu dữ liệu lần nào. Hãy xuất file JSON để không mất dữ liệu."
                    unsaved -> "Có thay đổi chưa được lưu ra file JSON."
                    else -> "Lần sao lưu gần nhất đã quá 30 ngày."
                },
                style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
            )
            TextButton(onClick = onExport) { Text("Xuất file") }
        }
    }
}

@Composable
private fun EmptyState(onAdd: () -> Unit, onSample: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Outlined.Savings, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.height(64.dp))
        Text("Chưa có sổ tiết kiệm nào", style = MaterialTheme.typography.titleLarge)
        Text(
            "Thêm sổ đầu tiên để theo dõi tiền lãi, ngày đáo hạn và thống kê tổng quan. Dữ liệu chỉ lưu trên máy này.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Button(onClick = onAdd) { Icon(Icons.Outlined.Add, null); Text(" Thêm sổ") }
        OutlinedButton(onClick = onSample) { Text("Xem thử với dữ liệu mẫu") }
    }
}
