package vn.sotietkiem.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import vn.sotietkiem.data.Account
import vn.sotietkiem.data.Owner
import vn.sotietkiem.data.InterestPayout
import vn.sotietkiem.domain.Calc
import vn.sotietkiem.domain.Status
import vn.sotietkiem.util.Fmt

interface AccountActions {
    fun open(a: Account)
    fun edit(a: Account)
    fun delete(a: Account)
    fun renew(a: Account)
    fun close(a: Account)
    fun reopen(a: Account)
}

@Composable
fun AccountCard(a: Account, actions: AccountActions, nowMillis: Long, owners: List<Owner> = emptyList()) {
    val status = Calc.status(a)
    val left = Calc.daysLeft(a)
    var menu by remember { mutableStateOf(false) }
    var askDelete by remember { mutableStateOf(false) }

    SectionCard(modifier = Modifier.clip(RoundedCornerShape(18.dp)).clickable { actions.open(a) }) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BankLogo(a.bankCode, a.bankName, 42.dp)
            Column(Modifier.weight(1f)) {
                Text(a.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(a.bankName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    StatusChip(status)
                }
            }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, "Thao tác") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Sửa") }, leadingIcon = { Icon(Icons.Outlined.Edit, null) },
                        onClick = { menu = false; actions.edit(a) })
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
        }

        Text(Fmt.vnd(a.amount), style = MaterialTheme.typography.headlineSmall)

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Info("Lãi suất", Fmt.rate(a.interestRate) + "/năm", Modifier.weight(1f))
            Info("Kỳ hạn", Fmt.term(a.termMonths), Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Info("Lãi dự kiến", "+" + Fmt.vnd(Calc.interest(a)), Modifier.weight(1f))
            val payout = Calc.monthlyInterest(a)?.let { "${a.interestPayout.label} · ${Fmt.vnd(it)}/th" } ?: a.interestPayout.label
            Info("Lĩnh lãi", payout, Modifier.weight(1f))
        }

        LinearProgressIndicator(
            progress = { Calc.progress(a, nowMillis) },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = statusColor(status),
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            drawStopIndicator = {},
        )
        Row(Modifier.fillMaxWidth()) {
            Text("${Fmt.date(a.startDate)} → ${Fmt.date(Calc.maturity(a))}", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            Text(
                when (status) {
                    Status.CLOSED -> "Tất toán ${a.closedAt?.let { Fmt.date(it) } ?: ""}"
                    Status.MATURED -> if (left == 0L) "Đáo hạn hôm nay" else "Quá hạn ${-left} ngày"
                    else -> "Còn $left ngày"
                },
                style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = statusColor(status),
            )
        }

        if (a.owners.isNotEmpty()) OwnerShareLine(a, owners)

        if (a.note.isNotBlank()) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.AutoMirrored.Outlined.StickyNote2, null, tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.height(16.dp))
                Text(a.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        if (status == Status.MATURED) MaturedButtons(a, actions)
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
fun MaturedButtons(a: Account, actions: AccountActions) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilledTonalButton(onClick = { actions.renew(a) }, modifier = Modifier.weight(1f)) {
            Icon(Icons.Outlined.Autorenew, null); Text(" Tái tục")
        }
        OutlinedButton(onClick = { actions.close(a) }, modifier = Modifier.weight(1f)) {
            Icon(Icons.Outlined.CheckCircle, null); Text(" Tất toán")
        }
    }
}

@Composable
private fun Info(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 2)
    }
}

/** Số tiền gửi mới khi tái tục: lĩnh lãi cuối kỳ thì gốc + lãi, còn lại giữ nguyên gốc. */
fun renewAmount(a: Account): Long = if (a.interestPayout == InterestPayout.MATURITY) Calc.total(a) else a.amount

/** Dòng người sở hữu trên thẻ sổ: "Bố 100 tr · Mẹ 50 tr". */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun OwnerShareLine(a: Account, owners: List<Owner>) {
    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        a.owners.forEach { sh ->
            val name = owners.firstOrNull { it.id == sh.ownerId }?.name ?: "?"
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            OwnerAvatar(name, ownerColor(owners, sh.ownerId), 18.dp)
            Text(
                if (a.owners.size == 1) name else "$name ${Fmt.short(sh.amount)}",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            }
        }
    }
}
