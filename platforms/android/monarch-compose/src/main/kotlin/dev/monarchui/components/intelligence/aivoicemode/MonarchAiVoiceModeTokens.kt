package dev.monarchui.components.intelligence.aivoicemode

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.intelligence.aiconversation.MonarchAiConversationMessage
import dev.monarchui.components.intelligence.aiconversation.MonarchAiRole
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens
import java.util.Locale

/** One line of captions, from a conversation message. */
internal data class MonarchAiVoiceModeCaption(
    val id: String,
    val user: Boolean,
    val speaker: String,
    val text: String,
    val final: Boolean,
)

/** What the orb of the visual shows. */
internal enum class MonarchAiVoiceModeGlyph { Error, Muted, Bars, Dots, Rest }

/** The colours of the visual: halo, orb, the ring round the orb (when it has one) and glyph. */
internal data class MonarchAiVoiceModeVisualColors(
    val halo: Color,
    val orb: Color,
    val glyph: Color,
    val ring: Color? = null,
)

/** Component tokens of MonarchAiVoiceMode: sizes and colours. */
internal object MonarchAiVoiceModeTokens {
    /** How many space24 rows tall the captions log grows in the Md size. */
    private const val CaptionsMdRows = 3

    /** The width of the ring round a pale orb. */
    val RingStroke: Dp = MonarchTokens.Space.px.dp

    /** The halo's size relative to the orb at rest. */
    const val HALO_REST: Float = 1.2f

    /** How much larger than the orb the halo grows at full level. */
    const val HALO_GROWTH: Float = 0.5f

    /** The bar heights relative to the tallest, from the start bar to the end bar, as on the web. */
    val BarProfile: List<Float> = listOf(0.5f, 0.8f, 1f, 0.8f, 0.5f)

    /** The bar heights while animations are off, as shares of the glyph area: the web's 35%, 55% and 75%. */
    val ReducedBarHeights: List<Float> = listOf(0.35f, 0.55f, 0.75f, 0.55f, 0.35f)

    /** The smallest bar height, as a share of the full height, so bars never vanish. */
    const val BAR_FLOOR: Float = 0.25f

    /** The share of the orb's diameter the bars, dots and marks span. */
    const val GLYPH_AREA: Float = 0.5f

    /** The number of dots while the call waits. */
    const val DOT_COUNT: Int = 3

    /** One cycle of the waiting dots: three quarters of the shimmer duration, as `MonarchAiTypingDots`. */
    const val DOT_CYCLE_MILLIS: Int = MonarchTokens.Duration.shimmer * 3 / 4

    /** How close to the end of the captions, in dp, still counts as reading the newest. */
    val StickThreshold: Dp = MonarchTokens.Space.space6.dp

    /** The gap between lines of text in the summary and the captions. */
    val LineGap: Dp = MonarchTokens.Space.space1.dp

    /** The gap between the controls. */
    val ControlGap: Dp = MonarchTokens.Space.space2.dp

    /** The side of the agent's avatar. */
    val AvatarSize: Dp = MonarchTokens.Space.space6.dp

    /** The diameter of the orb. */
    fun orbSize(size: MonarchAiVoiceModeSize): Dp = when (size) {
        MonarchAiVoiceModeSize.Md -> MonarchTokens.Space.space24.dp
        MonarchAiVoiceModeSize.Compact -> MonarchTokens.Space.space16.dp
    }

    /** The padding around the surface. */
    fun padding(size: MonarchAiVoiceModeSize): Dp = when (size) {
        MonarchAiVoiceModeSize.Md -> MonarchTokens.Space.space6.dp
        MonarchAiVoiceModeSize.Compact -> MonarchTokens.Space.space3.dp
    }

    /** The tallest the captions log grows before it scrolls, at the default font size. */
    fun captionsHeight(size: MonarchAiVoiceModeSize): Dp = when (size) {
        MonarchAiVoiceModeSize.Md -> (MonarchTokens.Space.space24 * CaptionsMdRows).dp
        MonarchAiVoiceModeSize.Compact -> MonarchTokens.Space.space24.dp
    }

    /** The button size of the controls. */
    fun buttonSize(size: MonarchAiVoiceModeSize): MonarchButtonSize = when (size) {
        MonarchAiVoiceModeSize.Md -> MonarchButtonSize.Md
        MonarchAiVoiceModeSize.Compact -> MonarchButtonSize.Sm
    }

    /** The type role of the state text. */
    fun stateStyle(size: MonarchAiVoiceModeSize): MonarchTextStyle =
        if (size == MonarchAiVoiceModeSize.Md) MonarchTextStyle.Heading else MonarchTextStyle.BodyStrong

    /** The colours of the visual: danger for an error, neutral when muted or ended, the assistant roles otherwise. */
    fun visualColors(state: MonarchAiVoiceModeState, colors: MonarchColors): MonarchAiVoiceModeVisualColors =
        when (state) {
            MonarchAiVoiceModeState.Error ->
                MonarchAiVoiceModeVisualColors(colors.dangerSubtle, colors.dangerSolid, colors.dangerOnSolid)
            MonarchAiVoiceModeState.Muted, MonarchAiVoiceModeState.Ended ->
                MonarchAiVoiceModeVisualColors(colors.bgSubtle, colors.bgEmphasis, colors.textMuted)
            else -> MonarchAiVoiceModeVisualColors(
                halo = colors.aiSubtle,
                orb = colors.aiSubtle,
                glyph = colors.aiSolid,
                ring = colors.aiBorder,
            )
        }

