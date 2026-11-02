// Modified by Vamos (2026): verify the libraries actually shipped in V1.
package com.yongpeng.vamos.ui
import org.junit.Assert.*
import org.junit.Test
import java.io.File
class LicensesTest {
 @Test fun actualNoticeEntriesShown() {
  val names = Regex("(?m)^## (.+)$").findAll(File("../licenses/THIRD_PARTY_NOTICES.md").readText()).map{it.groupValues[1]}.toSet()
  assertEquals(names,Licenses.ENTRIES.filter{it.name!="Vamos"}.map{it.name}.toSet())
  assertFalse(Licenses.ENTRIES.any{it.license.contains("GPL")})
  assertTrue(Licenses.ENTRIES.any{it.name=="EdDSA-Java"})
 }
}
