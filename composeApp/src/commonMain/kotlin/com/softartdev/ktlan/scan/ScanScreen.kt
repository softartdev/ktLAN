@file:OptIn(ExperimentalMaterial3Api::class)

package com.softartdev.ktlan.scan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.softartdev.ktlan.Error
import com.softartdev.ktlan.Loader
import com.softartdev.ktlan.domain.model.HostModel
import com.softartdev.ktlan.presentation.scan.ScanAction
import com.softartdev.ktlan.presentation.scan.ScanResult
import com.softartdev.ktlan.presentation.scan.ScanViewModel
import ktlan.composeapp.generated.resources.Res
import ktlan.composeapp.generated.resources.end_ip
import ktlan.composeapp.generated.resources.networks_copy
import ktlan.composeapp.generated.resources.networks_use
import ktlan.composeapp.generated.resources.ports
import ktlan.composeapp.generated.resources.scan
import ktlan.composeapp.generated.resources.start_ip
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun ScanScreen(scanViewModel: ScanViewModel) {
    val scanResult: ScanResult by scanViewModel.stateFlow.collectAsState()
    LaunchedEffect(scanViewModel) { scanViewModel.launch() }
    ScanContent(
        modifier = Modifier.padding(8.dp),
        onAction = scanViewModel::onAction,
        scanResult = scanResult
    )
}

@Composable
fun ScanContent(
    modifier: Modifier = Modifier,
    onAction: (ScanAction) -> Unit,
    scanResult: ScanResult
) {
    when (scanResult) {
        is ScanResult.Loading -> Loader(modifier = modifier)
        is ScanResult.Success -> ScanSuccessContent(
            modifier = modifier,
            onAction = onAction,
            scanResult = scanResult
        )
        is ScanResult.Error -> Error(
            modifier = modifier,
            message = scanResult.message
        )
    }
}

@Composable
fun ScanSuccessContent(
    modifier: Modifier = Modifier,
    onAction: (ScanAction) -> Unit,
    scanResult: ScanResult.Success
) {
    val clipboard: ClipboardManager = LocalClipboardManager.current
    Column(modifier = modifier) {
        TextField(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            value = scanResult.startIp,
            onValueChange = { onAction(ScanAction.UpdateStartIp(it)) },
            label = { Text(text = stringResource(Res.string.start_ip)) }
        )
        TextField(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            value = scanResult.endIp,
            onValueChange = { onAction(ScanAction.UpdateEndIp(it)) },
            label = { Text(text = stringResource(Res.string.end_ip)) }
        )
        TextField(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            value = scanResult.ports.joinToString(),
            onValueChange = { onAction(ScanAction.UpdatePorts(it)) },
            label = { Text(text = stringResource(Res.string.ports)) }
        )
        Button(
            modifier = Modifier.fillMaxWidth().padding(all = 8.dp),
            content = { Text(text = stringResource(Res.string.scan)) },
            onClick = { onAction(ScanAction.LaunchScan) },
        )
        LazyColumn {
            items(scanResult.hosts) { result: HostModel ->
                ListItem(
                    leadingContent = { Text(text = "🟢") },
                    overlineContent = { Text(text = "ip / ports") },
                    headlineContent = { Text(text = result.ip) },
                    supportingContent = { Text(text = result.openPorts.joinToString()) },
                    trailingContent = {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { clipboard.setText(AnnotatedString(result.ip)) },
                                content = { Text(text = stringResource(Res.string.networks_copy)) }
                            )
                            Button(
                                onClick = { onAction(ScanAction.UseAsRemoteHost(result.ip)) },
                                content = { Text(text = stringResource(Res.string.networks_use)) }
                            )
                        }
                    }
                )
                HorizontalDivider()
            }
        }
    }
}

@Preview
@Composable
fun ScanScreenPreview() {
    ScanContent(
        modifier = Modifier.padding(8.dp),
        onAction = {},
        scanResult = ScanResult.Success(hosts = ScanResult.Success.previewHosts)
    )
}

@Preview
@Composable
fun ScanLoadingScreenPreview() {
    ScanContent(
        modifier = Modifier.padding(8.dp),
        onAction = {},
        scanResult = ScanResult.Loading
    )
}

@Preview
@Composable
fun ScanErrorScreenPreview() {
    ScanContent(
        modifier = Modifier.padding(8.dp),
        onAction = {},
        scanResult = ScanResult.Error("An error occurred")
    )
}
