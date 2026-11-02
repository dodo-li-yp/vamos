// Modified by Vamos (2026): ephemeral-key update security and resume regression tests.
package com.yongpeng.vamos.core.update

import org.junit.Assert.*
import org.junit.Test
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.security.*
import java.util.Base64
import okhttp3.*
import okhttp3.ResponseBody.Companion.toResponseBody

class SignedUpdateTest {
    @get:Rule val temp = TemporaryFolder()
    private val keys = KeyPairGenerator.getInstance("Ed25519").generateKeyPair()
    private val publicKey get() = keys.public.encoded.takeLast(32).toByteArray()
    private fun signed(raw: ByteArray) = Base64.getEncoder().encodeToString(Signature.getInstance("Ed25519").run { initSign(keys.private); update(raw); sign() })
    private fun json(force: Boolean = false, url: String = "https://updates.invalid/vamos.apk") = """{"versionCode":34,"versionName":"1.0.1","apkUrl":"$url","sha256":"${"a".repeat(64)}","size":100,"forceUpdate":$force,"minSupportedVersionCode":33,"changelog":"修复"}""".toByteArray()
    @Test fun acceptsAuthenticatedRawBytesAndRejectsMissingOrWrongKeys() {
        val raw = json(); val sig = signed(raw)
        assertEquals(34L,SignedVersion.verify(raw,sig,listOf(publicKey)).versionCode)
        for (pair in listOf(raw to "bad", raw to Base64.getEncoder().encodeToString(ByteArray(64)), (raw + 32.toByte()) to sig)) {
            assertThrows(IllegalArgumentException::class.java) { SignedVersion.verify(pair.first,pair.second,listOf(publicKey)) }
        }
        assertThrows(IllegalArgumentException::class.java) { SignedVersion.verify(raw,sig,emptyList()) }
    }
    @Test fun signatureIsCheckedBeforeJsonParsingAndSupportsRotation() {
        val invalid = "not JSON".toByteArray()
        val e = assertThrows(IllegalArgumentException::class.java) { SignedVersion.verify(invalid,Base64.getEncoder().encodeToString(ByteArray(64)),listOf(publicKey)) }
        assertTrue(e.message!!.contains("校验失败"))
        val raw = json(); assertEquals(34L,SignedVersion.verify(raw,signed(raw),listOf(ByteArray(32),publicKey)).versionCode)
    }
    @Test fun versionCodeNeverDowngradesAndMandatoryRulesApplyOnlyToNewerVersions() {
        val raw = json(true); val v = SignedVersion.verify(raw,signed(raw),listOf(publicKey))
        assertTrue(v.mandatoryFor(33)); assertFalse(v.mandatoryFor(34)); assertFalse(v.newerThan(35))
        assertTrue(v.copy(forceUpdate=false,minSupportedVersionCode=34).mandatoryFor(33))
        assertFalse(v.copy(forceUpdate=false).mandatoryFor(33))
    }
    @Test fun signedHttpUrlIsStillRejected() {
        val raw = json(url="http://updates.invalid/a.apk")
        assertThrows(IllegalArgumentException::class.java) { SignedVersion.verify(raw,signed(raw),listOf(publicKey)) }
    }
    private val bytes = ByteArray(100_000) { (it % 251).toByte() }
    private fun version() = CosVersion(34,"1.0.1","https://updates.invalid/vamos.apk",MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) },bytes.size.toLong(),"fix",false,0)
    private fun client(answer: (Request) -> Response) = OkHttpClient.Builder().addInterceptor { answer(it.request()) }.build()
    private fun response(request: Request, code: Int, body: ByteArray, tag: String = "\"v1\"", range: String? = null) = Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(code).message("test").header("ETag",tag).apply { range?.let { header("Content-Range",it) } }.body(body.toResponseBody()).build()
    @Test fun normalDownloadVerifiesAndPromotesOnlyCompleteFile() {
        val target = temp.root.resolve("normal.apk")
        assertEquals(ResumableDownload.Result.Ready,ResumableDownload(client { response(it,200,bytes) }).download(version(),target))
        assertArrayEquals(bytes,target.readBytes()); assertFalse(temp.root.resolve("normal.apk.part").exists())
    }
    @Test fun shaMismatchDeletesUntrustedDownload() {
        val target = temp.root.resolve("bad.apk")
        assertThrows(SecurityException::class.java) { ResumableDownload(client { response(it,200,ByteArray(bytes.size)) }).download(version(),target) }
        assertFalse(target.exists()); assertFalse(temp.root.resolve("bad.apk.part").exists())
    }
    @Test fun pauseThenRangeResumeUsesMatchingEtag() {
        val target = temp.root.resolve("resume.apk"); var pause = false; var ranged = false
        val http = client { request ->
            val offset = request.header("Range")?.removePrefix("bytes=")?.removeSuffix("-")?.toIntOrNull()
            if(offset != null) { ranged=true; assertEquals("\"v1\"",request.header("If-Range")); response(request,206,bytes.copyOfRange(offset,bytes.size),range="bytes $offset-${bytes.lastIndex}/${bytes.size}") }
            else response(request,200,bytes)
        }
        val download = ResumableDownload(http)
        assertEquals(ResumableDownload.Result.Paused,download.download(version(),target,{pause}) { pause=true })
        assertFalse(target.exists()); pause=false
        assertEquals(ResumableDownload.Result.Ready,download.download(version(),target))
        assertTrue(ranged); assertArrayEquals(bytes,target.readBytes())
    }
    @Test fun changedEtagOrUnsupportedRangeRestartsFromZero() {
        val target=temp.root.resolve("restart.apk")
        temp.root.resolve("restart.apk.part").writeBytes(ByteArray(100))
        temp.root.resolve("restart.apk.etag").writeText("\"old\"")
        val download=ResumableDownload(client { assertEquals("bytes=100-",it.header("Range")); response(it,200,bytes,"\"new\"") })
        assertEquals(ResumableDownload.Result.Ready,download.download(version(),target)); assertArrayEquals(bytes,target.readBytes())
    }
    @Test fun truncatedResponseResumesInsteadOfDeletingPartialDownload() {
        val target=temp.root.resolve("truncated.apk"); var requests=0
        val http=client { request ->
            requests++
            if(requests==1) response(request,200,bytes.copyOfRange(0,20000))
            else {
                assertEquals("bytes=20000-",request.header("Range"))
                response(request,206,bytes.copyOfRange(20000,bytes.size),range="bytes 20000-${bytes.lastIndex}/${bytes.size}")
            }
        }
        assertEquals(ResumableDownload.Result.Ready,ResumableDownload(http).download(version(),target))
        assertEquals(2,requests);assertArrayEquals(bytes,target.readBytes())
    }
    @Test fun malformedRangeEndRestartsSafely() {
        val target=temp.root.resolve("range.apk");var requests=0
        temp.root.resolve("range.apk.part").writeBytes(bytes.copyOfRange(0,100))
        temp.root.resolve("range.apk.etag").writeText("\"v1\"")
        val http=client { request ->
            requests++
            if(requests==1) response(request,206,bytes.copyOfRange(100,bytes.size),range="bytes 100-99/${bytes.size}")
            else {assertNull(request.header("Range"));response(request,200,bytes)}
        }
        assertEquals(ResumableDownload.Result.Ready,ResumableDownload(http).download(version(),target))
        assertEquals(2,requests)
    }

}
