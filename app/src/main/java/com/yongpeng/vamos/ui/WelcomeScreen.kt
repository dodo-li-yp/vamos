// Modified by Vamos (2026): design v0.1 welcome, consent and permission explanation.
package com.yongpeng.vamos.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.yongpeng.vamos.ui.components.*
import com.yongpeng.vamos.ui.theme.*

@Composable
fun WelcomeScreen(onStart: () -> Unit) {
    val c = VmTheme.colors
    val context = LocalContext.current
    val consent = remember { context.getSharedPreferences("vamos-consent", 0) }
    var page by rememberSaveable { mutableIntStateOf(0) }
    var agreed by rememberSaveable { mutableStateOf(false) }
    var document by rememberSaveable { mutableStateOf<String?>(null) }
    BackHandler(enabled = page > 0 || document != null) { if (document != null) document = null else page-- }
    PageBackground {
        Column(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 16.dp)) {
            if (document != null) {
                VmTopBar(document!!, left = VamosIcons.Back, onLeft = { document = null })
                Text(legalCopy(document!!), Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), style = VmType.Body, color = c.textInk)
                VmButton("返回阅读页", { document = null }, Modifier.fillMaxWidth().padding(bottom = 16.dp))
            } else {
                if (page == 0) {
                    Row(Modifier.fillMaxWidth().height(56.dp), horizontalArrangement = Arrangement.End) {
                        VmButton("跳过", { page = 1 }, kind = ButtonKind.Text)
                    }
                } else VmTopBar(if (page == 1) "使用前请阅读" else "Vamos 会用到这些权限", left = VamosIcons.Back, onLeft = { page-- })
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    when (page) {
                        0 -> {
                            Spacer(Modifier.height(80.dp))
                            VamosLogo(72.dp)
                            Text("你说一句话，\nVamos 替你操作手机", style = VmType.LargeTitle, color = c.textInk)
                            Text("在后台屏打开 App、点击、输入、滑动，不打断你正在用的界面。敏感操作前，会先请你确认。", style = VmType.Body, color = c.textInk2)
                            VmGlassCard {
                                Text("「给文件传输助手发：明早 9 点开会」\n\n「每天 8 点查天气，下雨才提醒我」", Modifier.padding(16.dp), style = VmType.Body, color = c.textInk)
                            }
                        }
                        1 -> {
                            Text("Vamos 会代你操作其他 App。为了让你清楚知道风险和数据去向，请先阅读以下文件。", style = VmType.Body, color = c.textInk2)
                            GroupCard {
                                listOf("用户协议", "隐私政策", "免责声明").forEachIndexed { i, title ->
                                    VmListItem(VamosIcons.Doc, title, "正式文本待提供 · 开发占位", first = i == 0, onClick = { document = title }) { RowChevron() }
                                }
                            }
                            VmNotice("自动操作可能违反部分 App 的使用条款，由此产生的账号风险需要你自行判断。法律文档仍为开发占位文本，正式发布前须完成审核。", VmNoticeTone.Warn)
                        }
                        2 -> {
                            ProgressDots(3, 2)
                            Text("现在不会一次全部申请。用到对应功能时申请；拒绝后只影响该功能。", style = VmType.Body, color = c.textInk2)
                            GroupCard {
                                listOf(
                                    Triple(VamosIcons.Globe, "网络", "调用你配置的模型 API"),
                                    Triple(VamosIcons.Phone, "无线调试与无障碍", "helper 创建后台屏与注入操作；无障碍按需读取控件、辅助操作"),
                                    Triple(VamosIcons.Bell, "通知", "显示任务进度和停止按钮"),
                                    Triple(VamosIcons.Mic, "麦克风", "按住说话时申请"),
                                    Triple(VamosIcons.Clock, "闹钟与开机自启", "按时恢复定时任务"),
                                    Triple(VamosIcons.Refresh, "安装未知应用", "安装更新时申请"),
                                ).forEachIndexed { i, (icon,title,body) -> VmListItem(icon,title,body,first = i == 0) }
                            }
                        }
                    }
                }
                if (page == 0) {
                    ProgressDots(3, 0, Modifier.padding(vertical = 16.dp))
                    Text("接下来：连接模型 · 设置手机控制（可跳过）· 权限说明", style = VmType.Caption, color = c.textInk2)
                }
                if (page == 1) Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(agreed, { agreed = it }, Modifier.testTag("legal-consent"))
                    Text("我已阅读并同意《用户协议》《隐私政策》《免责声明》开发占位文本", style = VmType.Body, color = c.textInk)
                }
                VmButton(
                    when(page) { 0 -> "开始设置"; 1 -> "同意并继续"; else -> "知道了，连接模型" },
                    { if (page == 2) { consent.edit().putBoolean("accepted-draft-v1", true).apply(); onStart() } else page++ },
                    Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 16.dp).testTag("welcome-start"),
                    enabled = page != 1 || agreed,
                )
            }
        }
    }
}

private fun legalCopy(title: String): String = "$title · 待提供正式文本\n\n占位内容。产品与法务尚未提供正式版本；本开发包的勾选不构成正式法律文件的签署，发布前必须补齐审核。"

object WelcomeCopy {
    const val TITLE = "你说一句话，Vamos 替你操作手机"
    const val LEAD = "在后台屏操作，不打断你正在用的界面"
    const val NOTE = "连接模型 · 设置手机控制（可跳过）· 权限说明"
    const val START = "开始设置"
}
