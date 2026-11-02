// Modified by Vamos (2026): task and confirmation previews use actual local frames only.
package com.yongpeng.vamos.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.yongpeng.vamos.ui.theme.*

val LocalVmFrame = staticCompositionLocalOf<Bitmap?> { null }
val LocalVmExpandPreview = staticCompositionLocalOf<() -> Unit> { {} }
val LocalVmConfirmationTimeout = staticCompositionLocalOf<() -> Unit> { {} }

@Composable
fun VmBackgroundScreenPreview(frame: Bitmap?, operating: Boolean, reduceMotion: Boolean, modifier: Modifier = Modifier) {
    val c = VmTheme.colors
    val pulse = if(operating && !reduceMotion) rememberInfiniteTransition(label="preview").animateFloat(.22f,.55f,infiniteRepeatable(tween(800),RepeatMode.Reverse),label="glow").value else .22f
    Box(modifier.width(132.dp).height(240.dp).border(if(operating) 4.dp else 0.dp,c.agentQingbi.copy(alpha=if(operating)pulse else 0f),RoundedCornerShape(20.dp)).padding(4.dp).clip(RoundedCornerShape(16.dp)).background(c.tone).border(if(operating) 2.dp else .5.dp,if(operating)c.agentQingbi else c.glassStroke,RoundedCornerShape(16.dp)),contentAlignment=Alignment.Center) {
        if(frame != null && !frame.isRecycled) Image(frame.asImageBitmap(),"后台屏实时画面",Modifier.fillMaxSize(),contentScale=ContentScale.Fit)
        else Text("等待后台屏画面",Modifier.padding(12.dp),style=VmType.Caption,color=c.textInk2)
    }
}

/** One accessible field supports paste and system pairing-code autofill; decoration shows six slots. */
@Composable
fun VmPinInput(value: String, onValueChange: (String)->Unit, modifier: Modifier = Modifier) {
    val c=VmTheme.colors
    BasicTextField(value,{onValueChange(it.filter(Char::isDigit).take(6))},modifier.fillMaxWidth(),singleLine=true,
        keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.NumberPassword),
        textStyle=VmType.Title.copy(color=c.textInk),
        decorationBox={ field ->
            Column {
                Box(Modifier.height(1.dp).width(1.dp)) { field() }
                Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                    repeat(6) { i -> Box(Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(12.dp)).background(c.tone).border(if(i==value.length)1.5.dp else .5.dp,if(i==value.length)c.brandDai else c.glassStroke,RoundedCornerShape(12.dp)),contentAlignment=Alignment.Center) {Text(value.getOrNull(i)?.toString() ?: "",style=VmType.Title,color=c.textInk)} }
                }
            }
        })
}

@Composable
fun VmEmptyState(title: String, detail: String, action: String? = null, onAction: ()->Unit = {}) {
    val c=VmTheme.colors
    Column(Modifier.fillMaxWidth().padding(vertical=32.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(16.dp)) {
        Box(Modifier.size(96.dp).background(c.tone,RoundedCornerShape(28.dp)),contentAlignment=Alignment.Center) {Text("V",style=VmType.LargeTitle,color=c.brandDai)}
        Text(title,style=VmType.Title,color=c.textInk)
        Text(detail,style=VmType.Body,color=c.textInk2)
        if(action != null) VmPrimaryButton(action,onAction)
    }
}

@Composable
fun VmSkeleton(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        repeat(3) { Box(Modifier.fillMaxWidth(if(it==2).65f else 1f).height(16.dp).background(VmTheme.colors.tone2,RoundedCornerShape(8.dp))) }
    }
}
