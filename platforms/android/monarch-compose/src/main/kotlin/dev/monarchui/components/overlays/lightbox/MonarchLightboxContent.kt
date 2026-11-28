package dev.monarchui.components.overlays.lightbox

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.actions.iconbutton.MonarchIconButton
import dev.monarchui.internal.MonarchDefaultIcons
import dev.monarchui.theme.MonarchTheme
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * The viewer without its dialog window: the toolbar, the pager of items,
 * previous and next, and the caption. `MonarchLightbox` shows it full screen.
 */
@Composable
internal fun MonarchLightboxContent(
    items: List<MonarchLightboxItem>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    looping: Boolean,
    maximumZoom: Float,
    onShare: ((Int) -> Unit)?,
    labels: MonarchLightboxLabels,
) {
    val resources = LocalResources.current
    val resolved = remember(labels, resources) { ResolvedLightboxLabels(labels, resources) }
    val count = items.size
    val index = MonarchLightboxTokens.clampIndex(selectedIndex, count)
    val motion = MonarchTheme.motion
    val scope = rememberCoroutineScope()
    val pager = rememberPagerState(initialPage = index) { count }
    val zoom = remember(maximumZoom) { LightboxZoomState(MonarchLightboxTokens.zoomLimit(maximumZoom)) }
    val currentIndex by rememberUpdatedState(index)
    val onChange by rememberUpdatedState(onSelectedIndexChange)

    // The app moved the selection: show that item.
    LaunchedEffect(index, count) {
        if (count > 0 && pager.currentPage != index) {
            if (abs(pager.currentPage - index) > 1) {
                pager.scrollToPage(index)
            } else {
                pager.animateScrollToPage(index, animationSpec = motion.spec())
            }
        }
    }
    // The person swiped to another item: report it, and start that item fitted.
    LaunchedEffect(pager) {
        snapshotFlow { pager.settledPage }.collect { page ->
            zoom.reset()
            if (page != currentIndex) onChange(page)
        }
    }

    fun go(delta: Int) {
        MonarchLightboxTokens.step(index, delta, count, looping)?.let(onSelectedIndexChange)
    }
    fun zoomTo(target: Float) {
        scope.launch { zoom.animateTo(target, motion.spec()) }
    }
    val zoomable = items.getOrNull(index)?.let { it.image != null && it.video == null } == true && zoom.zoomable
    val layoutDirection = LocalLayoutDirection.current

    Surface(
        modifier = modifier
            .fillMaxSize()
            .semantics { paneTitle = resolved.title }
            .onPreviewKeyEvent { event ->
                handleKey(event, layoutDirection, zoomable, zoom.zoom, ::go, ::zoomTo, onDismissRequest)
            },
        color = MonarchTheme.colors.bgCanvas,
        contentColor = MonarchTheme.colors.textDefault,
    ) {
        Column(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(MonarchLightboxTokens.Padding),
            verticalArrangement = Arrangement.spacedBy(MonarchLightboxTokens.Gap),
        ) {
            Toolbar(
                count = count,
                index = index,
                item = items.getOrNull(index),
                zoomable = zoomable,
                zoom = zoom,
                labels = resolved,
                onZoom = ::zoomTo,
                onShare = onShare?.let { share -> { share(index) } },
                onClose = onDismissRequest,
            )
            Pager(items, pager, zoom, resolved, Modifier.weight(1f).fillMaxWidth())
            if (count > 1) {
                StepButtons(
                    labels = resolved,
                    previousEnabled = MonarchLightboxTokens.step(index, -1, count, looping) != null,
                    nextEnabled = MonarchLightboxTokens.step(index, 1, count, looping) != null,
                    onStep = ::go,
                )
            }
            items.getOrNull(index)?.caption?.let { Caption(it) }
        }
    }
}

@Composable
private fun Pager(
    items: List<MonarchLightboxItem>,
    pager: PagerState,
    zoom: LightboxZoomState,
    labels: ResolvedLightboxLabels,
    modifier: Modifier,
) {
    Box(modifier = modifier) {
        if (items.isEmpty()) {
            LightboxStage(item = null, zoom = null, labels = labels)
        } else {
            HorizontalPager(
                state = pager,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = zoom.zoom <= 1f,
                beyondViewportPageCount = 0,
            ) { page ->
                LightboxStage(
                    item = items[page],
                    zoom = if (page == pager.settledPage) zoom else null,
                    labels = labels,
                )
            }
        }
    }
}

