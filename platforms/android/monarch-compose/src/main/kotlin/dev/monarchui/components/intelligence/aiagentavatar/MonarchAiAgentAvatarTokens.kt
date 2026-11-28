package dev.monarchui.components.intelligence.aiagentavatar

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.components.identity.avatar.MonarchAvatarSize
import dev.monarchui.components.identity.avatar.MonarchAvatarTokens
import dev.monarchui.components.intelligence.aiconversation.MonarchAiAgentRef
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** The colours of the ring around the picture: a solid colour, or two colours of a turning sweep. */
internal data class MonarchAiAgentAvatarRing(val colors: List<Color>, val dashed: Boolean)

/** Component tokens and the pure logic of MonarchAiAgentAvatar. */
internal object MonarchAiAgentAvatarTokens {
    /** The width of the state ring. */
    val RingWidth: Dp = MonarchTokens.Space.space0_5.dp

    /** The space between the picture and the ring. */
    val RingGap: Dp = MonarchTokens.Space.px.dp

    /** The ring around the badge, in the surface colour, so it stands off the picture. */
    val BadgeBorder: Dp = MonarchTokens.Space.px.dp

    /** The gap between the picture and the visible state. */
    val StateGap: Dp = MonarchTokens.Space.space2.dp

    /** The dash and the gap of the waiting ring. */
    val Dash: Dp = MonarchTokens.Space.space1.dp

    /** One turn of the working ring, in milliseconds. */
    const val TURN_MILLIS: Int = 1600

    /** A full turn in degrees. */
    const val FULL_TURN: Float = 360f

    /** The corner radius of the ring, concentric with the picture's corners. */
    fun ringRadius(size: MonarchAvatarSize): Dp = MonarchAvatarTokens.squareRadius(size) + RingGap + RingWidth / 2

    /** The side of the badge at the default font size. */
    fun badgeSide(size: MonarchAvatarSize): Dp = when (size) {
        MonarchAvatarSize.Xs, MonarchAvatarSize.Sm -> MonarchTokens.Space.space2_5.dp
        MonarchAvatarSize.Md -> MonarchTokens.Space.space3.dp
        MonarchAvatarSize.Lg -> MonarchTokens.Space.space4.dp
        MonarchAvatarSize.Xl -> MonarchTokens.Space.space5.dp
    }

    /**
     * The ring of a state, or `null` for idle: a turning sweep in the AI colours while working (one still
     * colour when animations are off), dashed warning while waiting, danger on error.
     */
    fun ring(state: MonarchAiAgentState, colors: MonarchColors, reducedMotion: Boolean): MonarchAiAgentAvatarRing? =
        when (state) {
            MonarchAiAgentState.Idle -> null
            MonarchAiAgentState.Working -> if (reducedMotion) {
                MonarchAiAgentAvatarRing(listOf(colors.aiStrong), dashed = false)
            } else {
                MonarchAiAgentAvatarRing(listOf(colors.aiStrong, colors.aiSubtle, colors.aiStrong), dashed = false)
            }
            MonarchAiAgentState.Waiting -> MonarchAiAgentAvatarRing(listOf(colors.warningStrong), dashed = true)
            MonarchAiAgentState.Error -> MonarchAiAgentAvatarRing(listOf(colors.dangerStrong), dashed = false)
        }

    /** Whether the ring turns. */
    fun turns(state: MonarchAiAgentState, reducedMotion: Boolean): Boolean =
        state == MonarchAiAgentState.Working && !reducedMotion

    /** The fill of the badge: AI solid, danger solid on error. */
    fun badgeColor(state: MonarchAiAgentState, colors: MonarchColors): Color =
        if (state == MonarchAiAgentState.Error) colors.dangerSolid else colors.aiSolid

    /** The content colour of an icon in the badge. */
    fun badgeContentColor(state: MonarchAiAgentState, colors: MonarchColors): Color =
        if (state == MonarchAiAgentState.Error) colors.dangerOnSolid else colors.aiOnSolid

    /** The colour of the visible state text. */
    fun stateColor(state: MonarchAiAgentState, colors: MonarchColors): Color = when (state) {
        MonarchAiAgentState.Error -> colors.dangerStrong
        MonarchAiAgentState.Waiting -> colors.warningStrong
        MonarchAiAgentState.Working -> colors.textDefault
        MonarchAiAgentState.Idle -> colors.textMuted
    }

    /** The name the initials come from: the codename, else the name. */
    fun initialsSource(agent: MonarchAiAgentRef): String =
        agent.codename?.takeIf { it.isNotBlank() } ?: agent.name

    /** What picks the fallback colour: the id, else the name. */
    fun seed(agent: MonarchAiAgentRef): String = agent.id?.takeIf { it.isNotBlank() } ?: agent.name

    /** What TalkBack reads: the name and the kind plus a busy state; only a busy state when decorative. */
    fun spoken(
        name: String,
        kind: String,
        state: MonarchAiAgentState,
        stateText: String,
        decorative: Boolean,
    ): String? {
        val busy = state != MonarchAiAgentState.Idle
        return when {
            decorative -> if (busy) stateText else null
            busy -> listOf(name, kind, stateText).joinToString(", ")
            else -> listOf(name, kind).joinToString(", ")
        }
    }
}
