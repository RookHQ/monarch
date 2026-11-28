package dev.monarchui.components.intelligence.aiagentavatar

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.SweepGradientShader
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import dev.monarchui.R
import dev.monarchui.components.identity.avatar.MonarchAvatar
import dev.monarchui.components.identity.avatar.MonarchAvatarLogic
import dev.monarchui.components.identity.avatar.MonarchAvatarShape
import dev.monarchui.components.identity.avatar.MonarchAvatarSize
import dev.monarchui.components.identity.avatar.MonarchAvatarTokens
import dev.monarchui.components.intelligence.aiconversation.MonarchAiAgentRef
import dev.monarchui.internal.MonarchGlyphs
import dev.monarchui.internal.monarchString
import dev.monarchui.theme.MonarchTheme

/**
 * The picture of an AI agent: a rounded square with an AI badge, so it is
 * never taken for a person's round avatar, and a ring that shows whether the
 * agent is working, waiting for someone or stopped with an error.
 *
 * Built by Monarch on `MonarchAvatar` in its square shape, with the ring drawn
 * behind it and a small badge at the bottom end corner. Monarch bundles no
 * icons, so the badge is a filled mark unless the app passes [badgeIcon], for
 * example a bot glyph. The state is always in words for TalkBack, so it never
 * depends on the ring's colour, dash or motion. The working ring turns once
 * every 1.6 seconds and stands still when the person has turned animations off.
 *
 * @param agent the agent from the conversation model: [MonarchAiAgentRef.name] names the avatar,
 * [MonarchAiAgentRef.codename] feeds the initials and [MonarchAiAgentRef.id] picks the fallback colour.
 * Monarch loads no images; load [MonarchAiAgentRef.avatar] in the app and pass it as [image].
 * @param modifier the modifier of the avatar.
 * @param image the agent's picture, or `null` to show the initials.
 * @param state what the agent is doing.
 * @param size 24, 32 or 40dp at the default font size, as `MonarchAvatar`.
 * @param showState writes the state beside the picture.
 * @param decorative hides the avatar from TalkBack when the agent's name is written beside it; a state other
 * than idle is still read.
 * @param badgeIcon an icon inside the badge, given the badge's content colour. Decorative.
 * @param labels replacements for the built-in strings.
 */
@Composable
public fun MonarchAiAgentAvatar(
    agent: MonarchAiAgentRef,
    modifier: Modifier = Modifier,
    image: Painter? = null,
    state: MonarchAiAgentState = MonarchAiAgentState.Idle,
    size: MonarchAvatarSize = MonarchAvatarSize.Md,
    showState: Boolean = false,
    decorative: Boolean = false,
    badgeIcon: (@Composable () -> Unit)? = null,
    labels: MonarchAiAgentAvatarLabels = MonarchAiAgentAvatarLabels(),
) {
    val tokens = MonarchAiAgentAvatarTokens
    val colors = MonarchTheme.colors
    val kind = monarchString(labels.kind, R.string.monarch_aiagentavatar_kind)
    val stateText = labels.state?.invoke(state) ?: stringResource(stateResource(state))
    val spoken = tokens.spoken(agent.name, kind, state, stateText, decorative)
    val fontScale = LocalDensity.current.fontScale
    Row(
        modifier = modifier.semantics(mergeDescendants = spoken != null) {
            if (spoken != null) {
                contentDescription = spoken
                role = Role.Image
                if (state != MonarchAiAgentState.Idle) stateDescription = stateText
            } else {
                hideFromAccessibility()
            }
        },
        horizontalArrangement = Arrangement.spacedBy(tokens.StateGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.ring(state, size)) {
            if (image != null) {
                MonarchAvatar(
                    image = image,
                    name = tokens.initialsSource(agent),
                    shape = MonarchAvatarShape.Square,
                    size = size,
                    seed = tokens.seed(agent),
                    decorative = true,
                )
            } else {
                AiFace(tokens.initialsSource(agent), size)
            }
            if (size != MonarchAvatarSize.Xs) Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(tokens.badgeSide(size) * fontScale)
                    .background(tokens.badgeColor(state, colors), CircleShape)
                    .border(tokens.BadgeBorder, colors.bgSurface, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                CompositionLocalProvider(LocalContentColor provides tokens.badgeContentColor(state, colors)) {
                    (
                        badgeIcon ?: {
                            Icon(
                                MonarchGlyphs.SmartToy,
                                contentDescription = null,
                                modifier = Modifier.size(tokens.badgeSide(size) * fontScale * BadgeGlyphShare),
                            )
                        }
                        )()
                }
            }
        }
        if (showState) {
            // Already part of the avatar's name; not read a second time.
            Text(
                text = stateText,
                modifier = Modifier.semantics { hideFromAccessibility() },
                style = MonarchTheme.typography.caption,
                color = tokens.stateColor(state, colors),
            )
        }
    }
}

