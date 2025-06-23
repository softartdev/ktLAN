@file:OptIn(ExperimentalMaterial3Api::class)

package com.softartdev.ktlan.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.softartdev.ktlan.presentation.main.MainScanAction
import com.softartdev.ktlan.presentation.main.MainScanResult
import com.softartdev.ktlan.presentation.main.MainViewModel
import ktlan.composeapp.generated.resources.Res
import ktlan.composeapp.generated.resources.app_name
import ktlan.composeapp.generated.resources.end_ip
import ktlan.composeapp.generated.resources.ports
import ktlan.composeapp.generated.resources.start_ip
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.ui.tooling.preview.PreviewParameter

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
            value = scanResult.startIp,
            onValueChange = { onAction(MainScanAction.UpdateStartIp(it)) },
            label = { Text(text = stringResource(Res.string.start_ip)) }
        )
        TextField(
            value = scanResult.endIp,
            onValueChange = { onAction(MainScanAction.UpdateEndIp(it)) },
            label = { Text(text = stringResource(Res.string.end_ip)) }
        )
        TextField(
            value = scanResult.ports.joinToString(),
            onValueChange = { onAction(MainScanAction.UpdatePorts(it)) },
            label = { Text(text = stringResource(Res.string.ports)) }
        )
        LazyColumn {
            items(scanResult.hosts) { result ->
                Text(
                    modifier = Modifier.padding(8.dp),
                    text = result.ip,
                )
            }
        }
    }
}

@Preview
@Composable
fun MainScreenPreview(
    @PreviewParameter(MainScanResultPreviewProvider::class) result: MainScanResult
) {
    MainScreen(result = result, onAction = {})
}
