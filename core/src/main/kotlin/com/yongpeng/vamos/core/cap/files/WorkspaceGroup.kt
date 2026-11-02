// Modified by Vamos (2026): brand and package migration.
package com.yongpeng.vamos.core.cap.files

import com.yongpeng.vamos.core.tool.Entry
import com.yongpeng.vamos.core.observation.ObsKind
import com.yongpeng.vamos.core.observation.Observation
import com.yongpeng.vamos.core.observation.Place
import com.yongpeng.vamos.core.prompt.Prompts
import com.yongpeng.vamos.core.tool.AgentTool
import com.yongpeng.vamos.core.tool.Args
import com.yongpeng.vamos.core.tool.Concurrency
import com.yongpeng.vamos.core.tool.EvidenceLog
import com.yongpeng.vamos.core.tool.Risk
import com.yongpeng.vamos.core.tool.ToolContext
import com.yongpeng.vamos.core.tool.ToolDef
import com.yongpeng.vamos.core.tool.ToolExecution
import com.yongpeng.vamos.core.tool.Verb
import com.yongpeng.vamos.core.tool.argStr
import com.yongpeng.vamos.core.tool.bool
import com.yongpeng.vamos.core.tool.int
import com.yongpeng.vamos.core.tool.str
import com.yongpeng.vamos.core.tool.strArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put

internal const val FILES = "files"

private const val TIMEOUT = "Time limit in seconds, 1-600 (default 60). The command is stopped when it runs out."

/**
 * 沙箱一组:设备支持时提供(运行环境没准备好时工具调用会等它)。路径都是沙箱里的路径;
 * 不是屏幕操作,不过锁屏关卡,也不进屏幕守卫(沙箱守卫另管)。执行在 [com.yongpeng.vamos.core.cap.files.WorkspaceTools] 里。
 */
internal interface WorkspaceTool : AgentTool {
    override val capability: String get() = FILES
    override val offScreen: Boolean get() = true
    override fun available(ctx: ToolContext) = ctx.workspaceRunner != null
    override fun offered(ctx: ToolContext) = ctx.workspaceAvailable()
}

/** 命令与脚本的输出(失败的输出也是事实,照样记)、读到的文件、写入与导出的路径,都是模型可能引用的事实。 */
private fun res(r: ToolExecution, k: String) = (r.content[k] as? JsonPrimitive)?.contentOrNull
private fun ok(r: ToolExecution) = r.content["ok"] == JsonPrimitive(true)

internal object ReadFile : WorkspaceTool {
    override val verb = Verb.ReadFile
    override val label = "读文件"
    override val risk = Risk.Read
    override val concurrency = Concurrency.ParallelSafe
    override val def = ToolDef("read_file",
        Prompts.tool("read_file"),
        mapOf(
            "path" to str("Workspace path, absolute or relative to /workspace."),
            "offset" to int("Byte offset to start at (default 0).", 0),
            "limit" to int("Bytes to read, 1-262144 (default 65536).", 1, MAX_READ_BYTES),
        ), listOf("path"))

    override suspend fun execute(a: Args, ctx: ToolContext) = ctx.workspaceRunner!!.run { ws, _ -> withContext(Dispatchers.IO) { readFile(a, ws) } }
    override fun observations(o: JsonObject, r: ToolExecution, log: EvidenceLog) =
        // 读回助手自己用 write_file 写的文件,读到的是它自己写的字,不算证据
        listOfNotNull(if (ok(r) && res(r, "path") !in log.writtenPaths) res(r, "content")?.let { output(it, Place.file("read_file ${res(r, "path")}")) } else null)
}

internal object WriteFile : WorkspaceTool {
    override val verb = Verb.WriteFile
    override val label = "写文件"
    override val risk = Risk.Write
    override val logged = true
    override val wideTarget = true
    override val def = ToolDef("write_file",
        Prompts.tool("write_file"),
        mapOf(
            "path" to str("Workspace path under /workspace, absolute or relative."),
            "content" to str("The text to write, at most 1 MB."),
            "append" to bool("Append instead of replacing (default false)."),
        ), listOf("path", "content"))

    override suspend fun execute(a: Args, ctx: ToolContext) = ctx.workspaceRunner!!.run { ws, _ -> tree.shared { withContext(Dispatchers.IO) { writeFile(a, ws) } } }
    override fun logTarget(o: JsonObject, r: ToolExecution, ctx: ToolContext) = o.argStr("path")
    override fun observations(o: JsonObject, r: ToolExecution, log: EvidenceLog): List<Observation> {
        if (!ok(r)) return emptyList()
        res(r, "path")?.let { log.writtenPaths += it }
        return listOf(output("write_file 写入 ${res(r, "path")}(${res(r, "bytesWritten")} 字节,文件现为 ${res(r, "size")} 字节)"))
    }
}

internal object SearchFiles : WorkspaceTool {
    override val verb = Verb.SearchFiles
    override val label = "搜索文件"
    override val risk = Risk.Read
    override val concurrency = Concurrency.ParallelSafe
    override val def = ToolDef("search_files",
        Prompts.tool("search_files"),
        mapOf(
            "pattern" to str("Regular expression to search for."),
            "path" to str("Directory or file to search (default /workspace)."),
            "glob" to str("Optional file name filter, e.g. *.csv or **/*.py."),
        ), listOf("pattern"))

    override suspend fun execute(a: Args, ctx: ToolContext) = ctx.workspaceRunner!!.run { ws, _ -> withContext(Dispatchers.IO) { searchFiles(a, ws) } }
    override fun observations(o: JsonObject, r: ToolExecution, log: EvidenceLog): List<Observation> {
        if (!ok(r)) return emptyList()
        return listOfNotNull((r.content["matches"] as? JsonArray)?.joinToString("\n") { m ->
            val mo = m as? JsonObject
            "${(mo?.get("file") as? JsonPrimitive)?.contentOrNull}:${(mo?.get("line") as? JsonPrimitive)?.contentOrNull}: ${(mo?.get("text") as? JsonPrimitive)?.contentOrNull}"
        }?.let { output(it, Place.command("search_files「${o.argStr("pattern")?.take(100)}」")) })
    }
}

internal object ShareFile : WorkspaceTool {
    override val verb = Verb.ShareFile
    override val label = "导出文件"
    override val risk = Risk.Write
    override val logged = true
    override val wideTarget = true
    override val def = ToolDef("share_file",
        Prompts.tool("share_file"),
        mapOf(
            "path" to str("Workspace path of the file under /workspace."),
            "name" to str("Optional file name to save as (default: the file's own name)."),
        ), listOf("path"))

    override suspend fun execute(a: Args, ctx: ToolContext) = ctx.workspaceRunner!!.run { ws, _ -> shareFile(a, ws) }
    override fun logTarget(o: JsonObject, r: ToolExecution, ctx: ToolContext) = o.argStr("path")
    override fun observations(o: JsonObject, r: ToolExecution, log: EvidenceLog) =
        listOfNotNull(if (ok(r)) output("share_file 已存到 ${res(r, "savedAs")}") else null)
}

/** 沙箱给出的一条观察:命令输出、文件内容、写入与导出的路径。[authored] 是助手自己写的命令(输出里的回显不记)。 */
private fun output(text: String, place: Place? = null, authored: String? = null) =
    Observation(ObsKind.Output, text, source = FILES, place = place, authored = authored)

internal object WorkspaceGroup {
    val tools: List<AgentTool> = listOf(ReadFile, WriteFile, SearchFiles, ShareFile)
}