private const val BadgeGlyphShare = 0.72f

/** The face of an agent without a picture: the initials on the AI tint, in a rounded square. */
@Composable
private fun AiFace(name: String, size: MonarchAvatarSize) {
    val colors = MonarchTheme.colors
    val initials = MonarchAvatarLogic.initials(name)
    val side = MonarchAvatarTokens.side(size) * LocalDensity.current.fontScale
    val shape = MonarchAvatarTokens.shape(MonarchAvatarShape.Square, size)
    Box(
        modifier = Modifier
            .size(side)
            .clip(shape)
            .background(colors.aiSubtle)
            .border(MonarchAiAgentAvatarTokens.BadgeBorder, colors.aiBorder, shape),
        contentAlignment = Alignment.Center,
    ) {
        if (MonarchAvatarLogic.hasReadableInitials(initials)) {
            val style = MonarchAvatarTokens.initialsStyle(size, MonarchTheme.typography)
            BasicText(
                text = initials,
                style = style.copy(color = colors.aiStrong, textAlign = TextAlign.Center),
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

/** Reserves the space of the ring around the picture and draws it for the state. */
@Composable
private fun Modifier.ring(state: MonarchAiAgentState, size: MonarchAvatarSize): Modifier {
    val tokens = MonarchAiAgentAvatarTokens
    val colors = MonarchTheme.colors
    val reduced = MonarchTheme.motion.reducedMotion
    val ring = tokens.ring(state, colors, reduced)
    val angle: State<Float> = if (tokens.turns(state, reduced)) {
        rememberInfiniteTransition(label = "agent ring").animateFloat(
            initialValue = 0f,
            targetValue = tokens.FULL_TURN,
            animationSpec = infiniteRepeatable(tween(tokens.TURN_MILLIS, easing = LinearEasing), RepeatMode.Restart),
            label = "agent ring angle",
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }
    val cornerRadius = tokens.ringRadius(size)
    return this
        .drawBehind { if (ring != null) drawRing(ring, angle.value, cornerRadius.toPx()) }
        .padding(tokens.RingWidth + tokens.RingGap)
}

private fun DrawScope.drawRing(ring: MonarchAiAgentAvatarRing, angle: Float, corner: Float) {
    val tokens = MonarchAiAgentAvatarTokens
    val width = tokens.RingWidth.toPx()
    val inset = width / 2
    val dash = tokens.Dash.toPx()
    val stroke = Stroke(
        width = width,
        pathEffect = if (ring.dashed) PathEffect.dashPathEffect(floatArrayOf(dash, dash)) else null,
    )
    val brush: Brush = if (ring.colors.size == 1) {
        SolidColor(ring.colors.single())
    } else {
        TurnedSweep(ring.colors, angle)
    }
    drawRoundRect(
        brush = brush,
        topLeft = Offset(inset, inset),
        size = Size(this.size.width - width, this.size.height - width),
        cornerRadius = CornerRadius(corner),
        style = stroke,
    )
}

/** A sweep gradient turned by [degrees] around the centre, so the ring turns while its square stays still. */
private class TurnedSweep(private val colors: List<Color>, private val degrees: Float) : ShaderBrush() {
    override fun createShader(size: Size): Shader {
        val center = Offset(size.width / 2, size.height / 2)
        return SweepGradientShader(center, colors).apply {
            setLocalMatrix(android.graphics.Matrix().apply { setRotate(degrees, center.x, center.y) })
        }
    }
}

private fun stateResource(state: MonarchAiAgentState): Int = when (state) {
    MonarchAiAgentState.Idle -> R.string.monarch_aiagentavatar_idle
    MonarchAiAgentState.Working -> R.string.monarch_aiagentavatar_working
    MonarchAiAgentState.Waiting -> R.string.monarch_aiagentavatar_waiting
    MonarchAiAgentState.Error -> R.string.monarch_aiagentavatar_error
}
