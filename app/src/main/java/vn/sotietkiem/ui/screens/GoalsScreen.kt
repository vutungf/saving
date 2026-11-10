package vn.sotietkiem.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import vn.sotietkiem.data.Account
import vn.sotietkiem.data.Goal
import vn.sotietkiem.data.Owner
import vn.sotietkiem.domain.goalsOf
import vn.sotietkiem.domain.ownerPrincipal
import vn.sotietkiem.ui.components.ConfirmDialog
import vn.sotietkiem.ui.components.GoalBar
import vn.sotietkiem.ui.components.GoalEditDialog
import vn.sotietkiem.ui.components.OwnerAvatar
import vn.sotietkiem.ui.components.SectionCard
import vn.sotietkiem.ui.components.centeredLetter
import vn.sotietkiem.ui.components.ownerColor
import vn.sotietkiem.util.Fmt

/** Các thao tác trên mục tiêu, do MainActivity nối vào ViewModel. */
interface GoalActions {
    fun add(ownerId: String, name: String, amount: Long, date: String?)
    fun update(ownerId: String, goal: Goal)
    fun delete(ownerId: String, goalId: String)
    fun move(ownerId: String, goalId: String, delta: Int)
}

/**
 * Danh sách mục tiêu của một người, theo thứ tự ưu tiên. Tiền gốc đang gửi được dồn cho mục tiêu trên
 * cùng trước; dùng mũi tên lên/xuống để đổi thứ tự.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(
    owner: Owner,
    owners: List<Owner>,
    accounts: List<Account>,
    actions: GoalActions,
    onBack: () -> Unit,
) {
    var editing by remember { mutableStateOf<Goal?>(null) }
    var adding by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Goal?>(null) }
    val color = ownerColor(owners, owner.id)
    val progress = goalsOf(owner, accounts)
    val principal = ownerPrincipal(accounts, owner.id)
    val totalGoal = owner.goals.sumOf { it.amount }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mục tiêu của ${owner.name}") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại") } },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { adding = true },
                icon = { Icon(Icons.Outlined.Add, null) },
                text = { Text("Thêm mục tiêu") },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OwnerAvatar(owner.name, color, 40.dp)
                        Column(Modifier.weight(1f)) {
                            Text("Tiền gốc đang gửi", style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(Fmt.vnd(principal), style = MaterialTheme.typography.titleLarge)
                        }
                    }
                    if (owner.goals.isNotEmpty()) {
                        Text(
                            "Tổng các mục tiêu ${Fmt.vnd(totalGoal)} · đã đạt ${progress.count { it.reached }}/${progress.size}",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        "Tiền gốc được dồn cho mục tiêu trên cùng trước, đủ rồi mới sang mục tiêu tiếp theo. " +
                            "Dùng mũi tên để đổi thứ tự ưu tiên.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (owner.goals.isEmpty()) {
                item {
                    Text("Chưa có mục tiêu nào. Bấm Thêm mục tiêu để tạo.", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 16.dp))
                }
            }

            itemsIndexed(owner.goals, key = { _, g -> g.id }) { i, g ->
                val p = progress.getOrNull(i) ?: return@itemsIndexed
                SectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Số thứ tự ưu tiên
                        Box(Modifier.size(26.dp).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
                            Text("${i + 1}", color = Color.White, style = centeredLetter(12f))
                        }
                        Text(if (i == 0) "Ưu tiên cao nhất" else "Ưu tiên ${i + 1}",
                            style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                        IconButton(enabled = i > 0, onClick = { actions.move(owner.id, g.id, -1) }) {
                            Icon(Icons.Outlined.KeyboardArrowUp, "Lên trên")
                        }
                        IconButton(enabled = i < owner.goals.lastIndex, onClick = { actions.move(owner.id, g.id, +1) }) {
                            Icon(Icons.Outlined.KeyboardArrowDown, "Xuống dưới")
                        }
                        IconButton(onClick = { editing = g }) { Icon(Icons.Outlined.Edit, "Sửa") }
                        IconButton(onClick = { deleting = g }) {
                            Icon(Icons.Outlined.Delete, "Xóa", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    GoalBar(p, color)
                }
            }
        }
    }

    if (adding) {
        GoalEditDialog(null, onDone = { n, a, d -> actions.add(owner.id, n, a, d) }, onDismiss = { adding = false })
    }
    editing?.let { g ->
        GoalEditDialog(g, onDone = { n, a, d -> actions.update(owner.id, g.copy(name = n, amount = a, date = d)) },
            onDismiss = { editing = null })
    }
    deleting?.let { g ->
        ConfirmDialog(
            title = "Xóa mục tiêu?", text = "Mục tiêu \"${g.name}\" sẽ bị xóa.", confirm = "Xóa", danger = true,
            onConfirm = { actions.delete(owner.id, g.id) }, onDismiss = { deleting = null },
        )
    }
}
