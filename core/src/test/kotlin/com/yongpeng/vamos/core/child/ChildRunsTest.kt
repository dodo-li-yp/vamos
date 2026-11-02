// Modified by Vamos (2026): brand and package migration.
package com.yongpeng.vamos.core.child

import com.yongpeng.vamos.core.trigger.Scheduled
import com.yongpeng.vamos.core.agent.AgentLoop
import com.yongpeng.vamos.core.agent.FakeModel
import com.yongpeng.vamos.core.assembly.toolDispatcher
import com.yongpeng.vamos.core.cap.web.FakeWebPort
import com.yongpeng.vamos.core.model.Part
import com.yongpeng.vamos.core.observation.Trust
import com.yongpeng.vamos.core.run.Attendance
import com.yongpeng.vamos.core.run.RunId
import com.yongpeng.vamos.core.run.RunOrigin
import com.yongpeng.vamos.core.run.RunProvenance
import com.yongpeng.vamos.core.run.RunSpec
import com.yongpeng.vamos.core.testing.answerReply
import com.yongpeng.vamos.core.tool.ActionPass
import com.yongpeng.vamos.core.tool.FakeScreen
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChildRunsTest {
    @Test fun spawnReturnsBoundedObservationAndPersistsChildFirst() = runBlocking {
        val parent = RunSpec(RunId("parent"), "goal", "thread")
        val store = MemoryChildRunStore()
        val runner = ChildAgentRunner({ spec ->
            AgentLoop(FakeModel(answerReply("子结论")),
                toolDispatcher(FakeScreen(), web = FakeWebPort(), runSpec = spec), "BASE", confirmations = null)
        }, store, ownerAlive = { store.all().single().phase == ChildPhase.Completed })
        val tools = toolDispatcher(FakeScreen(), runSpec = parent, childHost = runner)
        val result = tools.execute(Part.ToolCall("s", "spawn_agent", """{"goal":"只查一条事实"}"""), ActionPass())
        val record = store.all().single()
        assertEquals(ChildPhase.Completed, record.phase)
        assertEquals("parent", record.parentId)
        assertEquals("子结论", record.result)
        assertEquals(Trust.Untrusted, result.observations.single().trust)
        assertEquals("子结论", result.observations.single().text)
    }

    @Test fun childRestrictionsCannotBeBypassedByRunSpecFlags() = runBlocking {
        val spec = RunSpec(RunId("child"), "research", "thread",
            provenance = RunProvenance(RunOrigin.Child, Attendance.Present),
            capabilities = null, readOnly = false, parent = RunId("parent"))
        val tools = toolDispatcher(FakeScreen(), web = FakeWebPort(), runSpec = spec,
            childHost = ChildRunHost { _, _ -> error("must not spawn") }, scheduleTrigger = { Scheduled(true) })
        val names = tools.definitions().map { it.name }
        assertTrue("web_read_page" in names)
        assertFalse(names.any { it.startsWith("phone_") || it == "web_interact" || it == "write_file" ||
            it == "spawn_agent" || it == "memory_save" || it == "schedule_task" ||
            it == "ask_user" || it == "hand_to_user" || it == "request_confirmation" })
        assertEquals("unknown_tool", tools.execute(Part.ToolCall("w", "web_interact", "{}"), ActionPass())
            .content["error"].toString().trim('"'))
    }

    @Test fun absentOwnerLeavesUnknownResultWithoutRerun() = runBlocking {
        val store = MemoryChildRunStore()
        val spec = RunSpec(RunId("c"), "research", "thread",
            provenance = RunProvenance(RunOrigin.Child, Attendance.Unattended), parent = RunId("p"))
        val runner = ChildAgentRunner({ s -> AgentLoop(FakeModel(answerReply("answer")),
            toolDispatcher(FakeScreen(), web = FakeWebPort(), runSpec = s), "BASE", confirmations = null) }, store,
            ownerAlive = { false })
        assertEquals(ChildPhase.Unknown, runner.spawn(spec, null).phase)
        assertEquals(ChildPhase.Unknown, store.get("c")?.phase)
    }
}
