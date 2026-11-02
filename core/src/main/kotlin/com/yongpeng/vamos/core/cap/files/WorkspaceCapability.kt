// Modified by Vamos (2026): Kotlin file tools without a Linux runtime.
package com.yongpeng.vamos.core.cap.files
import com.yongpeng.vamos.core.capability.*
import com.yongpeng.vamos.core.tool.*
internal class WorkspaceCapability(private val port:WorkspacePort?,workspace:()->String,visionEnabled:Boolean):Capability {
 override val id = FILES
 val runner = port?.let { WorkspaceTools(it,workspace,visionEnabled) }
 fun available() = port != null
 override val folds = true
 override fun status():CapabilityStatus = if(available()) CapabilityStatus.Available else CapabilityStatus.Unavailable("no workspace")
 override fun tools():List<AgentTool> = WorkspaceGroup.tools
 override fun preload(task:String) = available() && "/workspace/" in task
 override fun sections(env:PromptEnv) = if(env.isOpen(id)) listOf("Private workspace file tools. Paths stay under /workspace. No shell, Python, Node or executable scripts are available.") else emptyList()
 override fun onTaskStart(ctx:ToolContext) {ctx[KEY]=this}
 companion object {val KEY = CapabilityKey<WorkspaceCapability>(FILES)}
}
internal val ToolContext.workspaceRunner:WorkspaceTools? get() = this[WorkspaceCapability.KEY]?.runner
internal fun ToolContext.workspaceAvailable() = this[WorkspaceCapability.KEY]?.available() == true
