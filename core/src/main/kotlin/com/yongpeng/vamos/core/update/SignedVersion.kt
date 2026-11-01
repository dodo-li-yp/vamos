// Modified by Vamos (2026): authenticated COS update manifest. No production keys are bundled.
package com.yongpeng.vamos.core.update

import java.security.MessageDigest
import java.util.Base64
import net.i2p.crypto.eddsa.EdDSAEngine
import net.i2p.crypto.eddsa.EdDSAPublicKey
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable
import net.i2p.crypto.eddsa.spec.EdDSAPublicKeySpec
import kotlinx.serialization.json.*
import okhttp3.HttpUrl.Companion.toHttpUrl

data class CosVersion(
    val versionCode: Long, val versionName: String, val apkUrl: String,
    val sha256: String, val size: Long, val changelog: String,
    val forceUpdate: Boolean, val minSupportedVersionCode: Long,
) {
    fun newerThan(local: Long) = versionCode > local
    fun mandatoryFor(local: Long) = newerThan(local) && (forceUpdate || local < minSupportedVersionCode)
}

object SignedVersion {
    /** Detached signature covers the original bytes, never reserialized JSON. Keys are raw 32-byte Ed25519 public keys. */
    fun verify(raw: ByteArray, signature: String, publicKeys: List<ByteArray>): CosVersion {
        require(raw.size <= 256 * 1024) { "Update manifest too large" }
        val sig = runCatching { Base64.getDecoder().decode(signature.trim()) }.getOrNull()
        require(sig?.size == 64 && publicKeys.any { key -> valid(raw, sig, key) }) { "更新信息校验失败，请稍后再试" }
        val o = Json.parseToJsonElement(raw.decodeToString()).jsonObject
        fun str(k: String) = o.getValue(k).jsonPrimitive.content.also { require(it.isNotBlank()) }
        fun num(k: String) = o.getValue(k).jsonPrimitive.long
        val code = num("versionCode"); val size = num("size"); val min = num("minSupportedVersionCode")
        require(code > 0 && size > 0 && min in 0..code)
        val url = str("apkUrl"); requireHttps(url)
        val hash = str("sha256").lowercase(); require(hash.matches(Regex("[0-9a-f]{64}")))
        val force = (o["forceUpdate"] ?: o["force"])?.jsonPrimitive?.boolean ?: error("Missing forceUpdate")
        return CosVersion(code, str("versionName"), url, hash, size, o.getValue("changelog").jsonPrimitive.content, force, min)
    }
    fun requireHttps(url: String) {
        val u = url.toHttpUrl()
        require(u.isHttps && u.username.isEmpty() && u.password.isEmpty() && u.fragment == null) { "HTTPS update URL required" }
    }
    private fun valid(raw: ByteArray, sig: ByteArray, key: ByteArray): Boolean = runCatching {
        require(key.size == 32)
        val spec = EdDSANamedCurveTable.getByName("Ed25519")
        EdDSAEngine(MessageDigest.getInstance("SHA-512")).run {
            initVerify(EdDSAPublicKey(EdDSAPublicKeySpec(key, spec))); update(raw); verify(sig)
        }
    }.getOrDefault(false)
}
