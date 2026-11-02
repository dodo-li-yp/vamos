// Modified by Vamos (2026): brand and package migration.
package com.yongpeng.vamos.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * 询问 / 自主(新任务开工时取它),存成一个小 JSON 文件;读不懂按询问。
 * 先写临时文件再改名,写一半断电不会留下坏文件。
 */
class ApprovalStore(private val file: File) {
    private val _rules = MutableStateFlow(com.yongpeng.vamos.core.action.ConfirmationRules())
    val rules = _rules.asStateFlow()
    private val _autonomous: MutableStateFlow<Boolean>

    init {
        val o = runCatching { Json.parseToJsonElement(file.readText()) as? JsonObject }.getOrNull()
        fun enabled(key: String) = (o?.get(key) as? JsonPrimitive)?.booleanOrNull ?: true
        _rules.value = com.yongpeng.vamos.core.action.ConfirmationRules(enabled("message"), enabled("delete"), enabled("settings"), enabled("publish"))
        _autonomous = MutableStateFlow((o?.get("autonomous") as? JsonPrimitive)?.booleanOrNull ?: false)
    }

    /** 自主模式:打开时连付款也不先问你(询问时敏感操作按类别确认)。换它请走 [ApprovalControl.setAutonomous],正在跑的任务才会跟着换。 */
    val autonomous: StateFlow<Boolean> = _autonomous.asStateFlow()

    @Synchronized fun setAutonomous(on: Boolean) {
        _autonomous.value = on
        save()
    }

    @Synchronized fun setRule(category: com.yongpeng.vamos.core.action.SensitiveCategory, enabled: Boolean) {
        val next = _rules.value.with(category, enabled)
        persist(next)
        _rules.value = next
    }

    private fun persist(rules: com.yongpeng.vamos.core.action.ConfirmationRules) {
        file.parentFile?.mkdirs()
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(buildJsonObject {
            put("autonomous", _autonomous.value)
            put("message", rules.message); put("delete", rules.delete)
            put("settings", rules.settings); put("publish", rules.publish)
        }.toString())
        Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    }

    @Synchronized private fun save() = persist(_rules.value)
}
