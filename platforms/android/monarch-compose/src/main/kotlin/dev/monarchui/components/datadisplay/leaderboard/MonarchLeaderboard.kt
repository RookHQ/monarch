package dev.monarchui.components.datadisplay.leaderboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import dev.monarchui.R
import dev.monarchui.accessibility.MonarchTouchTarget
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.charts.barlist.MonarchBarListSort
import dev.monarchui.components.charts.deltaindicator.MonarchDeltaIndicator
import dev.monarchui.components.charts.deltaindicator.MonarchDeltaIndicatorGoodDirection
import dev.monarchui.components.datadisplay.badge.MonarchBadge
import dev.monarchui.components.feedback.emptystate.MonarchEmptyState
import dev.monarchui.components.feedback.emptystate.MonarchEmptyStateSize
import dev.monarchui.components.feedback.inlineerrorretry.MonarchInlineErrorRetry
import dev.monarchui.components.feedback.skeleton.MonarchSkeleton
import dev.monarchui.components.feedback.skeleton.MonarchSkeletonShape
import dev.monarchui.components.identity.avatar.MonarchAvatar
import dev.monarchui.components.identity.avatar.MonarchAvatarSize
import dev.monarchui.internal.MonarchShapeDefaults
import dev.monarchui.internal.monarchString
import dev.monarchui.internal.monarchSurface
import dev.monarchui.theme.MonarchTheme
import java.text.NumberFormat

/**
 * A ranked list for dashboards: rank, avatar, name, value, an optional
 * change and a bar relative to the leader, with ties sharing a rank and the
 * viewer's own row tagged "(you)".
 *
 * Built by Monarch from a `Column` of rows with `MonarchAvatar`,
 * `MonarchProgress`, `MonarchDeltaIndicator` and `MonarchBadge`, and the
 * loading, empty and error states of `MonarchSkeleton`, `MonarchEmptyState`
 * and `MonarchInlineErrorRetry`. A board shows a handful of rows inside a
 * scrolling dashboard, so it is not lazy. Each row is one TalkBack element
 * read as "Rank 1: Ana Lima, (you), 42 tickets solved, up 12%"; with
 * [onSelect] it is a button.
 *
 * @param entries the entries, best first, or set [sort].
 * @param modifier the modifier of the board.
 * @param sort Descending ranks the largest value first, Ascending the smallest; None keeps the order given.
 * @param label names the board for TalkBack, "Top agents by tickets solved".
 * @param valueLabel what the value counts, read after it: "tickets solved".
 * @param changeComparison the period every change compares with, "vs last week".
 * @param changeGoodDirection which direction of change is good, for every row.
 * @param showAvatars shows the avatars.
 * @param showBars shows a bar under each name, its length relative to the largest value shown.
 * @param highlightTop puts ranks 1 to 3 on a filled marker.
 * @param limit shows at most this many rows. `null`, 0 or less shows all.
 * @param total how many entries exist when [entries] is already a top N; adds "N more".
 * @param loading shows placeholder rows.
 * @param loadingRows the number of placeholder rows.
 * @param errorMessage replaces the list with this failure.
 * @param retrying the retry is running: the Retry button shows its busy state.
 * @param formatValue writes a value without `display`. `null` writes a number in the current locale.
 * @param onRetry repeats a failed load. Adds a Retry button to the error.
 * @param onShowMore makes "N more" a button, for example to open the full report.
 * @param onSelect makes each row a button.
 * @param labels replacements for the built-in strings.
 */
