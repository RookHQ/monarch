package com.rookhq.monarch.components.navigation.tabs

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.accessibility.MonarchTouchTarget
import com.rookhq.monarch.components.actions.button.buttonFocusRing
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.theme.LocalMonarchThemeTokens
import com.rookhq.monarch.theme.MonarchTheme
import com.rookhq.monarch.tokens.MonarchTokens
import kotlinx.coroutines.launch

/**
 * Tabs that switch between views of related content in the same place.
 *
 * The line tabs of the web: every label in the default text colour when selected and in the muted colour
 * otherwise, a 2dp accent underline under the selected tab (the label is not tinted), a 1dp divider under the
 * whole row. A tab is 36dp high, with the touch target completed to 48dp above it, so the underline stays
 * against the divider. When the labels do not fit the row scrolls sideways and keeps the selected tab in view.
 *
 * Each tab is a TalkBack tab ("Tab, selected, 2 of 4") with a focus stop; with a keyboard or D-pad the arrow
 * keys move between tabs and Enter or Space chooses one.
 *
 * @param selectedIndex the index of the selected tab. State is hoisted: set it in [onSelectedIndexChange].
 * @param onSelectedIndexChange called with the index of the tab the user chooses.
 * @param modifier the modifier of the tabs and their panel.
 * @param panel the content of the selected tab, drawn under the row with a 16dp gap above it. `null` when
 * the content is laid out by the caller.
 * @param variant [MonarchTabsVariant.Line] or the raised [MonarchTabsVariant.Pill] on a muted track.
 * @param tabs declares the tabs with [MonarchTabsScope.tab].
 */
@Composable
public fun MonarchTabs(
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    panel: (@Composable (selectedIndex: Int) -> Unit)? = null,
    variant: MonarchTabsVariant = MonarchTabsVariant.Line,
    tabs: MonarchTabsScope.() -> Unit,
) {
    val entries = MonarchTabsScopeImpl().apply(tabs).entries
    val dividerColor = MonarchTheme.colors.borderDefault
    val pill = variant == MonarchTabsVariant.Pill
    Column(modifier) {
        Box(
            Modifier
                .then(if (pill) Modifier else Modifier.fillMaxWidth())
                .then(
                    if (pill) {
                        Modifier
                            .background(MonarchTheme.colors.bgMuted, MonarchTheme.shapes.md)
                            .padding(MonarchTabsTokens.PillTrackPadding)
                    } else {
                        Modifier
                    },
                )
                .drawBehind {
                    if (pill) return@drawBehind
                    val thickness = MonarchTabsTokens.DividerThickness.toPx()
                    drawRect(dividerColor, Offset(0f, size.height - thickness), Size(size.width, thickness))
                },
        ) {
            val slots = remember { TabSlots() }
            val selectable = entries.getOrNull(selectedIndex)?.enabled == true
            val indicator = rememberTabIndicator(slots, selectedIndex, selectable)
            val indicatorColor = MonarchTabsTokens.colors(true, false, true, MonarchTheme.colors).indicator
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .onPlaced { slots.row = it }
                    .drawWithContent {
                        drawContent()
                        if (!pill) drawIndicator(indicator, indicatorColor, MonarchTabsTokens.IndicatorThickness.toPx())
                    }
                    .selectableGroup()
                    .semantics { collectionInfo = CollectionInfo(rowCount = 1, columnCount = entries.size) },
            ) {
                entries.forEachIndexed { index, entry ->
                    Tab(
                        entry = entry,
                        index = index,
                        selected = index == selectedIndex,
                        pill = pill,
                        onClick = { onSelectedIndexChange(index) },
                        onLabelPlaced = { coordinates -> slots.place(index, coordinates) },
                    )
                }
            }
        }
        if (panel != null) {
            Box(Modifier.fillMaxWidth().padding(top = MonarchTabsTokens.PanelTopPadding)) { panel(selectedIndex) }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Tab(
    entry: MonarchTabEntry,
    index: Int,
    selected: Boolean,
    pill: Boolean,
    onClick: () -> Unit,
    onLabelPlaced: (LayoutCoordinates) -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val focused by source.collectIsFocusedAsState()
    val colors = MonarchTabsTokens.colors(selected, hovered, entry.enabled, MonarchTheme.colors)
    val requester = remember { BringIntoViewRequester() }
    LaunchedEffect(selected) { if (selected) requester.bringIntoView() }
    val ringColor = MonarchTheme.colors.focusRing
    val ringWidth = LocalMonarchThemeTokens.current.focusRingWidth.dp
    val description = entry.contentDescription
    Box(
        modifier = Modifier
            .bringIntoViewRequester(requester)
            .selectable(
                selected = selected,
                enabled = entry.enabled,
                role = Role.Tab,
                interactionSource = source,
                indication = null,
                onClick = onClick,
            )
            .semantics {
                collectionItemInfo = CollectionItemInfo(0, 1, index, 1)
                if (description != null) contentDescription = description
            }
            .widthIn(min = MonarchTouchTarget.Minimum)
            .then(
                if (pill && selected) {
                    Modifier.background(MonarchTheme.colors.bgSurface, MonarchTheme.shapes.sm)
                } else {
                    Modifier
                },
            )
            .padding(horizontal = MonarchTabsTokens.SidePadding)
            .padding(top = if (pill) MonarchTokens.Space.space0.dp else MonarchTabsTokens.ReachAbove),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = MonarchTabsTokens.Height)
                .then(
                    if (focused && entry.enabled) {
                        Modifier.buttonFocusRing(MonarchTheme.shapes.sm, ringColor, ringWidth)
                    } else {
                        Modifier
                    },
                )
                // The indicator is drawn by the row, under the label of the selected tab, and slides between tabs.
                .onGloballyPositioned(onLabelPlaced),
            horizontalArrangement = Arrangement.spacedBy(MonarchTabsTokens.ContentGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val labelColor = MonarchTabsTokens.labelColor(entry.accent, colors, entry.enabled, MonarchTheme.colors)
            CompositionLocalProvider(LocalContentColor provides labelColor) {
                entry.icon?.invoke()
                if (entry.secondary == null) {
                    Text(
                        text = entry.text,
                        style = MonarchTheme.typography.label,
                        color = labelColor,
                        softWrap = false,
                        overflow = TextOverflow.Clip,
                    )
                } else {
                    // A rich face: the label over a muted second line, both centred on the tab.
                    Column(
                        modifier = Modifier.padding(vertical = MonarchTabsTokens.RichVerticalPadding),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = entry.text,
                            style = MonarchTheme.typography.label,
                            color = labelColor,
                            softWrap = false,
                            overflow = TextOverflow.Clip,
                        )
                        Text(
                            text = entry.secondary,
                            style = MonarchTheme.typography.caption,
                            color = MonarchTabsTokens.secondaryColor(entry.accent, entry.enabled, MonarchTheme.colors),
                            softWrap = false,
                            overflow = TextOverflow.Clip,
                        )
                    }
                }
                entry.count?.let { Count(it, entry.countTone, selected) }
            }
        }
    }
}

