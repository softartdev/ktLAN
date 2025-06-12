package com.softartdev.ktlan

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ktlan.composeapp.generated.resources.Res
import ktlan.composeapp.generated.resources.compose_multiplatform
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun App(
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    showContentState: MutableState<Boolean> = remember { mutableStateOf(false) },
    loadResultState: MutableState<String?> = remember { mutableStateOf(null) },
    loadingState: MutableState<Boolean> = remember { mutableStateOf(false) },
    errorState: MutableState<Boolean> = remember { mutableStateOf(false) }
) {
    LaunchedEffect(Unit) { AppState.launch() }
    MaterialTheme {
        Surface {
            Column(
                modifier = Modifier
                    .safeContentPadding()
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Button(onClick = {
                    Napier.d("Button clicked")
                    showContentState.value = !showContentState.value
                }) {
                    Text("Click me!")
                }
                AnimatedVisibility(showContentState.value) {
                    val greeting = remember { Greeting().greet() }
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(painterResource(Res.drawable.compose_multiplatform), null)
                        Text("Compose: $greeting")
                    }
                }
                if (loadingState.value) {
                    CircularProgressIndicator()
                } else {
                    Button(onClick = {
                        coroutineScope.launch(Dispatchers.Default) {
                            loadingState.value = true
                            errorState.value = false
                            loadResultState.value = null

                            Result.runCatching { ApplicationApi().localIp() }
                                .onSuccess(loadResultState::value::set)
                                .onFailure { throwable ->
                                    Napier.e("Error loading data", throwable)
                                    errorState.value = true
                                    loadResultState.value = throwable.message ?: throwable.stackTraceToString()
                                }
                            loadingState.value = false
                        }
                    }) {
                        Text(text = "Check IP")
                    }
                }
                loadResultState.value?.let {
                    Text(
                        text = it,
                        color = when {
                            errorState.value -> MaterialTheme.colorScheme.error
                            else -> Color.Unspecified
                        }
                    )
                }
                if (loadResultState.value != null) {
                    Icon(imageVector = Icons.Default.Done, contentDescription = null)
                } else if (loadingState.value) {
                    Icon(imageVector = Icons.Default.Download, contentDescription = null)
                } else if (errorState.value) {
                    Icon(imageVector = Icons.Default.Error, contentDescription = null)
                }
            }
        }
    }
}

@Preview
@Composable
fun AppPreview() {
    App(
        showContentState = mutableStateOf(true),
        loadResultState = mutableStateOf("Check IP result will be here"),
        loadingState = mutableStateOf(true),
        errorState = mutableStateOf(true)
    )
}