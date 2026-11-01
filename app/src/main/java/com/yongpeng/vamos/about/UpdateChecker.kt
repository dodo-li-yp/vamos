// Modified by Vamos (2026): signed COS updates, fail closed until production configuration is supplied.
package com.yongpeng.vamos.about

import com.yongpeng.vamos.BuildConfig
import com.yongpeng.vamos.core.update.SignedVersion
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.Base64
import java.util.concurrent.TimeUnit

enum class CheckStatus { Idle, Checking, Done, Failed }
data class UpdateState(val latest: ReleaseInfo? = null, val update: ReleaseInfo? = null, val status: CheckStatus = CheckStatus.Idle, val manualRounds: Int = 0, val error: String? = null) {
    val hasUpdate get() = update != null
}
object UpdateSchedule {
    const val MIN_GAP_MS = 24 * 60 * 60_000L
    fun due(lastAttempt: Long?, now: Long) = lastAttempt == null || now < lastAttempt || now - lastAttempt >= MIN_GAP_MS
}
class UpdateChecker(
    private val file: File, private val currentVersion: String,
    private val client: OkHttpClient = defaultClient(),
    private val releaseApi: String = BuildConfig.UPDATE_MANIFEST_URL,
    private val now: () -> Long = System::currentTimeMillis,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    private val publicKeys: List<ByteArray> = BuildConfig.UPDATE_PUBLIC_KEYS.split(',').filter { it.isNotBlank() }.mapNotNull { runCatching { Base64.getDecoder().decode(it.trim()) }.getOrNull() },
    private val currentCode: Long = BuildConfig.VERSION_CODE.toLong(),
) {
    private val _state = MutableStateFlow(UpdateState()); val state = _state.asStateFlow()
    private val mutex = Mutex()
    private var lastAttempt: Long? = null
    private var verifiedAt: Long? = null
    private var ignored: Long = 0
    private var loaded = false
    fun autoCheckInBackground() { scope.launch { delay(3000); autoCheck() } }
    fun checkInBackground() { scope.launch { check(true) } }
    suspend fun load() = mutex.withLock { loadLocked() }
    suspend fun autoCheck(): Boolean {
        val due = mutex.withLock { loadLocked(); UpdateSchedule.due(lastAttempt, now()).also { if(it) { lastAttempt = now(); saveState() } } }
        if (due) check(false)
        return due
    }
    fun ignore(versionCode: Long) { scope.launch { mutex.withLock {
        val release = _state.value.update ?: return@withLock
        if (release.versionCode != versionCode || release.forceUpdate || currentCode < release.minSupportedVersionCode) return@withLock
        ignored = versionCode; saveState(); _state.update { it.copy(update = null) }
    } } }
    suspend fun check(manual: Boolean) = mutex.withLock {
        loadLocked()
        if (manual) _state.update { it.copy(status = CheckStatus.Checking, error = null) }
        try {
            require(publicKeys.isNotEmpty() && !releaseApi.contains('<')) { "更新服务尚未配置" }
            SignedVersion.requireHttps(releaseApi)
            val raw = get(releaseApi, 256 * 1024)
            val sig = get("$releaseApi.sig", 4096).decodeToString()
            val v = SignedVersion.verify(raw, sig, publicKeys)
            val r = Releases.fromManifest(v)
            file.parentFile?.mkdirs()
            File(file.path + ".raw").writeBytes(raw)
            File(file.path + ".sig").writeText(sig)
            verifiedAt = now()
            saveState()
            // Expose the update only after its verified cache is ready for download/installation.
            _state.update { it.copy(latest = r, update = r.takeIf { v.newerThan(currentCode) && (manual || ignored != v.versionCode || v.mandatoryFor(currentCode)) }, status = CheckStatus.Done, error = null) }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            runCatching { android.util.Log.w("VamosUpdate", if(e.message?.contains("更新信息校验失败") == true) "更新信息签名无效" else "更新检查失败") }
            // Automatic failures never open a prompt. A manual check gets a short, non-sensitive reason.
            _state.update {
                val cacheValid = verifiedAt?.let { at -> now() - at in 0 until UpdateSchedule.MIN_GAP_MS } == true
                it.copy(latest = it.latest.takeIf { cacheValid }, update = it.update.takeIf { cacheValid }, status = CheckStatus.Failed, error = if (manual) (if (e is IllegalArgumentException) e.message else "暂时无法检查更新") else null) }
        } finally { if(manual) _state.update { it.copy(manualRounds = it.manualRounds + 1) } }
    }
    private fun get(url: String, max: Int): ByteArray {
        val req = Request.Builder().url(url).header("User-Agent", "Vamos/$currentVersion").build()
        return client.newCall(req).execute().use { response ->
            kotlin.check(response.isSuccessful) { "Update service unavailable" }
            val input = requireNotNull(response.body).byteStream()
            input.use {
                val out = java.io.ByteArrayOutputStream(); val buffer = ByteArray(8192)
                while(true) { val n = it.read(buffer); if(n < 0) break; require(out.size() + n <= max); out.write(buffer,0,n) }
                out.toByteArray()
            }
        }
    }
    private fun loadLocked() {
        if(loaded) return; loaded = true
        runCatching { val lines = file.readLines(); lastAttempt = lines.getOrNull(0)?.toLongOrNull(); ignored = lines.getOrNull(1)?.toLongOrNull() ?: 0; verifiedAt = lines.getOrNull(2)?.toLongOrNull() }
        if (verifiedAt?.let { now() - it in 0 until UpdateSchedule.MIN_GAP_MS } == true) runCatching {
            val v = SignedVersion.verify(File(file.path + ".raw").readBytes(), File(file.path + ".sig").readText(), publicKeys)
            val r = Releases.fromManifest(v)
            _state.value = UpdateState(r, r.takeIf { v.newerThan(currentCode) && (ignored != v.versionCode || v.mandatoryFor(currentCode)) })
        }
    }
    private fun saveState() { file.parentFile?.mkdirs(); val tmp = File(file.path + ".tmp"); tmp.writeText("${lastAttempt ?: 0}\n$ignored\n${verifiedAt ?: 0}\n"); kotlin.check(tmp.renameTo(file)) }
    companion object { fun defaultClient() = OkHttpClient.Builder().connectTimeout(10,TimeUnit.SECONDS).readTimeout(15,TimeUnit.SECONDS).callTimeout(30,TimeUnit.SECONDS).followSslRedirects(false).build() }
}
