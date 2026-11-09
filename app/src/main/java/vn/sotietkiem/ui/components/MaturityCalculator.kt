package vn.sotietkiem.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import vn.sotietkiem.data.Account
import vn.sotietkiem.domain.Calc
import vn.sotietkiem.domain.maturedBy
import vn.sotietkiem.ui.theme.LocalExtra
import vn.sotietkiem.util.Fmt
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.TemporalAdjusters

/**
 * Chọn một ngày, app cộng tiền gốc và tiền lãi của tất cả các sổ đã đáo hạn tính đến hết ngày đó.
 * Bấm vào từng sổ trong danh sách để mở trang chi tiết.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaturityCalculator(accounts: List<Account>, onOpen: (Account) -> Unit) {
    val today = LocalDate.now()
    var date by rememberSaveable { mutableStateOf(today.with(TemporalAdjusters.lastDayOfMonth()).toString()) }
    var includeClosed by rememberSaveable { mutableStateOf(false) }
    var pick by remember { mutableStateOf(false) }
    var expanded by rememberSaveable { mutableStateOf(false) }
    val d = LocalDate.parse(date)
    val r = maturedBy(accounts, d, includeClosed)
    val x = LocalExtra.current

    SectionCard("Tính tiền về đến ngày") {
        Text("Chọn một ngày để xem tổng tiền lãi của các sổ đã đáo hạn tính đến hết ngày đó.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

        OutlinedButton(onClick = { pick = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.CalendarMonth, null); Text("  Đến hết ngày ${Fmt.date(d)}")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                "Cuối tháng" to today.with(TemporalAdjusters.lastDayOfMonth()),
                "3 tháng" to today.plusMonths(3),
                "Cuối năm" to today.with(TemporalAdjusters.lastDayOfYear()),
            ).forEach { (label, v) ->
                FilterChip(selected = d == v, onClick = { date = v.toString() }, label = { Text(label) })
            }
        }
        FilterChip(selected = includeClosed, onClick = { includeClosed = !includeClosed },
            label = { Text("Tính cả sổ đã tất toán") })

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        if (r.accounts.isEmpty()) {
            Text("Không có sổ nào đáo hạn đến ngày ${Fmt.date(d)}.", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Text("Tổng tiền lãi (${r.accounts.size} sổ)", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("+" + Fmt.vnd(r.interest), style = MaterialTheme.typography.headlineSmall, color = x.positive)
            Row(Modifier.fillMaxWidth()) {
                Text("Tiền gốc", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f))
                Text(Fmt.vnd(r.principal), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            }
            Row(Modifier.fillMaxWidth()) {
                Text("Tổng gốc + lãi", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f))
                Text(Fmt.vnd(r.total), style = MaterialTheme.typography.titleMedium)
            }

            TextButton(onClick = { expanded = !expanded }) {
                Text(if (expanded) "Ẩn danh sách sổ" else "Xem ${r.accounts.size} sổ")
            }
            if (expanded) {
                r.accounts.forEach { a ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { onOpen(a) }.padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        BankLogo(a.bankCode, a.bankName, 32.dp)
                        Column(Modifier.weight(1f)) {
                            Text(a.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("Đáo hạn ${Fmt.date(Calc.maturity(a))}" + if (a.closed) " · đã tất toán" else "",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("+" + Fmt.vnd(Calc.interest(a)), style = MaterialTheme.typography.bodyMedium, color = x.positive)
                    }
                }
            }
        }
    }

    if (pick) {
        val st = rememberDatePickerState(initialSelectedDateMillis = d.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { pick = false },
            confirmButton = {
                TextButton(onClick = {
                    st.selectedDateMillis?.let { date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString() }
                    pick = false
                }) { Text("Chọn") }
            },
            dismissButton = { TextButton(onClick = { pick = false }) { Text("Hủy") } },
        ) { DatePicker(state = st) }
    }
}
