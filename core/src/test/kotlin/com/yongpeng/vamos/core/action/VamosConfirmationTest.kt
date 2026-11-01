// Modified by Vamos (2026): confirmation policy regression matrix.
package com.yongpeng.vamos.core.action

import org.junit.Assert.*
import org.junit.Test
import com.yongpeng.vamos.core.run.*
import com.yongpeng.vamos.core.gate.*
import com.yongpeng.vamos.core.risk.IrrevAction

class VamosConfirmationTest {
    @Test fun allCategoriesRequireConfirmationByDefaultAndOnlyPaymentIsLocked() {
        val initial = ConfirmationRules()
        SensitiveCategory.entries.forEach { category ->
            val risk = Risk(ActionRisk.External, category == SensitiveCategory.Payment, category)
            assertEquals(Approval.Ask, ApprovalPolicy.decide(risk, rules = initial))
            val disabled = initial.with(category, false)
            assertEquals(if (category == SensitiveCategory.Payment) Approval.Ask else Approval.Allow, ApprovalPolicy.decide(risk, rules = disabled))
        }
    }
    @Test fun unattendedAndRemoteCannotApproveSensitiveActions() {
        for (category in SensitiveCategory.entries) {
            val risk = Risk(ActionRisk.External, category = category)
            assertEquals(Approval.Deny, ApprovalPolicy.decide(risk, RunProvenance(RunOrigin.Trigger, Attendance.Unattended)))
            assertEquals(Approval.Deny, ApprovalPolicy.decide(risk, RunProvenance(RunOrigin.Mcp, Attendance.Reachable)))
        }
    }
    @Test fun paymentDeclarationRequiresARealOneTimeDecision() {
        val gate = ConfirmationGate()
        val request = ConfirmationRequest(IrrevAction.pay, "商家", "付款", "¥10", "shop")
        assertEquals(DeclareResult.Pending, gate.declare(request, "shop"))
        assertFalse(gate.hasGrant)
        gate.decide(true)
        assertTrue(gate.hasGrant)
        gate.reset()
        assertFalse(gate.hasGrant)
    }
}
