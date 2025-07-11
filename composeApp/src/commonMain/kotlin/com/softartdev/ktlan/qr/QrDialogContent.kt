package com.softartdev.ktlan.qr

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.alexzhirkevich.qrose.rememberQrCodePainter
import ktlan.composeapp.generated.resources.Res
import ktlan.composeapp.generated.resources.copy_text
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun QrDialogContent(
    modifier: Modifier = Modifier,
    text: String? = null,
    dismissDialog: () -> Unit,
) = Surface(
    modifier = modifier,
    shape = AlertDialogDefaults.shape,
    color = AlertDialogDefaults.containerColor,
    tonalElevation = AlertDialogDefaults.TonalElevation,
) {
    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    Column(
        modifier = Modifier.padding(all = 24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            modifier = Modifier.background(color = Color.White).padding(all = 12.dp),
            painter = rememberQrCodePainter(data = text.toString()),
            contentDescription = null
        )
        Text(
            text = text.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = AlertDialogDefaults.textContentColor,
            maxLines = 4,
            overflow = TextOverflow.MiddleEllipsis
        )
        Button(
            content = { Text(stringResource(Res.string.copy_text)) },
            onClick = {
                clipboardManager.setText(AnnotatedString(text.toString()))
                dismissDialog.invoke()
            },
        )
    }
}

@Preview
@Composable
fun QrDialogContentPreview() {
    QrDialogContent(text = "https://example.com", dismissDialog = {})
}