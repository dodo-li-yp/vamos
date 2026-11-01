// Modified by Vamos (2026): brand and package migration.
package com.yongpeng.vamos.core.general

import com.yongpeng.vamos.core.child.ChildPhase
import com.yongpeng.vamos.core.observation.ObsKind
import com.yongpeng.vamos.core.observation.Observation
import com.yongpeng.vamos.core.observation.Trust
import com.yongpeng.vamos.core.observation.Untrusted
import com.yongpeng.vamos.core.prompt.Prompts
import com.yongpeng.vamos.core.run.Attendance
import com.yongpeng.vamos.core.run.RunId
import com.yongpeng.vamos.core.run.RunOrigin
import com.yongpeng.vamos.core.run.RunProvenance
import com.yongpeng.vamos.core.run.RunSpec
import com.yongpeng.vamos.core.tool.AgentTool
import com.yongpeng.vamos.core.tool.Args
import com.yongpeng.vamos.core.tool.Risk
import com.yongpeng.vamos.core.tool.ToolContext
import com.yongpeng.vamos.core.tool.ToolDef
import com.yongpeng.vamos.core.tool.ToolExecution
import com.yongpeng.vamos.core.tool.Verb
import com.yongpeng.vamos.core.tool.str
import java.util.UUID
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal object SpawnAgent : AgentTool {
    override val verb = Verb.SpawnAgent
    override val risk = Risk.Write
    override val offScreen = true
    override val def = ToolDef("spawn_agent", Prompts.tool("spawn_agent"), mapOf(
        "goal" to str("A bounded research question for the child agent, using web and sandbox reads only."),
        "format" to str("Optional output format.", listOf("text", "json")),
    ), listOf("goal"))

    override fun available(ctx: ToolContext) = ctx.config.childHost != null && ctx.config.runSpec != null

    override suspend fun execute(a: Args, ctx: ToolContext): ToolExecution {
        val parent = ctx.config.runSpec ?: error("no parent RunSpec")
        val host = ctx.config.childHost ?: error("no child host")
        val goal = a.str("goal", max = 4000)!!
        val format = a.str("format")
        val child = RunSpec(RunId(UUID.randomUUID().toString()), goal, parent.threadId,
            provenance = RunProvenance(RunOrigin.Child, Attendance.Unattended),
            capabilities = setOf("web", "sandbox"), readOnly = true, parent = parent.id, stepBudget = 15)
        val result = host.spawn(child, format)
        val text = result.text.take(4000)
        return ToolExecution(buildJsonObject {
            put("ok", result.phase == ChildPhase.Completed)
            put("childRunId", result.id.value)
            put("status", result.phase.name.lowercase())
            put("result", Untrusted.wrap(text))
        }, observations = listOf(Observation(ObsKind.Reply, text, source = "child", trust = Trust.Untrusted)))
    }
}