@Composable
private fun StepButtons(
    labels: ResolvedLightboxLabels,
    previousEnabled: Boolean,
    nextEnabled: Boolean,
    onStep: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MonarchLightboxTokens.Gap, Alignment.CenterHorizontally),
    ) {
        ToolIconButton(name = labels.previous, icon = MonarchDefaultIcons.ChevronLeft, enabled = previousEnabled) {
            onStep(-1)
        }
        ToolIconButton(name = labels.next, icon = MonarchDefaultIcons.ChevronRight, enabled = nextEnabled) {
            onStep(1)
        }
    }
}

@Composable
private fun Caption(caption: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = MonarchLightboxTokens.CaptionMaxHeight)
            .verticalScroll(rememberScrollState()),
    ) {
        Text(text = caption, style = MonarchTheme.typography.body)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Toolbar(
    count: Int,
    index: Int,
    item: MonarchLightboxItem?,
    zoomable: Boolean,
    zoom: LightboxZoomState,
    labels: ResolvedLightboxLabels,
    onZoom: (Float) -> Unit,
    onShare: (() -> Unit)?,
    onClose: () -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MonarchLightboxTokens.Gap),
        verticalArrangement = Arrangement.spacedBy(MonarchLightboxTokens.Gap),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        // Always present, so that the polite live region announces each new position and item.
        val position = if (count > 1) labels.position(index + 1, count) else ""
        Text(
            text = position,
            modifier = Modifier
                .weight(1f)
                .semantics {
                    liveRegion = LiveRegionMode.Polite
                    if (count > 1 && item != null) contentDescription = "$position: ${item.alt}"
                },
            style = MonarchTheme.typography.label,
        )
        if (zoomable) {
            ToolIconButton(name = labels.zoomOut, icon = MonarchDefaultIcons.Dash, enabled = zoom.zoom > 1f) {
                onZoom(zoom.zoom - MonarchLightboxTokens.ZOOM_STEP)
            }
            ToolButton(text = zoomPercent(zoom.zoom), name = labels.resetZoom, enabled = zoom.zoom > 1f) { onZoom(1f) }
            ToolIconButton(name = labels.zoomIn, icon = MonarchDefaultIcons.Plus, enabled = zoom.zoom < zoom.limit) {
                onZoom(zoom.zoom + MonarchLightboxTokens.ZOOM_STEP)
            }
        }
        if (onShare != null) ToolButton(text = labels.share, name = null, onClick = onShare)
        ToolIconButton(name = labels.close, icon = MonarchDefaultIcons.Close, onClick = onClose)
    }
}

@Composable
private fun ToolButton(text: String, name: String?, enabled: Boolean = true, onClick: () -> Unit) {
    MonarchButton(
        text = text,
        onClick = onClick,
        modifier = if (name != null) Modifier.semantics { contentDescription = name } else Modifier,
        variant = MonarchButtonVariant.Ghost,
        size = MonarchButtonSize.Sm,
        enabled = enabled,
    )
}

@Composable
private fun ToolIconButton(
    name: String,
    icon: @Composable () -> Unit,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    MonarchIconButton(label = name, onClick = onClick, size = MonarchButtonSize.Sm, enabled = enabled, icon = icon)
}

/** Arrow keys step (mirrored right to left, and not while zoomed), +, - and 0 zoom, Escape closes. */
@Suppress("LongParameterList")
internal fun handleKey(
    event: KeyEvent,
    layoutDirection: LayoutDirection,
    zoomable: Boolean,
    zoom: Float,
    go: (Int) -> Unit,
    zoomTo: (Float) -> Unit,
    close: () -> Unit,
): Boolean {
    if (event.type != KeyEventType.KeyDown) return false
    if (event.key == Key.Escape) {
        close()
        return true
    }
    val step = stepFor(event.key, layoutDirection)
    val target = zoomFor(event.key, zoom)
    return when {
        step != null && zoom <= 1f -> true.also { go(step) }
        target != null && zoomable -> true.also { zoomTo(target) }
        else -> false
    }
}

private fun stepFor(key: Key, layoutDirection: LayoutDirection): Int? {
    val forward = if (layoutDirection == LayoutDirection.Ltr) 1 else -1
    return when (key) {
        Key.DirectionRight -> forward
        Key.DirectionLeft -> -forward
        else -> null
    }
}

private fun zoomFor(key: Key, zoom: Float): Float? = when (key) {
    Key.Plus, Key.Equals, Key.NumPadAdd -> zoom + MonarchLightboxTokens.ZOOM_STEP
    Key.Minus, Key.NumPadSubtract -> zoom - MonarchLightboxTokens.ZOOM_STEP
    Key.Zero, Key.NumPad0 -> 1f
    else -> null
}
