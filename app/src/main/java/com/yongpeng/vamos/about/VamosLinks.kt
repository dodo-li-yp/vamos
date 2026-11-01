// Modified by Vamos (2026): brand and package migration.
package com.yongpeng.vamos.about

import com.yongpeng.vamos.R
import com.yongpeng.vamos.i18n.tr
import java.net.URLEncoder

/** Vamos 对外的地址都在这里:仓库、问题与文档。换地址只改这一处。 */
object VamosLinks {
    const val REPO = "https://github.com/dodo-li-yp/vamos"
    const val REPO_LABEL = "github.com/dodo-li-yp/vamos"
    const val README = "$REPO#readme"
    const val RELEASES = "$REPO/blob/main/CHANGES.md"
    const val NEW_ISSUE = "$REPO/issues/new"
    const val X = "https://github.com/dodo-li-yp/vamos"
    const val X_HANDLE = "Vamos"

    /** X 上的 Vamos 交流群:点开即申请加入(不用微信的走这里)。 */
    const val X_GROUP = "https://x.com/i/chat/group_join/g2116500883139862983/xP74Qyff3X"


    /** 反馈时预先填进问题模板的设备与版本信息。 */
    data class Device(
        val manufacturer: String,
        val model: String,
        val androidRelease: String,
        val sdk: Int,
        val versionName: String,
        val build: String,
    )

    /** 预先填好的问题正文:机型、安卓版本、Vamos 版本三行,下面留空几段给用户写。 */
    fun issueBody(d: Device): String {
        val model = if (d.model.startsWith(d.manufacturer, ignoreCase = true)) d.model else "${d.manufacturer} ${d.model}"
        return buildString {
            append(tr(R.string.issue_body, model.trim(), d.androidRelease, d.sdk, d.versionName, d.build))
        }
    }

    /** 新建问题的地址,标题留空、正文预先填好;参数按 URL 编码(空格写成 %20)。 */
    fun issueUrl(d: Device): String = "$NEW_ISSUE?title=&body=${encode(issueBody(d))}"

    private fun encode(s: String): String = URLEncoder.encode(s, "UTF-8").replace("+", "%20")
}