    /** The halo's diameter relative to the orb. */
    fun haloScale(level: Float): Float = HALO_REST + level * HALO_GROWTH

    /** The bar heights as shares of the glyph area, from the level. */
    fun barHeights(level: Float): List<Float> = BarProfile.map { it * (BAR_FLOOR + (1 - BAR_FLOOR) * level) }

    /** The speaker colour of a caption: muted for the person, strong assistant for the assistant. */
    fun speakerColor(user: Boolean, colors: MonarchColors): Color = if (user) colors.textMuted else colors.aiStrong

    /** The surface of the compact size: the surface fill and the subtle border. */
    fun compactColors(colors: MonarchColors): Pair<Color, Color> = colors.bgSurface to colors.borderSubtle
}

/** The pure rules of MonarchAiVoiceMode: which state shows what, the captions and the clock. */
internal object MonarchAiVoiceModeRules {
    /** Whether the call is live: not ended and not failed. */
    fun live(state: MonarchAiVoiceModeState): Boolean =
        state != MonarchAiVoiceModeState.Ended && state != MonarchAiVoiceModeState.Error

    /** Whether the microphone is muted: the app's value, or the muted state. Never while not live. */
    fun muted(state: MonarchAiVoiceModeState, muted: Boolean?): Boolean =
        live(state) && (muted ?: (state == MonarchAiVoiceModeState.Muted))

    /** Whether someone is talking, so the level drives the visual. */
    fun voiced(state: MonarchAiVoiceModeState): Boolean =
        state == MonarchAiVoiceModeState.Listening || state == MonarchAiVoiceModeState.Speaking

    /** Whether the call is waiting: connecting, thinking or reconnecting. */
    private fun busy(state: MonarchAiVoiceModeState): Boolean = state == MonarchAiVoiceModeState.Connecting ||
        state == MonarchAiVoiceModeState.Thinking || state == MonarchAiVoiceModeState.Reconnecting

    /** Whether Interrupt is offered: while the assistant thinks or speaks, and only with a handler. */
    fun canInterrupt(state: MonarchAiVoiceModeState, hasHandler: Boolean): Boolean =
        hasHandler && (state == MonarchAiVoiceModeState.Thinking || state == MonarchAiVoiceModeState.Speaking)

    /** The level the visual follows: 0 to 1 while someone talks; 0 otherwise and when animations are off. */
    fun shownLevel(level: Float, state: MonarchAiVoiceModeState, reducedMotion: Boolean): Float =
        if (!voiced(state) || reducedMotion || !level.isFinite()) 0f else level.coerceIn(0f, 1f)

    /** The glyph in the orb for a state. */
    fun glyph(state: MonarchAiVoiceModeState): MonarchAiVoiceModeGlyph = when {
        state == MonarchAiVoiceModeState.Error -> MonarchAiVoiceModeGlyph.Error
        state == MonarchAiVoiceModeState.Muted -> MonarchAiVoiceModeGlyph.Muted
        voiced(state) -> MonarchAiVoiceModeGlyph.Bars
        busy(state) -> MonarchAiVoiceModeGlyph.Dots
        else -> MonarchAiVoiceModeGlyph.Rest
    }

    /** The running time as a clock with the locale's digits: `4:05`, or `1:02:09` past an hour. */
    fun duration(seconds: Int, locale: Locale): String {
        val total = seconds.coerceAtLeast(0)
        val hours = total / SECONDS_PER_HOUR
        val minutes = total % SECONDS_PER_HOUR / SECONDS_PER_MINUTE
        val rest = total % SECONDS_PER_MINUTE
        return if (hours > 0) {
            String.format(locale, "%d:%02d:%02d", hours, minutes, rest)
        } else {
            String.format(locale, "%d:%02d", minutes, rest)
        }
    }

    /**
     * The captions of the person's and the assistant's messages, oldest first.
     * A message is final once none of its text is streaming. Other roles and
     * messages without text are left out.
     */
    fun captions(
        messages: List<MonarchAiConversationMessage>,
        agentName: String,
        you: String,
    ): List<MonarchAiVoiceModeCaption> = messages.mapNotNull { message ->
        if (message.role != MonarchAiRole.User && message.role != MonarchAiRole.Assistant) return@mapNotNull null
        val text = message.text.trim()
        if (text.isEmpty()) return@mapNotNull null
        val user = message.role == MonarchAiRole.User
        MonarchAiVoiceModeCaption(
            id = message.id,
            user = user,
            speaker = if (user) you else message.agent?.name?.takeIf { it.isNotBlank() } ?: agentName,
            text = text,
            final = !message.streaming,
        )
    }

    /** The final captions that have not been announced yet, oldest first. */
    fun unannounced(
        captions: List<MonarchAiVoiceModeCaption>,
        announced: Set<String>,
    ): List<MonarchAiVoiceModeCaption> =
        captions.filter { it.final && it.id !in announced }

    /**
     * Whether a change of state is announced. The first state is not, nor a
     * change into listening or speaking: the audio already says whose turn it is.
     */
    fun announces(from: MonarchAiVoiceModeState?, to: MonarchAiVoiceModeState): Boolean =
        from != null && from != to && !voiced(to)

    private const val SECONDS_PER_MINUTE = 60
    private const val SECONDS_PER_HOUR = 3_600
}