@Composable
public fun MonarchLeaderboard(
    entries: List<MonarchLeaderboardEntry>,
    modifier: Modifier = Modifier,
    sort: MonarchBarListSort = MonarchBarListSort.None,
    label: String? = null,
    valueLabel: String? = null,
    changeComparison: String? = null,
    changeGoodDirection: MonarchDeltaIndicatorGoodDirection = MonarchDeltaIndicatorGoodDirection.Up,
    showAvatars: Boolean = true,
    showBars: Boolean = true,
    highlightTop: Boolean = false,
    limit: Int? = null,
    total: Int? = null,
    loading: Boolean = false,
    loadingRows: Int = 5,
    errorMessage: String? = null,
    retrying: Boolean = false,
    formatValue: ((Double) -> String)? = null,
    onRetry: (() -> Unit)? = null,
    onShowMore: (() -> Unit)? = null,
    onSelect: ((MonarchLeaderboardEntry) -> Unit)? = null,
    labels: MonarchLeaderboardLabels = MonarchLeaderboardLabels(),
) {
    val logic = MonarchLeaderboardLogic
    val ranked = logic.rank(entries, sort)
    val shown = logic.shown(ranked, limit)
    Column(
        modifier = modifier.semantics {
            if (label != null) contentDescription = label
            isTraversalGroup = true
        },
    ) {
        when {
            loading -> Placeholder(loadingRows, showAvatars, labels)
            errorMessage != null -> Failure(errorMessage, retrying, onRetry, labels)
            shown.isEmpty() -> MonarchEmptyState(
                title = monarchString(labels.empty, R.string.monarch_leaderboard_empty),
                size = MonarchEmptyStateSize.Sm,
            )
            else -> {
                val locale = LocalConfiguration.current.locales[0]
                val write = formatValue ?: remember(locale) {
                    val format = NumberFormat.getNumberInstance(locale)
                    val writer: (Double) -> String = { format.format(it) }
                    writer
                }
                val options = RowOptions(
                    valueLabel = valueLabel,
                    comparison = changeComparison,
                    goodDirection = changeGoodDirection,
                    showAvatars = showAvatars,
                    showBars = showBars,
                    highlightTop = highlightTop,
                )
                shown.forEach { ranked ->
                    LeaderboardRow(ranked, logic.share(ranked.entry.value, shown), write, options, onSelect, labels)
                }
                More(logic.hidden(total, entries.size, shown.size), onShowMore, labels)
            }
        }
    }
}

/** The options every row shares. */
private class RowOptions(
    val valueLabel: String?,
    val comparison: String?,
    val goodDirection: MonarchDeltaIndicatorGoodDirection,
    val showAvatars: Boolean,
    val showBars: Boolean,
    val highlightTop: Boolean,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LeaderboardRow(
    ranked: MonarchLeaderboardRanked,
    share: Float,
    write: (Double) -> String,
    options: RowOptions,
    onSelect: ((MonarchLeaderboardEntry) -> Unit)?,
    labels: MonarchLeaderboardLabels,
) {
    val tokens = MonarchLeaderboardTokens
    val colors = MonarchTheme.colors
    val entry = ranked.entry
    val shape = MonarchShapeDefaults.Row
    val interaction = if (onSelect != null) {
        Modifier.clickable(role = Role.Button) { onSelect(entry) }
    } else {
        Modifier.semantics(mergeDescendants = true) { }
    }
    val value = entry.display ?: write(entry.value)
    val spokenValue = if (options.valueLabel == null) {
        value
    } else {
        stringResource(R.string.monarch_leaderboard_value, value, options.valueLabel)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = MonarchTouchTarget.Minimum)
            .monarchSurface(tokens.rowColors(entry.current, colors), shape)
            .then(interaction)
            .padding(tokens.RowPadding),
        horizontalArrangement = Arrangement.spacedBy(tokens.Gap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Rank(ranked.rank, MonarchLeaderboardLogic.isTop(options.highlightTop, ranked.rank), labels)
        if (options.showAvatars) {
            MonarchAvatar(
                image = entry.image,
                name = entry.name,
                shape = entry.avatarShape,
                size = MonarchAvatarSize.Sm,
                seed = entry.seed,
                decorative = true,
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(tokens.LineGap)) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(tokens.LineGap),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = entry.name, style = MonarchTheme.typography.bodyStrong, color = colors.textDefault)
                if (entry.current) {
                    Text(
                        text = monarchString(labels.you, R.string.monarch_leaderboard_you),
                        style = MonarchTheme.typography.caption,
                        color = tokens.mutedColor(colors),
                    )
                }
                if (entry.badge != null) MonarchBadge(text = entry.badge, tone = entry.badgeTone)
            }
            if (entry.secondary != null) {
                Text(text = entry.secondary, style = MonarchTheme.typography.caption, color = tokens.mutedColor(colors))
            }
            if (options.showBars) {
                // As the web draws it: a muted track with a fill in the first data colour.
                Box(
                    modifier = Modifier
                        .clearAndSetSemantics { }
                        .fillMaxWidth()
                        .height(MonarchLeaderboardTokens.BarHeight)
                        .background(colors.bgMuted, MonarchTheme.shapes.full),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(share.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .background(MonarchLeaderboardTokens.barColor(colors), MonarchTheme.shapes.full),
                    )
                }
            }
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(tokens.LineGap)) {
            Box(modifier = Modifier.clearAndSetSemantics { contentDescription = spokenValue }) {
                Text(text = value, style = MonarchTheme.typography.bodyStrong, color = colors.textDefault)
            }
            if (entry.change != null) {
                MonarchDeltaIndicator(
                    value = entry.change,
                    comparison = options.comparison,
                    goodDirection = options.goodDirection,
                )
            }
        }
    }
}

