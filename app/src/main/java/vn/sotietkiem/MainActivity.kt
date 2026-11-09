package vn.sotietkiem

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import vn.sotietkiem.data.Account
import vn.sotietkiem.notify.MaturityWorker
import vn.sotietkiem.data.Codec
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.ui.platform.LocalContext
import vn.sotietkiem.domain.OwnerScope
import vn.sotietkiem.ui.AppViewModel
import vn.sotietkiem.ui.components.AccountActions
import vn.sotietkiem.ui.components.ConfirmDialog
import vn.sotietkiem.ui.screens.AccountsScreen
import vn.sotietkiem.ui.screens.BackupScreen
import vn.sotietkiem.ui.screens.DetailScreen
import vn.sotietkiem.ui.screens.FormScreen
import vn.sotietkiem.ui.screens.OverviewScreen
import vn.sotietkiem.ui.theme.SoTietKiemTheme

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        MaturityWorker.schedule(applicationContext)
        setContent { SoTietKiemTheme { App(vm) } }
    }
}

private enum class Tab(val label: String) { OVERVIEW("Tổng quan"), ACCOUNTS("Sổ của tôi"), BACKUP("Cài đặt") }

/** Form đang mở: sửa sổ [editId], hoặc tái tục từ sổ [renewId], hoặc cả hai null = thêm mới. */
private data class FormTarget(val editId: String?, val renewId: String?)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun App(vm: AppViewModel) {
    val accounts by vm.accounts.collectAsStateWithLifecycle()
    val lastExport by vm.lastExport.collectAsStateWithLifecycle()
    val owners by vm.owners.collectAsStateWithLifecycle()
    val lastChange by vm.lastChange.collectAsStateWithLifecycle()
    // Có thay đổi sau lần xuất file gần nhất
    val unsaved = accounts.isNotEmpty() && lastChange > lastExport
    var scopeRaw by rememberSaveable { mutableStateOf(OwnerScope.ALL) }
    // Người đang chọn đã bị xóa thì quay về Tất cả
    val ownerScope = if (scopeRaw == OwnerScope.ALL || scopeRaw == OwnerScope.UNASSIGNED || owners.any { it.id == scopeRaw }) scopeRaw else OwnerScope.ALL
    var tab by rememberSaveable { mutableStateOf(Tab.OVERVIEW) }
    // Giữ trạng thái từng tab (bộ lọc, ô tìm kiếm, vị trí cuộn) khi mở trang chi tiết hoặc form rồi quay lại
    val tabState = rememberSaveableStateHolder()
    var formEdit by rememberSaveable { mutableStateOf<String?>(null) }
    var formRenew by rememberSaveable { mutableStateOf<String?>(null) }
    var formOpen by rememberSaveable { mutableStateOf(false) }
    var detailId by rememberSaveable { mutableStateOf<String?>(null) }
    var askClose by remember { mutableStateOf<Account?>(null) }
    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current

