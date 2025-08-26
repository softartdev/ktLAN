@file:OptIn(ExperimentalTime::class)

package com.softartdev.ktlan.connect

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.softartdev.ktlan.AnimatedKeyboardVisibility
import com.softartdev.ktlan.data.webrtc.P2pState.CHAT_ESTABLISHED
import com.softartdev.ktlan.data.webrtc.P2pState.WAITING_FOR_ANSWER
import com.softartdev.ktlan.data.webrtc.P2pState.WAITING_FOR_OFFER
import com.softartdev.ktlan.domain.model.ConsoleMessage
import com.softartdev.ktlan.isImeVisible
import com.softartdev.ktlan.presentation.connect.ConnectAction
import com.softartdev.ktlan.presentation.connect.ConnectResult
import com.softartdev.ktlan.presentation.connect.ConnectViewModel
import `in`.procyk.compose.camera.permission.CameraPermission
import `in`.procyk.compose.camera.permission.CameraPermissionState
import `in`.procyk.compose.camera.permission.rememberCameraPermissionState
import ktlan.composeapp.generated.resources.Res
import ktlan.composeapp.generated.resources.camera_is_not_available
import ktlan.composeapp.generated.resources.cancelled
import ktlan.composeapp.generated.resources.enter_message
import ktlan.composeapp.generated.resources.hint_paste_answer
import ktlan.composeapp.generated.resources.hint_paste_offer
import ktlan.composeapp.generated.resources.make_offer
import ktlan.composeapp.generated.resources.scan_qr
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.ncgroup.kscan.BarcodeFormat
import org.ncgroup.kscan.BarcodeResult
import org.ncgroup.kscan.ScannerView
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectScreen(connectViewModel: ConnectViewModel) {
    val result: ConnectResult by connectViewModel.stateFlow.collectAsState()
    LaunchedEffect(key1 = connectViewModel) {
        connectViewModel.launch()
    }
    ConnectContent(result = result, onAction = connectViewModel::onAction)
}

@Composable
fun ConnectContent(
    modifier: Modifier = Modifier,
    result: ConnectResult,
    onAction: (ConnectAction) -> Unit,
    cameraPermissionState: CameraPermissionState = rememberCameraPermissionState()
) {
    val lazyListState: LazyListState = rememberLazyListState()
    val scrollProgressState: State<Float> = remember {
        derivedStateOf {
            val lastIndex: Int = lazyListState.layoutInfo.totalItemsCount - 1
            val progress: Float = with(lazyListState.layoutInfo.visibleItemsInfo) {
                if (isEmpty() || lastIndex == 0) return@with 0f
                val (firstVisibleIndex, lastVisibleIndex) = first().index to last().index
                if (lastVisibleIndex == lastIndex) return@with 1f
                if (firstVisibleIndex == 0) return@with 0f
                return@with (firstVisibleIndex + size / 2f) / lastIndex.toFloat()
            }
            return@derivedStateOf if (progress.isNaN()) 0f else progress
        }
    }
    val inputState: MutableState<String> = remember { mutableStateOf("") }
    var showQrScanner: Boolean by remember { mutableStateOf(false) }
    val cancelledMessage: String = stringResource(Res.string.cancelled)
    val cameraUnavailableMessage: String = stringResource(Res.string.camera_is_not_available)
    LaunchedEffect(key1 = result.consoleMessages) {
        val lastIndex = result.consoleMessages.lastIndex
        if (lastIndex < 0) return@LaunchedEffect
        lazyListState.scrollToItem(index = lastIndex)
    }
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        if (scrollProgressState.value > 0f) LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth(),
            progress = scrollProgressState::value,
            drawStopIndicator = {}
        )
        LazyColumn(
            modifier = modifier.weight(1f),
            state = lazyListState,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(result.consoleMessages) { consoleMessage: ConsoleMessage ->
                ListItem(
                    modifier = Modifier.clickable {
                        onAction(ConnectAction.ShowQr(consoleMessage.headline.toString()))
                    },
                    leadingContent = consoleMessage.leading?.let { leading: String ->
                        { Text(text = leading) }
                    },
                    overlineContent = consoleMessage.overline?.let { overline: String ->
                        { Text(text = overline) }
                    },
                    headlineContent = {
                        Text(text = consoleMessage.headline.orEmpty())
                    },
                    supportingContent = consoleMessage.supporting?.let { supporting: String ->
                        { Text(text = supporting) }
                    },
                    trailingContent = consoleMessage.trailing?.let { trailing: String ->
                        { Text(text = trailing) }
                    },
                )
                HorizontalDivider()
            }
        }
        if (result.loading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                modifier = Modifier.weight(1f),
                value = inputState.value,
                onValueChange = inputState::value::set,
                label = {
                    Text(
                        text = when (result.p2pState) {
                            WAITING_FOR_OFFER -> stringResource(Res.string.hint_paste_offer)
                            WAITING_FOR_ANSWER -> stringResource(Res.string.hint_paste_answer)
                            CHAT_ESTABLISHED -> stringResource(Res.string.enter_message)
                            else -> result.p2pState?.name.orEmpty()
                        }
                    )
                },
                enabled = result.inputEnabled,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(
                    onSend = {
                        onAction(ConnectAction.Submit(inputState.value))
                        inputState.value = ""
                    }
                )
            )
            FloatingActionButton(
                onClick = {
                    onAction(ConnectAction.Submit(inputState.value))
                    inputState.value = ""
                },
                modifier = Modifier.size(56.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
            }
        }
        AnimatedKeyboardVisibility(visible = !WindowInsets.isImeVisible && result.createOfferVisible) {
            Button(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                onClick = { onAction(ConnectAction.CreateOffer) },
                content = { Text(text = stringResource(Res.string.make_offer)) }
            )
        }
        AnimatedKeyboardVisibility {
            Button(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                onClick = {
                    when {
                        cameraPermissionState.isAvailable -> when (cameraPermissionState.permission) {
                            CameraPermission.Granted -> showQrScanner = true
                            CameraPermission.Denied -> cameraPermissionState.launchRequest()
                        }
                        else -> onAction(ConnectAction.PrintConsole(cameraUnavailableMessage))
                    }
                },
                content = { Text(text = stringResource(Res.string.scan_qr)) }
            )
        }
    }
    if (showQrScanner) {
        ScannerView(
            codeTypes = listOf(BarcodeFormat.FORMAT_QR_CODE),
            result = { result: BarcodeResult ->
                when (result) {
                    is BarcodeResult.OnSuccess -> inputState.value = result.barcode.data
                    is BarcodeResult.OnFailed -> onAction(ConnectAction.PrintError(result.exception))
                    is BarcodeResult.OnCanceled -> onAction(
                        ConnectAction.PrintConsole(cancelledMessage)
                    )
                }
                showQrScanner = false
            },
        )
    }
}

@Preview
@Composable
fun ConnectContentPreview() {
    ConnectContent(
        result = ConnectResult(consoleMessages = ConnectResult.previewMessages),
        onAction = {},
        cameraPermissionState = PreviewCameraPermissionState()
    )
}

class PreviewCameraPermissionState : CameraPermissionState {
    override val isAvailable: Boolean = true
    override val permission: CameraPermission = CameraPermission.Granted
    override fun launchRequest() {}
}