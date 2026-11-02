// Modified by Vamos (2026): brand and package migration.
package com.yongpeng.vamos.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yongpeng.vamos.R
import com.yongpeng.vamos.core.mcp.McpUsb
import com.yongpeng.vamos.core.remote.RemoteActivity
import com.yongpeng.vamos.mcp.McpRuntime
import com.yongpeng.vamos.mcp.McpServerState
import com.yongpeng.vamos.ui.components.ButtonKind
import com.yongpeng.vamos.ui.components.ButtonSize
import com.yongpeng.vamos.ui.components.FooterNote
import com.yongpeng.vamos.ui.components.GroupCard
import com.yongpeng.vamos.ui.components.GroupLabel
import com.yongpeng.vamos.ui.components.VmListItem
import com.yongpeng.vamos.ui.components.VmButton
import com.yongpeng.vamos.ui.components.VmSwitch
import com.yongpeng.vamos.ui.components.PageScaffold
import com.yongpeng.vamos.ui.components.RadioRow
import com.yongpeng.vamos.ui.components.RowActions
import com.yongpeng.vamos.ui.components.SegmentedControl
import com.yongpeng.vamos.ui.components.TileTone
import com.yongpeng.vamos.ui.theme.VamosIcons
import com.yongpeng.vamos.ui.theme.VmTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** 展示现有连接状态,不把启用开关或监听端口当作电脑已连接。刷新仍只在本页,沿用两秒间隔。 */
@Composable
fun McpScreen(runtime: McpRuntime, onBack: () -> Unit) {
    val context = LocalContext.current
    val enabled by runtime.settings.enabled.collectAsState()
    val usbOn by runtime.usb.enabled.collectAsState()
    val usbCommand = remember { runtime.usbCommand() }
    val state by runtime.state.collectAsState()
    var addresses by remember { mutableStateOf<List<String>>(emptyList()) }
    var activity by remember { mutableStateOf(RemoteActivity()) }
    var token by remember { mutableStateOf<String?>(null) }
    var confirmRegenerate by remember { mutableStateOf(false) }
    LaunchedEffect(enabled, usbOn) {
        token = if (enabled) withContext(Dispatchers.IO) { runCatching { runtime.settings.token() }.getOrNull() } else null
        if (!enabled) addresses = emptyList()
        if (!enabled && !usbOn) { activity = RemoteActivity(); return@LaunchedEffect }
        while (true) {
            if (enabled) addresses = withContext(Dispatchers.IO) { McpRuntime.addresses() }
            activity = runtime.activity
            delay(2_000)
        }
    }
    ComputerAccessPage(
        ComputerAccessUi(state, enabled, usbOn, (state as? McpServerState.Listening)?.port ?: runtime.settings.port,
            activity, addresses, token, usbCommand),
        onBack, runtime::setUsbEnabled, runtime::setEnabled,
        onCopy = { copy(context, it) }, onRegenerate = { confirmRegenerate = true },
        deviceSettings = { DeviceAccessSection(runtime.device.settings) },
    )
    if (confirmRegenerate) AlertDialog(
        onDismissRequest = { confirmRegenerate = false },
        title = { Text(stringResource(R.string.mcp_regenerate_title)) },
        text = { Text(stringResource(R.string.mcp_regenerate_body)) },
        confirmButton = { TextButton({ confirmRegenerate = false; token = runtime.regenerateToken() }) { Text(stringResource(R.string.mcp_regenerate_confirm)) } },
        dismissButton = { TextButton({ confirmRegenerate = false }) { Text(stringResource(R.string.cancel)) } },
    )
}

/** 一份只读页面快照。引导选择和折叠状态不写入连接设置。 */
internal data class ComputerAccessUi(
    val server: McpServerState,
    val networkEnabled: Boolean,
    val usbEnabled: Boolean,
    val port: Int,
    val activity: RemoteActivity,
    val addresses: List<String>,
    val token: String?,
    val usbCommand: String,
)

