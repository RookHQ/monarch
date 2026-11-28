package dev.monarchui.components.intelligence.airelatedentities

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import dev.monarchui.R
import dev.monarchui.accessibility.MonarchTouchTarget
import dev.monarchui.accessibility.monarchFocusRing
import dev.monarchui.components.feedback.emptystate.MonarchEmptyState
import dev.monarchui.components.feedback.emptystate.MonarchEmptyStateSize
import dev.monarchui.components.identity.avatar.MonarchAvatar
import dev.monarchui.components.identity.avatar.MonarchAvatarShape
import dev.monarchui.internal.MonarchShapeDefaults
import dev.monarchui.internal.monarchString
import dev.monarchui.theme.MonarchTheme
import kotlin.math.roundToInt

/**
 * People, organisations, projects and records the assistant found related to
 * the current content, ranked by relevance with their relations, as a list or
 * a radial graph.
 *
 * Built by Monarch: Compose has no graph. The list is a column of bordered
 * rows (with a decorative `MonarchAvatar` when the entity has a picture) with the name, the kind as muted
 * text, the relations and a relevance bar (a Material 3 `LinearProgressIndicator`). The graph places the
 * avatars with a `Layout`:
 * the centre entity in the middle and the others on a ring from the top,
 * clockwise, with relation lines drawn behind them. The graph is drawn the
 * same way in both layout directions, because a radial diagram has no
 * reading direction. It falls back to the list when there are more than
 * [maxGraphNodes] entities or the font scale is above 1.3, where its one-line
 * names would be cut.
 *
 * In both forms each entity is one TalkBack element read as "Jordan Lee.
 * Person. Relevance 90%. Related to: works at Acme"; with [onSelect] it is a
 * button with a 48dp target. The list or graph is a group named "Related
 * entities".
 *
 * @param entities the entities. The list is sorted by weight, highest first.
 * @param modifier the modifier of the list or graph.
 * @param variant a ranked list, or a radial graph.
 * @param relations links between entities by id, shown under each entity and as lines in the graph.
 * @param centerId the entity in the middle of the graph. The one with the highest weight when `null` or unknown.
 * @param maxGraphNodes above this many entities the graph variant shows the list.
 * @param onSelect makes every entity a button that calls this with the entity.
 * @param labels replacements for the built-in strings.
 */
@Composable
public fun MonarchAiRelatedEntities(
    entities: List<MonarchAiRelatedEntity>,
    modifier: Modifier = Modifier,
    variant: MonarchAiRelatedEntitiesVariant = MonarchAiRelatedEntitiesVariant.List,
    relations: List<MonarchAiEntityRelation> = emptyList(),
    centerId: String? = null,
    maxGraphNodes: Int = DefaultMaxGraphNodes,
    onSelect: ((MonarchAiRelatedEntity) -> Unit)? = null,
    labels: MonarchAiRelatedEntitiesLabels = MonarchAiRelatedEntitiesLabels(),
) {
    val tokens = MonarchAiRelatedEntitiesTokens
    val groupLabel = monarchString(labels.label, R.string.monarch_airelatedentities_label)
    val groupModifier = modifier.semantics {
        contentDescription = groupLabel
        isTraversalGroup = true
    }
    if (entities.isEmpty()) {
        MonarchEmptyState(
            title = monarchString(labels.empty, R.string.monarch_airelatedentities_empty),
            modifier = modifier,
            size = MonarchEmptyStateSize.Sm,
        )
        return
    }
    val sorted = MonarchAiRelatedEntitiesLogic.sorted(entities)
    val names = entities.associate { it.id to fullName(it, entities, relations, labels) }
    val fontScale = LocalDensity.current.fontScale
    if (MonarchAiRelatedEntitiesLogic.showsGraph(variant, entities.size, maxGraphNodes, fontScale)) {
        val center = MonarchAiRelatedEntitiesLogic.center(sorted, centerId) ?: return
        Graph(center, sorted.filter { it !== center }, relations, names, onSelect, groupModifier)
    } else {
        Column(modifier = groupModifier, verticalArrangement = Arrangement.spacedBy(tokens.ListGap)) {
            sorted.forEach { entity ->
                EntityRow(entity, names.getValue(entity.id), entities, relations, onSelect, labels)
            }
        }
    }
}

/** The default of `maxGraphNodes`, as on the web. */
private const val DefaultMaxGraphNodes = 12

