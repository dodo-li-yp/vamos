// Modified by Vamos (2026): normal and mandatory authenticated-update prompts.
package com.yongpeng.vamos.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.yongpeng.vamos.BuildConfig
import com.yongpeng.vamos.about.ReleaseInfo
import com.yongpeng.vamos.ui.components.*
import com.yongpeng.vamos.ui.theme.*

@Composable
fun VmAppUpdatePrompt(release: ReleaseInfo, onLater: () -> Unit, onIgnore: () -> Unit, onExit: () -> Unit) {
    val c = VmTheme.colors
    val mandatory = release.forceUpdate || BuildConfig.VERSION_CODE < release.minSupportedVersionCode
    Dialog(onDismissRequest = { if (!mandatory) onLater() }, properties = DialogProperties(
        dismissOnBackPress = !mandatory, dismissOnClickOutside = !mandatory, decorFitsSystemWindows = false,
    )) {
        Box(Modifier.fillMaxWidth().safeDrawingPadding()) {
            Column(Modifier.fillMaxWidth().background(c.surface, RoundedCornerShape(28.dp)).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(if (mandatory) "需要更新到 ${release.version}" else "发现新版本 ${release.version}", style = VmType.Title, color = c.textInk)
                    Text(if (mandatory) "当前版本已停止支持，更新后才能继续使用" else "当前 ${BuildConfig.VERSION_NAME} · 安装包 ${(release.apkSize ?: 0)/1024/1024} MB", style = VmType.Caption, color = c.textInk2)
                    VmGlassCard { Text(release.notes, Modifier.padding(16.dp), style = VmType.Body, color = c.textInk) }
                }
                UpdateDownloadControls(release)
                if (mandatory) VmButton("退出", onExit, Modifier.fillMaxWidth(), kind = ButtonKind.Tonal, allowMultiline = true)
                else Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    VmButton("忽略此版本", onIgnore, Modifier.weight(1f), kind = ButtonKind.Text, allowMultiline = true)
                    VmButton("稍后", onLater, Modifier.weight(1f), kind = ButtonKind.Tonal, allowMultiline = true)
                }
            }
        }
    }
}
