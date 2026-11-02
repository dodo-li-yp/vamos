// Modified by Vamos (2026): ordinary private workspace files; no executable runtime.
package com.yongpeng.vamos.cap.files
import com.yongpeng.vamos.chat.SharedFile
import com.yongpeng.vamos.core.workspace.WorkspacePathCheck
import android.content.Context
import android.util.Log
import com.yongpeng.vamos.core.cap.files.*
import com.yongpeng.vamos.core.workspace.*
import kotlinx.coroutines.*
import java.io.*
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.util.concurrent.ConcurrentHashMap
internal const val SHARE_MAX_FILES = 20
internal const val SHARE_MAX_BYTES = 500L shl 20

/**
 * 一个任务(一个 WorkspaceFiles)的导出额度,share_file 使用。
 * 复制前按文件大小预留,复制完按实际字节结算;导出失败不占额度。
 */
internal class ShareQuota(private val maxFiles: Int = SHARE_MAX_FILES, private val maxBytes: Long = SHARE_MAX_BYTES) {
    private var files = 0
    private var bytes = 0L

    @Synchronized fun reserve(size: Long) {
        if (files + 1 > maxFiles || bytes + size > maxBytes) {
            throw WorkspaceError("share_limit", "One task can share at most $maxFiles files and ${maxBytes shr 20} MB in total.", recoverable = false)
        }
        files++
        bytes += size
    }

    @Synchronized fun settle(reserved: Long, actual: Long) { bytes += actual - reserved }

    @Synchronized fun release(reserved: Long) {
        files--
        bytes -= reserved
    }
}


class WorkspaceFiles(private val dirs: WorkspaceDirs, private val exporter: (suspend (GatedFile,String)->SharedFile)? = null, private val onShared:(SharedFile)->Unit = {}) : WorkspacePort {
 constructor(context:Context,onShared:(SharedFile)->Unit = {}):this(WorkspaceDirs(context),ShareExporter(context)::export,onShared)
 override val tree get() = dirs.tree
 private val quota = ShareQuota()
 private class SizeEntry(val bytes:Long,val at:Long)
 private val sizes = ConcurrentHashMap<String,SizeEntry>()
    override fun hostPath(workspace: String, sandboxPath: String, forWrite: Boolean): File? {
        val p = WorkspacePaths.normalize(sandboxPath) ?: return null
        return when {
            WorkspacePaths.isWritable(p) -> WorkspacePaths.resolveHost(dirs.workspace(workspace), p.removePrefix(WorkspacePaths.WORKSPACE).trimStart('/'))
            forWrite -> null
            else -> null
        }
    }

    override val files: WorkspaceFileGate get() = dirs.gate

    // 只有写(调用方持有 WorkspaceTree 的许可)才建工作区目录:读和查路径不持有许可,建了会和重置的删除撞上
    override fun hostRoot(workspace: String, normalized: String, forWrite: Boolean): Pair<File, String>? = when {
        WorkspacePaths.isWritable(normalized) -> dirs.workspace(workspace).also { if (forWrite) it.mkdirs() } to normalized.removePrefix(WorkspacePaths.WORKSPACE).trimStart('/')
        forWrite -> null
        else -> null
    }

    /** web_upload 的路径检查:只认这个工作区里过得了关口的普通文件;技能目录和工作区外都算越界。 */
    fun uploadCheck(workspace: String, sandboxPath: String): WorkspacePathCheck {
        hostPath(workspace, sandboxPath, forWrite = true) ?: return WorkspacePathCheck.Outside
        val n = WorkspacePaths.normalize(sandboxPath) ?: return WorkspacePathCheck.Outside
        val (root, rel) = hostRoot(workspace, n, forWrite = false) ?: return WorkspacePathCheck.Outside
        return try {
            files.openRead(root, rel).close()
            WorkspacePathCheck.Ok
        } catch (e: WorkspaceError) {
            if (e.code == WorkspaceFileGate.OUTSIDE) WorkspacePathCheck.Outside else WorkspacePathCheck.NotFound
        }
    }

