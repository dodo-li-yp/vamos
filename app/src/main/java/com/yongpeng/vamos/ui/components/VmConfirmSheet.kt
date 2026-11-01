// Modified by Vamos (2026): locked confirmation sheet with a sixty-second countdown.
package com.yongpeng.vamos.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.yongpeng.vamos.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun VmConfirmSheet(title: String, body: String, onDeny: () -> Unit, onAllow: () -> Unit) {
    val c = VmTheme.colors
    var remaining by remember { mutableIntStateOf(60) }
    val deny by rememberUpdatedState(onDeny)
    LaunchedEffect(Unit) {
        while (remaining > 0) { delay(1000); remaining-- }
        deny()
    }
    Dialog(onDismissRequest = {}, properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false, usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Column(Modifier.fillMaxWidth().background(c.surface, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)).navigationBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, Modifier.weight(1f), style = VmType.Title, color = c.textInk)
                    VmCountdownRing(remaining)
                }
                Text(body, style = VmType.Body, color = c.textInk)
                Text("仅同意这一次。登录、验证码和支付密码需你本人在目标 App 内输入。", style = VmType.Caption, color = c.textInk2)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    VmSecondaryButton("拒绝", onDeny, Modifier.weight(1f))
                    Button(onAllow, Modifier.weight(1f).heightIn(min = 48.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = c.accentCinnabar, contentColor = c.onAccent)) { Text("允许", style = VmType.BodyStrong) }
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
        Text(remaining.toString(), style = VmType.BodyStrong, color = c.textInk)
    }
}
