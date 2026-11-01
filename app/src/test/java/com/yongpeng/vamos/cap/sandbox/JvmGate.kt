// Modified by Vamos (2026): brand and package migration.
package com.yongpeng.vamos.cap.sandbox

import com.yongpeng.vamos.core.workspace.NioFdSys
import com.yongpeng.vamos.core.workspace.WorkspaceFileGate

/** JVM 测试里的关口(按路径的近似;手机上是 AndroidFdSys.gate)。 */
internal val JVM_GATE = WorkspaceFileGate(NioFdSys)