    // Xuất file JSON trực tiếp từ thông báo nhắc, không cần vào tab Cài đặt
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val ok = runCatching {
            ctx.contentResolver.openOutputStream(uri)?.use { it.write(Codec.export(vm.accounts.value, vm.owners.value).toByteArray()) } != null
        }.getOrDefault(false)
        if (ok) vm.markExported()
        scope.launch { snack.showSnackbar(if (ok) "Đã xuất file sao lưu" else "Không ghi được file, hãy thử lại ở tab Cài đặt") }
    }
    fun exportNow() = exportLauncher.launch(Codec.fileName())

    /** Báo ngắn thao tác vừa làm. Việc nhắc xuất file do dòng nhắc ở Tổng quan và chấm đỏ tab Cài đặt đảm nhận. */
    fun changed(msg: String) {
        scope.launch { snack.showSnackbar(msg) }
    }

    // Đồng hồ cho con số lãi tích lũy chạy theo từng giây.
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) { delay(1000); now = System.currentTimeMillis() }
    }

    fun openForm(t: FormTarget) { formEdit = t.editId; formRenew = t.renewId; formOpen = true }
    fun closeForm() { formOpen = false; formEdit = null; formRenew = null }

    val actions = remember(vm) {
        object : AccountActions {
            override fun open(a: Account) { detailId = a.id }
            override fun edit(a: Account) = openForm(FormTarget(a.id, null))
            override fun renew(a: Account) = openForm(FormTarget(null, a.id))
            override fun delete(a: Account) {
                vm.delete(a.id)
                if (detailId == a.id) detailId = null
                changed("Đã xóa \"${a.name}\"")
            }
            override fun close(a: Account) { askClose = a }
            override fun reopen(a: Account) {
                vm.reopen(a.id)
                changed("Đã mở lại \"${a.name}\"")
            }
        }
    }

    askClose?.let { a ->
        ConfirmDialog(
            title = "Tất toán sổ?",
            text = "Sổ \"${a.name}\" sẽ chuyển sang trạng thái Đã tất toán. Bạn có thể mở lại sau.",
            confirm = "Tất toán",
            onConfirm = { vm.close(a.id); changed("Đã tất toán \"${a.name}\"") },
            onDismiss = { askClose = null },
        )
    }

    if (formOpen) {
        BackHandler { closeForm() }
        val editing = formEdit?.let { vm.get(it) }
        val renewFrom = formRenew?.let { vm.get(it) }
        // key: đổi sổ thì form khởi tạo lại trạng thái
        androidx.compose.runtime.key(formEdit, formRenew) {
            FormScreen(
                editing = editing,
                renewFrom = renewFrom,
                suggestedName = suggestName(accounts),
                owners = owners,
                onAddOwner = { vm.addOwner(it) },
                onSave = { acc, renewId ->
                    val isNew = editing == null
                    vm.upsert(acc, renewId)
                    closeForm()
                    // Tái tục từ trang chi tiết: chuyển sang chi tiết của sổ mới
                    if (renewId != null && detailId == renewId) detailId = acc.id
                    changed(if (renewId != null) "Đã tái tục sổ" else if (isNew) "Đã thêm sổ" else "Đã lưu thay đổi")
                },
                onBack = { closeForm() },
            )
        }
        return
    }

    // Trang chi tiết sổ: mở khi bấm vào thẻ sổ hoặc dòng trong "Đến hạn trong 30 ngày tới"
    val detail = detailId?.let { vm.get(it) }
    if (detail != null) {
        BackHandler { detailId = null }
        DetailScreen(detail, owners, now, actions, snack, onBack = { detailId = null })
        return
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (tab == Tab.OVERVIEW) "Sổ tiết kiệm" else tab.label) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t, onClick = { tab = t },
                        icon = {
                            // Chấm đỏ trên tab Cài đặt khi có thay đổi chưa xuất file
                            BadgedBox(badge = { if (t == Tab.BACKUP && unsaved) Badge() }) {
                                Icon(
                                    when (t) {
                                        Tab.OVERVIEW -> Icons.Outlined.Dashboard
                                        Tab.ACCOUNTS -> Icons.Outlined.Savings
                                        Tab.BACKUP -> Icons.Outlined.Settings
                                    }, null,
                                )
                            }
                        },
                        label = { Text(t.label) },
                    )
                }
            }
        },
        floatingActionButton = {
            if (tab == Tab.ACCOUNTS || (tab == Tab.OVERVIEW && accounts.isNotEmpty())) {
                ExtendedFloatingActionButton(
                    onClick = { openForm(FormTarget(null, null)) },
                    icon = { Icon(Icons.Outlined.Add, null) },
                    text = { Text("Thêm sổ") },
                )
            }
        },
        snackbarHost = { SnackbarHost(snack) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { pad ->
        Box(Modifier.fillMaxSize()) {
            tabState.SaveableStateProvider(tab.name) {
            when (tab) {
                Tab.OVERVIEW -> OverviewScreen(
                    allAccounts = accounts, owners = owners, scope = ownerScope, onScope = { scopeRaw = it }, nowMillis = now, lastExport = lastExport, contentPadding = pad, actions = actions,
                    onAdd = { openForm(FormTarget(null, null)) },
                    onSample = { vm.loadSample() },
                    onBackup = { tab = Tab.BACKUP },
                    unsaved = unsaved,
                    onExport = { exportNow() },
                )
                Tab.ACCOUNTS -> AccountsScreen(accounts, owners, now, pad, actions)
                Tab.BACKUP -> BackupScreen(vm, accounts.size, lastExport, pad)
            }
            }
        }
    }
}

/** Tên gợi ý cho sổ mới: "Tiết kiệm N", với N lớn hơn số lớn nhất đang dùng để không bị trùng. */
private fun suggestName(accounts: List<Account>): String {
    val re = Regex("""^\s*Tiết kiệm\s+(\d+)\s*$""", RegexOption.IGNORE_CASE)
    val max = accounts.mapNotNull { re.find(it.name)?.groupValues?.get(1)?.toIntOrNull() }.maxOrNull() ?: 0
    return "Tiết kiệm ${max + 1}"
}
