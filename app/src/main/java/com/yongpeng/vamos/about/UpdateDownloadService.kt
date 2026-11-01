// Modified by Vamos (2026): foreground COS downloader with pause and resume.
package com.yongpeng.vamos.about

import android.app.*
import android.content.*
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.yongpeng.vamos.R
import com.yongpeng.vamos.core.update.ResumableDownload
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean

class UpdateDownloadService : Service() {
    private val scope = CoroutineScope(SupervisorJob()+Dispatchers.IO)
    private val paused = AtomicBoolean(false)
    private var discard = false
    private var job: Job? = null
    private val client = UpdateChecker.defaultClient()
    private val prefs by lazy { getSharedPreferences("cos-update",MODE_PRIVATE) }
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if(intent?.action in listOf(PAUSE,CANCEL)) {
            discard = intent?.action == CANCEL; paused.set(true); client.dispatcher.cancelAll()
            if(job == null) stopSelf()
            return START_NOT_STICKY
        }
        if(job?.isActive == true) return START_NOT_STICKY
        running = true; paused.set(false); discard = false
        getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(CHANNEL,"Vamos 下载更新",NotificationManager.IMPORTANCE_LOW))
        startForeground(ID,notification(0,0))
        job = scope.launch {
            try {
                val r = ApkUpdater.verifiedRelease(this@UpdateDownloadService)
                val v = r.manifest()
                prefs.edit().putLong("code",v.versionCode).putString("phase",DownloadPhase.Downloading.name).remove("error").commit()
                var lastProgressAt = 0L
                val result = ResumableDownload(client).download(v,ApkUpdater(this@UpdateDownloadService).file(v.versionCode),{ paused.get() }) { progress ->
                    val now = android.os.SystemClock.elapsedRealtime()
                    if(now - lastProgressAt >= 500 || progress.bytes == progress.total) {
                    lastProgressAt = now
                    prefs.edit().putLong("bytes",progress.bytes).putLong("speed",progress.bytesPerSecond).apply()
                    getSystemService(NotificationManager::class.java).notify(ID,notification(progress.bytes,progress.total))
                    if(progress.bytes == progress.total) prefs.edit().putString("phase",DownloadPhase.Verifying.name).apply()
                    }
                }
                prefs.edit().putString("phase",if(result == ResumableDownload.Result.Ready) DownloadPhase.Ready.name else DownloadPhase.Paused.name).commit()
            } catch(e: Exception) {
                if(e is SecurityException) prefs.edit().putInt("hashFailures-${prefs.getLong("code",0)}",prefs.getInt("hashFailures-${prefs.getLong("code",0)}",0)+1).commit()
                prefs.edit().putString("phase",if(paused.get()) DownloadPhase.Paused.name else DownloadPhase.Failed.name)
                    .putString("error",if(e is SecurityException) "安装包校验失败，文件已删除" else "下载失败，请重试").commit()
            } finally {
                running = false
                if(discard) ApkUpdater(this@UpdateDownloadService).cancel()
                stopForeground(STOP_FOREGROUND_REMOVE); stopSelf()
            }
        }
        return START_NOT_STICKY
    }
    private fun notification(bytes: Long,total: Long): Notification {
        val pause = PendingIntent.getService(this,1,Intent(this,UpdateDownloadService::class.java).setAction(PAUSE),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return Notification.Builder(this,CHANNEL).setSmallIcon(R.drawable.vamos_launcher_art).setContentTitle("Vamos · 下载更新")
            .setContentText(if(total>0) "${bytes*100/total}%" else "正在连接更新服务")
            .setProgress(100,if(total>0)(bytes*100/total).toInt() else 0,total<=0).setOngoing(true)
            .addAction(Notification.Action.Builder(null,"暂停",pause).build()).build()
    }
    override fun onDestroy() { paused.set(true); client.dispatcher.cancelAll(); scope.cancel(); running = false; super.onDestroy() }
    companion object {
        const val PAUSE = "com.yongpeng.vamos.UPDATE_PAUSE"
        const val CANCEL = "com.yongpeng.vamos.UPDATE_CANCEL"
        private const val CHANNEL = "vamos-update"; private const val ID = 4001
        @Volatile var running = false; private set
        fun start(context: Context) = ContextCompat.startForegroundService(context,Intent(context,UpdateDownloadService::class.java))
    }
}
