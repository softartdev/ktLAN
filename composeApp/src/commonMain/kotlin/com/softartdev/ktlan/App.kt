package com.softartdev.ktlan

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CoroutineScope
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
    MaterialTheme {
        Surface {
            Column(
                modifier = Modifier
                    .safeContentPadding()
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Button(onClick = { showContentState.value = !showContentState.value }) {
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
                if (loadResultState.value != null) {
                    Text("Load result: ${loadResultState.value}")
                }
                if (loadingState.value) {
                    CircularProgressIndicator()
                }
                if (errorState.value) {
                    Text("Error loading data", color = MaterialTheme.colorScheme.error)
                }
                Button(onClick = {
                    coroutineScope.launch {
                        loadingState.value = true
                        errorState.value = false
                        loadResultState.value = null

                        Result.runCatching { ApplicationApi().loadAbout() }
                            .onSuccess(loadResultState::value::set)
                            .onFailure { throwable ->
                                throwable.printStackTrace()
                                errorState.value = true
                                loadResultState.value = throwable.message
                            }

                        loadingState.value = false
                    }
                }) {
                    Text(text = if (loadingState.value) "Loading..." else "Load something")
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