package com.softartdev.ktlan

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview

val WindowInsets.Companion.isImeVisible: Boolean
    @Composable
    get() {
        val density = LocalDensity.current
        val ime = this.ime
        return remember { derivedStateOf { ime.getBottom(density) > 0 } }.value
    }

@Composable
fun ColumnScope.AnimatedKeyboardVisibility(
    visible: Boolean = !WindowInsets.isImeVisible,
    modifier: Modifier = Modifier,
    enter: EnterTransition = fadeIn(
        animationSpec = tween(durationMillis = 300, easing = LinearOutSlowInEasing)
    ) + expandVertically(
        animationSpec = tween(durationMillis = 300, easing = LinearOutSlowInEasing)
    ),
    exit: ExitTransition = fadeOut(
        animationSpec = tween(durationMillis = 200, easing = FastOutLinearInEasing)
    ) + shrinkVertically(
        animationSpec = tween(durationMillis = 200, easing = FastOutLinearInEasing)
    ),
    label: String = "AnimatedKeyboardVisibility",
    content: @Composable AnimatedVisibilityScope.() -> Unit
) {
    AnimatedVisibility(visible, modifier, enter, exit, label, content)
}

@Composable
expect fun EnableEdgeToEdge()

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
fun Error(modifier: Modifier = Modifier, message: String, onRetry: (() -> Unit)? = null) {
    Column(
        modifier = modifier
            .background(
                shape = RoundedCornerShape(size = 24.dp),
                color = MaterialTheme.colorScheme.errorContainer
            )
            .fillMaxWidth()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        Text(
            text = message,
            style = TextStyle(color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
        )
        if (onRetry != null) {
            Button(
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
            ) {
                Text(text = "Retry")
            }
        }
    }
}

@Preview
@Composable
fun PreviewLoader() = Surface { Loader() }

@Preview
@Composable
fun PreviewError() = Surface { Error(message = "Mock error", onRetry = {}) }

@Preview
@Composable
fun PreviewCommons() = Surface {
    Column(modifier = Modifier.fillMaxWidth()) {
        PreviewLoader()
        HorizontalDivider()
        PreviewError()
    }
}