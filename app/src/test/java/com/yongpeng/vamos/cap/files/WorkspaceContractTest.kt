// Modified by Vamos (2026): file boundaries survive removal of the executable runtime.
package com.yongpeng.vamos.cap.files
import com.yongpeng.vamos.core.cap.files.SandboxPathContract
import com.yongpeng.vamos.core.cap.files.WorkspacePort
import com.yongpeng.vamos.core.workspace.WorkspaceTree
import org.junit.Rule
import org.junit.rules.TemporaryFolder
class WorkspaceContractTest:SandboxPathContract() {
 @get:Rule val temp=TemporaryFolder()
 private val dirs by lazy {WorkspaceDirs(temp.newFolder("private"),JVM_GATE,WorkspaceTree())}
 override fun sandbox():WorkspacePort = WorkspaceFiles(dirs)
 override fun outsideDir() = temp.newFolder("outside")
}
