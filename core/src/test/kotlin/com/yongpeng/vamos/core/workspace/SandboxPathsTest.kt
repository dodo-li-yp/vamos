// Modified by Vamos (2026): brand and package migration.
package com.yongpeng.vamos.core.workspace

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import com.yongpeng.vamos.core.cap.files.FakeSandbox

class WorkspacePathsTest {
    @Test fun relativeResolvesUnderWorkspace() {
        assertEquals("/workspace/a/b.txt", WorkspacePaths.normalize("a/b.txt"))
        assertEquals("/workspace/a/b.txt", WorkspacePaths.normalize("./a//c/../b.txt"))
        assertEquals("/workspace/sub/x", WorkspacePaths.normalize("x", "/workspace/sub"))
        assertEquals("/workspace", WorkspacePaths.normalize("/workspace/"))
    }

    @Test fun dotDotEscapeRejected() {
        assertNull(WorkspacePaths.normalize("../etc/passwd"))
        assertNull(WorkspacePaths.normalize("/workspace/../etc"))
        assertNull(WorkspacePaths.normalize("/etc/passwd"))
        assertNull(WorkspacePaths.normalize("/workspacex/a"))
        assertNull(WorkspacePaths.normalize("/opt/vamos/skills/../../../root"))
        assertNull(WorkspacePaths.resolveHost(Files.createTempDirectory("r").toFile(), "../x"))
    }

    @Test fun skillsReadableNotWritable() {
        val p = WorkspacePaths.normalize("/opt/vamos/skills/app/x/scripts/a.py")
        assertEquals("/opt/vamos/skills/app/x/scripts/a.py", p)
        assertFalse(WorkspacePaths.isWritable(p!!))
        assertTrue(WorkspacePaths.isWritable(WorkspacePaths.normalize("out/a.png")!!))
        // 宿主映射也一样:技能目录只能读
        val sb = FakeSandbox()
        assertNotNull(sb.hostPath("c1", "/opt/vamos/skills/app/x/scripts/a.py", forWrite = false))
        assertNull(sb.hostPath("c1", "/opt/vamos/skills/app/x/scripts/a.py", forWrite = true))
    }

    @Test fun symlinkEscapeRejected() {
        val tmp = Files.createTempDirectory("paths").toFile()
        val root = File(tmp, "ws").apply { mkdirs() }
        val outside = File(tmp, "outside").apply { mkdirs() }
        File(outside, "secret").writeText("key")
        File(root, "inner").mkdirs()
        Files.createSymbolicLink(File(root, "x").toPath(), outside.toPath())
        Files.createSymbolicLink(File(root, "dangling").toPath(), File(outside, "not-yet").toPath())
        Files.createSymbolicLink(File(root, "ok").toPath(), File(root, "inner").toPath())

        assertNull(WorkspacePaths.resolveHost(root, "x"))
        assertNull(WorkspacePaths.resolveHost(root, "x/secret"))
        // 指向外面、目标还不存在的链接:写下去就会落到外面
        assertNull(WorkspacePaths.resolveHost(root, "dangling"))
        assertEquals(File(root, "inner/f").canonicalFile, WorkspacePaths.resolveHost(root, "ok/f"))
        assertEquals(File(root, "new/file.txt").canonicalFile, WorkspacePaths.resolveHost(root, "new/file.txt"))
    }
}
