package com.softartdev.ktlan

import androidx.compose.ui.window.ComposeUIViewController
import com.softartdev.ktlan.util.CommonAppLauncher

fun MainViewController() = ComposeUIViewController { App() }

// Proxy function for getting through Pod's dependency
fun sharedAppLauncher(): CommonAppLauncher = CommonAppLauncher
