package vn.sotietkiem.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import vn.sotietkiem.data.Account
import vn.sotietkiem.domain.Calc
import vn.sotietkiem.domain.Status
import vn.sotietkiem.ui.components.AccountActions
import vn.sotietkiem.ui.components.AccountCard
import vn.sotietkiem.util.Fmt

private enum class StatusFilter(val label: String, val status: Status?) {
    ACTIVE("Đang gửi", Status.ACTIVE),
    SOON("Sắp đáo hạn", Status.SOON),
    MATURED("Đã đáo hạn", Status.MATURED),
    CLOSED("Đã tất toán", Status.CLOSED),
    ALL("Tất cả", null),
}

private enum class SortBy(val label: String) {
    MATURITY("Đáo hạn sớm nhất"),
    AMOUNT("Số tiền lớn nhất"),
    RATE("Lãi suất cao nhất"),
    RECENT("Mới thêm gần đây"),
}

@Composable
fun AccountsScreen(
    accounts: List<Account>,
    nowMillis: Long,
    contentPadding: PaddingValues,
    actions: AccountActions,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(StatusFilter.ALL) }
    var bank by rememberSaveable { mutableStateOf<String?>(null) }
    var sort by rememberSaveable { mutableStateOf(SortBy.MATURITY) }
    var sortMenu by remember { mutableStateOf(false) }
    var bankMenu by remember { mutableStateOf(false) }

    val banks = accounts.map { it.bankName }.distinct().sorted()
    val fq = Fmt.fold(query.trim())
    val shown = accounts
        .filter { filter.status == null || Calc.status(it) == filter.status }
        .filter { bank == null || it.bankName == bank }
        .filter { fq.isEmpty() || Fmt.fold("${it.name} ${it.bankName} ${it.bankCode} ${it.note}").contains(fq) }
        .let { list ->
            when (sort) {
                SortBy.MATURITY -> list.sortedBy { Calc.maturity(it) }
                SortBy.AMOUNT -> list.sortedByDescending { it.amount }
                SortBy.RATE -> list.sortedByDescending { it.interestRate }
                SortBy.RECENT -> list.sortedByDescending { it.createdAt }
            }
        }

    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 96.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query, onValueChange = { query = it }, singleLine = true,
                    leadingIcon = { Icon(Icons.Outlined.Search, null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Outlined.Close, "Xóa") }
                    },
                    placeholder = { Text("Tìm theo tên sổ, ngân hàng, ghi chú") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusFilter.entries.forEach { f ->
                        val n = if (f.status == null) accounts.size else accounts.count { Calc.status(it) == f.status }
                        FilterChip(selected = filter == f, onClick = { filter = f }, label = { Text("${f.label} ($n)") })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box {
                        AssistChip(
                            onClick = { bankMenu = true },
                            label = { Text(bank ?: "Mọi ngân hàng") },
                            leadingIcon = { Icon(Icons.Outlined.AccountBalance, null) },
                        )
                        DropdownMenu(expanded = bankMenu, onDismissRequest = { bankMenu = false }) {
                            DropdownMenuItem(text = { Text("Mọi ngân hàng") }, onClick = { bank = null; bankMenu = false })
                            banks.forEach { b -> DropdownMenuItem(text = { Text(b) }, onClick = { bank = b; bankMenu = false }) }
                        }
                    }
                    Box {
                        AssistChip(
                            onClick = { sortMenu = true },
                            label = { Text(sort.label) },
                            leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Sort, null) },
                        )
                        DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                            SortBy.entries.forEach { s -> DropdownMenuItem(text = { Text(s.label) }, onClick = { sort = s; sortMenu = false }) }
                        }
                    }
                }
            }
        }

        if (shown.isEmpty()) {
            item {
                Text(
                    if (accounts.isEmpty()) "Chưa có sổ nào. Bấm nút + để thêm sổ." else "Không có sổ nào khớp bộ lọc.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            }
        }
        items(shown, key = { it.id }) { a -> AccountCard(a, actions, nowMillis) }
    }
}
