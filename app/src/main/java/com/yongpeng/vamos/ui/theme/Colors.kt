// Modified by Vamos (2026): brand and package migration.
package com.yongpeng.vamos.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver

/**
 * 界面用到的全部颜色。页面一律从 [VmTheme.colors] 取,不写死色值。
 * 实色只用在强调(主按钮、选中、开关、用户消息)和状态(成功、需要你、出错);图标块一律中性。
 */
@Immutable
class VmColors(
    /** 页面背景。 */
    val bg: Color,
    /** 卡片、分组。 */
    val surface: Color,
    /** 浅填充:次按钮、分段控件底、代码块底。 */
    val fill: Color,
    /** 文字三级:正文、次要、辅助。 */
    val ink: Color,
    val ink2: Color,
    val ink3: Color,
    /** 分隔线。 */
    val hair: Color,
    /** 卡片描边。 */
    val edge: Color,
    val accent: Color,
    val accentSoft: Color,
    /** 强调色上的文字和图标。 */
    val onAccent: Color,
    val ok: Color,
    val okSoft: Color,
    /** 「需要你」:[warn] 用于文字,[warnSolid] 用于圆点、描边。 */
    val warn: Color,
    val warnSolid: Color,
    val warnSoft: Color,
    val bad: Color,
    val badSoft: Color,
    /** 图标块的底。 */
    val neutralSoft: Color,
    /** Vamos 回答的底:比页面背景深一点点的灰,和用户的蓝色气泡区分开。 */
    val answer: Color,
    /** 灰色胶囊、关着的开关。 */
    val greySoft: Color,
    /** 悬浮玻璃(输入栏、浮在内容上的按钮)的底和描边。 */
    val glass: Color,
    val glassEdge: Color,
    /** 卡片阴影;深色下卡片只靠描边,为透明。 */
    val cardShadow: Color,
    /** 悬浮元素的阴影。 */
    val floatShadow: Color,
    /** 开关的圆钮。 */
    val thumb: Color,
    /**
     * 图表里分类的颜色(例如每家服务商一种),按固定的先后取;超出的归到 [seriesOther]。
     * 只给图形上色,文字一律用 ink 三级。
     */
    val series: List<Color>,
    val seriesOther: Color,
    /** 图表的网格线。 */
    val grid: Color,
    val isDark: Boolean,
) {
    val bgPaper get() = bg
    val textInk get() = ink
    val textInk2 get() = ink2
    val brandDai get() = accent
    val agentQingbi get() = Color(if (isDark) 0xFF5FB3A8 else 0xFF3F8F86)
    val accentCinnabar get() = bad
    val stateAmber get() = warnSolid
    val glassFill get() = glass
    val glassStroke get() = glassEdge
    val tone get() = ink.copy(alpha = .06f)
    val tone2 get() = ink.copy(alpha = .10f)
}

private fun palette(dark: Boolean): VmColors {
    val paper = Color(if (dark) 0xFF1A1C1B else 0xFFF4F1E8)
    val ink = Color(if (dark) 0xFFE6E2D6 else 0xFF3A3A36)
    val ink2 = Color(if (dark) 0xFFA3A096 else 0xFF6E6C64)
    val dai = Color(if (dark) 0xFF7FA097 else 0xFF5B7A72)
    val cinnabar = Color(if (dark) 0xFFD0645A else 0xFFB5473A)
    val amber = Color(if (dark) 0xFFD9A55A else 0xFFC48A3A)
    val glass = Color.White.copy(alpha = if (dark) .08f else .28f)
    val stroke = Color.White.copy(alpha = if (dark) .12f else .50f)
    val surface = glass.compositeOver(ink.copy(alpha = if (dark) 0f else .035f).compositeOver(paper))
    return VmColors(
        bg = paper, surface = surface, fill = ink.copy(alpha = .06f),
        ink = ink, ink2 = ink2, ink3 = ink2, hair = ink.copy(alpha = .10f), edge = stroke,
        accent = dai, accentSoft = dai.copy(alpha = .12f), onAccent = if (dark) paper else Color.White,
        ok = dai, okSoft = dai.copy(alpha = .12f),
        warn = ink, warnSolid = amber, warnSoft = amber.copy(alpha = .16f),
        bad = cinnabar, badSoft = cinnabar.copy(alpha = .12f),
        neutralSoft = ink.copy(alpha = .10f), answer = ink.copy(alpha = .06f), greySoft = ink.copy(alpha = .10f),
        glass = glass, glassEdge = stroke, cardShadow = Color.Transparent,
        floatShadow = paper, thumb = if (dark) paper else Color.White,
        series = listOf(dai, cinnabar, amber), seriesOther = ink2, grid = ink.copy(alpha = .10f), isDark = dark,
    )
}
val LightColors = palette(false)
val DarkColors = palette(true)
val LocalVmColors = staticCompositionLocalOf { LightColors }
