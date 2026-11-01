// Modified by Vamos (2026): actual Compose component renders, explicitly visual fixtures only.
package com.yongpeng.vamos.ui

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.yongpeng.vamos.about.ReleaseInfo
import com.yongpeng.vamos.ui.components.*
import com.yongpeng.vamos.ui.theme.*
import org.junit.Rule
import org.junit.Test
import java.io.File

/** Fixtures verify rendering only: no model, payment, helper, download or installation is performed. */
class VmDesignScreenshotsTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Test fun captureProductionComponentsInDayAndNight() {
        var dark by mutableStateOf(false)
        var page by mutableIntStateOf(0)
        compose.setContent {
            VmTheme(dark=dark,reduceMotion=true) {
                key(dark,page) {
                    PageBackground {
                        Column(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(24.dp)) {
                            Text("Vamos · 视觉状态夹具",style=VmType.Title,color=VmTheme.colors.textInk)
                            when(page) {
                                0 -> {Text("输入配对码",style=VmType.LargeTitle);VmPinInput("123",{});VmNotice("请填入系统配对窗口中的六位数字")}
                                1 -> {VmGlassCard {Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){Text("任务执行中",style=VmType.Subtitle);VmBackgroundScreenPreview(null,true,true);VmStopButton({})}}}
                                2 -> VmConfirmSheet("确认发消息","App：示例应用\n对象：文件传输助手\n内容：明早 9 点开会",{},{})
                                3 -> {VmEmptyState("还没有定时任务","在对话中告诉 Vamos 什么时候做什么");VmSkeleton()}
                                4 -> {VmNotice("网络暂不可用，请检查连接后重试",VmNoticeTone.Error);VmNotice("还没有连接模型，请先配置服务商",VmNoticeTone.Warn);VmPrimaryButton("去配置模型",{})}
                            }
                        }
                    }
                }
            }
        }
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val output=File(context.getExternalFilesDir(null),"designshots").apply{mkdirs()}
        for(night in listOf(false,true)) for(state in 0..4) {
            compose.runOnIdle {dark=night;page=state}
            compose.waitForIdle()
            val image=InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
                ?: error("No real screenshot available")
            File(output,"fixture-${state}-${if(night)"night" else "day"}.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG,100,it) }
            image.recycle()
        }
    }
}
