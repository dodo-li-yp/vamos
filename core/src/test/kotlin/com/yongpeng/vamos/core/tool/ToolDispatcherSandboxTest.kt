// Modified by Vamos (2026): brand and package migration.
package com.yongpeng.vamos.core.tool

import com.yongpeng.vamos.core.observation.ObsKind
import com.yongpeng.vamos.core.model.Part
import com.yongpeng.vamos.core.cap.files.CommandResult
import com.yongpeng.vamos.core.cap.files.FakeSandbox
import com.yongpeng.vamos.core.cap.files.SandboxStatus
import com.yongpeng.vamos.core.knowledge.ScriptEntry
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import com.yongpeng.vamos.core.cap.files.WorkspaceTools
import com.yongpeng.vamos.core.cap.files.SearchLimits
import com.yongpeng.vamos.core.assembly.toolDispatcher

private var seq = 0
private fun call(name: String, args: JsonObject) = Part.ToolCall("c${++seq}", name, args.toString())
private fun JsonObject.s(k: String) = this[k]!!.jsonPrimitive.content
private val SANDBOX_NAMES = setOf("run_command", "run_bundled_script", "read_file", "write_file", "search_files", "share_file")

class ToolDispatcherSandboxTest {
    private val sb = FakeSandbox()
    private fun dispatcher(vision: Boolean = false) = toolDispatcher(FakeScreen(), vision, files = sb, workspace = { "conv1" })
    private val ws get() = sb.workspaceDir("conv1")

    @Test fun writeOutsideWorkspaceRejected() = runTest {
        val d = dispatcher()
        fun write(p: String) = call("write_file", buildJsonObject { put("path", p); put("content", "x") })
        for (p in listOf("/etc/passwd", "../x", "/opt/vamos/skills/app/a/SKILL.md")) {
            assertEquals(p, "path_outside_workspace", d.execute(write(p)).content.s("error"))
        }
        val outside = File(sb.root, "outside").apply { mkdirs() }
        java.nio.file.Files.createSymbolicLink(File(ws, "link").toPath(), outside.toPath())
        assertEquals("path_outside_workspace", d.execute(write("link/a.txt")).content.s("error"))
        assertFalse(File(outside, "a.txt").exists())
        assertEquals("path_outside_workspace", d.execute(call("read_file", buildJsonObject { put("path", "link/a.txt") })).content.s("error"))

        val ok = d.execute(call("write_file", buildJsonObject { put("path", "out/notes.txt"); put("content", "第一行\n") })).content
        assertEquals(ok.toString(), "true", ok.s("ok"))
        d.execute(call("write_file", buildJsonObject { put("path", "/workspace/out/notes.txt"); put("content", "第二行\n"); put("append", true) }))
        assertEquals("第一行\n第二行\n", File(ws, "out/notes.txt").readText())
        val read = d.execute(call("read_file", buildJsonObject { put("path", "out/notes.txt") })).content
        assertEquals("第一行\n第二行\n", read.s("content"))
        val found = d.execute(call("search_files", buildJsonObject { put("pattern", "第二") })).content
        val m = found["matches"]!!.jsonArray.single().jsonObject
        assertEquals("/workspace/out/notes.txt", m.s("file")); assertEquals("2", m.s("line"))
        val shared = d.execute(call("share_file", buildJsonObject { put("path", "out/notes.txt"); put("name", "笔记.txt") })).content
        assertEquals("Download/Vamos/笔记.txt", shared.s("savedAs"))
        assertEquals(File(ws, "out/notes.txt").canonicalFile, sb.shared.single().first.canonicalFile)
    }

    @Test fun binaryReadIsNotText() = runTest {
        File(ws, "data.bin").writeBytes(byteArrayOf(0x50, 0x4b, 0x03, 0x04, 0, 0, -1, -2, 7))
        File(ws, "latin1.txt").writeBytes(byteArrayOf(0x63, 0x61, 0x66, 0xe9.toByte(), 0x0a))
        val png = byteArrayOf(-119, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 0, 0)
        File(ws, "chart.png").writeBytes(png)
        val d = dispatcher()
        suspend fun read(p: String, disp: ToolDispatcher = d) = disp.execute(call("read_file", buildJsonObject { put("path", p) }))
        val bin = read("data.bin").content
        assertEquals("not_text", bin.s("error")); assertEquals("9", bin.s("size")); assertTrue(bin.containsKey("type"))
        assertEquals("not_text", read("latin1.txt").content.s("error"))
        // 看不了图的模型:图片也只给大小和类型
        val img = read("chart.png")
        assertEquals("not_text", img.content.s("error")); assertEquals("image/png", img.content.s("type"))
        assertNull(img.image)
        val seen = read("chart.png", dispatcher(vision = true))
        assertEquals("true", seen.content.s("ok"))
        assertArrayEquals(png, seen.image)
        assertEquals("file_not_found", read("none.txt").content.s("error"))
    }

    @Test fun readFileUsesByteOffsetAndLimit() = runTest {
        File(ws, "t.txt").writeText("你好世界abc")
        val d = dispatcher()
        // 第 4 个字节落在「好」中间:跳过残字节,截断处的半个字留给下一次
        val r = d.execute(call("read_file", buildJsonObject { put("path", "t.txt"); put("offset", 4); put("limit", 7) })).content
        assertEquals("世", r.s("content"))
        assertEquals("false", r.s("eof"))
        assertEquals("9", r.s("nextOffset"))
    }

