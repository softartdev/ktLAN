package com.softartdev.ktlan.socket

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.softartdev.ktlan.domain.model.ChatMessage
import com.softartdev.ktlan.presentation.socket.SocketAction
import com.softartdev.ktlan.presentation.socket.SocketResult
import com.softartdev.ktlan.presentation.socket.SocketViewModel
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

@Composable
fun SocketConnectScreen(viewModel: SocketViewModel) {
    val result by viewModel.stateFlow.collectAsState()
    LaunchedEffect(viewModel) { viewModel.launch() }
    SocketConnectContent(result, viewModel::onAction)
}

@Composable
fun SocketConnectContent(result: SocketResult, onAction: (SocketAction) -> Unit) {
    Column(modifier = Modifier) {
        SocketConnectSettings(
            modifier = Modifier.padding(horizontal = 16.dp),
            result = result,
            onAction = onAction
        )
        SocketConnectMessages(
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
            result = result,
        )
        SocketConnectInput(
            modifier = Modifier.padding(horizontal = 16.dp),
            result = result,
            onAction = onAction
        )
    }
}

@Composable
fun SocketConnectSettings(
    modifier: Modifier = Modifier,
    result: SocketResult,
    onAction: (SocketAction) -> Unit
) {
    if (result.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
    Column(modifier) {
        Text(text = stringResource(Res.string.bind_host))
        Row {
            TextField(
                modifier = Modifier.weight(1f),
                value = result.bindHost,
                onValueChange = { onAction(SocketAction.SetBindHost(it)) }
            )
            Spacer(Modifier.width(8.dp))
            TextField(
                modifier = Modifier.weight(1f),
                value = result.bindPort,
                onValueChange = { onAction(SocketAction.SetBindPort(it)) },
                label = { Text(stringResource(Res.string.port)) }
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                onAction(SocketAction.StartServer(result.bindHost, result.bindPort))
            }) {
                Text(stringResource(Res.string.start_server))
            }
            Button(onClick = { onAction(SocketAction.StopAll) }) { Text(stringResource(Res.string.stop)) }
            Button(onClick = { onAction(SocketAction.ShowQrForServer) }) { Text(stringResource(Res.string.show_qr)) }
        }
        Spacer(Modifier.height(8.dp))
        Text(text = stringResource(Res.string.remote_host))
        Row {
            TextField(
                modifier = Modifier.weight(1f),
                value = result.remoteHost,
                onValueChange = { onAction(SocketAction.SetRemoteHost(it)) }
            )
            Spacer(Modifier.width(8.dp))
            TextField(
                modifier = Modifier.weight(1f),
                value = result.remotePort,
                onValueChange = { onAction(SocketAction.SetRemotePort(it)) },
                label = { Text(stringResource(Res.string.port)) }
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                onAction(SocketAction.Connect(result.remoteHost, result.remotePort))
            }) {
                Text(stringResource(Res.string.connect))
            }
            Button(onClick = { onAction(SocketAction.ApplyQrPayload("")) }) {
                Text(stringResource(Res.string.scan_qr))
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun SocketConnectMessages(
    modifier: Modifier = Modifier,
    result: SocketResult
) {
    val listState = rememberLazyListState()
    LaunchedEffect(result.messages.size) {
        if (result.messages.isNotEmpty()) {
            listState.animateScrollToItem(result.messages.lastIndex)
        }
    }
    LazyColumn(modifier, state = listState) {
        items(result.messages) { message ->
            ListItem(
                headlineContent = { Text(text = message.text) },
                overlineContent = { Text(if (message.sender == ChatMessage.Sender.Local) "You" else "Peer") }
            )
            HorizontalDivider()
        }
    }
}

@Composable
fun SocketConnectInput(
    modifier: Modifier = Modifier,
    result: SocketResult,
    onAction: (SocketAction) -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(bottom = 8.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            modifier = Modifier.weight(1f),
            value = result.draft,
            onValueChange = { onAction(SocketAction.EditDraft(it)) },
            label = { Text(stringResource(Res.string.type_message)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Send
            ),
            keyboardActions = KeyboardActions(
                onSend = { onAction(SocketAction.Send(result.draft)) }
            )
        )
        FloatingActionButton(
            modifier = Modifier.size(56.dp),
            onClick = { onAction(SocketAction.Send(result.draft)) }
        ) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
        }
    }
}

@Preview
@Composable
fun SocketConnectPreview() {
    val messages = listOf(
        ChatMessage(
            sender = ChatMessage.Sender.Local,
            text = "Hello",
            timestamp = 0
        ),
        ChatMessage(
            sender = ChatMessage.Sender.Remote,
            text = "Hi",
            timestamp = 0
        )
    )
    SocketConnectContent(
        result = SocketResult(connected = true, messages = messages, bindHost = "192.168.1.2"),
        onAction = {}
    )
}
