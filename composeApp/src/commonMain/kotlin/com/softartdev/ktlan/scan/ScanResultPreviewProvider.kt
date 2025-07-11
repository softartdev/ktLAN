package com.softartdev.ktlan.scan

import com.softartdev.ktlan.presentation.scan.ScanResult
import org.jetbrains.compose.ui.tooling.preview.PreviewParameterProvider

class ScanResultPreviewProvider : PreviewParameterProvider<ScanResult> {
    override val values: Sequence<ScanResult> = sequenceOf(
        ScanResult.Loading,
        ScanResult.Success(),
        ScanResult.Error("An error occurred")
    )
}