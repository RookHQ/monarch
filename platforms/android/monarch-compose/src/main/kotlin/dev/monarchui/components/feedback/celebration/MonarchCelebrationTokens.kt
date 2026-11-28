package dev.monarchui.components.feedback.celebration

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.internal.MonarchSurfaceColors
import dev.monarchui.internal.MonarchToneEmphasis
import dev.monarchui.internal.MonarchTonePalette
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/**
 * One piece of confetti, as on the web: where it starts across the width, when
 * it starts, its rotation and sideways drift, its colour (1 to 5) and shape.
 */
internal class CelebrationPiece(
    val start: Float,
    val delayMillis: Int,
    val rotation: Float,
    val drift: Float,
    val color: Int,
    val dot: Boolean,
)

/** Where a piece is drawn at one moment, in dp from its start, with its rotation and opacity. */
internal data class CelebrationPieceFrame(val dx: Float, val dy: Float, val rotation: Float, val opacity: Float)

/** Component tokens and rules of MonarchCelebration, built from primitives and roles only. */
@Suppress("MagicNumber")
internal object MonarchCelebrationTokens {
    val MaxWidth: Dp = MonarchTokens.Space.space24.dp * 4 + MonarchTokens.Space.space16.dp
    val VisualMaxWidth: Dp = MonarchTokens.Space.space24.dp * 2 + MonarchTokens.Space.space16.dp
    val HorizontalPadding: Dp = MonarchTokens.Space.space4.dp
    val VerticalPadding: Dp = MonarchTokens.Space.space6.dp
    val VisualTopPadding: Dp = MonarchTokens.Space.space6.dp
    val Gap: Dp = MonarchTokens.Space.space2.dp
    val ActionsTopPadding: Dp = MonarchTokens.Space.space3.dp
    val TileSize: Dp = MonarchTokens.Space.space16.dp
    val BarWidth: Dp = MonarchTokens.Space.space1_5.dp
    val BarHeight: Dp = MonarchTokens.Space.space3.dp
    val DotSize: Dp = MonarchTokens.Space.space1_5.dp
    val PieceRadius: Dp = MonarchTokens.Radius.xs.dp

    /** One dp, the unit of the drift and fall distances in the piece table. */
    val Unit: Dp = MonarchTokens.Space.px.dp

    /** How far a piece falls, in dp, and where it starts above the top. */
    const val FALL_DISTANCE: Float = 80f
    const val START_OFFSET: Float = -8f

    /** How long one piece falls, and how long the whole fall lasts with the last delay. */
    const val FALL_MILLIS: Int = 1600
    const val TOTAL_MILLIS: Int = 1860

    /** The opacity of the still scatter when animations are removed. */
    const val REST_OPACITY: Float = 0.7f

    /** The scale the tile grows from. */
    const val TILE_START_SCALE: Float = 0.85f

    private const val FADE_IN_END = 0.15f
    private const val FADE_OUT_START = 0.7f
    private const val TURN = 180f
    private const val REST_PER_100_MILLIS = 16f

    /** The twelve pieces, at fixed positions so that every run looks the same. */
    val Pieces: List<CelebrationPiece> = listOf(
        CelebrationPiece(0.06f, 0, 20f, -12f, 1, dot = false),
        CelebrationPiece(0.14f, 120, -35f, 10f, 2, dot = true),
        CelebrationPiece(0.22f, 60, 60f, -8f, 3, dot = false),
        CelebrationPiece(0.31f, 200, -15f, 14f, 4, dot = false),
        CelebrationPiece(0.39f, 40, 45f, -10f, 5, dot = true),
        CelebrationPiece(0.47f, 160, -60f, 6f, 1, dot = false),
        CelebrationPiece(0.55f, 20, 30f, -14f, 2, dot = false),
        CelebrationPiece(0.62f, 240, -25f, 12f, 3, dot = true),
        CelebrationPiece(0.70f, 80, 70f, -6f, 4, dot = false),
        CelebrationPiece(0.78f, 180, -45f, 10f, 5, dot = false),
        CelebrationPiece(0.86f, 100, 15f, -12f, 1, dot = true),
        CelebrationPiece(0.93f, 260, -70f, 8f, 2, dot = false),
    )

    /** The tile: the subtle fill and the strong foreground of the tone. */
    fun tile(tone: MonarchTone, colors: MonarchColors): MonarchSurfaceColors =
        MonarchTonePalette.of(tone, colors).surface(MonarchToneEmphasis.Subtle)

    /** The title is default text, the message muted text. */
    fun titleColor(colors: MonarchColors): Color = colors.textDefault

    /** The colour of the message. */
    fun messageColor(colors: MonarchColors): Color = colors.textMuted

    /** The colour of a piece: the five data roles, as on the web. */
    fun pieceColor(color: Int, colors: MonarchColors): Color = when (color) {
        1 -> colors.data1
        2 -> colors.data2
        3 -> colors.data3
        4 -> colors.data4
        else -> colors.data5
    }

    /**
     * Where [piece] is [elapsedMillis] after the celebration appeared, with the
     * fall eased by [easing]. With [reducedMotion] it rests in a still scatter.
     * Before its delay and after its fall it is fully transparent.
     */
    fun frame(
        piece: CelebrationPiece,
        elapsedMillis: Long,
        reducedMotion: Boolean,
        easing: (Float) -> Float = { it },
    ): CelebrationPieceFrame {
        if (reducedMotion) {
            return CelebrationPieceFrame(
                dx = piece.drift,
                dy = piece.delayMillis / 100f * REST_PER_100_MILLIS,
                rotation = piece.rotation,
                opacity = REST_OPACITY,
            )
        }
        val t = ((elapsedMillis - piece.delayMillis).toFloat() / FALL_MILLIS).coerceIn(0f, 1f)
        val eased = easing(t)
        val opacity = when {
            elapsedMillis < piece.delayMillis -> 0f
            t < FADE_IN_END -> t / FADE_IN_END
            t <= FADE_OUT_START -> 1f
            else -> (1f - t) / (1f - FADE_OUT_START)
        }
        return CelebrationPieceFrame(
            dx = piece.drift * eased,
            dy = START_OFFSET + (FALL_DISTANCE - START_OFFSET) * eased,
            rotation = piece.rotation + TURN * eased,
            opacity = opacity,
        )
    }

    /** Whether the confetti still draws: it stops once the last piece has fallen, and rests still without motion. */
    fun confettiVisible(
        decoration: MonarchCelebrationDecoration,
        elapsedMillis: Long,
        reducedMotion: Boolean,
    ): Boolean = decoration == MonarchCelebrationDecoration.Confetti && (reducedMotion || elapsedMillis < TOTAL_MILLIS)
}
