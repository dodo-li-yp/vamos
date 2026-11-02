// Modified by Vamos (2026): UI data from authenticated COS manifests only.
package com.yongpeng.vamos.about

import com.yongpeng.vamos.core.update.CosVersion

data class ReleaseInfo(
    val version: String, val name: String, val notes: String, val date: String?,
    val pageUrl: String, val apkUrl: String?, val apkSize: Long? = null, val apkDigest: String? = null,
    val versionCode: Long = 0, val forceUpdate: Boolean = false, val minSupportedVersionCode: Long = 0,
) {
    val downloadUrl get() = apkUrl ?: pageUrl
    fun manifest() = CosVersion(versionCode, version, requireNotNull(apkUrl), requireNotNull(apkDigest).removePrefix("sha256:"), requireNotNull(apkSize), notes, forceUpdate, minSupportedVersionCode)
}
object Releases {
    fun fromManifest(v: CosVersion) = ReleaseInfo(v.versionName, "Vamos ${v.versionName}", v.changelog, null, v.apkUrl, v.apkUrl, v.size, "sha256:${v.sha256}", v.versionCode, v.forceUpdate, v.minSupportedVersionCode)
}
