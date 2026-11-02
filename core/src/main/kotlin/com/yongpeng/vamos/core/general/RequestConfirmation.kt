// Modified by Vamos (2026): brand and package migration.
package com.yongpeng.vamos.core.general

import com.yongpeng.vamos.core.gate.ConfirmationRequest
import com.yongpeng.vamos.core.risk.IrrevAction
import com.yongpeng.vamos.core.prompt.Prompts
import com.yongpeng.vamos.core.tool.ArgError
import com.yongpeng.vamos.core.tool.Args
import com.yongpeng.vamos.core.tool.CONFIRM_ACTIONS
import com.yongpeng.vamos.core.tool.CONFIRM_FIELD_MAX
import com.yongpeng.vamos.core.tool.HINT_PAYMENT_TAKEOVER
import com.yongpeng.vamos.core.tool.Outcome
import com.yongpeng.vamos.core.tool.Risk
import com.yongpeng.vamos.core.tool.ToolContext
import com.yongpeng.vamos.core.tool.ToolDef
import com.yongpeng.vamos.core.tool.ToolExecution
import com.yongpeng.vamos.core.tool.Verb
import com.yongpeng.vamos.core.tool.capField
import com.yongpeng.vamos.core.tool.fail
import com.yongpeng.vamos.core.tool.okObj
import com.yongpeng.vamos.core.tool.str
import kotlinx.serialization.json.put
import com.yongpeng.vamos.core.uistate.confirmTarget
import com.yongpeng.vamos.core.uistate.focus
import com.yongpeng.vamos.core.uistate.lastSnapshot
import com.yongpeng.vamos.core.uistate.session

/**
 * 花钱的那一步(下单、屏上有应付金额时的提交等)前请用户同意;询问时只有这一种要问。付款本身不在这里,交给用户亲手付。
 * 申报的前台核对用此刻重新读的一屏(不登记编号,不动模型手里的 ref)。
 */
internal object RequestConfirmation : SurfaceAware {
    override val verb = Verb.RequestConfirmation
    override val label = "请你同意"
    override val risk = Risk.Read
    override val def = ToolDef("request_confirmation",
        Prompts.tool("request_confirmation"),
        mapOf(
            "action" to str("Sensitive action: pay/order, send, delete, submit (publish), or other (settings).", CONFIRM_ACTIONS),
            "target" to str("Exact recipient, item, account or setting, as shown on screen. At most $CONFIRM_FIELD_MAX characters."),
            "content" to str("Exact action and content, including message text or setting change. At most $CONFIRM_FIELD_MAX characters."),
            "amount" to str("Amount and currency the user will pay, as shown on screen."),
            "app" to str(APP_NOTE),
        ), listOf("action", "target", "content", "app"))

    /** app 参数写什么按本任务的屏说:一块屏时写那块的写法,几块屏时各写一句。 */
    override fun def(ctx: ToolContext): ToolDef {
        val base = super.def(ctx)
        val surfaces = ctx.focus.surfaces
        val how = if (surfaces.size == 1) surfaces.single().targetNote
        else surfaces.joinToString("; ") { "on the ${it.id} screen, ${it.targetNote}" }
        return ToolDef(base.name, base.description, base.props + ("app" to str("$APP_NOTE: $how.")), base.required)
    }

    private const val APP_NOTE = "Where the action happens, as on screen now"

    override suspend fun execute(a: Args, ctx: ToolContext): ToolExecution {
        checkSurface(a, ctx)
        val action = a.str("action")!!.trim()
        val kind = IrrevAction.entries.firstOrNull { it.name == action }
            ?: throw ArgError("action", "must be one of ${CONFIRM_ACTIONS.joinToString("|")}")
        if (kind in listOf(IrrevAction.pay, IrrevAction.order) && a.str("amount")?.any(Char::isDigit) != true)
            throw ArgError("amount", "Payment/order requires the exact amount and currency shown on screen")
        val app = ctx.confirmTarget(a.str("app")!!.trim())
        val req = ConfirmationRequest(
            kind, capField(a.str("target", max = 10000)!!), capField(a.str("content", max = 10000)!!),
            a.str("amount", max = 10000)?.let(::capField), app,
        )
        val fg = ctx.session.peek()?.packageName ?: ctx.lastSnapshot?.packageName
        return ToolExecution(okObj { put("asked", true) }, outcome = Outcome.Confirm(req, fg))
    }
}
