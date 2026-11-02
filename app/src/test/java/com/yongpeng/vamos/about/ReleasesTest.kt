// Modified by Vamos (2026): authenticated manifest UI adapter.
package com.yongpeng.vamos.about
import com.yongpeng.vamos.core.update.CosVersion
import org.junit.Assert.*
import org.junit.Test
class ReleasesTest {
    @Test fun preservesSignedFieldsIncludingMandatoryVersion() {
        val v = CosVersion(34,"1.0.1","https://updates.invalid/vamos.apk","a".repeat(64),100,"fix",true,34)
        val r = Releases.fromManifest(v)
        assertEquals(v,r.manifest()); assertTrue(r.forceUpdate); assertEquals(34L,r.versionCode)
    }
}
