package com.rookhq.monarch.components.collaboration.callstage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.R
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.actions.iconbutton.MonarchIconButton
import com.rookhq.monarch.components.datadisplay.badge.MonarchBadge
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.internal.MonarchClock
import com.rookhq.monarch.internal.MonarchShapeDefaults
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.internal.rememberMonarchClock
import com.rookhq.monarch.theme.MonarchTheme

/** What the header of a call stage shows and does. */
internal class CallStageHeaderSpec(
    val title: String?,
    val startedAt: Long?,
    val now: Long?,
    val headCount: String,
    val lowData: Boolean,
    val tools: List<CallStageTool>,
    val icons: MonarchCallStageIcons,
    val labels: MonarchCallStageLabels,
    val onTool: (CallStageTool) -> Unit,
)

/** The header: the title, then the timer, head count and Low data badge, and the buttons at the end. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CallStageHeader(spec: CallStageHeaderSpec) {
    val tokens = MonarchCallStageTokens
    val colors = MonarchTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(tokens.HeaderGap),
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(tokens.HeaderGap)) {
            if (spec.title != null) {
                MonarchText(text = spec.title, style = MonarchTextStyle.BodyStrong, heading = true)
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(tokens.HeaderGap),
                verticalArrangement = Arrangement.spacedBy(tokens.HeaderGap),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                if (spec.startedAt != null) CallStageTimer(spec.startedAt, spec.now, spec.labels)
                Text(
                    text = spec.headCount,
                    // Polite, so a new head count is announced once and not interrupting.
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    style = MonarchTheme.typography.caption,
                    color = colors.textMuted,
                )
                if (spec.lowData) {
                    MonarchBadge(
                        text = monarchString(spec.labels.lowData, R.string.monarch_callstage_low_data),
                        tone = MonarchTone.Warning,
                    )
                }
            }
        }
        spec.tools.forEach { tool -> CallStageToolButton(tool, spec) }
    }
}

/**
 * The running time since [startedAt], ticking every second unless [fixedNow] is given. It is not a live region,
 * so TalkBack reads it when focused and never announces each second.
 */
@Composable
private fun CallStageTimer(startedAt: Long, fixedNow: Long?, labels: MonarchCallStageLabels) {
    val locale = LocalConfiguration.current.locales[0]
    val clock = rememberMonarchClock(fixedNow, MonarchClock.SECOND_MILLIS)
    val time = MonarchCallStageTokens.duration(startedAt, clock, locale)
    val name = monarchString(labels.duration, R.string.monarch_callstage_duration)
    Text(
        text = time,
        modifier = Modifier.clearAndSetSemantics { contentDescription = "$name, $time" },
        style = MonarchTheme.typography.caption,
        color = MonarchTheme.colors.textMuted,
    )
}

/** A header button: an icon button when the app gave an icon, a small text button otherwise. */
@Composable
private fun CallStageToolButton(tool: CallStageTool, spec: CallStageHeaderSpec) {
    val labels = spec.labels
    val (label, icon) = when (tool) {
        CallStageTool.PictureInPicture ->
            monarchString(labels.pictureInPicture, R.string.monarch_callstage_picture_in_picture) to
                spec.icons.pictureInPicture
        CallStageTool.Shrink -> monarchString(labels.shrink, R.string.monarch_callstage_shrink) to spec.icons.shrink
        CallStageTool.Expand -> monarchString(labels.expand, R.string.monarch_callstage_expand) to spec.icons.expand
        CallStageTool.Minimize ->
            monarchString(labels.minimize, R.string.monarch_callstage_minimize) to spec.icons.minimize
    }
    if (icon != null) {
        MonarchIconButton(label = label, onClick = { spec.onTool(tool) }, shape = MonarchShapeDefaults.Control) {
            icon()
        }
    } else {
        MonarchButton(
            text = label,
            onClick = { spec.onTool(tool) },
            variant = MonarchButtonVariant.Ghost,
            size = MonarchButtonSize.Sm,
        )
    }
}
