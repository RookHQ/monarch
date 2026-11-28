package dev.monarchui.components.layout.sectionheader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Constraints
import dev.monarchui.R
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.internal.MonarchProgressRing
import dev.monarchui.internal.monarchString
import dev.monarchui.theme.MonarchTheme

/**
 * An unboxed section title with an optional description, meta line and
 * trailing actions.
 *
 * Built by Monarch from Compose layouts: Material 3 has no section header. It
 * draws no surface of its own, so it works on the screen, inside a card or in
 * a pane. The title is a TalkBack heading. The actions stay beside the text
 * while they take at most half the width, and move below it otherwise, for
 * example at a large font size. While [busy], a small progress ring follows
 * the title and TalkBack reads "Loading" as the title's state. The content of
 * the section follows the header in the app's own column or list.
 *
 * @param title the section title, already localised.
 * @param modifier the modifier of the header.
 * @param description a sentence under the title that explains the section.
 * @param meta a short secondary line: a count, a date range, "Updated 5 min ago".
 * @param size `Sm` for dashboard sections and card headers, `Md` for the sections of a screen.
 * @param busy the section's content is loading or refreshing.
 * @param busyLabel replaces the built-in state "Loading" read while [busy].
 * @param tone colours the title with the strong colour of a tone, for a section whose state matters
 * ("Overdue" in danger). `null` keeps the default text colour. Say the state in the title or [meta] too.
 * @param actions buttons, a menu or a "View all" link at the end, in a row with the standard gap.
 */
@Composable
public fun MonarchSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    meta: String? = null,
    size: MonarchSectionHeaderSize = MonarchSectionHeaderSize.Sm,
    busy: Boolean = false,
    busyLabel: String? = null,
    tone: MonarchTone? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    val tokens = MonarchSectionHeaderTokens
    val busyText = monarchString(busyLabel, R.string.monarch_sectionheader_loading)
    val text: @Composable () -> Unit = {
        HeaderText(title, description, meta, size, busy, busyText, tone)
    }
    val trailing: @Composable () -> Unit = {
        if (actions != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(tokens.ActionGap),
                verticalAlignment = Alignment.CenterVertically,
                content = actions,
            )
        }
    }
    val gapPx = with(LocalDensity.current) { tokens.Gap.roundToPx() }
    Layout(contents = listOf(text, trailing), modifier = modifier) { measurables, constraints ->
        val (textMeasurables, actionMeasurables) = measurables
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val actions = actionMeasurables.map { it.measure(loose) }
        val actionsWidth = actions.maxOfOrNull { it.width } ?: 0
        // The actions keep their own width beside the title, as on the web; the text wraps in what is left. Only
        // when the text would get less than a third of the row do the actions go below it.
        val stacked = tokens.stacksActions(constraints.maxWidth, actionsWidth, gapPx, constraints.hasBoundedWidth) &&
            constraints.maxWidth - actionsWidth - gapPx < constraints.maxWidth * tokens.MIN_TEXT_SHARE
        val textMaxWidth = headerTextMaxWidth(constraints, stacked, actionsWidth, gapPx)
        val texts = textMeasurables.map { it.measure(loose.copy(maxWidth = textMaxWidth)) }
        placeHeader(constraints, texts, actions, stacked, gapPx)
    }
}

/** The width the text may take: all of it when the actions are below, the rest when they are beside it. */
private fun headerTextMaxWidth(constraints: Constraints, stacked: Boolean, actionsWidth: Int, gap: Int): Int = when {
    !constraints.hasBoundedWidth -> Constraints.Infinity
    stacked || actionsWidth == 0 -> constraints.maxWidth
    else -> (constraints.maxWidth - actionsWidth - gap).coerceAtLeast(0)
}

/** Places the text at the start and the actions at the end of the row, or below the text when [stacked]. */
private fun MeasureScope.placeHeader(
    constraints: Constraints,
    texts: List<Placeable>,
    actions: List<Placeable>,
    stacked: Boolean,
    gap: Int,
): MeasureResult {
    val textWidth = texts.widest()
    val textHeight = texts.tallest()
    val actionsWidth = actions.widest()
    val actionsHeight = actions.tallest()
    val besideWidth = if (actionsWidth > 0) actionsWidth + gap else 0
    val contentWidth = if (stacked) maxOf(textWidth, actionsWidth) else textWidth + besideWidth
    val width = if (constraints.hasBoundedWidth) constraints.maxWidth else contentWidth
    val rowHeight = maxOf(textHeight, actionsHeight)
    val stackedHeight = textHeight + if (actionsHeight > 0) gap + actionsHeight else 0
    val height = (if (stacked) stackedHeight else rowHeight).coerceIn(constraints.minHeight, constraints.maxHeight)
    return layout(width.coerceAtLeast(constraints.minWidth), height) {
        if (stacked) {
            texts.forEach { it.placeRelative(0, 0) }
            actions.forEach { it.placeRelative(0, textHeight + gap) }
        } else {
            texts.forEach { it.placeRelative(0, (rowHeight - it.height) / 2) }
            actions.forEach { it.placeRelative(width - it.width, (rowHeight - it.height) / 2) }
        }
    }
}

private fun List<Placeable>.widest(): Int = maxOfOrNull { it.width } ?: 0

private fun List<Placeable>.tallest(): Int = maxOfOrNull { it.height } ?: 0

@Composable
private fun HeaderText(
    title: String,
    description: String?,
    meta: String?,
    size: MonarchSectionHeaderSize,
    busy: Boolean,
    busyText: String,
    tone: MonarchTone?,
) {
    val tokens = MonarchSectionHeaderTokens
    val colors = MonarchTheme.colors
    val typography = MonarchTheme.typography
    Column(verticalArrangement = Arrangement.spacedBy(tokens.TextGap)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(tokens.BusyGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .semantics {
                        heading()
                        if (busy) stateDescription = busyText
                    },
                style = tokens.titleStyle(size, typography),
                color = tokens.titleColor(colors, tone),
            )
            if (busy) BusyRing()
        }
        if (description != null) {
            Text(text = description, style = tokens.descriptionStyle(typography), color = tokens.secondaryColor(colors))
        }
        if (meta != null) {
            Text(text = meta, style = tokens.metaStyle(typography), color = tokens.secondaryColor(colors))
        }
    }
}

/** The busy ring after the title; still when animations are off. Hidden from TalkBack, which reads the state. */
@Composable
private fun BusyRing() {
    val tokens = MonarchSectionHeaderTokens
    Box(modifier = Modifier.clearAndSetSemantics {}) {
        MonarchProgressRing(
            progress = null,
            diameter = tokens.BusySize * LocalDensity.current.fontScale,
            color = MonarchTheme.colors.accentSolid,
            trackColor = MonarchTheme.colors.bgEmphasis,
            strokeWidth = tokens.BusyStroke,
            stillFraction = tokens.STILL_BUSY_FRACTION,
        )
    }
}
