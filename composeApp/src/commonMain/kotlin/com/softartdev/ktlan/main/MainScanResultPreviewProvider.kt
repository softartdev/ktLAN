package com.softartdev.ktlan.main

import com.softartdev.ktlan.presentation.main.MainScanResult
import org.jetbrains.compose.ui.tooling.preview.PreviewParameterProvider

class MainScanResultPreviewProvider : PreviewParameterProvider<MainScanResult> {
    override val values: Sequence<MainScanResult> = sequenceOf(
        MainScanResult.Loading,
        MainScanResult.Success(),
        MainScanResult.Error("An error occurred")
    )
}