package com.softartdev.ktlan

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun Loader(modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxSize().padding(all = 20.dp)
    ) {
        CircularProgressIndicator()
    }
}

@Composable
fun Error(modifier: Modifier = Modifier, message: String) {
    Box(
        modifier = modifier.fillMaxWidth().padding(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            style = TextStyle(color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
        )
    }
}

@Preview
@Composable
fun PreviewLoader() = Loader()

@Preview
@Composable
fun PreviewError() = Error(message = "Mock error")

@Preview
@Composable
fun PreviewCommons() = Column(modifier = Modifier.fillMaxWidth()) {
    PreviewLoader()
    HorizontalDivider()
    PreviewError()
}