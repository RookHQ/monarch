package dev.monarchui.components.geo.worldmap

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.unit.Constraints
import dev.monarchui.theme.MonarchColors
import dev.monarchui.theme.MonarchTheme
import kotlin.math.roundToInt

/**
 * The drawn map of MonarchWorldMap: every country's outline, filled by
 * level, the outline of the inspected country, then the dots or bubbles.
 *
 * It is a `Canvas`, which Compose does not mirror, and the TalkBack elements
 * are placed with absolute positions, so the map, its taps and its elements
 * keep their geography in right-to-left layouts. One invisible element per
 * country with a value sits on its centre; TalkBack reads them largest value
 * first. The plot itself is a traversal group named by [summary].
 */
@Composable
internal fun WorldMapPlot(
    model: WorldMapPlotModel,
    words: WorldMapWords,
    summary: String,
    selectLabel: String,
    onTap: (String?) -> Unit,
    onSelect: ((String) -> Unit)?,
) {
    val colors = MonarchTheme.colors
    val density = LocalDensity.current
    val tap by rememberUpdatedState(onTap)
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(MonarchWorldMapTokens.ASPECT_RATIO)
            // Russia and Fiji run past the 180th meridian; keep them inside the map.
            .clipToBounds()
            .semantics {
                isTraversalGroup = true
                contentDescription = summary
                collectionInfo = CollectionInfo(rowCount = model.placed.size, columnCount = 1)
            },
    ) {
        val ratio = constraints.maxWidth / WorldMapGeometry.WIDTH
        val paths = remember(ratio) { WorldMapGeometry.shapes.mapValues { WorldMapGeometry.path(it.value, ratio) } }
        val tolerance = with(density) { MonarchWorldMapTokens.TapRadius.toPx() } / ratio
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(model.placed, model.variant, model.maximum, ratio) {
                    detectTapGestures { offset ->
                        if (ratio > 0f) {
                            val point = Offset(offset.x / ratio, offset.y / ratio)
                            tap(WorldMapData.hit(point, model.placed, model.variant, model.maximum, tolerance))
                        }
                    }
                },
        ) {
            drawCountries(model, paths, colors)
            drawMarks(model, ratio, colors)
        }
        WorldMapCountryElements(model, ratio, words, selectLabel, onSelect, Modifier.matchParentSize())
    }
}

/** Fills every outline by level, then outlines the inspected country of a choropleth. */
private fun DrawScope.drawCountries(model: WorldMapPlotModel, paths: Map<String, Path>, colors: MonarchColors) {
    val border = Stroke(MonarchWorldMapTokens.CountryBorder.toPx(), join = StrokeJoin.Round)
    paths.forEach { (code, path) ->
        val level = WorldMapData.level(model.byCode[code], model.scale, model.variant)
        drawPath(path, MonarchWorldMapTokens.fill(level, model.slot, colors))
        drawPath(path, MonarchWorldMapTokens.border(colors), style = border)
    }
    val active = model.active?.let(paths::get)
    if (model.variant == MonarchWorldMapVariant.Choropleth && active != null) {
        drawPath(
            active,
            MonarchWorldMapTokens.activeBorder(colors),
            style = Stroke(MonarchWorldMapTokens.ActiveBorder.toPx(), join = StrokeJoin.Round),
        )
    }
}

/** Draws the dots of small countries, or the bubbles, largest first so smaller ones sit on top. */
private fun DrawScope.drawMarks(model: WorldMapPlotModel, ratio: Float, colors: MonarchColors) {
    val bubble = model.variant == MonarchWorldMapVariant.Bubble
    WorldMapData.marks(model.placed, model.variant).forEach { datum ->
        val centre = WorldMapGeometry.centroids[datum.country] ?: return@forEach
        val radius = WorldMapData.markRadius(datum.value, model.maximum, model.variant) * ratio
        val active = datum.country == model.active
        val at = Offset(centre.x * ratio, centre.y * ratio)
        val fill = if (bubble) {
            MonarchWorldMapTokens.bubbleFill(active, model.slot, colors)
        } else {
            MonarchWorldMapTokens.fill(model.scale.level(datum.value), model.slot, colors)
        }
        val outline = when {
            active -> MonarchWorldMapTokens.activeBorder(colors)
            bubble -> MonarchWorldMapTokens.bubbleBorder(model.slot, colors)
            else -> MonarchWorldMapTokens.border(colors)
        }
        val width = if (active) MonarchWorldMapTokens.ActiveBorder else MonarchWorldMapTokens.MarkBorder
        drawCircle(fill, radius, at)
        drawCircle(outline, radius, at, style = Stroke(width.toPx()))
    }
}

/**
 * One invisible TalkBack element per country with a value, on the country's
 * centre, read as "Germany: 300, Users 90" in value order, largest first.
 */
@Composable
private fun WorldMapCountryElements(
    model: WorldMapPlotModel,
    ratio: Float,
    words: WorldMapWords,
    selectLabel: String,
    onSelect: ((String) -> Unit)?,
    modifier: Modifier,
) {
    val side = with(LocalDensity.current) { MonarchWorldMapTokens.CountryNode.roundToPx() }
    Layout(
        modifier = modifier,
        content = {
            model.placed.forEachIndexed { index, datum ->
                val spoken = WorldMapText.describe(datum, words)
                Box(
                    Modifier
                        .size(MonarchWorldMapTokens.CountryNode)
                        .semantics {
                            contentDescription = spoken
                            traversalIndex = index.toFloat()
                            collectionItemInfo = CollectionItemInfo(index, 1, 0, 1)
                            selected = datum.country == model.active
                            if (onSelect != null) {
                                customActions = listOf(
                                    CustomAccessibilityAction(selectLabel) {
                                        onSelect(datum.country)
                                        true
                                    },
                                )
                            }
                        },
                )
            }
        },
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(Constraints.fixed(side, side)) }
        layout(constraints.maxWidth, constraints.maxHeight) {
            placeables.forEachIndexed { index, placeable ->
                val anchor = WorldMapGeometry.anchor(model.placed[index].country) ?: Offset.Zero
                val x = (anchor.x * ratio - side / 2f).roundToInt().coerceIn(0, maxOf(0, constraints.maxWidth - side))
                val y = (anchor.y * ratio - side / 2f).roundToInt().coerceIn(0, maxOf(0, constraints.maxHeight - side))
                // place, not placeRelative: the map keeps its geography right to left.
                placeable.place(x, y)
            }
        }
    }
}