@Composable
private fun Count(text: String, tone: MonarchTone, selected: Boolean) {
    val colors = MonarchTabsTokens.countColors(tone, selected, MonarchTheme.colors)
    Box(
        modifier = Modifier
            .widthIn(min = MonarchTabsTokens.CountMinWidth)
            .heightIn(min = MonarchTabsTokens.CountHeight)
            .background(colors.container, MonarchTheme.shapes.full)
            .padding(horizontal = MonarchTabsTokens.ContentGap),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MonarchTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold),
            color = colors.content,
            softWrap = false,
        )
    }
}

/** Where each tab's label sits in the scrolling row, so the row can draw the indicator under the selected one. */
private class TabSlots {
    var row: LayoutCoordinates? = null
    val bounds = mutableStateMapOf<Int, Pair<Float, Float>>()

    fun place(index: Int, label: LayoutCoordinates) {
        val parent = row ?: return
        if (!parent.isAttached || !label.isAttached) return
        val left = parent.localPositionOf(label, Offset.Zero).x
        val slot = left to label.size.width.toFloat()
        if (bounds[index] != slot) bounds[index] = slot
    }
}

/** The left edge and width of the indicator, in px, and whether it shows (not before the first layout). */
private class TabIndicator(
    val left: Animatable<Float, AnimationVector1D>,
    val width: Animatable<Float, AnimationVector1D>,
    val visible: Boolean,
)

/**
 * The indicator follows the selected tab with a spring that does not bounce (PREMIUM rule 13: the indicator moves,
 * the layout does not). With "Remove animations" on, it jumps.
 */
@Composable
private fun rememberTabIndicator(slots: TabSlots, selectedIndex: Int, selectable: Boolean): TabIndicator {
    val left = remember { Animatable(0f) }
    val width = remember { Animatable(0f) }
    val target = slots.bounds[selectedIndex]
    val reduced = MonarchTheme.motion.reducedMotion
    var placed by remember { mutableStateOf(false) }
    LaunchedEffect(target) {
        val (x, w) = target ?: return@LaunchedEffect
        if (!placed || reduced) {
            left.snapTo(x)
            width.snapTo(w)
            placed = true
        } else {
            val spec = spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
            launch { left.animateTo(x, spec) }
            launch { width.animateTo(w, spec) }
        }
    }
    return TabIndicator(left, width, placed && target != null && selectable)
}

/** The web's `border-radius: 2px 2px 0 0`: round on top, flat on the divider. */
private fun DrawScope.drawIndicator(indicator: TabIndicator, color: Color, thickness: Float) {
    if (!indicator.visible || indicator.width.value <= 0f) return
    val bar = Path().apply {
        addRoundRect(
            RoundRect(
                rect = Rect(
                    Offset(indicator.left.value, size.height - thickness),
                    Size(indicator.width.value, thickness),
                ),
                topLeft = CornerRadius(thickness),
                topRight = CornerRadius(thickness),
            ),
        )
    }
    drawPath(bar, color)
}
