// Modified by Vamos (2026): brand and package migration.
package com.yongpeng.vamos

import com.yongpeng.vamos.core.action.ActionExecutor
import com.yongpeng.vamos.core.action.ActionRecorder
import com.yongpeng.vamos.core.action.Origin
import com.yongpeng.vamos.core.action.execution
import com.yongpeng.vamos.core.guard.OperationGuards
import com.yongpeng.vamos.core.model.Part
import com.yongpeng.vamos.core.record.RunSink
import com.yongpeng.vamos.core.tool.ToolDispatcher
import com.yongpeng.vamos.core.tool.ToolExecution

/** 设备测试里直接做一个工具调用:和产品一样经执行入口(不配确认关卡,不等锁屏)。 */
suspend fun ToolDispatcher.runAction(call: Part.ToolCall): ToolExecution =
    ActionExecutor(this, OperationGuards(), null, gate = {}, recorder = ActionRecorder(RunSink.NONE)).run(call, Origin.Model(0)).execution()
