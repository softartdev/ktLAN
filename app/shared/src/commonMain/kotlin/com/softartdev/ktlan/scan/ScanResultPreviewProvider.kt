package com.softartdev.ktlan.scan

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.softartdev.ktlan.presentation.scan.ScanResult

class ScanResultPreviewProvider : PreviewParameterProvider<ScanResult> {
    override val values: Sequence<ScanResult> = sequenceOf(
        ScanResult.Loading,
        ScanResult.Success(hosts = ScanResult.Success.previewHosts),
        ScanResult.Error("An error occurred")
    )
}