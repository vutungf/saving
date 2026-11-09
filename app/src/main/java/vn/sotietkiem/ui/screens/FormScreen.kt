package vn.sotietkiem.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import vn.sotietkiem.data.Account
import vn.sotietkiem.data.Bank
import vn.sotietkiem.data.BankGroup
import vn.sotietkiem.data.Banks
import vn.sotietkiem.data.InterestPayout
import vn.sotietkiem.domain.Calc
import vn.sotietkiem.ui.components.BankLogo
import vn.sotietkiem.ui.components.SectionCard
import vn.sotietkiem.ui.components.ThousandsTransformation
import vn.sotietkiem.ui.components.renewAmount
import vn.sotietkiem.ui.components.OwnerAvatar
import vn.sotietkiem.ui.components.OwnerNameDialog
import vn.sotietkiem.ui.components.ownerColor
import vn.sotietkiem.data.Owner
import vn.sotietkiem.data.OwnerShare
import vn.sotietkiem.domain.scaleShares
import vn.sotietkiem.domain.splitEven
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.AssistChip
import vn.sotietkiem.util.Fmt
import vn.sotietkiem.util.VnWords
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

private val QUICK_TERMS = listOf(1, 3, 6, 9, 12, 18, 24, 36)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FormScreen(
    editing: Account?,
    renewFrom: Account?,
    suggestedName: String = "",
    owners: List<Owner> = emptyList(),
    onAddOwner: (String) -> Owner = { Owner("", it) },
    onSave: (Account, String?) -> Unit,
    onBack: () -> Unit,
) {
    val base = editing ?: renewFrom
    var name by rememberSaveable { mutableStateOf(base?.name ?: suggestedName) }  // sổ mới: tên gợi ý, sửa được
    var bankCode by rememberSaveable { mutableStateOf(base?.bankCode.orEmpty()) }
    var bankName by rememberSaveable { mutableStateOf(base?.bankName.orEmpty()) }
    var amount by rememberSaveable {
        mutableStateOf((if (renewFrom != null) renewAmount(renewFrom) else editing?.amount)?.toString().orEmpty())
    }
    var start by rememberSaveable {
        mutableStateOf(
            when {
                renewFrom != null -> Calc.maturity(renewFrom).toString()
                editing != null -> editing.startDate
                else -> LocalDate.now().toString()
            }
        )
    }
    var term by rememberSaveable { mutableStateOf(base?.termMonths?.toString() ?: "6") }
    var rate by rememberSaveable { mutableStateOf(base?.interestRate?.let { Fmt.rate(it).removeSuffix("%") }.orEmpty()) }
    var payout by rememberSaveable { mutableStateOf(base?.interestPayout ?: InterestPayout.MATURITY) }
    var note by rememberSaveable { mutableStateOf(if (renewFrom != null) renewFrom.note else editing?.note.orEmpty()) }
    // Phần tiền từng người sở hữu, lưu dạng "id=sốtiền;id=sốtiền" để giữ được khi xoay màn hình.
    var sharesRaw by rememberSaveable {
        mutableStateOf(
            encodeShares(
                when {
                    renewFrom != null -> scaleShares(renewFrom.owners, renewFrom.amount, renewAmount(renewFrom))
                    editing != null -> editing.owners
                    owners.size == 1 -> listOf(OwnerShare(owners[0].id, 0))
                    else -> emptyList()
                }
            )
        )
    }
    var addingOwner by remember { mutableStateOf(false) }
    var submitted by rememberSaveable { mutableStateOf(false) }
    var touched by remember { mutableStateOf(setOf<String>()) }
    var showBanks by remember { mutableStateOf(false) }
    var showDate by remember { mutableStateOf(false) }

    // ---- Kiểm tra dữ liệu ----
    val amountVal = Fmt.parseAmount(amount)
    val termVal = term.toIntOrNull()
    val rateVal = Fmt.parseRate(rate)
    val startVal = runCatching { LocalDate.parse(start) }.getOrNull()
    val errors = buildMap {
        if (name.isBlank()) put("name", "Vui lòng nhập tên sổ")
        if (bankName.isBlank()) put("bank", "Vui lòng chọn ngân hàng")
        if (amountVal == null || amountVal <= 0) put("amount", "Số tiền phải lớn hơn 0")
        if (startVal == null) put("start", "Ngày gửi không hợp lệ")
        if (termVal == null || termVal !in 1..600) put("term", "Kỳ hạn từ 1 đến 600 tháng")
        if (rateVal == null || rateVal < 0 || rateVal > 100) put("rate", "Lãi suất từ 0 đến 100%/năm")
        val sel = decodeShares(sharesRaw)
        if (sel.size >= 2 && amountVal != null && amountVal > 0) {
            if (sel.any { it.amount <= 0 }) put("owners", "Nhập số tiền cho từng người")
            else if (sel.sumOf { it.amount } != amountVal) put("owners", "Tổng phần của từng người phải bằng số tiền gửi")
        }
    }
    fun err(k: String) = if (submitted || k in touched) errors[k] else null
    fun touch(k: String) { if (k !in touched) touched = touched + k }

    val title = when {
        renewFrom != null -> "Tái tục sổ"
        editing != null -> "Sửa sổ"
        else -> "Thêm sổ mới"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại") } },
            )
        },
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (renewFrom != null) {
                Text(
                    "Sổ \"${renewFrom.name}\" sẽ chuyển sang Đã tất toán sau khi lưu.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            OutlinedTextField(
                value = name, onValueChange = { name = it.take(80); touch("name") },
                label = { Text("Tên sổ") }, placeholder = { Text("Ví dụ: Quỹ mua nhà") }, singleLine = true,
                isError = err("name") != null, supportingText = { Text(err("name") ?: "${name.length}/80") },
                modifier = Modifier.fillMaxWidth(),
            )

            // Ngân hàng
            OutlinedTextField(
                value = bankName, onValueChange = {}, readOnly = true, enabled = false,
                label = { Text("Ngân hàng") }, placeholder = { Text("Chọn ngân hàng") },
                leadingIcon = logoOrNull(bankCode, bankName),
                trailingIcon = { Icon(Icons.Outlined.ExpandMore, null) },
                isError = err("bank") != null, supportingText = textOrNull(err("bank")),
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = if (err("bank") != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledSupportingTextColor = MaterialTheme.colorScheme.error,
                ),
                modifier = Modifier.fillMaxWidth().clickable { showBanks = true; touch("bank") },
            )

            OutlinedTextField(
                value = amount,
                onValueChange = { v -> amount = v.filter { it.isDigit() }.trimStart('0').take(15); touch("amount") },
                label = { Text("Số tiền gửi (VND)") }, singleLine = true,
                visualTransformation = ThousandsTransformation,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                suffix = { Text("₫") },
                isError = err("amount") != null,
                supportingText = { Text(err("amount") ?: amountVal?.takeIf { it > 0 }?.let { VnWords.read(it) } ?: "") },
                modifier = Modifier.fillMaxWidth(),
            )

            OwnerSharesEditor(
                owners = owners,
                raw = sharesRaw,
                total = amountVal ?: 0L,
                error = if (submitted || "amount" in touched) errors["owners"] else null,
                onChange = { sharesRaw = it },
                onAddOwner = { addingOwner = true },
            )

            OutlinedTextField(
                value = startVal?.let { Fmt.date(it) } ?: start, onValueChange = {}, readOnly = true, enabled = false,
                label = { Text("Ngày gửi") },
                trailingIcon = { Icon(Icons.Outlined.CalendarMonth, null) },
                isError = err("start") != null, supportingText = textOrNull(err("start")),
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                modifier = Modifier.fillMaxWidth().clickable { showDate = true },
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Kỳ hạn", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QUICK_TERMS.forEach { m ->
                        FilterChip(selected = termVal == m, onClick = { term = m.toString(); touch("term") }, label = { Text("$m th") })
                    }
                }
                OutlinedTextField(
                    value = term, onValueChange = { v -> term = v.filter { it.isDigit() }.take(3); touch("term") },
                    label = { Text("Số tháng (1–600)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    suffix = { Text("tháng") },
                    isError = err("term") != null, supportingText = textOrNull(err("term")),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            OutlinedTextField(
                value = rate,
                onValueChange = { v -> rate = v.filter { it.isDigit() || it == ',' || it == '.' }.take(6); touch("rate") },
                label = { Text("Lãi suất") }, placeholder = { Text("5,2") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                suffix = { Text("%/năm") },
                isError = err("rate") != null,
                supportingText = {
                    Text(err("rate") ?: if (payout == InterestPayout.UPFRONT) "Nhập đúng mức lãi suất đầu kỳ của ngân hàng (thường thấp hơn cuối kỳ)" else "")
                },
                modifier = Modifier.fillMaxWidth(),
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Hình thức lĩnh lãi", style = MaterialTheme.typography.labelLarge)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    InterestPayout.entries.forEachIndexed { i, p ->
                        SegmentedButton(
                            selected = payout == p, onClick = { payout = p },
                            shape = SegmentedButtonDefaults.itemShape(i, InterestPayout.entries.size),
                        ) { Text(p.label) }
                    }
                }
            }

            OutlinedTextField(
                value = note, onValueChange = { note = it.take(500) },
                label = { Text("Ghi chú (không bắt buộc)") }, placeholder = { Text("Số sổ, chi nhánh, mục đích...") },
                minLines = 2, supportingText = { Text("${note.length}/500") },
                modifier = Modifier.fillMaxWidth(),
            )

            // Xem trước
            if (amountVal != null && amountVal > 0 && startVal != null && termVal != null && termVal in 1..600 && rateVal != null && rateVal in 0.0..100.0) {
                val mat = Calc.maturityDate(startVal, termVal)
                val days = Calc.days(startVal, mat)
                val interest = Calc.interest(amountVal, rateVal, days)
                SectionCard("Xem trước") {
                    PreviewRow("Ngày đáo hạn", Fmt.date(mat))
                    PreviewRow("Số ngày gửi", "$days ngày")
                    PreviewRow("Tiền lãi dự kiến", "+" + Fmt.vnd(interest))
                    if (payout == InterestPayout.MONTHLY) PreviewRow("Lãi mỗi tháng", Fmt.vnd(interest / termVal))
                    HorizontalDivider()
                    PreviewRow("Tổng nhận khi đáo hạn", Fmt.vnd(amountVal + interest), bold = true)
                }
            }

            Button(
                onClick = {
                    submitted = true
                    if (errors.isEmpty()) {
                        val now = Instant.now().toString()
                        val acc = Account(
                            id = editing?.id ?: UUID.randomUUID().toString(),
                            name = name.trim(),
                            amount = amountVal!!,
                            startDate = startVal!!.toString(),
                            termMonths = termVal!!,
                            maturityDate = Calc.maturityDate(startVal, termVal).toString(),
                            interestRate = rateVal!!,
                            interestPayout = payout,
                            bankCode = bankCode.ifBlank { Banks.OTHER },
                            bankName = bankName.trim(),
                            owners = finalShares(decodeShares(sharesRaw), amountVal!!),
                            note = note.trim(),
                            closed = editing?.closed ?: false,
                            closedAt = editing?.closedAt,
                            createdAt = editing?.createdAt ?: now,
                            updatedAt = now,
                        )
                        onSave(acc, renewFrom?.id)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text(if (editing != null) "Lưu thay đổi" else "Lưu sổ") }
            if (submitted && errors.isNotEmpty()) {
                Text("Vui lòng kiểm tra lại các trường được đánh dấu.", color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (addingOwner) {
        OwnerNameDialog("Thêm người sở hữu", onDone = { n ->
            val o = onAddOwner(n)
            val cur = decodeShares(sharesRaw)
            if (cur.none { it.ownerId == o.id }) sharesRaw = encodeShares(cur + OwnerShare(o.id, 0))
        }, onDismiss = { addingOwner = false })
    }

    if (showBanks) {
        BankPicker(
            onPick = { code, n -> bankCode = code; bankName = n; showBanks = false },
            onDismiss = { showBanks = false },
        )
    }

    if (showDate) {
        val init = (startVal ?: LocalDate.now()).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        val st = rememberDatePickerState(initialSelectedDateMillis = init)
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    st.selectedDateMillis?.let { start = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString() }
                    showDate = false
                }) { Text("Chọn") }
            },
            dismissButton = { TextButton(onClick = { showDate = false }) { Text("Hủy") } },
        ) { DatePicker(state = st) }
    }
}

@Composable
private fun PreviewRow(label: String, value: String, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, style = if (bold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium)
    }
}

