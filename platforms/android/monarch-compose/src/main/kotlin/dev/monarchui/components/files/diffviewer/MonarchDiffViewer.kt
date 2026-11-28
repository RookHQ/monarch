package dev.monarchui.components.files.diffviewer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import dev.monarchui.R
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.forms.segmentedcontrol.MonarchSegmentedControl
import dev.monarchui.components.forms.segmentedcontrol.MonarchSegmentedControlSize
import dev.monarchui.components.layout.divider.MonarchDivider
import dev.monarchui.components.layout.divider.MonarchDividerVariant
import dev.monarchui.internal.MonarchLiveStatus
import dev.monarchui.internal.monarchString
import dev.monarchui.theme.MonarchTheme
import kotlinx.coroutines.launch

/**
 * Shows what changed between two texts or two versions of a document,
 * unified or side by side: added and removed lines, the changed words inside
 * them, folded runs of unchanged lines, the totals, and previous and next
 * change buttons.
 *
 * Built by Monarch on a `LazyColumn` of rows with `AnnotatedString`
 * highlights: Compose has no diff view. The diff (a longest common
 * subsequence over lines, then over words) is ported from the web, not
 * shared. Lines wrap instead of scrolling sideways, so the text grows with
 * the font size. From the `md` breakpoint (768dp of width) the viewer is side
 * by side by default and offers a `MonarchSegmentedControl` layout switch; below
 * it, it is unified. Each row is one TalkBack element read as its text after
 * "Added" or "Removed". Give the viewer a bounded height, as for any lazy
 * list.
 *
 * @param before the lines or blocks of the earlier text (one entry per paragraph for a document).
 * @param after the lines or blocks of the later text.
 * @param modifier the modifier of the viewer.
 * @param mode the layout when the app owns it, with [onModeChange]. `null` is side by side when it fits and
 * unified otherwise; the reader's choice in the switch is then kept by the viewer.
 * @param onModeChange called when the reader picks a layout in the switch.
 * @param granularity [MonarchDiffViewerGranularity.Word] also marks the changed words inside a changed line.
 * @param context unchanged lines kept around each change; longer runs fold behind a "Show N unchanged lines"
 * button. `null` never folds. A negative value counts as 0.
 * @param beforeLabel the name of the earlier side, such as a version name; read in the TalkBack name of the lines
 * and for side by side rows.
 * @param afterLabel the name of the later side.
 * @param showModeSwitch whether the Unified and Side by side switch is shown, also on a phone, where unified is the
 * starting layout.
 * @param showLineNumbers whether line numbers are shown: both numbers in unified, one per side in side by side.
 * @param state the scroll state of the lines.
 * @param labels replacements for the built-in strings.
 * @param actions controls at the end of the header, such as Restore.
 */
