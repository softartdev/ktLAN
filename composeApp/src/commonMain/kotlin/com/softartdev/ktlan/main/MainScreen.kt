@file:OptIn(ExperimentalMaterial3Api::class)

package com.softartdev.ktlan.main

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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.softartdev.ktlan.Error
import com.softartdev.ktlan.Loader
import com.softartdev.ktlan.domain.model.HostModel
import com.softartdev.ktlan.presentation.main.MainScanAction
import com.softartdev.ktlan.presentation.main.MainScanResult
import com.softartdev.ktlan.presentation.main.MainViewModel
import ktlan.composeapp.generated.resources.Res
import ktlan.composeapp.generated.resources.app_name
import ktlan.composeapp.generated.resources.end_ip
import ktlan.composeapp.generated.resources.ports
import ktlan.composeapp.generated.resources.scan
import ktlan.composeapp.generated.resources.start_ip
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun MainScreen(mainViewModel: MainViewModel) {
    val resultState: State<MainScanResult> = mainViewModel.stateFlow.collectAsState()
    MainScreen(resultState.value, mainViewModel::onAction)
}

@Composable
fun MainScreen(result: MainScanResult, onAction: (MainScanAction) -> Unit) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(text = stringResource(Res.string.app_name)) }) },
    ) { paddingValues ->
        when (result) {
            is MainScanResult.Loading -> Loader(
                modifier = Modifier.padding(paddingValues)
            )
            is MainScanResult.Success -> MainContent(
                modifier = Modifier.padding(paddingValues),
                onAction = onAction,
                scanResult = result
            )
            is MainScanResult.Error -> Error(
                modifier = Modifier.padding(paddingValues),
                message = result.message
            )
        }
    }
}

@Composable
fun MainContent(
    modifier: Modifier = Modifier,
    onAction: (MainScanAction) -> Unit,
    scanResult: MainScanResult.Success
) {
    Column(modifier = modifier) {
        TextField(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            value = scanResult.startIp,
            onValueChange = { onAction(MainScanAction.UpdateStartIp(it)) },
            label = { Text(text = stringResource(Res.string.start_ip)) }
        )
        TextField(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            value = scanResult.endIp,
            onValueChange = { onAction(MainScanAction.UpdateEndIp(it)) },
            label = { Text(text = stringResource(Res.string.end_ip)) }
        )
        TextField(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            value = scanResult.ports.joinToString(),
            onValueChange = { onAction(MainScanAction.UpdatePorts(it)) },
            label = { Text(text = stringResource(Res.string.ports)) }
        )
        Button(
            modifier = Modifier.fillMaxWidth().padding(all = 8.dp),
            content = { Text(text = stringResource(Res.string.scan)) },
            onClick = { onAction(MainScanAction.LaunchScan) },
        )
        LazyColumn {
            items(scanResult.hosts) { result ->
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
fun MainScreenPreview(
//    @PreviewParameter(MainScanResultPreviewProvider::class) result: MainScanResult
) {
    MainScreen(
        result = MainScanResult.Success(
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
fun MainLoadingScreenPreview() {
    MainScreen(result = MainScanResult.Loading, onAction = {})
}

@Preview
@Composable
fun MainErrorScreenPreview() {
    MainScreen(result = MainScanResult.Error("An error occurred"), onAction = {})
}
