package com.rookhq.monarch.components.intelligence.aitoolcall

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.R
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.intelligence.aicodeblock.MonarchAiCodeBlock
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiJSON
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiToolCallPart
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.theme.MonarchTheme
import java.text.NumberFormat

/** The details of a `MonarchAiToolCall`: the tool's exact name, then the input and the output, or a note. */
@Composable
internal fun MonarchAiToolCallDetails(
    part: MonarchAiToolCallPart,
    maxValueLength: Int,
    labels: MonarchAiToolCallLabels,
) {
    val tokens = MonarchAiToolCallTokens
    val colors = MonarchTheme.colors
    val input = tokens.input(part)
    val output = part.output
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = tokens.Padding, end = tokens.Padding, bottom = tokens.Padding),
        verticalArrangement = Arrangement.spacedBy(tokens.Gap),
    ) {
        Text(
            text = stringResource(
                R.string.monarch_aitoolcall_labelled,
                monarchString(labels.tool, R.string.monarch_aitoolcall_tool),
                part.name,
            ),
            style = MonarchTheme.typography.caption,
            color = colors.textMuted,
        )
        if (input != null) {
            MonarchAiToolCallValueBlock(
                label = monarchString(labels.input, R.string.monarch_aitoolcall_input),
                value = input,
                maxValueLength = maxValueLength,
                labels = labels,
            )
        }
        if (output != null) {
            MonarchAiToolCallValueBlock(
                label = monarchString(labels.output, R.string.monarch_aitoolcall_output),
                value = output,
                maxValueLength = maxValueLength,
                labels = labels,
            )
        }
        if (input == null && output == null) {
            Text(
                text = monarchString(labels.empty, R.string.monarch_aitoolcall_empty),
                style = MonarchTheme.typography.caption,
                color = colors.textMuted,
            )
        }
    }
}

/** One value in a code block named by [label], cut at [maxValueLength] characters with Show all. */
@OptIn(ExperimentalMonarchApi::class, ExperimentalLayoutApi::class)
@Composable
private fun MonarchAiToolCallValueBlock(
    label: String,
    value: MonarchAiJSON,
    maxValueLength: Int,
    labels: MonarchAiToolCallLabels,
) {
    val tokens = MonarchAiToolCallTokens
    // Formatting a large result is costly: only when the value changes.
    val formatted = remember(value) { MonarchAiToolCallFormat.format(value) }
    var all by rememberSaveable { mutableStateOf(false) }
    val shown = if (all) {
        MonarchAiToolCallCut(formatted.text, truncated = false)
    } else {
        remember(formatted.text, maxValueLength) { MonarchAiToolCallFormat.truncate(formatted.text, maxValueLength) }
    }
    Column(verticalArrangement = Arrangement.spacedBy(tokens.LineGap)) {
        MonarchAiCodeBlock(
            code = shown.text,
            language = if (formatted.json) "json" else null,
            title = label,
            showWrapToggle = false,
            wrapped = true,
        )
        if (shown.truncated) {
            val locale = LocalConfiguration.current.locales[0]
            val note = labels.truncated?.invoke(shown.text.length, formatted.text.length) ?: run {
                val numbers = NumberFormat.getIntegerInstance(locale)
                stringResource(
                    R.string.monarch_aitoolcall_truncated,
                    numbers.format(shown.text.length),
                    numbers.format(formatted.text.length),
                )
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(tokens.Gap),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = note, style = MonarchTheme.typography.caption, color = MonarchTheme.colors.textMuted)
                MonarchButton(
                    text = monarchString(labels.showMore, R.string.monarch_aitoolcall_show_more),
                    onClick = { all = true },
                    variant = MonarchButtonVariant.Ghost,
                    size = MonarchButtonSize.Sm,
                )
            }
        }
    }
}
