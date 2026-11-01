// Modified by Vamos (2026): brand and package migration.
package com.yongpeng.vamos.cap.phone.a11y

import com.yongpeng.vamos.cap.phone.shell.ShellService
import com.yongpeng.vamos.core.uistate.AppCopies
import com.yongpeng.vamos.core.uistate.AppCopy
import com.yongpeng.vamos.core.uistate.AppProfiles
import com.yongpeng.vamos.core.uistate.UserProfile
import com.yongpeng.vamos.shell.IVamosShell

/** 问内置助手:能打开的另一份 App(装在分身、工作资料一类的另一份用户里)。 */
internal object AppCopyReader {
    /** Vamos 所在的用户:本来那份 App 在这里;别的用户里的是另一份。 */
    val myUser: Int get() = android.os.Process.myUid() / 100_000 // UserHandle.PER_USER_RANGE

    /**
     * 内置助手判哪几份用户能用(规则见 [AppCopies]),再查那几份用户里装了什么,和当前用户能打开的 [apps] 对上;
     * 名字取 `pm list users` 里系统给的(随系统语言)。问不到为 null,没有另一份为空。
     */
    fun read(shell: IVamosShell, apps: List<Pair<String, String>>): List<AppCopy>? {
        val ids = shell.appCopyUsers()?.getIntArray(ShellService.KEY_COPY_USERS) ?: return null
        if (ids.isEmpty()) return emptyList()
        val names = AppProfiles.parseUsers(shell.listUsers().orEmpty()).associate { it.id to it.name }
        val packages = ids.associateWith { AppProfiles.parsePackages(shell.listPackages(it) ?: return null) }
        return AppCopies.copies(apps, ids.map { UserProfile(it, names[it].orEmpty(), running = true) }, packages)
    }
}
