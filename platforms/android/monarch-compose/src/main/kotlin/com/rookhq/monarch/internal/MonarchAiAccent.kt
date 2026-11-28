package com.rookhq.monarch.internal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import com.rookhq.monarch.theme.LocalMonarchColors
import com.rookhq.monarch.theme.MonarchTheme

/**
 * Draws [content] with the accent roles reading the AI accent, as the web does inside an AI component: the check
 * of a selected agent, the radio of a tone, the checkbox of a knowledge scope are magenta, not the brand red.
 */
@Composable
internal fun MonarchAiAccent(content: @Composable () -> Unit) {
    val colors = MonarchTheme.colors
    val ai = remember(colors) { colors.withAiAsAccent() }
    CompositionLocalProvider(LocalMonarchColors provides ai, content = content)
}
