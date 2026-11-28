package dev.monarchui.components.intelligence.aihandoff

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.R
import dev.monarchui.components.identity.avatar.MonarchAvatar
import dev.monarchui.components.identity.avatar.MonarchAvatarSize
import dev.monarchui.components.intelligence.aiagentavatar.MonarchAiAgentAvatar
import dev.monarchui.components.intelligence.aiconversation.MonarchAiAgentRef
import dev.monarchui.internal.MonarchDefaultIcons
import dev.monarchui.internal.monarchString
import dev.monarchui.theme.MonarchTheme

/**
 * A marker in a thread where one agent hands the work to another agent or to
 * a person: both pictures, a sentence saying who handed off to whom, the
 * reason and the time.
 *
 * Built by Monarch from `MonarchAiAgentAvatar`, `MonarchAvatar` and text in a
 * bordered row; Material 3 has no equivalent. The border is the AI colour when
 * the work goes to an agent and the default border colour when it goes to a
 * person, and the target is a square agent picture or a round person's
 * picture, so the direction never depends on colour. On a window narrower
 * than 640dp the marker fills the width; wider, dashed lines run to each side.
 *
 * The marker is one TalkBack element, not interactive, read as "Handoff", the
 * sentence with "(AI agent)" after each agent's name, the reason and the time.
 *
 * @param from the agent that hands the work over.
 * @param to the agent, or the person, that takes the work.
 * @param modifier the modifier of the marker.
 * @param reason why the work moved, shown under the sentence in muted text. Already localized.
 * @param dateMillis when it happened, in epoch milliseconds (UTC), shown as a relative time such as
 * "3 minutes ago".
 * @param nowMillis the reference time for [dateMillis]. `null` is the current time.
 * @param fromImage the picture of [from], loaded by the app. `null` shows initials.
 * @param arrowIcon an arrow between the pictures, for example a `MonarchIcon` of an `ImageVector` with
 * `autoMirror = true` and a `null` content description. Drawn by default, as on the web; `null` draws none,
 * and the pictures sit side by side. It is given the subtle text colour.
 * @param labels replacements for the built-in strings.
 */
@ExperimentalMonarchApi
@Composable
public fun MonarchAiHandoff(
    from: MonarchAiAgentRef,
    to: MonarchAiHandoffTarget,
    modifier: Modifier = Modifier,
    reason: String? = null,
    dateMillis: Long? = null,
    nowMillis: Long? = null,
    fromImage: Painter? = null,
    arrowIcon: (@Composable () -> Unit)? = MonarchDefaultIcons.ArrowForward,
    labels: MonarchAiHandoffLabels = MonarchAiHandoffLabels(),
) {
    val tokens = MonarchAiHandoffTokens
    val colors = MonarchTheme.colors
    val toPerson = to is MonarchAiHandoffTarget.Person
    val agentKind = monarchString(labels.agentKind, R.string.monarch_aihandoff_agent_kind)
    val sentence = handedOff(labels, from.name, to.name, toPerson)
    val spokenSentence = handedOff(
        labels,
        MonarchAiHandoffLogic.spokenName(from.name, agent = true, agentKind = agentKind),
        MonarchAiHandoffLogic.spokenName(to.name, agent = !toPerson, agentKind = agentKind),
        toPerson,
    )
    val now = nowMillis ?: remember { System.currentTimeMillis() }
    val time = dateMillis?.let { remember(it, now) { MonarchAiHandoffLogic.relativeTime(it, now) } }
    val visibleReason = reason?.takeIf { it.isNotBlank() }
    val spoken = MonarchAiHandoffLogic.spoken(
        label = monarchString(labels.label, R.string.monarch_aihandoff_label),
        sentence = spokenSentence,
        reason = visibleReason,
        reasonPrefix = monarchString(labels.reason, R.string.monarch_aihandoff_reason),
        time = time,
    )
    val border = tokens.borderColor(toPerson, colors)
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = tokens.VerticalMargin)
            .clearAndSetSemantics { contentDescription = spoken },
    ) {
        val width = maxWidth
        val lines = tokens.showsLines(width)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(tokens.Gap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (lines) Line(border)
            Row(
                modifier = Modifier
                    .then(if (lines) Modifier.widthIn(max = tokens.bodyMaxWidth(width)) else Modifier.weight(1f))
                    .background(tokens.container(colors), MonarchTheme.shapes.lg)
                    .border(tokens.LineWidth, border, MonarchTheme.shapes.lg)
                    .padding(horizontal = tokens.HorizontalPadding, vertical = tokens.VerticalPadding),
                horizontalArrangement = Arrangement.spacedBy(tokens.BodyGap),
                verticalAlignment = if (lines) Alignment.CenterVertically else Alignment.Top,
            ) {
                Pictures(from, fromImage, to, arrowIcon)
                Column(verticalArrangement = Arrangement.spacedBy(tokens.TextGap)) {
                    Text(
                        text = MonarchAiHandoffLogic.emphasized(sentence, listOf(from.name, to.name)),
                        style = MonarchTheme.typography.caption,
                        color = tokens.sentenceColor(colors),
                    )
                    if (visibleReason != null) {
                        Text(
                            text = visibleReason,
                            style = MonarchTheme.typography.caption,
                            color = tokens.reasonColor(colors),
                        )
                    }
                    if (time != null) {
                        Text(text = time, style = MonarchTheme.typography.caption, color = tokens.timeColor(colors))
                    }
                }
            }
            if (lines) Line(border)
        }
    }
}

@Composable
private fun handedOff(labels: MonarchAiHandoffLabels, from: String, to: String, toPerson: Boolean): String =
    labels.handedOff?.invoke(from, to, toPerson) ?: stringResource(R.string.monarch_aihandoff_handed_off, from, to)

@Composable
private fun Pictures(
    from: MonarchAiAgentRef,
    fromImage: Painter?,
    to: MonarchAiHandoffTarget,
    arrowIcon: (@Composable () -> Unit)?,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(MonarchAiHandoffTokens.PictureGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MonarchAiAgentAvatar(agent = from, image = fromImage, size = MonarchAvatarSize.Sm, decorative = true)
        if (arrowIcon != null) {
            CompositionLocalProvider(LocalContentColor provides MonarchAiHandoffTokens.timeColor(MonarchTheme.colors)) {
                arrowIcon()
            }
        }
        when (to) {
            is MonarchAiHandoffTarget.Agent -> MonarchAiAgentAvatar(
                agent = to.agent,
                image = to.image,
                size = MonarchAvatarSize.Sm,
                decorative = true,
            )
            is MonarchAiHandoffTarget.Person -> MonarchAvatar(
                image = to.image,
                name = to.name,
                size = MonarchAvatarSize.Sm,
                seed = to.seed,
                decorative = true,
            )
        }
    }
}

/** A dashed hairline that takes the room left beside the marker, at least 16dp long. */
@Composable
private fun RowScope.Line(color: Color) {
    Box(
        modifier = Modifier
            .weight(1f)
            .widthIn(min = MonarchAiHandoffTokens.LineMinLength)
            .height(MonarchAiHandoffTokens.LineWidth)
            .drawBehind {
                val dash = MonarchAiHandoffTokens.Dash.toPx()
                drawLine(
                    color = color,
                    start = Offset(0f, size.height / 2),
                    end = Offset(size.width, size.height / 2),
                    strokeWidth = size.height,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash)),
                )
            },
    )
}
