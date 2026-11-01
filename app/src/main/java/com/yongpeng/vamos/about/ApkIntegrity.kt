// Modified by Vamos (2026): brand and package migration.
package com.yongpeng.vamos.about

import okhttp3.HttpUrl.Companion.toHttpUrl
import java.io.File
import java.security.MessageDigest

/** 更新包没通过校验时为什么拒绝;界面按这个给出不同的说明。 */
class ApkRejected(val reason: Reason) : Exception(reason.name) {
    enum class Reason {
        /** 大小、SHA-256 对不上,或者根本读不出安装包。 */
        Corrupt,
        /** 不是 Vamos,或者版本不比已装的新。 */
        NotNewer,
        /** 签名证书和已装的 Vamos 不同。 */
        Signer,
    }
}

/** URL 来自已验签的 COS 清单；安装前再次校验内容、包名、版本和证书。 */
internal object ApkIntegrity {
    fun requireOfficialUrl(url: String) {
        com.yongpeng.vamos.core.update.SignedVersion.requireHttps(url)
        require(url.toHttpUrl().encodedPath.endsWith(".apk", true))
    }

    /** 大小和 SHA-256 均为必填；拒绝不完整或被修改的文件。 */
    fun verify(file: File, size: Long?, digest: String?) {
        corruptUnless(file.isFile && file.length() > 0)
        corruptUnless(size != null && size > 0 && file.length() == size)
        corruptUnless(digest != null)
        if (digest != null) {
            corruptUnless(digest.matches(Regex("sha256:[0-9a-fA-F]{64}")))
            corruptUnless(sha256(file).equals(digest.substringAfter(':'), ignoreCase = true))
        }
    }

    /**
     * 新包的签名证书(各证书 SHA-256)要和已装的一致;新包轮换过签名时,已装的证书要在它的签名历史里。
     * 系统安装器也会拦,这里提前拦下是为了给出说得清的提示。
     */
    fun requireSameSigner(installed: Set<String>, archive: Set<String>, archiveHistory: Set<String>) {
        if (installed.isEmpty() || archive.isEmpty()) throw ApkRejected(ApkRejected.Reason.Signer)
        if (archive != installed && !archiveHistory.containsAll(installed)) throw ApkRejected(ApkRejected.Reason.Signer)
    }

    fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).hex()

    private fun sha256(file: File): String {
        val sha = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                sha.update(buffer, 0, count)
            }
        }
        return sha.digest().hex()
    }

    private fun ByteArray.hex() = joinToString("") { "%02x".format(it) }

    private fun corruptUnless(ok: Boolean) {
        if (!ok) throw ApkRejected(ApkRejected.Reason.Corrupt)
    }
}
