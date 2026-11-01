// Modified by Vamos (2026): brand and package migration.
package com.yongpeng.vamos.core.mcp

import com.yongpeng.vamos.core.remote.RemoteHost
import com.yongpeng.vamos.core.remote.HttpResponse
import com.yongpeng.vamos.core.remote.HttpRequest
import com.yongpeng.vamos.core.device.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.*
import org.junit.Test

class McpDeviceToolsTest {
    private var writes = 0
    private val store = object : DeviceAccessStore {
        override val enabled = true
        override val credentialConfigured = false
        override val attemptBlocked = false
        override fun credential(): DeviceCredential? = null
        override fun beginAttempt(credential: DeviceCredential) = false
        override fun completeAttempt(credential: DeviceCredential) = Unit
    }
    private val port = object : DeviceAccessPort {
        override suspend fun state() = DeviceState(true, false, false, false, true, true)
        override suspend fun wake(): Boolean { writes++; return true }
        override suspend fun dismiss(): Boolean { writes++; return true }
        override suspend fun credentialReady(kind: CredentialKind) = false
        override suspend fun enter(credential: DeviceCredential): Boolean { writes++; return false }
    }
    private val tools = McpTaskTools(FakeRemoteTasks(), device = DeviceAccess(port, store))

    @Test fun productionToolSetAddsOnlyThreeCredentialFreeDeviceTools() = runTest {
        assertEquals(McpTaskTools(FakeRemoteTasks()).tools.size + 3, tools.tools.size)
        assertTrue(tools.has("vamos_device_state"))
        assertTrue(tools.has("vamos_wake_device"))
        assertTrue(tools.has("vamos_unlock_device"))
        val result = tools.call("vamos_unlock_device", JsonObject(emptyMap()))
        assertEquals(JsonPrimitive("unlocked"), result["status"])
        assertEquals(0, writes)
        assertFalse(result.toString().contains("password"))
    }

    @Test fun credentialsAndUnknownArgumentsAreRejectedBeforeExecution() = runTest {
        for (name in listOf("vamos_device_state", "vamos_wake_device", "vamos_unlock_device")) {
            try {
                tools.call(name, JsonObject(mapOf("pin" to JsonPrimitive("123456"))))
                fail("unexpected argument accepted")
            } catch (e: McpTaskTools.ToolError) { assertFalse(e.message!!.contains("123456")) }
        }
        assertEquals(0, writes)
    }

    @Test fun deviceToolsUseTheExistingTokenAndBrowserGuards() {
        val host = RemoteHost(FakeRemoteTasks(), { "test-token" }, 0, false, "test", device = DeviceAccess(port, store))
        val body = """{"jsonrpc":"2.0","id":1,"method":"tools/call","params":{"name":"vamos_unlock_device","arguments":{}}}""".toByteArray()
        fun call(token: String?, origin: String? = null): HttpResponse {
            val headers = mutableMapOf("content-type" to "application/json")
            token?.let { headers["authorization"] = "Bearer $it" }
            origin?.let { headers["origin"] = it }
            return host.handle(HttpRequest("POST", "/mcp", headers, body, java.net.InetAddress.getLoopbackAddress()))
        }
        assertEquals(401, call(null).status)
        assertEquals(401, call("incorrect").status)
        assertEquals(403, call("test-token", "https://example.com").status)
        assertEquals(200, call("test-token").status)
        assertEquals(0, writes)
    }
}
