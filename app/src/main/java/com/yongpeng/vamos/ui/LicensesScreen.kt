// Modified by Vamos (2026): brand and package migration.
package com.yongpeng.vamos.ui

import com.yongpeng.vamos.R
import androidx.compose.ui.res.stringResource
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import com.yongpeng.vamos.ui.components.FooterNote
import com.yongpeng.vamos.ui.components.GroupCard
import com.yongpeng.vamos.ui.components.GroupLabel
import com.yongpeng.vamos.ui.components.VmListItem
import com.yongpeng.vamos.ui.components.VmGlassCard
import com.yongpeng.vamos.ui.components.SubPage
import com.yongpeng.vamos.ui.theme.VmTheme
import com.yongpeng.vamos.ui.theme.VmType

/** 许可证页上第三方组件的数据;与仓库 licenses/THIRD_PARTY_NOTICES.md 的条目一一对应(LicensesTest 核对)。 */
object Licenses {
    data class Entry(val name: String, val license: String, val note: String)

    val ENTRIES = listOf(
        Entry("Noto Serif SC", "SIL OFL 1.1", "思源宋体 UI 子集，500/600 字重。Copyright 2012 Google Inc. All Rights Reserved. 生成脚本 tools/fonts/subset_noto.py。"),
        Entry("Google ML Kit Text Recognition", "Google APIs / ML Kit terms", "本机 OCR，版本 16.0.1（中文与拉丁文字）。https://developers.google.com/ml-kit/terms"),
        Entry("EdDSA-Java", "CC0-1.0", "net.i2p.crypto:eddsa:0.3.0，COS 更新清单 Ed25519 验签。https://github.com/str4d/ed25519-java"),
        Entry("Vamos", "Apache-2.0", "Vamos Android 应用。许可证全文见本页顶上。"),
        Entry("Kotlin 标准库", "Apache-2.0", "kotlin-stdlib 2.1.20 等,以及 JetBrains annotations 23.0.0。https://kotlinlang.org/"),
        Entry("kotlinx.coroutines", "Apache-2.0", "版本 1.9.0。https://github.com/Kotlin/kotlinx.coroutines"),
        Entry("kotlinx.serialization", "Apache-2.0", "版本 1.7.3。https://github.com/Kotlin/kotlinx.serialization"),
        Entry("OkHttp", "Apache-2.0", "版本 4.12.0,用于访问模型接口。https://square.github.io/okhttp/"),
        Entry("Okio", "Apache-2.0", "版本 3.6.0,OkHttp 的依赖。https://github.com/square/okio/"),
        Entry("AndroidX(含 Jetpack Compose)", "Apache-2.0", "Compose 1.7.6、Material3 1.3.1、Activity 1.9.3、Lifecycle 2.8.3、Core 1.13.1、WebKit 1.12.1 等界面与系统组件库。https://developer.android.com/jetpack/androidx"),
        Entry("Guava ListenableFuture(由 AndroidX 库引入)", "Apache-2.0", "listenablefuture 1.0。https://github.com/google/guava"),
        Entry("scrcpy", "Apache-2.0", "以 shell 身份取得系统上下文的做法仿照 scrcpy,相关源文件头部保留出处。"),
        Entry("commonmark-java", "BSD-2-Clause", "版本 0.30.1(含表格、删除线扩展),把回答里的 Markdown 解析成结构后显示。Copyright (c) 2015, Atlassian Pty Ltd. https://github.com/commonmark/commonmark-java"),
    )
}

/**
 * 许可证页(关于页底部「Apache-2.0 开源许可」进来):先是许可证原文(assets/licenses/Apache-2.0.txt),
 * 下面列出随 Vamos 发布的第三方组件,一个组件一行(名称、许可证),说明在行下面;页脚指向完整文本。
 */
@Composable
fun LicensesScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    var texts by remember { mutableStateOf(List(3) { "" }) }
    LaunchedEffect(context) {
        texts = withContext(Dispatchers.IO) {
            fun read(name: String) = context.assets.open("licenses/$name").bufferedReader().use { it.readText() }
            listOf(read("Apache-2.0.txt"), read("NOTICE.txt"),
                listOf("THIRD_PARTY_NOTICES.md", "OFL-Noto-Serif.txt", "CC0-1.0.txt").joinToString("\n\n", transform = ::read))
        }
    }
    val (licenseText, noticeText, thirdPartyText) = texts
    val thirdParty = Licenses.ENTRIES.filter { it.name != "Vamos" }
    SubPage(AboutCopy.LICENSE_TITLE, onBack) {
        Text(noticeText, Modifier.padding(16.dp), style = VmType.Hint, color = VmTheme.colors.ink)
        Text(thirdPartyText, Modifier.padding(16.dp), style = VmType.Caption, color = VmTheme.colors.ink2)
        if (licenseText.isNotEmpty()) {
            VmGlassCard(Modifier.padding(bottom = 12.dp).testTag("license-text")) {
                Text(
                    licenseText.trimEnd(), Modifier.padding(16.dp),
                    style = VmType.Hint.copy(fontFamily = FontFamily.Monospace, fontSize = 11.5.sp, lineHeight = 19.sp),
                    color = VmTheme.colors.ink2,
                )
            }
        }
        GroupLabel(stringResource(R.string.licenses_third_party, thirdParty.size))
        GroupCard {
            thirdParty.forEachIndexed { i, e ->
                Column {
                    VmListItem(null, e.name, e.license, first = i == 0)
                    Text(e.note, Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp), style = VmType.Hint, color = VmTheme.colors.ink2)
                }
            }
        }
        FooterNote(stringResource(R.string.licenses_footer))
    }
}

private const val LICENSE_ASSET = "licenses/Apache-2.0.txt"
