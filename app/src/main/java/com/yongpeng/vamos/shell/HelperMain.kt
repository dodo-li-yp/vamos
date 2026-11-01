// Modified by Vamos (2026): brand and package migration.
package com.yongpeng.vamos.shell

import com.yongpeng.vamos.cap.phone.shell.HelperProcess

/** 内置助手进程的入口:启动命令里写的是这个全名(见 HelperHandoff.HELPER_CLASS);实现在手机能力里。 */
object HelperMain {
    @JvmStatic
    fun main(args: Array<String>) = HelperProcess.main(args)
}
