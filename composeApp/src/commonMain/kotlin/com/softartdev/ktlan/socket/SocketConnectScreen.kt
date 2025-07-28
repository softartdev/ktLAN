@file:OptIn(ExperimentalMaterial3Api::class)

package com.softartdev.ktlan.socket

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.softartdev.ktlan.presentation.socket.SocketAction
import com.softartdev.ktlan.presentation.socket.SocketResult
import com.softartdev.ktlan.presentation.socket.SocketViewModel
import com.softartdev.ktlan.domain.model.ChatMessage
import com.softartdev.ktlan.domain.model.ChatMessage.Sender
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.ncgroup.kscan.BarcodeFormat
import org.ncgroup.kscan.BarcodeResult
import org.ncgroup.kscan.ScannerView
import `in`.procyk.compose.camera.permission.CameraPermission
import `in`.procyk.compose.camera.permission.CameraPermissionState
import `in`.procyk.compose.camera.permission.rememberCameraPermissionState
import ktlan.composeapp.generated.resources.Res
import ktlan.composeapp.generated.resources.cancelled
import ktlan.composeapp.generated.resources.camera_is_not_available

@Composable
fun SocketConnectScreen(socketViewModel: SocketViewModel) {
    val result by socketViewModel.stateFlow.collectAsState()
    LaunchedEffect(socketViewModel) { socketViewModel.launch() }
    SocketConnectContent(result, socketViewModel::onAction)
}

@Composable
fun SocketConnectContent(result: SocketResult, onAction: (SocketAction) -> Unit) {
    val lazyListState = rememberLazyListState()
    var showScanner by remember { mutableStateOf(false) }
    val cancelledMessage = stringResource(Res.string.cancelled)
    val cameraUnavailableMessage = stringResource(Res.string.camera_is_not_available)
    val cameraPermissionState: CameraPermissionState = rememberCameraPermissionState()
    Scaffold(topBar = { TopAppBar(title = { Text("LAN Chat") }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TextField(
                value = result.bindHost,
                onValueChange = { onAction(SocketAction.SetBindHost(it)) },
                label = { Text("Bind host") },
                modifier = Modifier.fillMaxWidth().padding(4.dp)
            )
            TextField(
                value = result.bindPort,
                onValueChange = { onAction(SocketAction.SetBindPort(it)) },
                label = { Text("Port") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().padding(4.dp)
            )
            Row(modifier = Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Button(onClick = { onAction(SocketAction.StartServer(result.bindHost, result.bindPort)) }) { Text("Start server") }
                Button(onClick = { onAction(SocketAction.StopAll) }) { Text("Stop") }
                Button(onClick = { onAction(SocketAction.ShowQrForServer) }) { Text("Show QR") }
            }
            Divider()
            TextField(
                value = result.remoteHost,
                onValueChange = { onAction(SocketAction.SetRemoteHost(it)) },
                label = { Text("Remote host") },
                modifier = Modifier.fillMaxWidth().padding(4.dp)
            )
            TextField(
                value = result.remotePort,
                onValueChange = { onAction(SocketAction.SetRemotePort(it)) },
                label = { Text("Port") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().padding(4.dp)
            )
            Row(modifier = Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Button(onClick = { onAction(SocketAction.Connect(result.remoteHost, result.remotePort)) }) { Text("Connect") }
                Button(onClick = {
                    when {
                        cameraPermissionState.isAvailable -> when (cameraPermissionState.permission) {
                            CameraPermission.Granted -> showScanner = true
                            CameraPermission.Denied -> cameraPermissionState.launchRequest()
                        }
                        else -> onAction(SocketAction.EditDraft(cameraUnavailableMessage))
                    }
                }) { Text("Scan QR") }
            }
            LazyColumn(modifier = Modifier.weight(1f), state = lazyListState) {
                items(result.messages) { message: ChatMessage ->
                    Text(text = if (message.sender == Sender.Local) "You: ${'$'}{message.text}" else "Peer: ${'$'}{message.text}", modifier = Modifier.padding(4.dp))
                }
            }
            Row(modifier = Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextField(
                    modifier = Modifier.weight(1f),
                    value = result.draft,
                    onValueChange = { onAction(SocketAction.EditDraft(it)) },
                    label = { Text("Type a message…") },
                    singleLine = true
                )
                Button(onClick = { onAction(SocketAction.Send(result.draft)) }) { Text("Send") }
            }
        }
    }
    if (showScanner) {
        ScannerView(
            codeTypes = listOf(BarcodeFormat.FORMAT_QR_CODE),
            result = { res: BarcodeResult ->
                when (res) {
                    is BarcodeResult.OnSuccess -> onAction(SocketAction.ApplyQrPayload(res.barcode.data))
                    is BarcodeResult.OnFailed -> onAction(SocketAction.EditDraft(res.exception.message.orEmpty()))
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
    val preview = SocketResult(
        messages = listOf(
            ChatMessage(Sender.Remote, "Hello", 0),
            ChatMessage(Sender.Local, "Hi", 0)
        ),
        connected = true,
        serverRunning = false
    )
    SocketConnectContent(preview) {}
}
