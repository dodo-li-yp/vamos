// Modified by Vamos (2026): brand and package migration.
package com.yongpeng.vamos.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yongpeng.vamos.R
import com.yongpeng.vamos.about.ApkDownloadState
import com.yongpeng.vamos.about.ApkRejected
import com.yongpeng.vamos.about.ApkUpdater
import com.yongpeng.vamos.about.DownloadPhase
import com.yongpeng.vamos.about.ReleaseInfo
import com.yongpeng.vamos.ui.components.ButtonKind
import com.yongpeng.vamos.ui.components.VmButton
import com.yongpeng.vamos.ui.theme.VmTheme
import com.yongpeng.vamos.ui.theme.VmType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun UpdateDownloadControls(release: ReleaseInfo) {
    val context = LocalContext.current
    val updater = remember { ApkUpdater(context.applicationContext) }
    val scope = rememberCoroutineScope()
    var state by remember(release.apkUrl) { mutableStateOf(ApkDownloadState()) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<Int?>(null) }
    var refresh by remember { mutableIntStateOf(0) }
    var mobilePrompt by remember { mutableStateOf(false) }
    var installing by remember { mutableStateOf(false) }
    val installer = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { installing = false; refresh++ }

    fun run(action: suspend () -> Unit) {
        if (busy) return
        busy = true
        error = null
        scope.launch {
            try {
                action()
            } catch (e: CancellationException) {
                throw e
            } catch (e: ApkRejected) {
                error = when (e.reason) {
                    ApkRejected.Reason.Corrupt -> R.string.about_update_bad_file
                    ApkRejected.Reason.NotNewer -> R.string.about_update_not_newer
                    ApkRejected.Reason.Signer -> R.string.about_update_bad_signer
                }
            } catch (_: Exception) {
                error = R.string.about_update_error
            } finally {
                busy = false
                refresh++
            }
        }
    }

    fun install() = run {
        val intent = withContext(Dispatchers.IO) { updater.installIntent(release) }
        installing = true
        installer.launch(intent)
    }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (context.packageManager.canRequestPackageInstalls()) install()
    }

    LaunchedEffect(release.apkUrl, refresh) {
        try {
            do {
                state = withContext(Dispatchers.IO) { updater.state(release) }
                if (state.phase !in listOf(DownloadPhase.Downloading, DownloadPhase.Verifying)) break
                delay(1000)
            } while (true)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            error = R.string.about_update_error
        }
    }

    UpdateDownloadActions(state, busy, error,
        onDownload = {
            val network = context.getSystemService(android.net.ConnectivityManager::class.java)
            val wifi = network.getNetworkCapabilities(network.activeNetwork)?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) == true
            if(wifi || !network.isActiveNetworkMetered) run { withContext(Dispatchers.IO) { updater.download(release) } } else mobilePrompt = true
        },
        onCancel = { run { withContext(Dispatchers.IO) { updater.cancel() } } },
        onInstall = {
            if (context.packageManager.canRequestPackageInstalls()) install()
            else run {
                permission.launch(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}")))
            }
        },
    )
    if(state.phase == DownloadPhase.Downloading) VmButton("暂停", { updater.pause() }, Modifier.fillMaxWidth().padding(top = 8.dp), kind = ButtonKind.Tonal, allowMultiline = true)
    if(installing) Text("正在系统安装器中安装，完成后数据会保留", style = VmType.Caption)
    state.error?.let { Text(it, color = VmTheme.colors.accentCinnabar, style = VmType.Body) }
    if(mobilePrompt) AlertDialog(onDismissRequest = { mobilePrompt = false }, title = { Text("正在使用移动网络") },
        text = { Text("下载约 ${(release.apkSize ?: 0) / 1024 / 1024} MB，继续会使用移动流量。") },
        confirmButton = { TextButton({ mobilePrompt = false; run { withContext(Dispatchers.IO) { updater.download(release) } } }) { Text("继续下载") } },
        dismissButton = { TextButton({ mobilePrompt = false }) { Text("等 Wi-Fi") } })
}

@Composable
internal fun UpdateDownloadActions(
    state: ApkDownloadState,
    busy: Boolean,
    /** 上次操作没成时的说明;为空时显示安装提示。 */
    error: Int?,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
    onCancel: () -> Unit,
) {
    val active = state.phase == DownloadPhase.Downloading || state.phase == DownloadPhase.Verifying
    val text = when {
        state.phase == DownloadPhase.Verifying -> "正在校验安装包"
        busy -> stringResource(R.string.about_update_working)
        state.phase == DownloadPhase.Paused -> stringResource(R.string.about_update_paused)
        active -> state.percent?.let { stringResource(R.string.about_update_progress, it) }
            ?: stringResource(R.string.about_update_downloading)
        state.phase == DownloadPhase.Ready -> stringResource(R.string.about_update_install)
        state.phase == DownloadPhase.Failed -> stringResource(R.string.about_update_retry)
        else -> stringResource(R.string.about_update_download_in_app)
    }
    if(active || state.phase==DownloadPhase.Paused) {
        Text(state.percent?.let { "$it%" } ?: "连接中",style=VmType.Display,color=VmTheme.colors.textInk)
        androidx.compose.material3.LinearProgressIndicator(progress={ (state.percent ?: 0)/100f },modifier=Modifier.fillMaxWidth().height(8.dp),color=if(state.phase==DownloadPhase.Paused)VmTheme.colors.textInk2 else VmTheme.colors.brandDai,trackColor=VmTheme.colors.tone2)
    }
    VmButton(text, if (state.phase == DownloadPhase.Ready) onInstall else onDownload,
        Modifier.fillMaxWidth().padding(top = 14.dp), enabled = !busy && !active, allowMultiline = true)
    if (active || state.phase == DownloadPhase.Ready || state.phase == DownloadPhase.Paused) {
        VmButton(stringResource(R.string.about_update_cancel), onCancel,
            Modifier.fillMaxWidth().padding(top = 10.dp), kind = ButtonKind.Tonal, enabled = !busy, allowMultiline = true)
    }
    if(state.total > 0) Text("${state.bytes / 1024 / 1024} / ${state.total / 1024 / 1024} MB · ${state.speed / 1024} KB/s", style = VmType.Caption)
    val note = error ?: if (state.phase == DownloadPhase.Failed) R.string.about_update_error else R.string.about_update_install_hint
    Text(stringResource(note),
        Modifier.padding(top = 8.dp), style = VmType.Caption, color = VmTheme.colors.ink3)
}
