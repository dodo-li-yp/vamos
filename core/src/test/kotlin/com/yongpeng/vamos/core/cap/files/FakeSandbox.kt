// Modified by Vamos (2026): brand and package migration.
package com.yongpeng.vamos.core.cap.files

import java.io.File
import java.nio.file.Files
import com.yongpeng.vamos.core.workspace.NioFdSys
import com.yongpeng.vamos.core.workspace.WorkspacePaths
import com.yongpeng.vamos.core.knowledge.ScriptEntry
import com.yongpeng.vamos.core.workspace.WorkspaceFileGate
import com.yongpeng.vamos.core.workspace.WorkspaceTree

/** 宿主目录放在临时目录里的沙箱;命令不真跑,按 [next] 回结果并记下收到的参数。 */
class FakeSandbox(var status: SandboxStatus = SandboxStatus.Ready(network = false)) : WorkspacePort {
    class Run(val workspace: String, val argv: List<String>, val cwd: String?, val timeoutMs: Long)

    override val tree = WorkspaceTree()
    val root: File = Files.createTempDirectory("fake-sandbox").toFile()
    val skillsDir = File(root, "skills").apply { mkdirs() }
    val runs = mutableListOf<Run>()
    var phoneHandler: PhoneCommandHandler? = null
    val shared = mutableListOf<Pair<File, String>>()
    var next: (Run) -> CommandResult = { CommandResult(0, false, false, 10, "", 0, false) }
    var free: Long = 10L shl 30
    var used: Long = 0
    val manifests = mutableMapOf<String, List<ScriptEntry>>()
    private var outputs = 0

    fun workspaceDir(ws: String) = File(root, "work/$ws").apply { mkdirs() }

    fun status() = status

    /** awaitReady 之后的状态(模拟后台准备好了或失败了);null = 不变。 */
    var afterWait: SandboxStatus? = null
    var waits = 0

    suspend fun awaitReady(timeoutMs: Long): SandboxStatus {
        waits++
        afterWait?.let { status = it }
        return status
    }

    suspend fun run(workspace: String, argv: List<String>, cwd: String?, timeoutMs: Long, onProgress: (String) -> Unit): CommandResult {
        val r = Run(workspace, argv, cwd, timeoutMs)
        runs += r
        return next(r)
    }

    suspend fun runWithPhone(workspace: String, argv: List<String>, cwd: String?, timeoutMs: Long,
        onProgress: (String) -> Unit, phone: PhoneCommandHandler?): CommandResult {
        phoneHandler = phone
        return run(workspace, argv, cwd, timeoutMs, onProgress)
    }

    override fun hostPath(workspace: String, sandboxPath: String, forWrite: Boolean): File? {
        val p = WorkspacePaths.normalize(sandboxPath) ?: return null
        return when {
            WorkspacePaths.isWritable(p) -> WorkspacePaths.resolveHost(workspaceDir(workspace), p.removePrefix(WorkspacePaths.WORKSPACE).trimStart('/'))
            forWrite -> null
            else -> WorkspacePaths.resolveHost(skillsDir, p.removePrefix(WorkspacePaths.SKILLS).trimStart('/'))
        }
    }

    override val files = WorkspaceFileGate(NioFdSys)

    override fun hostRoot(workspace: String, normalized: String, forWrite: Boolean): Pair<File, String>? = when {
        WorkspacePaths.isWritable(normalized) -> workspaceDir(workspace) to normalized.removePrefix(WorkspacePaths.WORKSPACE).trimStart('/')
        forWrite -> null
        else -> skillsDir to normalized.removePrefix(WorkspacePaths.SKILLS).trimStart('/')
    }

    override suspend fun share(file: File, name: String): String { shared += file to name; return "Download/Vamos/$name" }
    override fun freeBytes() = free
    override fun workspaceBytes(workspace: String) = used

    override fun saveOutput(workspace: String, text: String): String {
        val rel = ".vamos/out-${++outputs}.txt"
        val f = WorkspacePaths.resolveHost(workspaceDir(workspace), rel) ?: throw java.io.IOException("$rel escapes the workspace")
        f.parentFile.mkdirs()
        f.writeText(text)
        return "${WorkspacePaths.WORKSPACE}/.vamos/out-$outputs.txt"
    }

    fun scriptManifest(skillPath: String): List<ScriptEntry> = manifests[skillPath].orEmpty()

    var prepares = 0
    /** 与打包的那份不一致(且恢复不了)的脚本:"<技能>/<文件>"。 */
    val tampered = mutableSetOf<String>()
    suspend fun prepare() { prepares++ }
    suspend fun verifyScript(skillPath: String, file: String) = "$skillPath/$file" !in tampered
}

// Legacy test data only. V1 production has no runtime status, command runner or bridge.
sealed interface SandboxStatus {data class Unsupported(val reason:String):SandboxStatus;data object NotInstalled:SandboxStatus;data class Installing(val percent:Int,val step:String):SandboxStatus;data class Ready(val network:Boolean):SandboxStatus;data class Broken(val reason:String):SandboxStatus}
data class CommandResult(val exitCode:Int,val timedOut:Boolean,val killedBySystem:Boolean,val durationMs:Long,val output:String,val droppedBytes:Long,val network:Boolean)
fun interface PhoneCommandHandler {suspend fun call(tool:String,args:kotlinx.serialization.json.JsonObject):kotlinx.serialization.json.JsonObject}