@Composable
public fun MonarchDiffViewer(
    before: List<String>,
    after: List<String>,
    modifier: Modifier = Modifier,
    mode: MonarchDiffViewerMode? = null,
    onModeChange: ((MonarchDiffViewerMode) -> Unit)? = null,
    granularity: MonarchDiffViewerGranularity = MonarchDiffViewerGranularity.Word,
    context: Int? = DefaultDiffViewerContext,
    beforeLabel: String? = null,
    afterLabel: String? = null,
    showModeSwitch: Boolean = true,
    showLineNumbers: Boolean = true,
    state: LazyListState = rememberLazyListState(),
    labels: MonarchDiffViewerLabels = MonarchDiffViewerLabels(),
    actions: (@Composable () -> Unit)? = null,
) {
    val lines = remember(before, after) { MonarchDiffViewerLine.diff(before, after) }
    val words = remember(lines, granularity) { diffViewerWordMap(lines, granularity) }
    val stats = remember(lines) { diffViewerStats(lines) }
    // Expanded runs and the current change are positions in this diff, so they start over when the texts change.
    var expanded by remember(lines) { mutableStateOf(emptySet<Int>()) }
    var position by remember(lines) { mutableIntStateOf(-1) }
    var chosenMode by rememberSaveable { mutableStateOf<MonarchDiffViewerMode?>(null) }
    val texts = diffViewerTexts(labels, beforeLabel, afterLabel)
    val colors = MonarchTheme.colors
    val scope = rememberCoroutineScope()
    val reducedMotion = MonarchTheme.motion.reducedMotion
    BoxWithConstraints(modifier = modifier) {
        // An unbounded width is Dp.Unspecified, which never fits two columns.
        val width = if (constraints.hasBoundedWidth) maxWidth else Dp.Unspecified
        val shownMode = mode ?: chosenMode ?: diffViewerDefaultMode(width)
        val split = shownMode == MonarchDiffViewerMode.Split
        val built = remember(lines, split, context, expanded) { diffViewerRows(lines, split, context, expanded) }
        val goTo: (Int) -> Unit = { hunk ->
            val index = built.rows.indexOfFirst { it.hunk == hunk }
            if (index >= 0) {
                position = hunk
                scope.launch { if (reducedMotion) state.scrollToItem(index) else state.animateScrollToItem(index) }
            }
        }
        Surface(
            shape = MonarchTheme.shapes.md,
            color = MonarchDiffViewerTokens.container(colors),
            contentColor = MonarchDiffViewerTokens.text(colors),
            border = BorderStroke(MonarchDiffViewerTokens.BorderWidth, MonarchDiffViewerTokens.border(colors)),
        ) {
            Column {
                DiffViewerHeader(
                    stats = stats,
                    hunks = built.hunks,
                    showSwitch = showModeSwitch,
                    mode = shownMode,
                    labels = labels,
                    onPrevious = { goTo(diffViewerPrevious(position, built.hunks)) },
                    onNext = { goTo(diffViewerNext(position, built.hunks)) },
                    onMode = { picked ->
                        if (mode == null) chosenMode = picked
                        onModeChange?.invoke(picked)
                    },
                    actions = actions,
                )
                DiffViewerPosition(position, built.hunks, labels)
                MonarchDivider(variant = MonarchDividerVariant.Subtle)
                if (lines.isEmpty() || !stats.changed) {
                    Text(
                        text = monarchString(labels.noChanges, R.string.monarch_diffviewer_no_changes),
                        modifier = Modifier.padding(MonarchDiffViewerTokens.HeaderPadding),
                        style = MonarchTheme.typography.body,
                        color = MonarchDiffViewerTokens.emptyText(colors),
                    )
                } else {
                    DiffViewerBody(
                        built = built,
                        position = position,
                        rowContext = DiffViewerRowContext(
                            lines = lines,
                            words = words,
                            numberWidth = diffViewerNumberWidth(lines, showLineNumbers),
                            texts = texts,
                        ),
                        state = state,
                        caption = labels.caption?.invoke(texts.before, texts.after)
                            ?: stringResource(R.string.monarch_diffviewer_caption, texts.before, texts.after),
                        labels = labels,
                        onExpand = { id -> expanded = expanded + id },
                    )
                }
            }
        }
    }
}

/**
 * Shows what changed between two texts, split into lines on `\n`, `\r\n` and `\r`. See the overload that
 * takes lists for the behaviour; this one splits [before] and [after] with [MonarchDiffViewerLine.lines].
 *
 * @param before the earlier text.
 * @param after the later text.
 * @param modifier the modifier of the viewer.
 * @param mode the layout when the app owns it, with [onModeChange]; `null` follows the width.
 * @param onModeChange called when the reader picks a layout in the switch.
 * @param granularity whether the changed words inside a changed line are marked.
 * @param context unchanged lines kept around each change; `null` never folds.
 * @param beforeLabel the name of the earlier side.
 * @param afterLabel the name of the later side.
 * @param showModeSwitch whether the Unified and Side by side switch is shown.
 * @param showLineNumbers whether line numbers are shown.
 * @param state the scroll state of the lines.
 * @param labels replacements for the built-in strings.
 * @param actions controls at the end of the header, such as Restore.
 */