@OptIn(ExperimentalLayoutApi::class)
@Suppress("LongParameterList")
@Composable
private fun EntityRow(
    entity: MonarchAiRelatedEntity,
    name: String,
    entities: List<MonarchAiRelatedEntity>,
    relations: List<MonarchAiEntityRelation>,
    onSelect: ((MonarchAiRelatedEntity) -> Unit)?,
    labels: MonarchAiRelatedEntitiesLabels,
) {
    val tokens = MonarchAiRelatedEntitiesTokens
    val colors = MonarchTheme.colors
    val shape = MonarchShapeDefaults.Container
    val weight = MonarchAiRelatedEntitiesLogic.weight(entity.weight)
    val relationLine = relationLine(entity, entities, relations, labels)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = MonarchTouchTarget.Minimum)
            .border(MonarchShapeDefaults.BorderWidth, tokens.rowBorder(colors), shape)
            .entityInteraction(entity, name, onSelect, shape)
            .padding(tokens.RowPadding),
        horizontalArrangement = Arrangement.spacedBy(tokens.RowGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The web's list draws no avatar; one shows only when the app gives the entity a picture.
        if (entity.image != null) {
            MonarchAvatar(image = entity.image, name = entity.label, seed = entity.id, decorative = true)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(tokens.TextGap)) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(tokens.ListGap),
                verticalArrangement = Arrangement.spacedBy(tokens.TextGap),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = entity.label, style = MonarchTheme.typography.bodyStrong, color = colors.textDefault)
                Text(
                    text = kindName(entity.kind, labels),
                    style = MonarchTheme.typography.caption,
                    color = colors.textMuted,
                )
            }
            if (relationLine != null) {
                Text(text = relationLine, style = MonarchTheme.typography.caption, color = colors.textMuted)
            }
            if (weight != null) {
                LinearProgressIndicator(
                    progress = { weight.toFloat() },
                    modifier = Modifier.fillMaxWidth().height(tokens.WeightBarHeight).clearAndSetSemantics { },
                    color = tokens.weightFill(colors),
                    trackColor = tokens.weightTrack(colors),
                    strokeCap = StrokeCap.Round,
                    gapSize = MonarchTheme.spacing.space0,
                    drawStopIndicator = {},
                )
            }
        }
    }
}

/** A button with the entity's full name when [onSelect] is given, else one element read with that name. */
private fun Modifier.entityInteraction(
    entity: MonarchAiRelatedEntity,
    name: String,
    onSelect: ((MonarchAiRelatedEntity) -> Unit)?,
    shape: Shape,
): Modifier = if (onSelect != null) {
    monarchFocusRing(shape)
        .clickable(role = Role.Button, interactionSource = null, indication = ripple()) { onSelect(entity) }
        .semantics { contentDescription = name }
} else {
    semantics(mergeDescendants = true) { contentDescription = name }
}

@Suppress("LongParameterList")
@Composable
private fun Graph(
    center: MonarchAiRelatedEntity,
    ring: List<MonarchAiRelatedEntity>,
    relations: List<MonarchAiEntityRelation>,
    names: Map<String, String>,
    onSelect: ((MonarchAiRelatedEntity) -> Unit)?,
    modifier: Modifier,
) {
    val tokens = MonarchAiRelatedEntitiesTokens
    val edgeColor = tokens.edge(MonarchTheme.colors)
    val points = remember { HashMap<String, Offset>() }
    val nodes = listOf(center) + ring
    Layout(
        content = {
            nodes.forEach { entity ->
                GraphNode(entity, entity === center, names.getValue(entity.id), onSelect)
            }
        },
        modifier = modifier.drawBehind {
            val width = tokens.EdgeWidth.toPx()
            relations.forEach { relation ->
                val from = points[relation.from]
                val to = points[relation.to]
                if (from != null && to != null) drawLine(edgeColor, from, to, strokeWidth = width)
            }
        },
    ) { measurables, constraints ->
        val side = minOf(constraints.maxWidth, tokens.GraphMaxSize.roundToPx())
        val nodeWidth = side / tokens.NODES_ACROSS
        val middle = side / 2f
        val radius = middle - nodeWidth / 2f
        val placeables = measurables.map { it.measure(Constraints(maxWidth = maxOf(nodeWidth, 0))) }
        val origins = nodes.mapIndexed { index, entity ->
            val avatar = tokens.avatarDiameter(if (index == 0) tokens.CenterAvatar else tokens.RingAvatar)
            val avatarCenter = if (index == 0) {
                Offset(middle, middle)
            } else {
                Offset(middle, middle) + MonarchAiRelatedEntitiesLogic.ringPoint(index - 1, ring.size, radius)
            }
            points[entity.id] = avatarCenter
            val half = (avatar * fontScale).toPx() / 2f
            val placeable = placeables[index]
            val x = (avatarCenter.x - placeable.width / 2f).roundToInt()
            val y = (avatarCenter.y - maxOf(half, MonarchTouchTarget.Minimum.toPx() / 2f)).roundToInt()
            x to y
        }
        val height = maxOf(side, placeables.indices.maxOf { origins[it].second + placeables[it].height })
        layout(side, height) {
            // Absolute placement: the diagram is not mirrored in right to left.
            placeables.forEachIndexed { index, placeable ->
                placeable.place(origins[index].first, origins[index].second)
            }
        }
    }
}

