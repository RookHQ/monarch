package com.rookhq.monarch.components.feedback.consequencelist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.rookhq.monarch.R
import com.rookhq.monarch.components.feedback.skeleton.MonarchSkeleton
import com.rookhq.monarch.components.feedback.skeleton.MonarchSkeletonShape
import com.rookhq.monarch.internal.MonarchDefaultIcons
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.theme.MonarchTheme
import com.rookhq.monarch.tokens.MonarchTokens
import java.text.NumberFormat

/**
 * What an action will delete, change and keep, and what to know first, to
 * read before confirming. Used inside confirmation dialogs and destructive
 * flows.
 *
 * Built by Monarch from a column of rows: Material 3 has no equivalent. Each
 * row is one TalkBack element. Ungrouped, TalkBack reads the kind before each
 * row ("Removed"); grouped, the rows sit under a heading per kind.
 *
 * Pass [icon] so that each kind also has its own shape: the kind is then
 * never carried by colour alone. Without icons the label must say what
 * happens, for example "tasks will be deleted".
 *
 * @param items the consequences, in the order they are read.
 * @param modifier the modifier of the list.
 * @param grouped splits the items under the headings "Will be deleted", "Will change", "Before you
 * continue" and "Will be kept", in that order.
 * @param loading shows placeholder rows instead of the items while the counts load.
 * @param heading a heading above the list, for example "What happens".
 * @param icon the icon of a kind, for example a `MonarchIcon` of size `Sm` with a `null` content description;
 * it is tinted by kind and hidden from TalkBack. Monarch bundles no icons.
 * @param loadingRowCount the number of placeholder rows while [loading].
 * @param kindLabel the word TalkBack reads before an ungrouped row. `null` uses the built-in strings
 * `monarch_consequencelist_kind_*`.
 * @param groupTitle the heading of a group. `null` uses the built-in strings `monarch_consequencelist_group_*`.
 * @param loadingLabel what TalkBack reads for the placeholder rows. `null` uses
 * `monarch_consequencelist_loading`.
 */
@Composable
public fun MonarchConsequenceList(
    items: List<MonarchConsequence>,
    modifier: Modifier = Modifier,
    grouped: Boolean = false,
    loading: Boolean = false,
    heading: String? = null,
    icon: (@Composable (MonarchConsequenceKind) -> Unit)? = MonarchDefaultIcons.consequence,
    loadingRowCount: Int = MonarchConsequenceListTokens.DEFAULT_LOADING_ROWS,
    kindLabel: ((MonarchConsequenceKind) -> String)? = null,
    groupTitle: ((MonarchConsequenceKind) -> String)? = null,
    loadingLabel: String? = null,
) {
    val colors = MonarchTheme.colors
    val kindNames = kindLabel ?: builtInKindNames()
    val groupNames = groupTitle ?: builtInGroupNames()
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MonarchConsequenceListTokens.SectionGap),
    ) {
        if (heading != null) {
            Text(
                text = heading,
                modifier = Modifier.semantics { heading() },
                style = MonarchTheme.typography.label,
                color = colors.textDefault,
            )
        }
        when {
            loading -> LoadingRows(
                count = loadingRowCount.coerceAtLeast(1),
                label = monarchString(loadingLabel, R.string.monarch_consequencelist_loading),
            )
            grouped -> MonarchConsequenceListTokens.GroupOrder.forEach { kind ->
                val group = items.filter { it.kind == kind }
                if (group.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(MonarchConsequenceListTokens.GroupGap)) {
                        Text(
                            text = groupNames(kind),
                            modifier = Modifier.semantics { heading() },
                            style = MonarchTheme.typography.label,
                            color = colors.textMuted,
                        )
                        ConsequenceRows(group, spokenKind = null, icon = icon)
                    }
                }
            }
            else -> ConsequenceRows(items, spokenKind = kindNames, icon = icon)
        }
    }
}

@Composable
private fun ConsequenceRows(
    items: List<MonarchConsequence>,
    spokenKind: ((MonarchConsequenceKind) -> String)?,
    icon: (@Composable (MonarchConsequenceKind) -> Unit)?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(MonarchConsequenceListTokens.ItemGap)) {
        items.forEachIndexed { index, item ->
            key(item.id ?: index) { ConsequenceRow(item, spokenKind?.invoke(item.kind), icon) }
        }
    }
}

