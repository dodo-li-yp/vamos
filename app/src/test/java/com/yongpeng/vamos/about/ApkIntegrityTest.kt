// Modified by Vamos (2026): brand and package migration.
package com.yongpeng.vamos.about

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ApkIntegrityTest {
    @get:Rule val temp = TemporaryFolder()

    @Test fun acceptsAuthenticatedHttpsApkLocation() {
        ApkIntegrity.requireOfficialUrl("https://example.test/vamos/app.apk")
    }

    @Test fun rejectsUnsafeSchemesCredentialsAndNonApks() {
        for (url in listOf("http://example.test/app.apk", "https://user:pass@example.test/app.apk",
            "https://example.test/app.apk#fragment", "https://example.test/app.zip"))
            rejects { ApkIntegrity.requireOfficialUrl(url) }
    }

    @Test fun verifiesSizeAndDigestAndRejectsPartialOrModifiedFiles() {
        val file = temp.newFile().apply { writeText("abc") }
        val digest = "sha256:ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"
        ApkIntegrity.verify(file, 3, digest)
        rejects { ApkIntegrity.verify(file, null, null) }
        rejects { ApkIntegrity.verify(file, 4, digest) }
        rejects { ApkIntegrity.verify(file, 3, "sha256:bad") }
        rejects { ApkIntegrity.verify(file, 3, "md5:" + "0".repeat(64)) }
        file.writeText("abd")
        rejects { ApkIntegrity.verify(file, 3, digest) }
        file.writeText("")
        rejects { ApkIntegrity.verify(file, null, null) }
    }

    @Test fun signerMustMatchInstalledOrCarryItInHistory() {
        ApkIntegrity.requireSameSigner(setOf("a"), setOf("a"), setOf("a"))
        // 新包轮换到新证书,旧证书在签名历史里
        ApkIntegrity.requireSameSigner(setOf("a"), setOf("b"), setOf("a", "b"))
        for ((installed, archive, history) in listOf(
            Triple(setOf("a"), setOf("b"), setOf("b")),
            Triple(setOf("a"), emptySet(), emptySet()),
            Triple(emptySet(), setOf("a"), setOf("a")),
            Triple(setOf("a", "b"), setOf("a"), emptySet()),
        )) {
            try {
                ApkIntegrity.requireSameSigner(installed, archive, history)
                throw AssertionError("签名不同应拒绝: $installed / $archive")
            } catch (e: ApkRejected) {
                assertEquals(ApkRejected.Reason.Signer, e.reason)
            }
        }
    }

    @Test fun progressHandlesUnknownSizeAndLargeApks() {
        assertNull(ApkDownloadState(bytes = 10).percent)
        assertEquals(50, ApkDownloadState(bytes = 3_000_000_000, total = 6_000_000_000).percent)
        assertEquals(100, ApkDownloadState(bytes = 101, total = 100).percent)
    }

    private fun rejects(block: () -> Unit) {
        try {
            block()
        } catch (_: IllegalArgumentException) {
            return
        } catch (e: ApkRejected) {
            assertEquals(ApkRejected.Reason.Corrupt, e.reason)
            return
        }
        throw AssertionError("应拒绝不可信或不完整的 APK")
    }
}