@Composable
private fun GraphNode(
    entity: MonarchAiRelatedEntity,
    isCenter: Boolean,
    name: String,
    onSelect: ((MonarchAiRelatedEntity) -> Unit)?,
) {
    val tokens = MonarchAiRelatedEntitiesTokens
    val shape = MonarchShapeDefaults.Row
    val interaction = if (onSelect != null) {
        Modifier
            .monarchFocusRing(shape)
            .clickable(role = Role.Button, interactionSource = null, indication = ripple()) { onSelect(entity) }
            .semantics { contentDescription = name }
    } else {
        Modifier.semantics(mergeDescendants = true) {
            contentDescription = name
            role = Role.Image
        }
    }
    Column(
        modifier = Modifier
            .widthIn(min = MonarchTouchTarget.Minimum)
            .heightIn(min = MonarchTouchTarget.Minimum)
            .then(interaction),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(tokens.NodeGap),
    ) {
        Box(Modifier.heightIn(min = MonarchTouchTarget.Minimum), contentAlignment = Alignment.Center) {
            MonarchAvatar(
                image = entity.image,
                name = entity.label,
                shape = MonarchAvatarShape.Circle,
                size = if (isCenter) tokens.CenterAvatar else tokens.RingAvatar,
                seed = entity.id,
                decorative = true,
            )
        }
        Text(
            text = entity.label,
            style = MonarchTheme.typography.caption,
            color = MonarchTheme.colors.textDefault,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun kindName(kind: MonarchAiRelatedEntityKind, labels: MonarchAiRelatedEntitiesLabels): String =
    labels.kind?.invoke(kind) ?: MonarchAiRelatedEntitiesTokens.kindRes(kind)?.let { stringResource(it) } ?: kind.value

@Composable
private fun relationLine(
    entity: MonarchAiRelatedEntity,
    entities: List<MonarchAiRelatedEntity>,
    relations: List<MonarchAiEntityRelation>,
    labels: MonarchAiRelatedEntitiesLabels,
): String? {
    val parts = MonarchAiRelatedEntitiesLogic.relationsOf(entity.id, entities, relations).map { (other, label) ->
        labels.relation?.invoke(other, label) ?: if (label.isNullOrBlank()) {
            other
        } else {
            stringResource(R.string.monarch_airelatedentities_relation, label, other)
        }
    }
    if (parts.isEmpty()) return null
    val intro = monarchString(labels.relatedTo, R.string.monarch_airelatedentities_related_to)
    return stringResource(R.string.monarch_airelatedentities_related_line, intro, parts.joinToString(", "))
}

/** "Jordan Lee. Person. Relevance 90%. Related to: works at Acme". */
@Composable
private fun fullName(
    entity: MonarchAiRelatedEntity,
    entities: List<MonarchAiRelatedEntity>,
    relations: List<MonarchAiEntityRelation>,
    labels: MonarchAiRelatedEntitiesLabels,
): String {
    val weight = MonarchAiRelatedEntitiesLogic.weight(entity.weight)?.let {
        labels.weight?.invoke(it)
            ?: stringResource(R.string.monarch_airelatedentities_weight, MonarchAiRelatedEntitiesLogic.percent(it))
    }
    return listOfNotNull(
        entity.label,
        kindName(entity.kind, labels),
        weight,
        relationLine(entity, entities, relations, labels),
    ).joinToString(". ")
}