/** One consequence: the icon, then the count and label with the detail under them, as one element. */
@Composable
private fun ConsequenceRow(
    item: MonarchConsequence,
    spokenKind: String?,
    icon: (@Composable (MonarchConsequenceKind) -> Unit)?,
) {
    val colors = MonarchTheme.colors
    val locale = LocalConfiguration.current.locales[0]
    val text = buildAnnotatedString {
        if (item.count != null) {
            withStyle(
                SpanStyle(
                    color = MonarchConsequenceListTokens.countColor(item.kind, colors),
                    fontWeight = FontWeight(MonarchTokens.FontWeight.semibold),
                    fontFeatureSettings = "tnum",
                ),
            ) { append(NumberFormat.getIntegerInstance(locale).format(item.count)) }
            append(" ")
        }
        append(item.label)
    }
    Row(
        modifier = Modifier.semantics(mergeDescendants = true) { },
        horizontalArrangement = Arrangement.spacedBy(MonarchConsequenceListTokens.ItemGap),
    ) {
        if (icon != null) {
            CompositionLocalProvider(
                LocalContentColor provides MonarchConsequenceListTokens.iconColor(item.kind, colors),
            ) {
                Box(Modifier.clearAndSetSemantics { }) { icon(item.kind) }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(MonarchConsequenceListTokens.DetailGap)) {
            if (spokenKind != null) Box(Modifier.semantics { this.text = AnnotatedString(spokenKind) })
            Text(text = text, style = MonarchTheme.typography.body, color = colors.textDefault)
            if (item.detail != null) {
                Text(text = item.detail, style = MonarchTheme.typography.caption, color = colors.textMuted)
            }
        }
    }
}

/** Placeholder rows, read by TalkBack as one element named [label]. */
@Composable
private fun LoadingRows(count: Int, label: String) {
    val widths = MonarchConsequenceListTokens.SkeletonWidths
    Column(
        modifier = Modifier.clearAndSetSemantics { contentDescription = label },
        verticalArrangement = Arrangement.spacedBy(MonarchConsequenceListTokens.SkeletonRowGap),
    ) {
        repeat(count) { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(MonarchConsequenceListTokens.ItemGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MonarchSkeleton(
                    modifier = Modifier.size(MonarchConsequenceListTokens.IconSize),
                    shape = MonarchSkeletonShape.Circle,
                )
                MonarchSkeleton(
                    modifier = Modifier.fillMaxWidth(widths[row % widths.size]),
                    shape = MonarchSkeletonShape.Text,
                )
            }
        }
    }
}

@Composable
private fun builtInKindNames(): (MonarchConsequenceKind) -> String {
    val removed = stringResource(R.string.monarch_consequencelist_kind_removed)
    val changed = stringResource(R.string.monarch_consequencelist_kind_changed)
    val warning = stringResource(R.string.monarch_consequencelist_kind_warning)
    val kept = stringResource(R.string.monarch_consequencelist_kind_kept)
    return { kind ->
        when (kind) {
            MonarchConsequenceKind.Removed -> removed
            MonarchConsequenceKind.Changed -> changed
            MonarchConsequenceKind.Warning -> warning
            MonarchConsequenceKind.Kept -> kept
        }
    }
}

@Composable
private fun builtInGroupNames(): (MonarchConsequenceKind) -> String {
    val removed = stringResource(R.string.monarch_consequencelist_group_removed)
    val changed = stringResource(R.string.monarch_consequencelist_group_changed)
    val warning = stringResource(R.string.monarch_consequencelist_group_warning)
    val kept = stringResource(R.string.monarch_consequencelist_group_kept)
    return { kind ->
        when (kind) {
            MonarchConsequenceKind.Removed -> removed
            MonarchConsequenceKind.Changed -> changed
            MonarchConsequenceKind.Warning -> warning
            MonarchConsequenceKind.Kept -> kept
        }
    }
}
