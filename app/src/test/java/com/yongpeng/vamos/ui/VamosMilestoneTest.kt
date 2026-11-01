// Modified by Vamos (2026): verify product defaults and stop gesture.
package com.yongpeng.vamos.ui

import com.yongpeng.vamos.settings.ProviderPresets
import com.yongpeng.vamos.settings.RoleChoice
import com.yongpeng.vamos.settings.RoleChoices
import com.yongpeng.vamos.task.VolumeStopChord
import org.junit.Assert.*
import org.junit.Test

class VamosMilestoneTest {
    @Test fun recommendedModelsHaveVisionAndExplicitNames() {
        val deepseek = ProviderPresets.all.first()
        assertEquals("DeepSeek", deepseek.label)
        assertTrue(deepseek.capabilities.vision)
        assertTrue(deepseek.capabilities.toolCalls)
        assertEquals("deepseek-flash", AddForm.of(deepseek).typedModel)
        assertEquals("qwen3.7-plus", AddForm.of(ProviderPresets.all.first { it.label == "通义千问" }).typedModel)
        assertEquals(RoleChoice.SameAsMain, RoleChoices().vision)
        assertTrue(ProviderPresets.all.any { it.label == "OpenRouter" })
        assertTrue(ProviderPresets.all.any { it.label == "xAI" })
        assertTrue(ProviderPresets.all.any { it.label == "OpenAI" })
    }
    @Test fun stopChordFiresOnceInEitherOrderAndIgnoresOtherKeys() {
        val chord = VolumeStopChord()
        assertFalse(chord.update(24, true))
        assertFalse(chord.update(26, true))
        assertTrue(chord.update(25, true))
        assertFalse(chord.update(25, true))
        assertFalse(chord.update(24, false))
        assertFalse(chord.update(24, true))
        chord.update(24, false)
        chord.update(25, false)
        assertFalse(chord.update(25, true))
        assertTrue(chord.update(24, true))
    }
}
