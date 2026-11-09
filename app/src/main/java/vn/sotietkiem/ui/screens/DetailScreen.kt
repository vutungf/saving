package vn.sotietkiem.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import vn.sotietkiem.data.Account
import vn.sotietkiem.data.Owner
import vn.sotietkiem.ui.components.OwnerAvatar
import vn.sotietkiem.ui.components.ownerColor
import vn.sotietkiem.data.InterestPayout
import vn.sotietkiem.domain.Calc
import vn.sotietkiem.domain.Status
import vn.sotietkiem.ui.components.AccountActions
import vn.sotietkiem.ui.components.BankLogo
import vn.sotietkiem.ui.components.ConfirmDialog
import vn.sotietkiem.ui.components.SectionCard
import vn.sotietkiem.ui.components.StatusChip
import vn.sotietkiem.ui.components.statusColor
import vn.sotietkiem.util.Fmt
import vn.sotietkiem.util.VnWords
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToLong

/** Trang chi tiết một sổ: mở khi bấm vào thẻ sổ. Sửa, tái tục, tất toán, xóa đều làm được từ đây. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    a: Account,
    owners: List<Owner>,
    nowMillis: Long,
    actions: AccountActions,
    snack: SnackbarHostState,
    onBack: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    var askDelete by remember { mutableStateOf(false) }
    val status = Calc.status(a)
    val days = Calc.days(a)
    val left = Calc.daysLeft(a)
    val elapsedDays = (days - left).coerceIn(0, days)
    val interest = Calc.interest(a)
    val progress = Calc.progress(a, nowMillis)
    val accruedFmt = NumberFormat.getNumberInstance(Locale("vi", "VN")).apply {
        minimumFractionDigits = 2; maximumFractionDigits = 2
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(a.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại") } },
                actions = {
                    IconButton(onClick = { actions.edit(a) }) { Icon(Icons.Outlined.Edit, "Sửa") }
                    Box {
                        IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, "Thao tác khác") }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            if (!a.closed) {
                                DropdownMenuItem(text = { Text("Tái tục") }, leadingIcon = { Icon(Icons.Outlined.Autorenew, null) },
                                    onClick = { menu = false; actions.renew(a) })
                                DropdownMenuItem(text = { Text("Tất toán") }, leadingIcon = { Icon(Icons.Outlined.CheckCircle, null) },
                                    onClick = { menu = false; actions.close(a) })
                            } else {
                                DropdownMenuItem(text = { Text("Mở lại") }, leadingIcon = { Icon(Icons.Outlined.LockOpen, null) },
                                    onClick = { menu = false; actions.reopen(a) })
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Xóa", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error) },
                                onClick = { menu = false; askDelete = true },
                            )
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snack) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // ---- Khối đầu: ngân hàng, số tiền, lãi tích lũy ----
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    BankLogo(a.bankCode, a.bankName, 48.dp)
                    Column(Modifier.weight(1f)) {
                        Text(a.bankName, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(a.bankCode.takeIf { it != "OTHER" } ?: "Ngân hàng khác", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    StatusChip(status)
                }
                Text("Tiền gốc", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(Fmt.vnd(a.amount), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(VnWords.read(a.amount), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                if (!a.closed) {
                    Text("Lãi đã tích lũy đến lúc này", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("+" + accruedFmt.format(Calc.accrued(a, nowMillis)) + " ₫", style = MaterialTheme.typography.titleLarge,
                        color = statusColor(Status.ACTIVE))
                }
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = statusColor(status),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    drawStopIndicator = {},
                )
                Row(Modifier.fillMaxWidth()) {
                    Text("Đã gửi $elapsedDays/$days ngày (${(progress * 100).roundToLong()}%)",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f))
                    Text(
                        when (status) {
                            Status.CLOSED -> "Đã tất toán"
                            Status.MATURED -> if (left == 0L) "Đáo hạn hôm nay" else "Quá hạn ${-left} ngày"
                            else -> "Còn $left ngày"
                        },
                        style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = statusColor(status),
                    )
                }
            }

            // ---- Người sở hữu: phần gốc, tỷ lệ và phần lãi của từng người ----
            if (a.owners.isNotEmpty()) {
                SectionCard("Người sở hữu") {
                    a.owners.forEachIndexed { i, sh ->
                        if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        val part = a.copy(amount = sh.amount)
                        val name = owners.firstOrNull { it.id == sh.ownerId }?.name ?: "Không rõ"
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OwnerAvatar(name, ownerColor(owners, sh.ownerId), 32.dp)
                            Column(Modifier.weight(1f)) {
                                Text(name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                Text("${pct(sh.amount, a.amount)} · lãi dự kiến +${Fmt.vnd(Calc.interest(part))}",
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(Fmt.vnd(sh.amount), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // ---- Thời gian ----
            SectionCard("Thời gian") {
                InfoRow("Ngày gửi", Fmt.date(a.startDate))
                InfoRow("Kỳ hạn", Fmt.term(a.termMonths))
                InfoRow("Ngày đáo hạn", Fmt.date(Calc.maturity(a)))
                InfoRow("Số ngày gửi", "$days ngày")
                if (a.closed && a.closedAt != null) InfoRow("Ngày tất toán", Fmt.date(a.closedAt))
            }

            // ---- Lãi ----
            SectionCard("Tiền lãi") {
                InfoRow("Lãi suất", Fmt.rate(a.interestRate) + "/năm")
                InfoRow("Hình thức lĩnh lãi", a.interestPayout.label)
                InfoRow("Lãi mỗi ngày", Fmt.vnd(a.amount.toDouble() * a.interestRate / 100.0 / 365.0))
                Calc.monthlyInterest(a)?.let { InfoRow("Lãi mỗi tháng", Fmt.vnd(it)) }
                InfoRow("Lãi dự kiến cả kỳ", "+" + Fmt.vnd(interest))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                InfoRow("Tổng nhận khi đáo hạn", Fmt.vnd(a.amount + interest), bold = true)
                if (a.interestPayout == InterestPayout.UPFRONT) {
                    Text("Lĩnh lãi đầu kỳ: tiền lãi được nhận ngay khi gửi, lúc đáo hạn chỉ nhận lại tiền gốc.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // ---- Lịch nhận lãi hàng tháng ----
            if (a.interestPayout == InterestPayout.MONTHLY) {
                SectionCard("Lịch nhận lãi") {
                    val today = LocalDate.now()
                    val per = Calc.monthlyInterest(a) ?: 0L
                    val start = Calc.start(a)
                    val rows = (1..a.termMonths).map { start.plusMonths(it.toLong()) }
                    val received = rows.count { !it.isAfter(today) }
                    Text("Đã nhận $received/${a.termMonths} kỳ · ${Fmt.vnd(per * received)}",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    // Kỳ hạn dài thì chỉ hiện quanh thời điểm hiện tại cho gọn
                    val from = (received - 2).coerceAtLeast(0)
                    val shown = rows.withIndex().toList().drop(from).take(12)
                    shown.forEach { (i, d) ->
                        val done = !d.isAfter(today)
                        Row(Modifier.fillMaxWidth()) {
                            Text("Kỳ ${i + 1} · ${Fmt.date(d)}", style = MaterialTheme.typography.bodyMedium,
                                color = if (done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f))
                            Text((if (done) "✓ " else "") + Fmt.vnd(per), style = MaterialTheme.typography.bodyMedium,
                                color = if (done) statusColor(Status.ACTIVE) else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    if (from + shown.size < rows.size) {
                        Text("… và ${rows.size - from - shown.size} kỳ nữa", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            if (a.note.isNotBlank()) {
                SectionCard("Ghi chú") { Text(a.note, style = MaterialTheme.typography.bodyMedium) }
            }

            Text(
                "Tạo lúc ${stamp(a.createdAt)} · Cập nhật ${stamp(a.updatedAt)}",
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
            )

            // ---- Nút thao tác chính ----
            when {
                a.closed -> OutlinedButton(onClick = { actions.reopen(a) }, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                    Icon(Icons.Outlined.LockOpen, null); Text(" Mở lại sổ")
                }
                status == Status.MATURED || status == Status.SOON -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { actions.renew(a) }, modifier = Modifier.weight(1f).height(48.dp)) {
                        Icon(Icons.Outlined.Autorenew, null); Text(" Tái tục")
                    }
                    OutlinedButton(onClick = { actions.close(a) }, modifier = Modifier.weight(1f).height(48.dp)) {
                        Icon(Icons.Outlined.CheckCircle, null); Text(" Tất toán")
                    }
                }
                else -> OutlinedButton(onClick = { actions.edit(a) }, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                    Icon(Icons.Outlined.Edit, null); Text(" Sửa sổ")
                }
            }
            TextButton(onClick = { askDelete = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Xóa sổ này", color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (askDelete) {
        ConfirmDialog(
            title = "Xóa sổ này?",
            text = "Sổ \"${a.name}\" sẽ bị xóa vĩnh viễn khỏi máy. Bạn không thể hoàn tác.",
            confirm = "Xóa", danger = true,
            onConfirm = { actions.delete(a) }, onDismiss = { askDelete = false },
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f))
        Text(value, style = if (bold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium, textAlign = TextAlign.End)
    }
}

private fun pct(part: Long, total: Long): String {
    if (total <= 0) return "0%"
    val r = Math.round(part * 1000.0 / total) / 10.0
    return (if (r == r.toLong().toDouble()) r.toLong().toString() else r.toString().replace('.', ',')) + "%"
}

private val stampFmt = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")

private fun stamp(iso: String): String =
    runCatching { Instant.parse(iso).atZone(ZoneId.systemDefault()).format(stampFmt) }.getOrDefault(iso)

