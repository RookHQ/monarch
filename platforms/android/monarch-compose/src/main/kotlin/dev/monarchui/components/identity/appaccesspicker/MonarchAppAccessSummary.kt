package dev.monarchui.components.identity.appaccesspicker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.R
import dev.monarchui.components.datadisplay.tag.MonarchTag
import dev.monarchui.components.datadisplay.tag.MonarchTagSize
import dev.monarchui.internal.monarchString
import dev.monarchui.theme.MonarchTheme

/**
 * A compact, read-only line of the apps someone can open, for a member row:
 * the first names as tags, then "+N"; "No app access" when nothing is granted.
 *
 * Built by Monarch from `MonarchTag` in a `FlowRow`. The names are sorted for
 * the device's language; TalkBack reads the whole line as one element,
 * including the names behind "+N" ("Notion, Slack, and 2 more: Linear, Zoom").
 *
 * @param apps the apps, to look up names. An id without a match shows the id.
 * @param value the ids of the granted apps.
 * @param modifier the modifier of the line.
 * @param max how many names to show before "+N", at least one.
 * @param noneLabel replaces "No app access".
 * @param moreLabel replaces the spoken end of the line, given the hidden count and their names joined by commas.
 */
@ExperimentalMonarchApi
@OptIn(ExperimentalLayoutApi::class)
@Composable
public fun MonarchAppAccessSummary(
    apps: List<MonarchAppAccessOption>,
    value: List<String>,
    modifier: Modifier = Modifier,
    max: Int = 2,
    noneLabel: String? = null,
    moreLabel: ((count: Int, names: String) -> String)? = null,
) {
    val noteColor = MonarchAppAccessPickerTokens.noteColor(MonarchTheme.colors)
    if (value.isEmpty()) {
        Text(
            text = monarchString(noneLabel, R.string.monarch_appaccesspicker_summary_none),
            modifier = modifier,
            style = MonarchTheme.typography.caption,
            color = noteColor,
        )
        return
    }
    val locale = LocalConfiguration.current.locales[0]
    val (shown, hidden) = MonarchAppAccessPickerRules.split(
        MonarchAppAccessPickerRules.names(apps, value, locale),
        max,
    )
    val names = hidden.joinToString(", ")
    val moreText = pluralStringResource(R.plurals.monarch_appaccesspicker_summary_more, hidden.size, hidden.size, names)
    val more = when {
        hidden.isEmpty() -> ""
        moreLabel != null -> moreLabel(hidden.size, names)
        else -> moreText
    }
    val spoken = shown.joinToString(", ") + more
    FlowRow(
        modifier = modifier.clearAndSetSemantics { contentDescription = spoken },
        horizontalArrangement = Arrangement.spacedBy(MonarchAppAccessPickerTokens.TextGap),
        verticalArrangement = Arrangement.spacedBy(MonarchAppAccessPickerTokens.TextGap),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        shown.forEach { MonarchTag(text = it, size = MonarchTagSize.Sm) }
        if (hidden.isNotEmpty()) {
            Text(text = "+${hidden.size}", style = MonarchTheme.typography.caption, color = noteColor)
        }
    }
}
