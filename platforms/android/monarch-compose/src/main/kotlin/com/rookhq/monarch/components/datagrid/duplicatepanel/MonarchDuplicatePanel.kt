package com.rookhq.monarch.components.datagrid.duplicatepanel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.rookhq.monarch.R
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.layout.divider.MonarchDivider
import com.rookhq.monarch.components.layout.divider.MonarchDividerVariant
import com.rookhq.monarch.internal.MonarchDefaultIcons
import com.rookhq.monarch.internal.MonarchShapeDefaults
import com.rookhq.monarch.internal.MonarchTextAction
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.internal.monarchSurface
import com.rookhq.monarch.theme.MonarchTheme

/**
 * Saved records that look like the one being created or viewed, why each one
 * matched, and what the user can do: open it, merge, or mark it as not a
 * duplicate. It is advice and never blocks saving.
 *
 * Built by Monarch from a column of `MonarchCard`s, because Material 3 has no
 * equivalent. A column rather than a lazy list, because the matches are few and
 * the panel sits inside a scrolling form. With no matches nothing is drawn and
 * no space is taken.
 *
 * @param matches the likely duplicates, in the order they are shown.
 * @param modifier the modifier of the panel.
 * @param variant a warning box under a form, or a plain section on a record screen.
 * @param footer a closing line, for example "You can still create it."
 * @param icon an icon before the heading of the hint, for example a warning `MonarchIcon` of size `Sm` with a
 * `null` content description. Monarch bundles no icons. Not shown in the panel variant.
 * @param onOpen adds an Open button to each match; the app navigates to the record.
 * @param onMerge adds a Merge button to each match; the app starts the merge.
 * @param onDismiss adds a "Not a duplicate" button to each match; the app removes the match.
 * @param title the heading. `null` uses the built-in strings `monarch_duplicatepanel_title_one` or
 * `monarch_duplicatepanel_title_other`, by the number of matches.
 * @param openLabel the label of the Open buttons. `null` uses `monarch_duplicatepanel_open`.
 * @param mergeLabel the label of the Merge buttons. `null` uses `monarch_duplicatepanel_merge`.
 * @param dismissLabel the label of the dismiss buttons. `null` uses `monarch_duplicatepanel_dismiss`.
 */
@Composable
public fun MonarchDuplicatePanel(
    matches: List<MonarchDuplicateMatch>,
    modifier: Modifier = Modifier,
    variant: MonarchDuplicatePanelVariant = MonarchDuplicatePanelVariant.Hint,
    footer: String? = null,
    icon: (@Composable () -> Unit)? = MonarchDefaultIcons.Warning,
    onOpen: ((MonarchDuplicateMatch) -> Unit)? = null,
    onMerge: ((MonarchDuplicateMatch) -> Unit)? = null,
    onDismiss: ((MonarchDuplicateMatch) -> Unit)? = null,
    title: String? = null,
    openLabel: String? = null,
    mergeLabel: String? = null,
    dismissLabel: String? = null,
) {
    if (matches.isEmpty()) return
    val colors = MonarchTheme.colors
    val hint = variant == MonarchDuplicatePanelVariant.Hint
    val headingRes = if (matches.size == 1) {
        R.string.monarch_duplicatepanel_title_one
    } else {
        R.string.monarch_duplicatepanel_title_other
    }
    val heading = title ?: stringResource(headingRes)
    val labels = DuplicateActionLabels(
        open = monarchString(openLabel, R.string.monarch_duplicatepanel_open),
        merge = monarchString(mergeLabel, R.string.monarch_duplicatepanel_merge),
        dismiss = monarchString(dismissLabel, R.string.monarch_duplicatepanel_dismiss),
    )
    val container = if (hint) {
        modifier
            .monarchSurface(MonarchDuplicatePanelTokens.hintColors(colors), MonarchShapeDefaults.Control)
            .padding(MonarchDuplicatePanelTokens.HintPadding)
    } else {
        modifier
    }
    val headingColor = MonarchDuplicatePanelTokens.headingColor(variant, colors)
    Column(modifier = container, verticalArrangement = Arrangement.spacedBy(MonarchDuplicatePanelTokens.SectionGap)) {
        Row(
            modifier = Modifier.semantics(mergeDescendants = true) {
                heading()
                if (hint) liveRegion = LiveRegionMode.Polite
            },
            horizontalArrangement = Arrangement.spacedBy(MonarchDuplicatePanelTokens.HeadingGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (hint && icon != null) {
                CompositionLocalProvider(LocalContentColor provides headingColor) {
                    Row(Modifier.clearAndSetSemantics { }) { icon() }
                }
            }
            Text(
                text = heading,
                style = MonarchTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold),
                color = headingColor,
            )
        }
        matches.forEachIndexed { index, match ->
            key(match.id) {
                if (index > 0) MonarchDivider(variant = MonarchDividerVariant.Subtle)
                DuplicateMatchRow(match, labels, onOpen, onMerge, onDismiss)
            }
        }
        if (footer != null) {
            Text(text = footer, style = MonarchTheme.typography.caption, color = colors.textMuted)
        }
    }
}