    /**
     * [file] 已由 ShareRules 按工作区检查过;导出时经关口打开一次(根目录是它所在的那个工作区),额度按句柄上的大小算,
     * 复制也从这个句柄读。意外的错误只记在 Vamos 这边,交出去的只有错误码和一句话,不带 Vamos 的私有路径。
     */
    override suspend fun share(file: File, name: String): String {
        val export = exporter ?: throw WorkspaceError("share_unavailable", "Saving files to Downloads is not available in this version.")
        val (within, rel) = withContext(Dispatchers.IO) { workspaceOf(file) }
            ?: throw WorkspaceError("path_outside_workspace", "Only files inside /workspace can be shared.", recoverable = true)
        val opened = withContext(Dispatchers.IO) { files.openRead(within, rel) }
        return opened.use { g ->
            val reserved = g.size
            quota.reserve(reserved)
            val shared = try {
                export(g, name)
            } catch (e: Throwable) {
                quota.release(reserved)
                throw mapExportError(e)
            }
            quota.settle(reserved, shared.size)
            onShared(shared)
            shared.savedAs
        }
    }

    private fun mapExportError(e: Throwable): Throwable = when (e) {
        is CancellationException, is WorkspaceError -> e
        is FileTooLarge -> WorkspaceError("file_too_large", "At most ${ShareRules.MAX_BYTES shr 20} MB can be shared.", recoverable = true)
        is Exception -> {
            Log.w(TAG, "export to Downloads failed", e)
            WorkspaceError("share_failed", "Saving to Downloads failed.", recoverable = true)
        }
        else -> e
    }

    /** 文件所在的那个工作区目录(work/ 下的第一层)与工作区里的相对路径;不在任何工作区里为 null。 */
    private fun workspaceOf(file: File): Pair<File, String>? {
        val work = dirs.work.canonicalFile
        val rel = file.canonicalFile.toRelativeString(work).takeIf { !it.startsWith("..") && it.isNotEmpty() && !File(it).isAbsolute } ?: return null
        val first = rel.substringBefore(File.separatorChar)
        return if (first == rel) null else File(work, first) to rel.substringAfter(File.separatorChar).replace(File.separatorChar, '/')
    }

    override fun freeBytes(): Long = dirs.root.takeIf { it.exists() }?.usableSpace ?: dirs.root.parentFile?.usableSpace ?: 0L

    /** 不跟随链接地累加;缓存一分钟,本工作区跑过命令后作废。 */
    override fun workspaceBytes(workspace: String): Long {
        val now = System.currentTimeMillis()
        sizes[workspace]?.takeIf { now - it.at < SIZE_CACHE_MS }?.let { return it.bytes }
        val bytes = treeBytes(dirs.workspace(workspace).toPath())
        sizes[workspace] = SizeEntry(bytes, now)
        return bytes
    }

    /** 经关口在 .vamos/ 里新建(不覆盖已有的);.vamos 被换成链接等时抛出,不写任何东西。 */
    @Synchronized
    override fun saveOutput(workspace: String, text: String): String {
        val root = dirs.workspace(workspace).apply { mkdirs() }
        var next = (files.list(root, ".vamos").mapNotNull { OUT_NAME.matchEntire(it)?.groupValues?.get(1)?.toIntOrNull() }.maxOrNull() ?: 0) + 1
        repeat(100) {
            val rel = ".vamos/out-$next.txt"
            try {
                files.createNew(root, rel, under = ".vamos").use { it.write(text.toByteArray(Charsets.UTF_8)) }
                return "${WorkspacePaths.WORKSPACE}/$rel"
            } catch (e: WorkspaceError) {
                if (e.code != WorkspaceFileGate.FILE_EXISTS) throw IOException("cannot save output: ${e.code}")
                next++
            }
        }
        throw IOException("cannot save output: too many files")
    }

    companion object {
 private val OUT_NAME = Regex("^out-(\\d+)\\.txt$")
 private const val SIZE_CACHE_MS = 60_000L
 private const val TAG = "VamosFiles"
        internal fun treeBytes(root: Path): Long {
            if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)) return 0
            var total = 0L
            Files.walkFileTree(root, object : SimpleFileVisitor<Path>() {
                override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                    if (attrs.isRegularFile) total += attrs.size()
                    return FileVisitResult.CONTINUE
                }
                override fun visitFileFailed(file: Path, exc: IOException) = FileVisitResult.CONTINUE
            })
            return total
        }

    }
}
