package com.softartdev.ktlan.networks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.softartdev.ktlan.domain.model.NetworkInterfaceInfo
import com.softartdev.ktlan.presentation.networks.NetworksAction
import com.softartdev.ktlan.presentation.networks.NetworksResult
import com.softartdev.ktlan.presentation.networks.NetworksViewModel
import ktlan.composeapp.generated.resources.Res
import ktlan.composeapp.generated.resources.networks_copy
import ktlan.composeapp.generated.resources.networks_flag_loopback
import ktlan.composeapp.generated.resources.networks_flag_multicast
import ktlan.composeapp.generated.resources.networks_flag_up
import ktlan.composeapp.generated.resources.networks_flags
import ktlan.composeapp.generated.resources.networks_interface
import ktlan.composeapp.generated.resources.networks_ipv4
import ktlan.composeapp.generated.resources.networks_ipv6
import ktlan.composeapp.generated.resources.networks_no_items
import ktlan.composeapp.generated.resources.networks_refresh
import ktlan.composeapp.generated.resources.networks_title
import ktlan.composeapp.generated.resources.networks_use
import ktlan.composeapp.generated.resources.networks_your_ip
import ktlan.composeapp.generated.resources.scan
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun NetworksScreen(viewModel: NetworksViewModel) {
    val result: NetworksResult by viewModel.stateFlow.collectAsState()
    LaunchedEffect(viewModel) { viewModel.launch() }
    NetworksContent(result, viewModel::onAction)
}

@Composable
fun NetworksContent(result: NetworksResult, onAction: (NetworksAction) -> Unit) {
    Column(modifier = Modifier.padding(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(stringResource(Res.string.networks_title))
            Button(onClick = { onAction(NetworksAction.Refresh) }) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(Modifier.padding(horizontal = 4.dp))
                Text(stringResource(Res.string.networks_refresh))
            }
        }
        result.yourIp?.let { ip: String ->
            Text(text = stringResource(Res.string.networks_your_ip) + ": $ip")
        }
        Spacer(Modifier.padding(vertical = 4.dp))
        if (result.interfaces.isEmpty()) {
            Text(stringResource(Res.string.networks_no_items))
        } else {
            LazyColumn {
                items(result.interfaces) { ni: NetworkInterfaceInfo ->
                    NetworkInterfaceItem(ni, onAction)
                }
            }
        }
    }
}

@Composable
private fun NetworkInterfaceItem(
    info: NetworkInterfaceInfo,
    onAction: (NetworksAction) -> Unit,
) {
    val clipboard: ClipboardManager = LocalClipboardManager.current
    Card(modifier = Modifier.padding(vertical = 4.dp)) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(stringResource(Res.string.networks_interface) + ": ${info.name}")
            Text(stringResource(Res.string.networks_flags) + ": " + buildString {
                if (info.isUp) append(stringResource(Res.string.networks_flag_up) + " ")
                if (info.isLoopback) append(stringResource(Res.string.networks_flag_loopback) + " ")
                if (info.supportsMulticast) append(stringResource(Res.string.networks_flag_multicast))
            })
            if (info.ipv4.isNotEmpty()) {
                Text(stringResource(Res.string.networks_ipv4))
                info.ipv4.forEach { ip ->
                    ListItem(
                        headlineContent = { Text(ip) },
                        supportingContent = {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { clipboard.setText(AnnotatedString(ip)) },
                                    content = { Text(stringResource(Res.string.networks_copy)) }
                                )
                                Button(
                                    onClick = { onAction(NetworksAction.UseAsBindHost(ip)) },
                                    content = { Text(stringResource(Res.string.networks_use)) }
                                )
                                Button(
                                    onClick = { onAction(NetworksAction.Scan(ip)) },
                                    content = { Text(stringResource(Res.string.scan)) }
                                )
                            }
                        }
                    )
                }
            }
            if (info.ipv6.isNotEmpty()) {
                Text(stringResource(Res.string.networks_ipv6))
                info.ipv6.forEach { ip ->
                    ListItem(headlineContent = { Text(ip) })
                }
            }
        }
    }
}

@Preview
@Composable
fun NetworksPreview() = Surface {
    NetworksContent(
        result = NetworksResult(interfaces = NetworksResult.previewInterfaces),
        onAction = {}
    )
}
