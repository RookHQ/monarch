package com.rookhq.monarch.components.datadisplay.insightstrip

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import com.rookhq.monarch.R
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.feedback.banner.MonarchBanner
import com.rookhq.monarch.components.feedback.banner.MonarchBannerAnnouncement
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.internal.MonarchShapeDefaults
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.theme.MonarchTheme

/**
 * A row of insights for the top of a dashboard, each labelled with where it
 * came from: a fixed rule or a model.
 *
 * Built by Monarch from `MonarchCard`, `MonarchBadge`, `MonarchCollapsible`
 * and `MonarchButton`, in a `LazyRow`, a column or a grid. Insights are sorted
 * most severe first, can be limited with a "Show N more" button, and can be
 * dismissed one by one; the app removes a dismissed insight from [insights].
 *
 * Each insight is one TalkBack element read as its tone word, its origin, its
 * title and its reason, so neither the tone nor the origin depends on colour.
 * The evidence, the action and the dismiss button are separate elements.
 *
 * @param insights the insights. Rule-based and model insights can be mixed.
 * @param modifier the modifier of the insight strip.
 * @param title a heading above the strip, for example "Needs attention".
 * @param layout a scrolling row, a column, or a grid of one to three columns.
 * @param loading shows three placeholder cards, read as "Loading insights".
 * @param errorText replaces the insights with this failure, in a danger banner.
 * @param limit shows at most this many insights, with a button for the rest. Zero or less is ignored.
 * @param sortBySeverity orders the insights most severe first: critical, warning, info, a note, positive.
 * @param onRetry adds "Try again" to the error banner.
 * @param onDismiss adds a dismiss button to every insight and is called with its id.
 * @param labels replacements for the built-in strings.
 */
@Composable
public fun MonarchInsightStrip(
    insights: List<MonarchInsight>,
    modifier: Modifier = Modifier,
    title: String? = null,
    layout: MonarchInsightStripLayout = MonarchInsightStripLayout.Scroll,
    loading: Boolean = false,
    errorText: String? = null,
    limit: Int? = null,
    sortBySeverity: Boolean = true,
    onRetry: (() -> Unit)? = null,
    onDismiss: ((String) -> Unit)? = null,
    labels: MonarchInsightStripLabels = MonarchInsightStripLabels(),
) {
    val tokens = MonarchInsightStripTokens
    var expanded by rememberSaveable { mutableStateOf(false) }
    val ordered = tokens.ordered(insights, sortBySeverity)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(tokens.Gap)) {
        if (title != null) MonarchText(text = title, style = MonarchTextStyle.Subheading, heading = true)
        when {
            loading -> Placeholders(
                layout = layout,
                label = monarchString(labels.loading, R.string.monarch_insightstrip_loading),
            )
            errorText != null -> MonarchBanner(
                message = errorText,
                tone = MonarchTone.Danger,
                announcement = MonarchBannerAnnouncement.Polite,
                action = onRetry?.let { retry ->
                    {
                        MonarchButton(
                            text = monarchString(labels.retry, R.string.monarch_insightstrip_retry),
                            onClick = retry,
                            variant = MonarchButtonVariant.Ghost,
                            size = MonarchButtonSize.Sm,
                        )
                    }
                },
            )
            ordered.isEmpty() -> Text(
                text = monarchString(labels.empty, R.string.monarch_insightstrip_empty),
                style = MonarchTheme.typography.body,
                color = tokens.emptyTextColor(MonarchTheme.colors),
            )
            else -> {
                val shown = tokens.shown(ordered, limit, expanded)
                Cards(shown = shown, layout = layout, labels = labels, onDismiss = onDismiss)
                if (tokens.limits(limit, ordered.size)) {
                    val hidden = ordered.size - shown.size
                    val text = when {
                        expanded -> monarchString(labels.showLess, R.string.monarch_insightstrip_show_less)
                        else -> labels.showMore?.invoke(hidden)
                            ?: pluralStringResource(R.plurals.monarch_insightstrip_show_more, hidden, hidden)
                    }
                    MonarchButton(
                        text = text,
                        onClick = { expanded = !expanded },
                        variant = MonarchButtonVariant.Ghost,
                        size = MonarchButtonSize.Sm,
                    )
                }
            }
        }
    }
}

@Composable
private fun Cards(
    shown: List<MonarchInsight>,
    layout: MonarchInsightStripLayout,
    labels: MonarchInsightStripLabels,
    onDismiss: ((String) -> Unit)?,
) {
    val gap = Arrangement.spacedBy(MonarchInsightStripTokens.Gap)
    when (layout) {
        MonarchInsightStripLayout.Scroll -> LazyRow(horizontalArrangement = gap) {
            items(shown, key = { it.id }) { insight ->
                InsightStripCard(
                    insight = insight,
                    labels = labels,
                    onDismiss = onDismiss,
                    // A lone card has no neighbour to peek at, so it takes the whole width like the cards
                    // around the strip; with several, each is most of the width and the next one shows.
                    modifier = if (shown.size == 1) {
                        Modifier.fillParentMaxWidth()
                    } else {
                        Modifier
                            .widthIn(max = MonarchInsightStripTokens.MaxCardWidth)
                            .fillParentMaxWidth(MonarchInsightStripTokens.ROW_CARD_FRACTION)
                    },
                )
            }
        }
        MonarchInsightStripLayout.Stack -> Column(verticalArrangement = gap) {
            shown.forEach { insight ->
                InsightStripCard(insight, labels, onDismiss, Modifier.fillMaxWidth())
            }
        }
        MonarchInsightStripLayout.Grid -> BoxWithConstraints {
            val columns = MonarchInsightStripTokens.columns(maxWidth)
            Column(verticalArrangement = gap) {
                shown.chunked(columns).forEach { row ->
                    Row(horizontalArrangement = gap) {
                        row.forEach { insight -> InsightStripCard(insight, labels, onDismiss, Modifier.weight(1f)) }
                        repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

/** Three placeholder cards, one TalkBack element named [label]. */
@Composable
private fun Placeholders(layout: MonarchInsightStripLayout, label: String) {
    val fill = MonarchInsightStripTokens.placeholderColor(MonarchTheme.colors)
    val card = Modifier
        .height(MonarchInsightStripTokens.PlaceholderHeight)
        .background(fill, MonarchShapeDefaults.Container)
    val semantics = Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = label }
    val gap = Arrangement.spacedBy(MonarchInsightStripTokens.Gap)
    if (layout == MonarchInsightStripLayout.Stack) {
        Column(modifier = semantics, verticalArrangement = gap) {
            repeat(MonarchInsightStripTokens.PLACEHOLDER_COUNT) { Spacer(card.fillMaxWidth()) }
        }
    } else {
        Row(modifier = semantics, horizontalArrangement = gap) {
            repeat(MonarchInsightStripTokens.PLACEHOLDER_COUNT) { Spacer(card.weight(1f)) }
        }
    }
}
