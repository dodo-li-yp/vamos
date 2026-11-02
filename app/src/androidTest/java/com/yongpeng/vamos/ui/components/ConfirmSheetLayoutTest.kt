// Modified by Vamos (2026): safe-area and large-font confirmation regression.
package com.yongpeng.vamos.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yongpeng.vamos.ui.theme.VmTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConfirmSheetLayoutTest {
    @get:Rule val compose = createComposeRule()

    @Test fun smallScreenWithGestureInset() = checkLayout(320, 360, 1f, 36)
    @Test fun smallScreenWithLargeFont() = checkLayout(320, 400, 2f, 48)
    @Test fun landscapeWithLargeFontAndNavigationInset() = checkLayout(360, 280, 2.5f, 48)

    private fun checkLayout(width: Int, height: Int, fontScale: Float, bottom: Int) {
        var denied = 0
        var allowed = 0
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, fontScale)) {
                VmTheme {
                    Box(Modifier.size(width.dp, height.dp).testTag("viewport")) {
                        VmConfirmSheetContent("要你同意：发送", "对象：回复\n" + "长消息，确认内容必须可以滚动查看。\n".repeat(60),
                            57, { denied++ }, { allowed++ }, insets = WindowInsets(top = 24.dp, bottom = bottom.dp))
                    }
                }
            }
        }
        val viewport = compose.onNodeWithTag("viewport").fetchSemanticsNode().boundsInRoot
        val actionsBefore = compose.onNodeWithTag("confirm-actions").fetchSemanticsNode().boundsInRoot
        for (tag in listOf("confirm-deny", "confirm-allow")) {
            compose.onNodeWithTag(tag).assertIsDisplayed()
            val button = compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
            assertTrue("$tag must be above navigation area", button.bottom <= viewport.bottom - bottom + 1f)
            assertTrue("$tag must be entirely inside safe viewport", button.top >= viewport.top + 24f)
            assertTrue("$tag must retain a 48px touch target", button.height >= 48f)
        }
        compose.onNodeWithTag("confirm-body").performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, 10000f) }
        compose.waitForIdle()
        assertEquals(actionsBefore, compose.onNodeWithTag("confirm-actions").fetchSemanticsNode().boundsInRoot)
        compose.onNodeWithTag("confirm-deny").performClick()
        compose.onNodeWithTag("confirm-allow").performClick()
        compose.runOnIdle { assertEquals(1, denied); assertEquals(1, allowed) }
    }
}
