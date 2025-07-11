@file:OptIn(ExperimentalMaterial3Api::class)

package com.softartdev.ktlan.scan

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.softartdev.ktlan.Error
import com.softartdev.ktlan.Loader
import com.softartdev.ktlan.domain.model.HostModel
import com.softartdev.ktlan.presentation.scan.ScanAction
import com.softartdev.ktlan.presentation.scan.ScanResult
import com.softartdev.ktlan.presentation.scan.ScanViewModel
import ktlan.composeapp.generated.resources.Res
import ktlan.composeapp.generated.resources.end_ip
import ktlan.composeapp.generated.resources.ports
import ktlan.composeapp.generated.resources.scan
import ktlan.composeapp.generated.resources.start_ip
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun ScanScreen(scanViewModel: ScanViewModel) {
    val resultState: State<ScanResult> = scanViewModel.stateFlow.collectAsState()
    ScanScreen(resultState.value, scanViewModel::onAction)
}

@Composable
fun ScanScreen(result: ScanResult, onAction: (ScanAction) -> Unit) {
    Scaffold { paddingValues ->
        when (result) {
            is ScanResult.Loading -> Loader(
                modifier = Modifier.padding(paddingValues)
            )
            is ScanResult.Success -> ScanContent(
                modifier = Modifier.padding(paddingValues),
                onAction = onAction,
                scanResult = result
            )
            is ScanResult.Error -> Error(
                modifier = Modifier.padding(paddingValues),
                message = result.message
            )
        }
    }
}

@Composable
fun ScanContent(
    modifier: Modifier = Modifier,
    onAction: (ScanAction) -> Unit,
    scanResult: ScanResult.Success
) {
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
                    trailingContent = { Text(text = "🛜") },
                )
                HorizontalDivider()
            }
        }
    }
}

@Preview
@Composable
fun ScanScreenPreview(
//    @PreviewParameter(MainScanResultPreviewProvider::class) result: MainScanResult
) {
    ScanScreen(
        result = ScanResult.Success(
            hosts = listOf(
                HostModel(ip = "192.168.0.1", openPorts = listOf(22, 80, 443)),
                HostModel(ip = "192.168.0.2", openPorts = listOf(21, 25, 8080)),
                HostModel(ip = "192.168.0.3", openPorts = listOf(53, 3306, 6379))
            )
        ),
        onAction = {}
    )
}

@Preview
@Composable
fun ScanLoadingScreenPreview() {
    ScanScreen(result = ScanResult.Loading, onAction = {})
}

@Preview
@Composable
fun ScanErrorScreenPreview() {
    ScanScreen(result = ScanResult.Error("An error occurred"), onAction = {})
}
