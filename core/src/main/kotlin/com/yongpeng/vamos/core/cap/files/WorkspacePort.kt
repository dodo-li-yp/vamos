// Modified by Vamos (2026): file-only workspace port.
package com.yongpeng.vamos.core.cap.files
import java.io.File
import com.yongpeng.vamos.core.workspace.*
interface WorkspacePort {
val tree: WorkspaceTree
fun hostPath(workspace: String, sandboxPath: String, forWrite: Boolean): File?
val files: WorkspaceFileGate
fun hostRoot(workspace: String, normalized: String, forWrite: Boolean): Pair<File, String>?
suspend fun share(file: File, name: String): String
fun freeBytes(): Long
fun workspaceBytes(workspace: String): Long
fun saveOutput(workspace: String, text: String): String
}
