package com.softartdev.ktlan.socket

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.softartdev.ktlan.presentation.socket.SocketAction
import com.softartdev.ktlan.presentation.socket.SocketResult
import com.softartdev.ktlan.presentation.socket.SocketViewModel
import `in`.procyk.compose.camera.permission.CameraPermission
import `in`.procyk.compose.camera.permission.CameraPermissionState
import `in`.procyk.compose.camera.permission.rememberCameraPermissionState
import ktlan.composeapp.generated.resources.Res
import ktlan.composeapp.generated.resources.bind_host
import ktlan.composeapp.generated.resources.connect
import ktlan.composeapp.generated.resources.port
import ktlan.composeapp.generated.resources.remote_host
import ktlan.composeapp.generated.resources.scan_qr
import ktlan.composeapp.generated.resources.show_qr
import ktlan.composeapp.generated.resources.start_server
import ktlan.composeapp.generated.resources.stop
import ktlan.composeapp.generated.resources.type_message
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel
import org.ncgroup.kscan.BarcodeFormat
import org.ncgroup.kscan.BarcodeResult
import org.ncgroup.kscan.ScannerView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocketConnectScreen(socketViewModel: SocketViewModel = koinViewModel()) {
    val result by socketViewModel.stateFlow.collectAsState()
    LaunchedEffect(Unit) { socketViewModel.launch() }
    SocketConnectContent(result, socketViewModel::onAction)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocketConnectContent(result: SocketResult, onAction: (SocketAction) -> Unit) {
    val listState = rememberLazyListState()
    var showScanner by remember { mutableStateOf(false) }
    val cameraPermissionState: CameraPermissionState = rememberCameraPermissionState()
    val cancelledMessage = "Cancelled"
    val cameraUnavailableMessage = "Camera unavailable"

    Column(modifier = Modifier.fillMaxSize()) {
        // Host section
        Text(text = stringResource(Res.string.bind_host))
        OutlinedTextField(
            value = result.bindHost,
            onValueChange = { onAction(SocketAction.SetBindHost(it)) },
            label = { Text(stringResource(Res.string.bind_host)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = result.bindPort,
            onValueChange = { onAction(SocketAction.SetBindPort(it)) },
            label = { Text(stringResource(Res.string.port)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                onAction(
                    SocketAction.StartServer(
                        result.bindHost,
                        result.bindPort
                    )
                )
            }) {
                Text(stringResource(Res.string.start_server))
            }
            Button(onClick = { onAction(SocketAction.StopAll) }) {
                Icon(Icons.Outlined.Stop, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text(stringResource(Res.string.stop))
            }
            Button(onClick = { onAction(SocketAction.ShowQrForServer) }) {
                Text(stringResource(Res.string.show_qr))
            }
        }

        // Join section
        Text(text = stringResource(Res.string.remote_host))
        OutlinedTextField(
            value = result.remoteHost,
            onValueChange = { onAction(SocketAction.SetRemoteHost(it)) },
            label = { Text(stringResource(Res.string.remote_host)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = result.remotePort,
            onValueChange = { onAction(SocketAction.SetRemotePort(it)) },
            label = { Text(stringResource(Res.string.port)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                onAction(
                    SocketAction.Connect(
                        result.remoteHost,
                        result.remotePort
                    )
                )
            }) {
                Text(stringResource(Res.string.connect))
            }
            Button(onClick = {
                when {
                    cameraPermissionState.isAvailable -> when (cameraPermissionState.permission) {
                        CameraPermission.Granted -> showScanner = true
                        CameraPermission.Denied -> cameraPermissionState.launchRequest()
                    }
                    else -> onAction(SocketAction.EditDraft(cameraUnavailableMessage))
                }
            }) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text(stringResource(Res.string.scan_qr))
            }
        }

        // Messages
        LazyColumn(modifier = Modifier.weight(1f), state = listState) {
            items(result.messages) { message ->
                Row(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                    Text(if (message.sender == com.softartdev.ktlan.domain.model.ChatMessage.Sender.Local) "You:" else "Peer:")
                    Spacer(Modifier.width(4.dp))
                    Text(message.text)
                }
            }
        }

        Row(verticalAlignment = Alignment.Bottom) {
            val draft = result.draft
            OutlinedTextField(
                modifier = Modifier.weight(1f),
                value = draft,
                onValueChange = { onAction(SocketAction.EditDraft(it)) },
                placeholder = { Text(stringResource(Res.string.type_message)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onAction(SocketAction.Send(draft)) })
            )
            FloatingActionButton(onClick = { onAction(SocketAction.Send(draft)) }) {
                Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null)
            }
        }
    }
    if (showScanner) {
        ScannerView(
            codeTypes = listOf(BarcodeFormat.FORMAT_QR_CODE),
            result = { result ->
                when (result) {
                    is BarcodeResult.OnSuccess -> onAction(SocketAction.ApplyQrPayload(result.barcode.data))
                    is BarcodeResult.OnFailed -> {}
                    is BarcodeResult.OnCanceled -> onAction(SocketAction.EditDraft(cancelledMessage))
                }
                showScanner = false
            }
        )
    }
}

@Preview
@Composable
fun SocketConnectPreview() {
    val sample = SocketResult(
        connected = true,
        messages = listOf(
            com.softartdev.ktlan.domain.model.ChatMessage(
                com.softartdev.ktlan.domain.model.ChatMessage.Sender.Remote,
                "Hello",
                0L
            ),
            com.softartdev.ktlan.domain.model.ChatMessage(
                com.softartdev.ktlan.domain.model.ChatMessage.Sender.Local,
                "Hi!",
                1L
            )
        )
    )
    SocketConnectContent(sample, onAction = {})
}
