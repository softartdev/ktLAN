package com.softartdev.ktlan

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.softartdev.ktlan.util.CommonAppLauncher
import kotlinx.browser.document

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    CommonAppLauncher.launch()

    ComposeViewport(document.body!!) {
        App()
    }
}