package dev.monarchui.components.workflow.roadmap

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.monarchui.components.datadisplay.statusbadge.MonarchStatusBadge
import dev.monarchui.components.datadisplay.statusbadge.MonarchStatusBadgeSize
import dev.monarchui.components.datadisplay.statusbadge.MonarchStatusDefinition
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.internal.MonarchTonePalette
import dev.monarchui.theme.MonarchTheme
import dev.monarchui.tokens.MonarchTokens

/** The tone of the health chip, or `null` for a health that shows no chip (not reported). */
internal fun roadmapHealthTone(health: MonarchRoadmapHealth?): MonarchTone? = when (health) {
    MonarchRoadmapHealth.OnTrack -> MonarchTone.Success
    MonarchRoadmapHealth.AtRisk -> MonarchTone.Warning
    MonarchRoadmapHealth.OffTrack -> MonarchTone.Danger
    MonarchRoadmapHealth.NotReported, null -> null
}

/**
 * The health of a project or group as a soft chip with a dot, as the web writes it. Nothing is drawn for a health
 * that was not reported. [description] replaces what TalkBack reads (a group adds its project count).
 */
@Composable
internal fun MonarchRoadmapHealthChip(health: MonarchRoadmapHealth?, label: String, description: String? = null) {
    val tone = roadmapHealthTone(health) ?: return
    val dot = MonarchTonePalette.of(tone, MonarchTheme.colors).solid
    MonarchStatusBadge(
        definition = MonarchStatusDefinition(
            label = label,
            tone = tone,
            icon = { Canvas(Modifier.size(MonarchTokens.Space.space2.dp)) { drawCircle(dot) } },
        ),
        modifier = if (description == null) Modifier else Modifier.semantics { contentDescription = description },
        size = MonarchStatusBadgeSize.Sm,
    )
}