/** The visible labels of the three buttons of a match. */
private class DuplicateActionLabels(val open: String, val merge: String, val dismiss: String)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DuplicateMatchRow(
    match: MonarchDuplicateMatch,
    labels: DuplicateActionLabels,
    onOpen: ((MonarchDuplicateMatch) -> Unit)?,
    onMerge: ((MonarchDuplicateMatch) -> Unit)?,
    onDismiss: ((MonarchDuplicateMatch) -> Unit)?,
) {
    val colors = MonarchTheme.colors
    val caption = MonarchTheme.typography.caption
    val score = match.score?.let {
        stringResource(R.string.monarch_duplicatepanel_score, MonarchDuplicatePanelTokens.percent(it))
    }
    val details = (listOfNotNull(match.kind) + match.reasons + listOfNotNull(score)).joinToString(" \u00B7 ")
    val open = onOpen
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(vertical = MonarchDuplicatePanelTokens.RowPadding),
        horizontalArrangement = Arrangement.spacedBy(MonarchDuplicatePanelTokens.ActionGap),
        verticalArrangement = Arrangement.spacedBy(MonarchDuplicatePanelTokens.LineGap),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f, fill = true).semantics(mergeDescendants = true) { },
        ) {
            Text(
                text = match.title,
                style = caption.copy(fontWeight = FontWeight.Medium),
                color = colors.textDefault,
            )
            match.detail?.let { Text(text = it, style = caption, color = colors.textMuted) }
            if (details.isNotEmpty()) Text(text = details, style = caption, color = colors.textMuted)
        }
        if (open != null || onMerge != null || onDismiss != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(MonarchDuplicatePanelTokens.ActionGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                open?.let { action ->
                    val name = stringResource(R.string.monarch_duplicatepanel_action_for, labels.open, match.title)
                    MonarchTextAction(
                        text = labels.open,
                        onClick = { action(match) },
                        modifier = Modifier.semantics { contentDescription = name },
                    )
                }
                onMerge?.let { MatchButton(labels.merge, match, MonarchButtonVariant.Secondary, it) }
                onDismiss?.let { MatchButton(labels.dismiss, match, MonarchButtonVariant.Ghost, it) }
            }
        }
    }
}

/** A small button of one match, named for TalkBack with its label and the name of the record. */
@Composable
private fun MatchButton(
    label: String,
    match: MonarchDuplicateMatch,
    variant: MonarchButtonVariant,
    onClick: (MonarchDuplicateMatch) -> Unit,
) {
    val name = stringResource(R.string.monarch_duplicatepanel_action_for, label, match.title)
    MonarchButton(
        text = label,
        onClick = { onClick(match) },
        modifier = Modifier.semantics { contentDescription = name },
        variant = variant,
        size = MonarchButtonSize.Sm,
    )
}
