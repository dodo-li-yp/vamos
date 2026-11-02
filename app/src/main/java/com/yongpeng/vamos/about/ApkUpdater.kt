// Modified by Vamos (2026): verified cache updates and narrowly scoped system installation.
package com.yongpeng.vamos.about

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.core.content.FileProvider
import com.yongpeng.vamos.BuildConfig
import com.yongpeng.vamos.core.update.SignedVersion
import java.io.File
import java.util.Base64

enum class DownloadPhase { Idle, Downloading, Paused, Verifying, Ready, Failed }
data class ApkDownloadState(val phase: DownloadPhase = DownloadPhase.Idle, val bytes: Long = 0, val total: Long = -1, val speed: Long = 0, val error: String? = null) {
    val percent get() = if (total > 0) (bytes.toDouble() * 100 / total).toInt().coerceIn(0,100) else null
}
class ApkUpdater(private val context: Context) {
    private val prefs = context.getSharedPreferences("cos-update", Context.MODE_PRIVATE)
    private val directory get() = File(context.cacheDir,"update").apply { mkdirs() }
    internal fun file(code: Long) = File(directory,"vamos-$code.apk")
    fun state(release: ReleaseInfo): ApkDownloadState {
        if (prefs.getLong("code",0) != release.versionCode) return ApkDownloadState()
        var phase = runCatching { DownloadPhase.valueOf(prefs.getString("phase", "Idle")!!) }.getOrDefault(DownloadPhase.Idle)
        if (phase in listOf(DownloadPhase.Downloading,DownloadPhase.Verifying) && !UpdateDownloadService.running && System.currentTimeMillis() - prefs.getLong("started",0) > 5000) phase = DownloadPhase.Paused
        return ApkDownloadState(phase,prefs.getLong("bytes",0),release.apkSize ?: -1,prefs.getLong("speed",0),prefs.getString("error",null))
    }
    fun download(release: ReleaseInfo) {
        val verified = verifiedRelease(context)
        require(verified == release) { "更新信息已变化，请重新检查" }
        require(release.versionCode > BuildConfig.VERSION_CODE)
        require(prefs.getInt("hashFailures-${release.versionCode}",0) < 2) { "此版本已两次校验失败，请重新检查更新或联系发布者" }
        prefs.edit().putLong("code",release.versionCode).putLong("started",System.currentTimeMillis()).putString("phase",DownloadPhase.Downloading.name).apply()
        UpdateDownloadService.start(context)
    }
    fun pause() = context.startService(Intent(context,UpdateDownloadService::class.java).setAction(UpdateDownloadService.PAUSE))
    fun cancel() {
        if (UpdateDownloadService.running) context.startService(Intent(context,UpdateDownloadService::class.java).setAction(UpdateDownloadService.CANCEL))
        else { directory.listFiles()?.forEach { it.delete() }; prefs.edit().clear().apply() }
    }
    fun dropInstalled() {
        val now = System.currentTimeMillis()
        directory.listFiles()?.forEach { f ->
            val code = Regex("vamos-(\\d+)\\.apk.*").matchEntire(f.name)?.groupValues?.get(1)?.toLongOrNull()
            if (code != null && (code <= BuildConfig.VERSION_CODE || now - f.lastModified() > 7 * 24 * 60 * 60_000L)) f.delete()
        }
    }
    fun installIntent(release: ReleaseInfo): Intent {
        require(context.packageManager.canRequestPackageInstalls()) { "需要允许安装未知应用" }
        require(verifiedRelease(context) == release) { "更新信息校验失败" }
        val target = file(release.versionCode)
        try {
            ApkIntegrity.verify(target,release.apkSize,release.apkDigest)
            val archive = archiveInfo(target, SIGNING_FLAGS) ?: throw ApkRejected(ApkRejected.Reason.Corrupt)
            val installed = installedInfo(SIGNING_FLAGS)
            if (archive.packageName != context.packageName || archive.longVersionCode != release.versionCode || archive.longVersionCode <= installed.longVersionCode) throw ApkRejected(ApkRejected.Reason.NotNewer)
            ApkIntegrity.requireSameSigner(installed.signers(),archive.signers(),archive.signerHistory())
        } catch (e: ApkRejected) { target.delete(); prefs.edit().putString("phase",DownloadPhase.Failed.name).putString("error","安装包校验失败").apply(); throw e }
        val uri = FileProvider.getUriForFile(context,"${context.packageName}.fileprovider",target)
        return Intent(Intent.ACTION_VIEW).setDataAndType(uri,"application/vnd.android.package-archive").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    @Suppress("DEPRECATION") private fun archiveInfo(file: File, flags: Int) = context.packageManager.getPackageArchiveInfo(file.path,flags)
    @Suppress("DEPRECATION") private fun installedInfo(flags: Int) = context.packageManager.getPackageInfo(context.packageName,flags)
    @Suppress("DEPRECATION") private fun PackageInfo.signers() = (signingInfo?.apkContentsSigners ?: signatures).orEmpty().map { ApkIntegrity.sha256(it.toByteArray()) }.toSet()
    private fun PackageInfo.signerHistory(): Set<String> = signingInfo?.takeUnless { it.hasMultipleSigners() }?.signingCertificateHistory.orEmpty().map { ApkIntegrity.sha256(it.toByteArray()) }.toSet()
    companion object {
        @Suppress("DEPRECATION") private const val SIGNING_FLAGS = PackageManager.GET_SIGNING_CERTIFICATES or PackageManager.GET_SIGNATURES
        internal fun verifiedRelease(context: Context): ReleaseInfo {
            val keys = BuildConfig.UPDATE_PUBLIC_KEYS.split(',').filter { it.isNotBlank() }.map { Base64.getDecoder().decode(it.trim()) }
            val base = File(context.filesDir,"update-v1.json")
            return Releases.fromManifest(SignedVersion.verify(File(base.path+".raw").readBytes(),File(base.path+".sig").readText(),keys))
        }
    }
}
