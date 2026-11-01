// Modified by Vamos (2026): persisted five-category confirmation controls.
package com.yongpeng.vamos.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yongpeng.vamos.core.action.SensitiveCategory
import com.yongpeng.vamos.settings.ApprovalStore
import com.yongpeng.vamos.ui.components.*
import com.yongpeng.vamos.ui.theme.*

@Composable
fun ConfirmationRulesScreen(store: ApprovalStore, onAutonomous: (Boolean) -> Unit, onBack: () -> Unit) {
    val rules by store.rules.collectAsState()
    val autonomous by store.autonomous.collectAsState()
    var disabling by remember { mutableStateOf<SensitiveCategory?>(null) }
    val labels = mapOf(SensitiveCategory.Payment to "支付或下单", SensitiveCategory.Message to "发送消息 / 邮件 / 评论", SensitiveCategory.Delete to "删除文件、聊天、联系人", SensitiveCategory.Settings to "修改账号或系统设置", SensitiveCategory.Publish to "对外发布内容")
    PageScaffold("执行模式与确认规则", onBack) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VmButton("询问模式", { onAutonomous(false) }, Modifier.weight(1f), kind = if (!autonomous) ButtonKind.Primary else ButtonKind.Tonal)
            VmButton("自主模式", { onAutonomous(true) }, Modifier.weight(1f), kind = if (autonomous) ButtonKind.Primary else ButtonKind.Tonal)
        }
        VmNotice("支付始终确认。其余类别默认开启；关闭前请仔细核对风险。", modifier = Modifier.padding(vertical = 16.dp))
        GroupCard {
            SensitiveCategory.entries.forEachIndexed { index, category ->
                VmListItem(null, labels.getValue(category), if (category == SensitiveCategory.Payment) "已锁定，始终需要确认" else null, first = index == 0) {
                    VmSwitch(rules.requires(category), { enabled -> if (enabled) store.setRule(category, true) else disabling = category }, enabled = category != SensitiveCategory.Payment)
                }
            }
        }
        VmNotice("60 秒没有回应视为拒绝并停止任务；继续时需要重新确认。")
    }
    disabling?.let { category ->
        AlertDialog(onDismissRequest = { disabling = null }, title = { Text("关闭${labels.getValue(category)}确认？") },
            text = { Text("关闭后，Vamos 可直接执行这类操作，可能导致误发、内容丢失或账号变化。支付确认始终保留。") },
            confirmButton = { TextButton({ store.setRule(category, false); disabling = null }) { Text("确认关闭") } },
            dismissButton = { TextButton({ disabling = null }) { Text("保持开启") } })
    }
}
