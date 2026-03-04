package com.softartdev.ktlan

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.softartdev.ktlan.util.CommonAppLauncher

fun main() {
    CommonAppLauncher.launch()

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "ktLAN",
        ) {
            App()
        }
    }
}