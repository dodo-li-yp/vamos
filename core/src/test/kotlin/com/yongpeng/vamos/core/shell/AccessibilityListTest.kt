// Modified by Vamos (2026): brand and package migration.
package com.yongpeng.vamos.core.shell

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessibilityListTest {
    private val vamos = PermissionCommands.accessibilityService("com.yongpeng.vamos")
    private val vamosLong = "com.yongpeng.vamos/com.yongpeng.vamos.a11y.VamosAccessibilityService"
    private val other = "com.example.reader/.ReaderService"
    private val other2 = "org.sample.keys/org.sample.keys.KeyService"

    @Test
    fun emptyOrUnsetValues() {
        listOf(null, "", "   ", "null", "null\n", ":", " : ").forEach { raw ->
            assertEquals("raw=$raw", emptyList<String>(), AccessibilityList.entries(raw))
            assertFalse(AccessibilityList.contains(raw, vamos))
            assertEquals(vamos, AccessibilityList.with(raw, vamos))
            assertEquals("", AccessibilityList.without(raw, vamos))
        }
    }

    @Test
    fun appendKeepsOtherServicesInOrder() {
        assertEquals("$other:$vamos", AccessibilityList.with(other, vamos))
        assertEquals("$other:$other2:$vamos", AccessibilityList.with("$other:$other2\n", vamos))
    }

    @Test
    fun alreadyPresentIsNotDuplicated() {
        assertEquals("$other:$vamos", AccessibilityList.with("$other:$vamos", vamos))
        // 系统里存的是完整类名写法,也算已经有了
        assertEquals("$vamosLong:$other", AccessibilityList.with("$vamosLong:$other", vamos))
        assertTrue(AccessibilityList.contains(vamosLong, vamos))
    }

    @Test
    fun removeKeepsOthersAndHandlesBothSpellings() {
        assertEquals("$other:$other2", AccessibilityList.without("$other:$vamos:$other2", vamos))
        assertEquals(other, AccessibilityList.without("$vamosLong:$other", vamos))
        assertEquals(other, AccessibilityList.without(other, vamos))
        assertEquals("", AccessibilityList.without(vamos, vamos))
    }

    @Test
    fun strayColonsAndSpacesAreCleanedUp() {
        assertEquals("$other:$other2:$vamos", AccessibilityList.with(":$other:: $other2 :", vamos))
        assertEquals("$other:$other2", AccessibilityList.without("$other::$vamos:$other2:", vamos))
    }

    @Test
    fun differentServiceOfSamePackageIsNotConfused() {
        val sibling = "com.yongpeng.vamos/.a11y.OtherService"
        assertFalse(AccessibilityList.contains(sibling, vamos))
        assertEquals(sibling, AccessibilityList.without("$sibling:$vamos", vamos))
    }
}
