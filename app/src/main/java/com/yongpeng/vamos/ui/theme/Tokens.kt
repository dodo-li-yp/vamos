// Modified by Vamos (2026): brand and package migration.
package com.yongpeng.vamos.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 字号与字重。只定大小、字重、行高,颜色由用的地方给。 */
object VmType {
    /** 大标题:空状态、欢迎页。 */
    val LargeTitle = TextStyle(fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Serif)
    /** 页面标题:顶栏。 */
    val Title = TextStyle(fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.Medium, fontFamily = FontFamily.Serif)
    /** 正文、行标题。 */
    val Body = TextStyle(fontSize = 15.sp, lineHeight = 22.sp, fontWeight = FontWeight.Normal)
    /** 卡片标题等需要强调的正文。 */
    val BodyStrong = TextStyle(fontSize = 15.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold)
    /** 说明文字。 */
    val Caption = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Normal)
    /** 分组小标题。 */
    val Label = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium)
    /** 提示、页脚。 */
    val Hint = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Normal)
    /** 行尾的值。 */
    val Value = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal)
}

/** 间距。页面左右留 [Page]。 */
object VmSpace {
    val S4 = 4.dp
    val S8 = 8.dp
    val S12 = 12.dp
    val S16 = 16.dp
    val S20 = 20.dp
    val S24 = 24.dp
    val Page = 16.dp
}

/** 各类元素的尺寸。 */
object VmSize {
    val VmTopBar = 56.dp
    val IconButton = 48.dp
    val RowMin = 52.dp
    val Button = 48.dp
    val Composer = 52.dp
    val Icon = 22.dp
    /** 列表行左边的图标块,里面的图形是 [TileIcon]。 */
    val Tile = 30.dp
    val TileIcon = 18.dp
    /** 行尾箭头等小图标。 */
    val SmallIcon = 16.dp
    const val IconStroke = 1.7f
}

/** 圆角。 */
object VmRadius {
    val Card = 20.dp
    val Bubble = 20.dp
    val BubbleTail = 6.dp
    val Button = 14.dp
    val Tile = 12.dp
}

/** 常用形状;顶层常量,重组时不新建。 */
object VmShapes {
    val Card = RoundedCornerShape(VmRadius.Card)
    /** 用户消息气泡:右下角是尾角。 */
    val UserBubble = RoundedCornerShape(VmRadius.Bubble, VmRadius.Bubble, VmRadius.BubbleTail, VmRadius.Bubble)
    /** Vamos 的回答:左下角是尾角,和用户气泡对称。 */
    val AnswerBubble = RoundedCornerShape(VmRadius.Bubble, VmRadius.Bubble, VmRadius.Bubble, VmRadius.BubbleTail)
    val Button = RoundedCornerShape(VmRadius.Button)
    val Tile = RoundedCornerShape(VmRadius.Tile)
    val Pill = CircleShape
}
