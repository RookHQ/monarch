package dev.monarchui.components.workflow.orgchart

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.theme.MonarchTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * An organisation chart: people as cards under their managers, with teams
 * that open and close, teams loaded on demand, zoom and pan, dotted reporting
 * lines and drag to move; an indented list on a phone or at large font sizes.
 *
 * A Monarch pattern. The chart layout lays cards out with Compose `Row` and
 * `Column`, scrolls both ways with `horizontalScroll` and `verticalScroll`,
 * zooms with the buttons or a pinch (`transformable`) and draws its lines on
 * the canvas. The list layout is a `LazyColumn`. Cards are `MonarchAvatar`
 * and `MonarchText`; zoom buttons are `MonarchButton`. Each card is one
 * TalkBack element with "Show team" or "Hide team" and "Move" actions; the
 * chip under a manager is a button with the same name. People move by
 * touching and holding a card, then dragging it onto their new manager.
 * The chart scrolls itself, so give it a height.
 *
 * @param nodes every person and open position, flat. Reporting lines come from [MonarchOrgChartNode.parentId].
 * @param expandIcon a chevron that points to the end, for the team chips. It turns to point down when the team is
 * shown. Monarch bundles no icons.
 * @param modifier the modifier of the chart. It needs a bounded height.
 * @param expanded the ids of people whose team is shown. `null` keeps them inside the chart, starting with the top
 * level, the level under it when there are three or fewer top-level people, and the path to [focusId].
 * @param onExpandedChange called with the new set of ids when a team is shown or hidden.
 * @param focusId the person to highlight: the path to them opens, the chart scrolls to them, and their card has an
 * accent border and the selected state.
 * @param dottedLines secondary reporting lines: dashed curves in the chart layout and "Also reports to …" on the card.
 * @param layout the chart, the list, or [MonarchOrgChartLayout.Auto] to choose by width and font scale.
 * @param zoomable shows the zoom buttons (40% to 160%) and allows pinching, in the chart layout.
 * @param enabled `false` greys the chart out; nothing responds.
 * @param labels replacements for the built-in strings.
 * @param onSelect called when a card is tapped, for example to open a detail screen. Without it, cards are not
 * buttons.
 * @param onLoadChildren loads a team whose `childCount` is larger than the reports in [nodes]; the app adds the
 * reports to [nodes]. The team opens when it returns; an exception is announced.
 * @param onMoveRequest turns on moving and shows the move hint. Receives the person and where they were dropped.
 * @param zoomInIcon the icon of the zoom-in button, for example a plus `MonarchIcon` with a `null` description.
 * Without it the button shows its symbol, "+" (see [MonarchOrgChartLabels.zoomInSymbol]). Monarch bundles no
 * icons.
 * @param zoomOutIcon the icon of the zoom-out button. Without it the button shows "−".
 */
@Suppress("LongParameterList", "LongMethod", "CyclomaticComplexMethod")
@ExperimentalMonarchApi
@Composable
public fun MonarchOrgChart(
    nodes: List<MonarchOrgChartNode>,
    expandIcon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    expanded: Set<String>? = null,
    onExpandedChange: ((Set<String>) -> Unit)? = null,
    focusId: String? = null,
    dottedLines: List<MonarchOrgChartDottedLine> = emptyList(),
    layout: MonarchOrgChartLayout = MonarchOrgChartLayout.Auto,
    zoomable: Boolean = true,
    enabled: Boolean = true,
    labels: MonarchOrgChartLabels = MonarchOrgChartLabels(),
    onSelect: ((MonarchOrgChartNode) -> Unit)? = null,
    onLoadChildren: (suspend (MonarchOrgChartNode) -> Unit)? = null,
    onMoveRequest: ((MonarchOrgChartNode, MonarchOrgChartMoveTarget) -> Unit)? = null,
    zoomInIcon: (@Composable () -> Unit)? = null,
    zoomOutIcon: (@Composable () -> Unit)? = null,
) {
    val tree = remember(nodes) { MonarchOrgChartTree(nodes) }
    val strings = monarchOrgChartStrings(labels)
    var inside by rememberSaveable(saver = expandedSaver) { mutableStateOf(tree.initiallyExpanded(focusId)) }
    val open = expanded ?: inside
    var loading by remember { mutableStateOf(emptySet<String>()) }
    var announcement by remember { mutableStateOf("") }
    val latestOpen by rememberUpdatedState(open)
    val latestControlled by rememberUpdatedState(expanded != null)
    val latestOnChange by rememberUpdatedState(onExpandedChange)
    val latestLoad by rememberUpdatedState(onLoadChildren)
    val latestStrings by rememberUpdatedState(strings)
    val setOpen: (Set<String>) -> Unit = { next ->
        if (!latestControlled) inside = next
        latestOnChange?.invoke(next)
    }
    val scope = rememberCoroutineScope()
    val toggle: (MonarchOrgChartNode) -> Unit = toggle@{ node ->
        val load = latestLoad
        if (node.id in latestOpen || load == null || !tree.needsLoad(node)) {
            setOpen(if (node.id in latestOpen) latestOpen - node.id else latestOpen + node.id)
            return@toggle
        }
        if (node.id in loading) return@toggle
        loading = loading + node.id
        scope.launch {
            @Suppress("TooGenericExceptionCaught", "SwallowedException") // A failed load is announced, not thrown.
            try {
                load(node)
                setOpen(latestOpen + node.id)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                announcement = latestStrings.loadFailed(node.name)
            } finally {
                loading = loading - node.id
            }
        }
    }
    LaunchedEffect(focusId, tree) {
        val path = focusId?.let(tree::ancestors).orEmpty()
        if (!latestOpen.containsAll(path)) setOpen(latestOpen + path)
    }
    val positions = remember { MonarchOrgChartDragState() }
    val context = MonarchOrgChartContext(
        tree = tree,
        open = open,
        loading = loading,
        focusId = focusId,
        enabled = enabled,
        strings = strings,
        dotted = tree.dottedManagers(dottedLines),
        dottedLines = dottedLines,
        positions = positions,
        expandIcon = expandIcon,
        zoomInIcon = zoomInIcon,
        zoomOutIcon = zoomOutIcon,
        onToggle = toggle,
        onSelect = onSelect,
        onMove = onMoveRequest,
    )
    BoxWithConstraints(
        modifier = modifier.semantics {
            contentDescription = strings.label
            isTraversalGroup = true
        },
    ) {
        val chart = MonarchOrgChartTokens.showsChart(layout, maxWidth, LocalDensity.current.fontScale)
        Column {
            if (onMoveRequest != null && enabled) {
                MonarchText(
                    text = strings.moveHint,
                    modifier = Modifier.fillMaxWidth(),
                    style = MonarchTextStyle.Caption,
                    color = MonarchTheme.colors.textMuted,
                )
            }
            if (chart) {
                MonarchOrgChartChartView(context, zoomable, Modifier.weight(1f))
            } else {
                MonarchOrgChartListView(context, Modifier.weight(1f))
            }
        }
        // Always present, so a failed load is announced without moving focus.
        Box(
            Modifier
                .size(MonarchOrgChartTokens.LineWidth)
                .semantics {
                    liveRegion = LiveRegionMode.Polite
                    if (announcement.isNotEmpty()) contentDescription = announcement
                },
        )
    }
}

private val expandedSaver = listSaver<MutableState<Set<String>>, String>(
    save = { it.value.toList() },
    restore = { mutableStateOf(it.toSet()) },
)