@Composable
public fun MonarchDiffViewer(
    before: String,
    after: String,
    modifier: Modifier = Modifier,
    mode: MonarchDiffViewerMode? = null,
    onModeChange: ((MonarchDiffViewerMode) -> Unit)? = null,
    granularity: MonarchDiffViewerGranularity = MonarchDiffViewerGranularity.Word,
    context: Int? = DefaultDiffViewerContext,
    beforeLabel: String? = null,
    afterLabel: String? = null,
    showModeSwitch: Boolean = true,
    showLineNumbers: Boolean = true,
    state: LazyListState = rememberLazyListState(),
    labels: MonarchDiffViewerLabels = MonarchDiffViewerLabels(),
    actions: (@Composable () -> Unit)? = null,
) {
    MonarchDiffViewer(
        before = remember(before) { MonarchDiffViewerLine.lines(before) },
        after = remember(after) { MonarchDiffViewerLine.lines(after) },
        modifier = modifier,
        mode = mode,
        onModeChange = onModeChange,
        granularity = granularity,
        context = context,
        beforeLabel = beforeLabel,
        afterLabel = afterLabel,
        showModeSwitch = showModeSwitch,
        showLineNumbers = showLineNumbers,
        state = state,
        labels = labels,
        actions = actions,
    )
}

/** The unchanged lines kept around each change by default. */
private const val DefaultDiffViewerContext = 3

@Composable
private fun diffViewerTexts(labels: MonarchDiffViewerLabels, beforeLabel: String?, afterLabel: String?) =
    DiffViewerRowTexts(
        before = beforeLabel ?: monarchString(labels.before, R.string.monarch_diffviewer_before),
        after = afterLabel ?: monarchString(labels.after, R.string.monarch_diffviewer_after),
        added = monarchString(labels.added, R.string.monarch_diffviewer_added),
        removed = monarchString(labels.removed, R.string.monarch_diffviewer_removed),
        blankLine = monarchString(labels.blankLine, R.string.monarch_diffviewer_blank_line),
    )

/** The width of the widest line number in the code style, so the numbers line up at every font size. */
@Composable
private fun diffViewerNumberWidth(lines: List<MonarchDiffViewerLine>, show: Boolean): Dp? {
    if (!show) return null
    val widest = lines.maxOfOrNull { maxOf(it.oldNumber ?: 0, it.newNumber ?: 0) } ?: 0
    val measurer = rememberTextMeasurer()
    val style = MonarchTheme.typography.code
    val sample = "9".repeat(widest.toString().length)
    val pixels = remember(sample, style, measurer) { measurer.measure(sample, style).size.width }
    return with(LocalDensity.current) { pixels.toDp() } + MonarchDiffViewerTokens.CellPadding
}

@Suppress("LongParameterList")
@Composable
private fun DiffViewerHeader(
    stats: DiffViewerStats,
    hunks: Int,
    showSwitch: Boolean,
    mode: MonarchDiffViewerMode,
    labels: MonarchDiffViewerLabels,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onMode: (MonarchDiffViewerMode) -> Unit,
    actions: (@Composable () -> Unit)?,
) {
    val colors = MonarchTheme.colors
    val addedColor = MonarchDiffViewerTokens.markerColor(MonarchDiffViewerLineKind.Added, colors)
    val removedColor = MonarchDiffViewerTokens.markerColor(MonarchDiffViewerLineKind.Removed, colors)
    val spokenStats = labels.stats?.invoke(stats.added, stats.removed)
        ?: stringResource(
            R.string.monarch_diffviewer_stats,
            pluralStringResource(R.plurals.monarch_diffviewer_stats_added, stats.added, stats.added),
            pluralStringResource(R.plurals.monarch_diffviewer_stats_removed, stats.removed, stats.removed),
        )
    FlowRow(
        // The buttons reserve a 48dp target around their 32dp shape, so the rows need no gap of their own: the
        // space above the first row, between the rows and below the last one all come out the same.
        modifier = Modifier.fillMaxWidth().padding(
            start = MonarchDiffViewerTokens.HeaderPadding,
            end = MonarchDiffViewerTokens.HeaderPadding,
            top = MonarchDiffViewerTokens.HeaderGap,
            bottom = MonarchDiffViewerTokens.HeaderPadding,
        ),
        horizontalArrangement = Arrangement.spacedBy(MonarchDiffViewerTokens.HeaderGap),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = addedColor)) { append("+${stats.added}") }
                append(" ")
                withStyle(SpanStyle(color = removedColor)) {
                    append(diffViewerMarker(MonarchDiffViewerLineKind.Removed) + stats.removed)
                }
            },
            modifier = Modifier.clearAndSetSemantics { contentDescription = spokenStats },
            style = MonarchTheme.typography.label,
        )
        if (hunks > 1) {
            MonarchButton(
                text = monarchString(labels.previous, R.string.monarch_diffviewer_previous),
                onClick = onPrevious,
                variant = MonarchButtonVariant.Ghost,
                size = MonarchButtonSize.Sm,
            )
            MonarchButton(
                text = monarchString(labels.next, R.string.monarch_diffviewer_next),
                onClick = onNext,
                variant = MonarchButtonVariant.Ghost,
                size = MonarchButtonSize.Sm,
            )
        }
        if (showSwitch) DiffViewerModeSwitch(mode, labels, onMode)
        actions?.invoke()
    }
}