    @Test fun specialFilesAreNotAFile() = runTest {
        val fifo = File(ws, "pipe")
        assertEquals(0, ProcessBuilder("mkfifo", fifo.path).start().waitFor())
        val d = dispatcher()
        assertEquals("not_a_file", d.execute(call("read_file", buildJsonObject { put("path", "pipe") })).content.s("error"))
        assertEquals("not_a_file", d.execute(call("write_file", buildJsonObject { put("path", "pipe"); put("content", "x") })).content.s("error"))
        assertEquals("not_a_file", d.execute(call("share_file", buildJsonObject { put("path", "pipe") })).content.s("error"))
        File(ws, "dir").mkdirs()
        assertEquals("not_a_file", d.execute(call("read_file", buildJsonObject { put("path", "dir") })).content.s("error"))
        assertEquals("not_a_file", d.execute(call("write_file", buildJsonObject { put("path", "dir"); put("content", "x") })).content.s("error"))
        // 搜索跳过 FIFO,不会卡住
        assertEquals("true", d.execute(call("search_files", buildJsonObject { put("pattern", "x") })).content.s("ok"))
    }

    @Test fun shareNameMustBePlain() = runTest {
        File(ws, "a.txt").writeText("a")
        val d = dispatcher()
        for (n in listOf("   ", "a\u0000b", "a\nb", "a\u007fb", "x/y", "..", "report\u202Efdp.exe", "a\u200Bb", "a\u2028b", "a\u0085b")) {
            val r = d.execute(call("share_file", buildJsonObject { put("path", "a.txt"); put("name", n) })).content
            assertEquals(n, "invalid_arguments", r.s("error"))
        }
        assertTrue(sb.shared.isEmpty())
    }

    @Test fun searchStopsOnSlowRegexAndLimits() = runTest {
        // 嵌套的懒惰重复:回溯量随长度多项式爆炸(JDK 对 (a+)+ 一类已有优化,用这种)
        File(ws, "evil.txt").writeText((1..60).joinToString(","))
        val tools = WorkspaceTools(sb, { "conv1" }, false, SearchLimits(timeMs = 200))
        val def = com.yongpeng.vamos.core.cap.files.SearchFiles.def
        val started = System.nanoTime()
        val slow = Args(def, buildJsonObject { put("pattern", "(.*?,){15}P") })
        val r = tools.run { ws, _ -> searchFiles(slow, ws) }.content
        assertTrue((System.nanoTime() - started) / 1_000_000 < 5_000)
        assertEquals("true", r.s("truncated"))
        assertTrue(r.s("hint").contains("seconds"))
        for (i in 1..5) File(ws, "f$i.txt").writeText("hit")
        val few = WorkspaceTools(sb, { "conv1" }, false, SearchLimits(maxFiles = 2))
        val hit = Args(def, buildJsonObject { put("pattern", "hit") })
        val capped = few.run { ws, _ -> searchFiles(hit, ws) }.content
        assertEquals("true", capped.s("truncated"))
        assertTrue(capped["matches"]!!.jsonArray.size <= 2)
    }

    @Test fun runtimeToolsAreNotRegistered() {
        val names = com.yongpeng.vamos.core.assembly.knownToolNames()
        assertFalse("run_command" in names)
        assertFalse("run_bundled_script" in names)
        assertTrue(names.containsAll(listOf("read_file","write_file","search_files","share_file")))
    }

    @Test fun installPackagesAreNotShared() = runTest {
        File(ws, "app.apk").writeText("PK")
        File(ws, "notes.txt").writeText("n")
        val d = dispatcher()
        assertEquals("share_refused_type", d.execute(call("share_file", buildJsonObject { put("path", "app.apk") })).content.s("error"))
        assertEquals("share_refused_type", d.execute(call("share_file", buildJsonObject { put("path", "notes.txt"); put("name", "x.APKS") })).content.s("error"))
        assertTrue(sb.shared.isEmpty())
    }

    @Test fun longNamesFitInBytesAndKeepExtension() {
        val long = "长".repeat(200) + ".csv"
        val fit = com.yongpeng.vamos.core.cap.files.ShareRules.fitName(long)
        assertTrue(fit.endsWith(".csv"))
        assertTrue(fit.toByteArray().size <= com.yongpeng.vamos.core.cap.files.ShareRules.NAME_MAX_BYTES)
        assertEquals("short.txt", com.yongpeng.vamos.core.cap.files.ShareRules.fitName("short.txt"))
        val noExt = com.yongpeng.vamos.core.cap.files.ShareRules.fitName("名".repeat(200))
        assertTrue(noExt.toByteArray().size <= com.yongpeng.vamos.core.cap.files.ShareRules.NAME_MAX_BYTES)
        assertEquals("abc.d", com.yongpeng.vamos.core.cap.files.ShareRules.fitName("abcdefgh.d", maxChars = 5))
    }

    /** 沙箱工具产出的事实(命令输出、读到的文件、写入与导出的路径)进核查证据;命令进动作清单。 */
}