@Composable
internal fun ComputerAccessPage(
    ui: ComputerAccessUi,
    onBack: () -> Unit,
    onUsbEnabled: (Boolean) -> Unit,
    onNetworkEnabled: (Boolean) -> Unit,
    onCopy: (String) -> Unit,
    onRegenerate: () -> Unit,
    deviceSettings: @Composable () -> Unit,
) {
    var client by remember { mutableStateOf(0) }
    var advanced by remember { mutableStateOf(false) }
    var network by remember { mutableStateOf(false) }
    PageScaffold(stringResource(R.string.mcp_title), onBack) {
        ConnectionStatus(ui)
        GroupLabel(stringResource(R.string.mcp_choose_client))
        SegmentedControl(listOf("CLI / Skill", "MCP"), client, { client = it },
            Modifier.padding(bottom = 12.dp), tags = listOf("computer-cli", "computer-mcp"))
        GroupLabel(stringResource(R.string.mcp_choose_transport))
        SegmentedControl(listOf(stringResource(R.string.mcp_transport_usb), stringResource(R.string.mcp_transport_network)),
            if (network) 1 else 0, { network = it == 1 }, Modifier.padding(bottom = 12.dp),
            tags = listOf("computer-usb", "computer-network"))
        if (client == 0) CliInstallGuide(onCopy)
        when {
            network -> NetworkGuide(ui, mcp = client == 1, onNetworkEnabled, onCopy)
            client == 0 -> GuideStep(stringResource(R.string.mcp_cli_connect), stringResource(R.string.mcp_cli_connect_sub),
                stringResource(R.string.mcp_copy_command), { onCopy("vamos connect") }, command = "vamos connect")
            else -> McpUsbGuide(ui, onCopy)
        }

        GroupCard {
            VmListItem(VamosIcons.Gear, stringResource(if (advanced) R.string.mcp_advanced_hide else R.string.mcp_advanced),
                stringResource(R.string.mcp_advanced_sub), first = true, onClick = { advanced = !advanced })
        }
        if (advanced) {
            GroupCard {
                VmListItem(VamosIcons.Link, stringResource(R.string.mcp_usb_title),
                    stringResource(if (ui.usbEnabled) R.string.mcp_usb_on else R.string.mcp_usb_off_sub), first = true,
                    onClick = { onUsbEnabled(!ui.usbEnabled) }) {
                    VmSwitch(ui.usbEnabled, onUsbEnabled, Modifier.testTag("mcp-usb"))
                }
                VmListItem(VamosIcons.Desktop, stringResource(R.string.mcp_network_title), stringResource(R.string.mcp_network_scope),
                    onClick = { onNetworkEnabled(!ui.networkEnabled) }) {
                    VmSwitch(ui.networkEnabled, onNetworkEnabled, Modifier.testTag("mcp-enabled"))
                }
            }
            FooterNote(stringResource(R.string.mcp_port_detail, ui.port))
            if (ui.networkEnabled) {
                GroupLabel(stringResource(R.string.mcp_token))
                GroupCard {
                    VmListItem(VamosIcons.Key, ui.token?.let { it.take(6) + "…" + it.takeLast(4) } ?: "…", first = true)
                    RowActions {
                        VmButton(stringResource(R.string.mcp_copy), { ui.token?.let(onCopy) }, size = ButtonSize.Small, enabled = ui.token != null)
                        VmButton(stringResource(R.string.mcp_regenerate), onRegenerate, kind = ButtonKind.Tonal, size = ButtonSize.Small)
                    }
                }
            }
            deviceSettings()
        }
    }
}

@Composable
private fun ConnectionStatus(ui: ComputerAccessUi) {
    val enabled = ui.networkEnabled || ui.usbEnabled
    val received = enabled && ui.server is McpServerState.Listening && ui.activity.lastRequestAtMs != null
    val title = when {
        !enabled -> stringResource(R.string.mcp_access_off)
        ui.server is McpServerState.Failed -> stringResource(R.string.mcp_failed, ui.server.message)
        ui.server is McpServerState.Off -> stringResource(R.string.mcp_starting)
        received -> stringResource(R.string.mcp_request_received)
        else -> stringResource(R.string.mcp_waiting)
    }
    val detail = if (received) stringResource(R.string.mcp_last_request, clock(ui.activity.lastRequestAtMs!!))
        else stringResource(R.string.mcp_follow_steps)
    val tone = when { ui.server is McpServerState.Failed && enabled -> TileTone.Bad; received -> TileTone.Ok; else -> TileTone.Neutral }
    GroupCard(Modifier.testTag("computer-status")) {
        VmListItem(if (received) VamosIcons.Check else VamosIcons.Desktop, title, detail, first = true, tone = tone)
    }
}

@Composable
private fun CliInstallGuide(onCopy: (String) -> Unit) {
    val install = stringResource(R.string.mcp_cli_install_prompt)
    GuideStep(stringResource(R.string.mcp_cli_install), stringResource(R.string.mcp_cli_install_sub),
        stringResource(R.string.mcp_copy_install), { onCopy(install) })
}

