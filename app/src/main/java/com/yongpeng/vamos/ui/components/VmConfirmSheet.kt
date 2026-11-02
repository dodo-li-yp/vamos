// Modified by Vamos (2026): locked confirmation sheet with a sixty-second countdown.
package com.yongpeng.vamos.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.yongpeng.vamos.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun VmConfirmSheet(title: String, body: String, onDeny: () -> Unit, onAllow: () -> Unit, deadlineMillis: Long = 0L) {
    val deadline = remember(deadlineMillis) { deadlineMillis.takeIf { it > 0 } ?: (System.currentTimeMillis()+60_000) }
    var remaining by remember { mutableIntStateOf(((deadline-System.currentTimeMillis()+999)/1000).toInt().coerceIn(0,60)) }
    val timeout by rememberUpdatedState(LocalVmConfirmationTimeout.current)
    LaunchedEffect(Unit) {
        while (remaining > 0) { delay(250); remaining=((deadline-System.currentTimeMillis()+999)/1000).toInt().coerceIn(0,60) }
        timeout()
    }
    Dialog(onDismissRequest = {}, properties = DialogProperties(
        dismissOnBackPress = false, dismissOnClickOutside = false,
        usePlatformDefaultWidth = false, decorFitsSystemWindows = false,
    )) {
        VmConfirmSheetContent(title, body, remaining, onDeny, onAllow)
    }
}

/** Bounded body with a fixed action row; insets are injectable for small-screen regression tests. */
@Composable
internal fun VmConfirmSheetContent(
    title: String, body: String, remaining: Int, onDeny: () -> Unit, onAllow: () -> Unit,
    modifier: Modifier = Modifier, insets: WindowInsets = WindowInsets.safeDrawing,
) {
    val c = VmTheme.colors
    val frame = LocalVmFrame.current
    Box(modifier.fillMaxSize().windowInsetsPadding(insets.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)), contentAlignment = Alignment.BottomCenter) {
        Column(Modifier.fillMaxWidth().background(c.surface, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .windowInsetsPadding(insets.only(WindowInsetsSides.Bottom)).padding(16.dp).testTag("confirm-sheet"),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).testTag("confirm-body"),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, Modifier.weight(1f), style = VmType.Title, color = c.textInk)
                    VmCountdownRing(remaining)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.width(104.dp).height(180.dp).background(c.tone, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                        if (frame != null && !frame.isRecycled) androidx.compose.foundation.Image(frame.asImageBitmap(), "操作前的真实画面", Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)), contentScale = androidx.compose.ui.layout.ContentScale.Fit)
                        else Text("未能获取截图", Modifier.padding(12.dp), style = VmType.Caption, color = c.textInk2)
                    }
                    Text(body, Modifier.weight(1f), style = VmType.Body, color = c.textInk)
                }
                Text("仅同意这一次。登录、验证码和支付密码需你本人在目标 App 内输入。", style = VmType.Caption, color = c.textInk2)
            }
            Row(Modifier.fillMaxWidth().testTag("confirm-actions"), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                VmButton("拒绝", onDeny, Modifier.weight(1f).testTag("confirm-deny"), kind = ButtonKind.Tonal, allowMultiline = true)
                Button(onAllow, Modifier.weight(1f).heightIn(min = 48.dp).testTag("confirm-allow"), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = c.accentCinnabar, contentColor = c.onAccent)) {
                    Text(if (title.contains("支付")) "允许并支付" else if (title.contains("删除")) "允许并删除" else "允许本次操作", style = VmType.BodyStrong)
                }
            }
        }
    }
}

@Composable
fun VmCountdownRing(remaining: Int) {
    val c = VmTheme.colors
    Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(progress = { remaining / 60f }, modifier = Modifier.fillMaxSize(), color = c.accentCinnabar, strokeWidth = 4.dp, trackColor = c.tone2)
        Text(remaining.toString(), style = VmType.BodyStrong.copy(fontWeight=if(remaining<=10) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal), color = c.textInk)
    }
}
