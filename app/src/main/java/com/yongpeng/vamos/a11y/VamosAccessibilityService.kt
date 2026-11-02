// Modified by Vamos (2026): brand and package migration.
package com.yongpeng.vamos.a11y

import com.yongpeng.vamos.cap.phone.a11y.ScreenAccessibility

/** 系统按这个全名找 Vamos 的无障碍服务(清单、系统设置里存的就是它);实现在手机能力里。 */
class VamosAccessibilityService : ScreenAccessibility() {
    private val volumeStop = com.yongpeng.vamos.task.VolumeStopChord()
    override fun onKeyEvent(event: android.view.KeyEvent): Boolean {
        if (volumeStop.update(event.keyCode, event.action != android.view.KeyEvent.ACTION_UP)) {
            val app = application as com.yongpeng.vamos.VamosApp
            if (app.taskState.busy.value) {
                app.taskState.runs.value.forEach { com.yongpeng.vamos.task.TaskService.cancel(this, it.key) }
                return true
            }
        }
        return super.onKeyEvent(event)
    }
}