@Composable
private fun McpUsbGuide(ui: ComputerAccessUi, onCopy: (String) -> Unit) {
    GuideStep(stringResource(R.string.mcp_usb_start), stringResource(R.string.mcp_usb_start_sub),
        stringResource(R.string.mcp_copy_command), { onCopy(ui.usbCommand) }, command = ui.usbCommand)
    val forward = McpUsb.forward(ui.port)
    GuideStep(stringResource(R.string.mcp_usb_forward), stringResource(R.string.mcp_usb_forward_sub),
        stringResource(R.string.mcp_copy_command), { onCopy(forward) }, command = forward)
    val instructions = stringResource(R.string.mcp_usb_client_instructions, ui.port)
    GuideStep(stringResource(R.string.mcp_usb_client), stringResource(R.string.mcp_usb_client_sub),
        stringResource(R.string.mcp_copy_instructions), { onCopy(instructions) })
}

@Composable
private fun NetworkGuide(ui: ComputerAccessUi, mcp: Boolean, onNetworkEnabled: (Boolean) -> Unit, onCopy: (String) -> Unit) {
    var selectedAddress by remember { mutableStateOf<String?>(null) }
    var preview by remember { mutableStateOf(false) }
    if (!ui.networkEnabled) {
        GuideStep(stringResource(R.string.mcp_network_enable), stringResource(R.string.mcp_network_scope),
            stringResource(R.string.mcp_network_enable_action), { onNetworkEnabled(true) })
        return
    }
    val address = selectedAddress?.takeIf { it in ui.addresses } ?: ui.addresses.firstOrNull()
    GroupLabel(stringResource(if (mcp) R.string.mcp_network_address_step else R.string.mcp_address))
    GroupCard {
        if (ui.addresses.isEmpty()) VmListItem(VamosIcons.Link, stringResource(R.string.mcp_no_address), first = true)
        ui.addresses.forEachIndexed { i, a ->
            RadioRow(if (mcp) McpRuntime.url(a, ui.port) else "http://$a:${ui.port}",
                a == address, { selectedAddress = a }, first = i == 0)
        }
    }
    FooterNote(stringResource(R.string.mcp_address_foot))
    if (!mcp) {
        val command = address?.let { "vamos connect --url http://$it:${ui.port} --token-file /path/to/token" }
        GroupCard {
            VmListItem(VamosIcons.Code, stringResource(R.string.mcp_cli_connect), stringResource(R.string.mcp_cli_network_sub), first = true)
            command?.let { CommandText(it) }
            RowActions {
                VmButton(stringResource(R.string.mcp_copy_command), { command?.let(onCopy) }, size = ButtonSize.Small,
                    enabled = command != null && ui.token != null && ui.server is McpServerState.Listening,
                    modifier = Modifier.testTag("computer-copy-cli-network"))
                VmButton(stringResource(R.string.mcp_copy_token), { ui.token?.let(onCopy) }, kind = ButtonKind.Tonal,
                    size = ButtonSize.Small, enabled = ui.token != null)
            }
        }
        FooterNote(stringResource(R.string.mcp_cli_connection_check))
        return
    }
    GroupCard {
        VmListItem(VamosIcons.Code, stringResource(R.string.mcp_network_config_step), stringResource(R.string.mcp_network_config_sub), first = true)
        val canCopy = address != null && ui.token != null && ui.server is McpServerState.Listening
        RowActions {
            VmButton(stringResource(R.string.mcp_copy_config), {
                if (canCopy) onCopy(McpRuntime.clientConfig(McpRuntime.url(address!!, ui.port), ui.token!!))
            }, size = ButtonSize.Small, enabled = canCopy, modifier = Modifier.testTag("computer-copy-config"))
            VmButton(stringResource(if (preview) R.string.mcp_hide_preview else R.string.mcp_show_preview), { preview = !preview },
                kind = ButtonKind.Text, size = ButtonSize.Small)
        }
        if (preview) {
            // 预览只用占位符;真实令牌只在用户点击复制时进入剪贴板。
            val config = McpRuntime.clientConfig(address?.let { McpRuntime.url(it, ui.port) } ?: "<address>", "<token>")
            CommandText(config, Modifier.testTag("computer-config-preview"))
        }
    }
    FooterNote(stringResource(R.string.mcp_network_verify))
}

@Composable
private fun GuideStep(title: String, body: String, button: String, onClick: () -> Unit, command: String? = null) {
    GroupCard {
        VmListItem(null, title, body, first = true)
        if (command != null) CommandText(command)
        RowActions { VmButton(button, onClick, size = ButtonSize.Small) }
    }
}

@Composable
private fun CommandText(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        fontFamily = FontFamily.Monospace, fontSize = 14.sp, lineHeight = 20.sp, color = VmTheme.colors.ink)
}

private fun clock(ms: Long): String =
    DateTimeFormatter.ofPattern("HH:mm:ss").format(Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()))

private fun copy(context: Context, text: String) {
    context.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("Vamos", text))
    Toast.makeText(context, context.getString(R.string.mcp_copied), Toast.LENGTH_SHORT).show()
}
