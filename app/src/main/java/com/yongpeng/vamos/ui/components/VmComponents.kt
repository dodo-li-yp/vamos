// Modified by Vamos (2026): semantic design v0.1 components.
package com.yongpeng.vamos.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yongpeng.vamos.ui.theme.*

@Composable
fun VmStopButton(onStop: () -> Unit, modifier: Modifier = Modifier) {
    val c = VmTheme.colors
    var stopping by remember { mutableStateOf(false) }
    Button({ stopping=true;onStop() }, modifier.heightIn(min = 48.dp), shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(containerColor = c.accentCinnabar, contentColor = c.onAccent),
        contentPadding = PaddingValues(horizontal = 16.dp)) {
        Icon(VamosIcons.Stop, null, Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(if(stopping) "停止中…" else "停止", style = VmType.BodyStrong)
    }
}

enum class VmNoticeTone { Info, Agent, Warn, Error }

@Composable
fun VmNotice(text: String, tone: VmNoticeTone = VmNoticeTone.Info, modifier: Modifier = Modifier) {
    val c = VmTheme.colors
    val color = when(tone) {
        VmNoticeTone.Info -> c.textInk
        VmNoticeTone.Agent -> c.agentQingbi
        VmNoticeTone.Warn -> c.stateAmber
        VmNoticeTone.Error -> c.accentCinnabar
    }
    Text(text, modifier.fillMaxWidth().background(color.copy(alpha = .16f), RoundedCornerShape(16.dp)).padding(16.dp),
        style = VmType.Body, color = c.textInk)
}

@Composable
fun VmPrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) =
    VmButton(text, onClick, modifier, enabled = enabled)

@Composable
fun VmSecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) =
    VmButton(text, onClick, modifier, kind = ButtonKind.Tonal)

@Composable
fun VmGhostButton(text:String,onClick:()->Unit,modifier:Modifier=Modifier) = VmButton(text,onClick,modifier,kind=ButtonKind.Text)

@Composable
fun VmDangerButton(text:String,onClick:()->Unit,modifier:Modifier=Modifier) {
    val c=VmTheme.colors
    Button(onClick,modifier.heightIn(min=48.dp),shape=RoundedCornerShape(14.dp),colors=ButtonDefaults.buttonColors(containerColor=c.accentCinnabar,contentColor=c.onAccent)){Text(text,style=VmType.Subtitle)}
}
