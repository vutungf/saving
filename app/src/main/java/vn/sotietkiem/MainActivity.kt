package vn.sotietkiem

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import vn.sotietkiem.util.AppLock
import vn.sotietkiem.util.canUseBiometric
import vn.sotietkiem.util.showBiometricPrompt
import vn.sotietkiem.ui.screens.LockScreen
import vn.sotietkiem.ui.screens.WelcomeScreen
import vn.sotietkiem.ui.components.CloudDialogs
import androidx.lifecycle.lifecycleScope
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
import vn.sotietkiem.ui.components.ExportTarget
import vn.sotietkiem.ui.components.rememberExporter
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
import vn.sotietkiem.ui.screens.GoalActions
import vn.sotietkiem.ui.screens.GoalsScreen
import vn.sotietkiem.data.Goal
import vn.sotietkiem.ui.screens.FormScreen
import vn.sotietkiem.ui.screens.OverviewScreen
import vn.sotietkiem.ui.theme.SoTietKiemTheme

class MainActivity : FragmentActivity() {
    private val vm: AppViewModel by viewModels()
    private lateinit var lock: AppLock
    /** Đang hiện màn khóa. Màn khóa phủ lên trên nên trạng thái app bên dưới (tab, bộ lọc) vẫn giữ. */
    private var locked by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        MaturityWorker.schedule(applicationContext)
        lock = AppLock(this)
        locked = lock.enabled && !AppLock.unlocked
        setContent {
            SoTietKiemTheme {
                Box(Modifier.fillMaxSize()) {
                    val chosen by vm.storageChosen.collectAsStateWithLifecycle()
                    // Lần đầu mở app: chọn lưu trên máy hay đăng nhập Google
                    if (chosen) App(vm) else WelcomeScreen(vm)
                    CloudDialogs(vm)
                    if (locked) {
                        BackHandler { moveTaskToBack(true) }
                        LockScreen(
                            pattern = lock.type == AppLock.TYPE_PATTERN,
                            onSecret = { p -> lock.check(p).also { ok -> if (ok) unlock() } },
                            blockedUntil = { lock.blockedUntil },
                            biometricAvailable = lock.biometric && canUseBiometric(this@MainActivity),
                            onBiometric = { showBiometricPrompt(this@MainActivity) { unlock() } },
                            // Quên mã khóa: chỉ xóa dữ liệu trên máy và đăng xuất Google (để không tải lại được từ mây),
                            // dữ liệu trên mây vẫn còn, đăng nhập lại cần mật khẩu đồng bộ
                            onForgot = {
                                vm.clearLocal()
                                lifecycleScope.launch { vm.cloud.signOut() }
                                lock.disable()
                                unlock()
                            },
                        )
                    }
                }
            }
        }
    }

    private fun unlock() {
        AppLock.unlocked = true
        locked = false
    }

    override fun onStart() {
        super.onStart()
        if (!lock.enabled) { locked = false; return }
        val away = System.currentTimeMillis() - AppLock.leftAt
        when {
            // Vừa mở trình chọn file / chia sẻ của chính app thì không khóa lại
            AppLock.skipNext -> AppLock.skipNext = false
            AppLock.unlocked && AppLock.leftAt > 0 && away >= lock.delaySeconds * 1000L -> AppLock.unlocked = false
        }
        locked = !AppLock.unlocked
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) AppLock.leftAt = System.currentTimeMillis()
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
    val lastSync by vm.lastSync.collectAsStateWithLifecycle()
    val cloudState by vm.cloud.state.collectAsStateWithLifecycle()
    // Đang đồng bộ Google thì lần đồng bộ thành công cũng tính là đã sao lưu
    val unsaved = accounts.isNotEmpty() && lastChange > maxOf(lastExport, if (cloudState.ready) lastSync else 0L)
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
    var goalsOwnerId by rememberSaveable { mutableStateOf<String?>(null) }
    var askClose by remember { mutableStateOf<Account?>(null) }
    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current

    // Xuất file sao lưu trực tiếp từ dòng nhắc, không cần vào tab Cài đặt (có hỏi mật khẩu mã hóa)
    val exporter = rememberExporter(vm) { msg -> scope.launch { snack.showSnackbar(msg) } }
    fun exportNow() = exporter.start(ExportTarget.FILE)

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

    val goalActions = remember(vm) {
        object : GoalActions {
            override fun add(ownerId: String, name: String, amount: Long, date: String?) = vm.addGoal(ownerId, name, amount, date)
            override fun update(ownerId: String, goal: Goal) = vm.updateGoal(ownerId, goal)
            override fun delete(ownerId: String, goalId: String) = vm.deleteGoal(ownerId, goalId)
            override fun move(ownerId: String, goalId: String, delta: Int) = vm.moveGoal(ownerId, goalId, delta)
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

    // Trang mục tiêu của một người (mở từ Tổng quan hoặc Cài đặt)
    val goalsOwner = goalsOwnerId?.let { id -> owners.firstOrNull { it.id == id } }
    if (goalsOwner != null) {
        BackHandler { goalsOwnerId = null }
        GoalsScreen(goalsOwner, owners, accounts, goalActions, onBack = { goalsOwnerId = null })
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
                    onOpenGoals = { goalsOwnerId = it },
                )
                Tab.ACCOUNTS -> AccountsScreen(accounts, owners, now, pad, actions)
                Tab.BACKUP -> BackupScreen(vm, accounts.size, lastExport, pad, onOpenGoals = { goalsOwnerId = it })
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