@Composable
private fun DiffViewerModeSwitch(
    mode: MonarchDiffViewerMode,
    labels: MonarchDiffViewerLabels,
    onMode: (MonarchDiffViewerMode) -> Unit,
) {
    val name = monarchString(labels.mode, R.string.monarch_diffviewer_mode)
    val options = listOf(
        MonarchDiffViewerMode.Unified to monarchString(labels.unified, R.string.monarch_diffviewer_unified),
        MonarchDiffViewerMode.Split to monarchString(labels.split, R.string.monarch_diffviewer_split),
    )
    MonarchSegmentedControl(
        modifier = Modifier.semantics {
            isTraversalGroup = true
            contentDescription = name
        },
        size = MonarchSegmentedControlSize.Sm,
    ) {
        options.forEach { (option, label) ->
            Segment(selected = option == mode, onClick = { onMode(option) }, label = label)
        }
    }
}

/** "Change 2 of 5" in a polite live region, after previous or next. Always present, empty until then. */
@Composable
private fun DiffViewerPosition(position: Int, hunks: Int, labels: MonarchDiffViewerLabels) {
    val text = when {
        position < 0 -> ""
        labels.position != null -> labels.position.invoke(position + 1, hunks)
        else -> stringResource(R.string.monarch_diffviewer_position, position + 1, hunks)
    }
    MonarchLiveStatus(text = text.takeIf { it.isNotEmpty() }, visible = false)
}

@Suppress("LongParameterList")
@Composable
private fun DiffViewerBody(
    built: DiffViewerRows,
    position: Int,
    rowContext: DiffViewerRowContext,
    state: LazyListState,
    caption: String,
    labels: MonarchDiffViewerLabels,
    onExpand: (Int) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth().semantics {
            isTraversalGroup = true
            contentDescription = caption
        },
        state = state,
    ) {
        itemsIndexed(built.rows, key = { index, row -> diffViewerRowKey(index, row) }) { _, row ->
            val current = row.hunk != null && row.hunk == position
            when (row) {
                is DiffViewerRow.Line -> DiffViewerUnifiedRow(row, current, rowContext)
                is DiffViewerRow.Pair -> DiffViewerSplitRow(row, current, rowContext)
                is DiffViewerRow.Gap -> MonarchButton(
                    text = labels.expand?.invoke(row.count)
                        ?: pluralStringResource(R.plurals.monarch_diffviewer_expand, row.count, row.count),
                    onClick = { onExpand(row.id) },
                    modifier = Modifier.fillMaxWidth(),
                    variant = MonarchButtonVariant.Ghost,
                    size = MonarchButtonSize.Sm,
                )
            }
        }
    }
}

/** A folded run keeps its key when rows above it change; other rows are keyed by position. */
private fun diffViewerRowKey(index: Int, row: DiffViewerRow): Any =
    if (row is DiffViewerRow.Gap) "gap-${row.id}" else index