@Composable
private fun Rank(rank: Int, top: Boolean, labels: MonarchLeaderboardLabels) {
    val tokens = MonarchLeaderboardTokens
    val colors = tokens.rankColors(top, MonarchTheme.colors)
    val spoken = labels.rank?.invoke(rank) ?: stringResource(R.string.monarch_leaderboard_rank, rank)
    Box(
        modifier = Modifier.widthIn(min = tokens.RankWidth).clearAndSetSemantics { contentDescription = spoken },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = if (top) {
                Modifier.size(tokens.TopMarker).monarchSurface(colors, MonarchShapeDefaults.Mark)
            } else {
                Modifier
            },
            contentAlignment = Alignment.Center,
        ) {
            Text(text = rank.toString(), style = MonarchTheme.typography.label, color = colors.content)
        }
    }
}

@Composable
private fun More(hidden: Int, onShowMore: (() -> Unit)?, labels: MonarchLeaderboardLabels) {
    if (hidden <= 0) return
    val text = labels.more?.invoke(hidden) ?: pluralStringResource(R.plurals.monarch_leaderboard_more, hidden, hidden)
    if (onShowMore != null) {
        MonarchButton(text = text, onClick = onShowMore, variant = MonarchButtonVariant.Ghost)
    } else {
        Text(
            text = text,
            modifier = Modifier.padding(MonarchLeaderboardTokens.RowPadding),
            style = MonarchTheme.typography.caption,
            color = MonarchLeaderboardTokens.mutedColor(MonarchTheme.colors),
        )
    }
}

@Composable
private fun Placeholder(rows: Int, showAvatars: Boolean, labels: MonarchLeaderboardLabels) {
    val tokens = MonarchLeaderboardTokens
    val loading = monarchString(labels.loading, R.string.monarch_leaderboard_loading)
    Column(
        modifier = Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = loading },
        verticalArrangement = Arrangement.spacedBy(tokens.Gap),
    ) {
        repeat(rows.coerceAtLeast(1)) {
            Row(
                modifier = Modifier.padding(tokens.RowPadding),
                horizontalArrangement = Arrangement.spacedBy(tokens.Gap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (showAvatars) MonarchSkeleton(shape = MonarchSkeletonShape.Circle)
                MonarchSkeleton(modifier = Modifier.width(tokens.PlaceholderWidth), shape = MonarchSkeletonShape.Text)
            }
        }
    }
}

@Composable
private fun Failure(message: String, retrying: Boolean, onRetry: (() -> Unit)?, labels: MonarchLeaderboardLabels) {
    if (onRetry != null) {
        MonarchInlineErrorRetry(message = message, onRetry = onRetry, retrying = retrying, retryLabel = labels.retry)
    } else {
        Text(
            text = message,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            style = MonarchTheme.typography.body,
            color = MonarchLeaderboardTokens.errorColor(MonarchTheme.colors),
        )
    }
}
