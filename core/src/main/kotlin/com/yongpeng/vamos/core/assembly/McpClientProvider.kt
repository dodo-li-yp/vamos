// Modified by Vamos (2026): brand and package migration.
package com.yongpeng.vamos.core.assembly

import com.yongpeng.vamos.core.cap.mcp.McpCapability
import com.yongpeng.vamos.core.cap.mcp.McpConnection
import com.yongpeng.vamos.core.run.RunSpec

internal class McpClientProvider(private val connection: McpConnection) : CapabilityProvider {
    override val id: String = "mcp:${connection.endpoint.serverId}"
    override fun open(spec: RunSpec) = McpCapability(connection, id)
}