/** Danh sách ngân hàng có tìm kiếm không dấu, theo tên hoặc mã. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BankPicker(onPick: (String, String) -> Unit, onDismiss: () -> Unit) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var q by remember { mutableStateOf("") }
    var custom by remember { mutableStateOf(false) }
    var customName by remember { mutableStateOf("") }
    val fq = Fmt.fold(q.trim())
    val list = remember(fq) {
        if (fq.isEmpty()) Banks.all
        else Banks.all.filter { b ->
            Fmt.fold("${b.name} ${b.code} ${b.fullName} ${b.aliases}").replace(" ", "").contains(fq.replace(" ", "")) ||
                Fmt.fold("${b.name} ${b.code} ${b.fullName} ${b.aliases}").contains(fq)
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet) {
        Column(Modifier.padding(horizontal = 16.dp).fillMaxWidth()) {
            if (!custom) {
                OutlinedTextField(
                    value = q, onValueChange = { q = it }, singleLine = true,
                    leadingIcon = { Icon(Icons.Outlined.Search, null) },
                    placeholder = { Text("Tìm: vietin, VCB, cake...") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.fillMaxWidth().height(480.dp)) {
                    item {
                        ListItem(
                            headlineContent = { Text("Ngân hàng khác (tự nhập tên)") },
                            leadingContent = { Icon(Icons.Outlined.Edit, null) },
                            modifier = Modifier.clickable { custom = true; customName = q.trim() },
                        )
                    }
                    BankGroup.entries.forEach { g ->
                        val inGroup = list.filter { it.group == g }
                        if (inGroup.isNotEmpty()) {
                            item {
                                Text(g.label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp, start = 4.dp))
                            }
                            items(inGroup, key = { it.code }) { b -> BankRow(b) { onPick(b.code, b.name) } }
                        }
                    }
                }
            } else {
                Text("Nhập tên ngân hàng", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = customName, onValueChange = { customName = it.take(60) }, singleLine = true,
                    label = { Text("Tên ngân hàng") }, modifier = Modifier.fillMaxWidth(),
                )
                Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { custom = false }) { Text("Quay lại") }
                    Button(enabled = customName.isNotBlank(), onClick = { onPick(Banks.OTHER, customName.trim()) }) { Text("Dùng tên này") }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun BankRow(b: Bank, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(b.name, fontWeight = FontWeight.Medium) },
        supportingContent = { Text(b.fullName, maxLines = 1, style = MaterialTheme.typography.bodySmall) },
        leadingContent = { BankLogo(b.code, b.name, 36.dp) },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

private fun logoOrNull(code: String, name: String): (@Composable () -> Unit)? {
    if (name.isBlank()) return null
    return { BankLogo(code, name, 28.dp) }
}

private fun textOrNull(s: String?): (@Composable () -> Unit)? {
    if (s == null) return null
    return { Text(s) }
}

// ---- Người sở hữu ----

private fun encodeShares(list: List<OwnerShare>) = list.joinToString(";") { "${it.ownerId}=${it.amount}" }

private fun decodeShares(raw: String): List<OwnerShare> =
    raw.split(';').filter { '=' in it }.map { OwnerShare(it.substringBefore('='), it.substringAfter('=').toLongOrNull() ?: 0L) }

/** Một người: toàn bộ số tiền thuộc về người đó. Nhiều người: dùng số đã nhập. Không ai: chưa gán. */
private fun finalShares(sel: List<OwnerShare>, amount: Long): List<OwnerShare> = when (sel.size) {
    0 -> emptyList()
    1 -> listOf(OwnerShare(sel[0].ownerId, amount))
    else -> sel
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OwnerSharesEditor(
    owners: List<Owner>,
    raw: String,
    total: Long,
    error: String?,
    onChange: (String) -> Unit,
    onAddOwner: () -> Unit,
) {
    val sel = decodeShares(raw).filter { s -> owners.any { it.id == s.ownerId } }
    fun nameOf(id: String) = owners.firstOrNull { it.id == id }?.name ?: "?"

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Người sở hữu", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            owners.forEach { o ->
                val on = sel.any { it.ownerId == o.id }
                FilterChip(
                    selected = on,
                    onClick = {
                        onChange(
                            encodeShares(
                                when {
                                    on -> sel.filterNot { it.ownerId == o.id }
                                    // từ 1 lên 2 người: chia đều trước, người dùng sửa lại nếu cần
                                    sel.size == 1 && total > 0 -> splitEven(sel.map { it.ownerId } + o.id, total)
                                    else -> sel + OwnerShare(o.id, (total - sel.sumOf { it.amount }).coerceAtLeast(0))
                                }
                            )
                        )
                    },
                    label = { Text(o.name) },
                    leadingIcon = { OwnerAvatar(o.name, ownerColor(owners, o.id), 20.dp) },
                )
            }
            AssistChip(onClick = onAddOwner, label = { Text("Thêm người") }, leadingIcon = { Icon(Icons.Outlined.PersonAdd, null) })
        }
        when (sel.size) {
            0 -> Text(
                if (owners.isEmpty()) "Chưa có người sở hữu nào. Bấm Thêm người để tạo (không bắt buộc)."
                else "Chưa gán người sở hữu (không bắt buộc).",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            1 -> Text(
                "Toàn bộ ${if (total > 0) Fmt.vnd(total) else "số tiền"} thuộc về ${nameOf(sel[0].ownerId)}.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            else -> {
                sel.forEach { sh ->
                    OutlinedTextField(
                        value = if (sh.amount > 0) sh.amount.toString() else "",
                        onValueChange = { v ->
                            val n = v.filter { it.isDigit() }.trimStart('0').take(15).toLongOrNull() ?: 0L
                            onChange(encodeShares(sel.map { if (it.ownerId == sh.ownerId) it.copy(amount = n) else it }))
                        },
                        label = { Text("Phần của ${nameOf(sh.ownerId)}") },
                        leadingIcon = { OwnerAvatar(nameOf(sh.ownerId), ownerColor(owners, sh.ownerId), 24.dp) },
                        singleLine = true,
                        visualTransformation = ThousandsTransformation,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        suffix = { Text("₫") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                val sum = sel.sumOf { it.amount }
                val rest = total - sum
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        when {
                            rest == 0L -> "Đã chia đủ ${Fmt.vnd(total)}"
                            rest > 0 -> "Còn thiếu ${Fmt.vnd(rest)}"
                            else -> "Vượt ${Fmt.vnd(-rest)}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (rest == 0L) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(enabled = total > 0, onClick = { onChange(encodeShares(splitEven(sel.map { it.ownerId }, total))) }) {
                        Text("Chia đều")
                    }
                }
            }
        }
        if (error != null) Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    }
}
