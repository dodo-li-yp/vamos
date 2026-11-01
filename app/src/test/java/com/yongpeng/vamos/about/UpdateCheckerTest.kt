// Modified by Vamos (2026): signed update checker without network or persistent test keys.
package com.yongpeng.vamos.about

import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.advanceUntilIdle
import okhttp3.*
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.*
import org.junit.Assert.*
import org.junit.rules.TemporaryFolder
import java.security.*
import java.util.Base64

class UpdateCheckerTest {
    @get:Rule val temp=TemporaryFolder()
    @Test fun signedManifestCodeNotVersionNameControlsUpdateAndCache() = runTest {
        val keys=KeyPairGenerator.getInstance("Ed25519").generateKeyPair()
        val raw="""{"versionCode":34,"versionName":"0.0.1","apkUrl":"https://updates.invalid/vamos.apk","sha256":"${"a".repeat(64)}","size":10,"force":true,"minSupportedVersionCode":34,"changelog":"fix"}""".toByteArray()
        val sig=Base64.getEncoder().encodeToString(Signature.getInstance("Ed25519").run { initSign(keys.private);update(raw);sign() })
        var requests=0
        val client=OkHttpClient.Builder().addInterceptor { chain -> requests++; Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("test").body((if(chain.request().url.encodedPath.endsWith(".sig"))sig.toByteArray() else raw).toResponseBody()).build() }.build()
        val file=temp.root.resolve("update")
        val checker=UpdateChecker(file,"1.0.0",client,"https://updates.invalid/version.json",{100_000L},this,listOf(keys.public.encoded.takeLast(32).toByteArray()),33)
        assertTrue(checker.autoCheck());assertNotNull(checker.state.value.update);assertEquals(2,requests)
        assertFalse(checker.autoCheck());assertEquals(2,requests)
        val equal=UpdateChecker(temp.root.resolve("same"),"0.0.1",client,"https://updates.invalid/version.json",{100_000L},this,listOf(keys.public.encoded.takeLast(32).toByteArray()),34)
        equal.check(true);assertNull(equal.state.value.update)
        file.resolveSibling("update.raw").appendText(" ")
        val cache=UpdateChecker(file,"1.0.0",client,"https://updates.invalid/version.json",{100_000L},this,listOf(keys.public.encoded.takeLast(32).toByteArray()),33)
        cache.load();assertNull(cache.state.value.update)
    }
    @Test fun missingConfigurationFailsClosedAndManualCheckHasReason() = runTest {
        val checker=UpdateChecker(temp.root.resolve("unconfigured"),"1.0.0",releaseApi="https://<bucket>.cos.ap-guangzhou.myqcloud.com/vamos/version.json",scope=this,publicKeys=emptyList())
        checker.check(true);assertEquals(CheckStatus.Failed,checker.state.value.status);assertNull(checker.state.value.update);assertNotNull(checker.state.value.error)
    }
}
