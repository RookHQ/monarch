package dev.monarchui.components.intelligence.aisuggestedactions

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.R
import dev.monarchui.components.intelligence.aiconversation.MonarchAiToolCallRisk
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.internal.MonarchSurfaceColors
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** Component tokens and pure logic of MonarchAiSuggestedActions, built from primitives and roles only. */
internal object MonarchAiSuggestedActionsTokens {
    /** The gap between the header, the actions and the empty text. */
    val Gap: Dp = MonarchTokens.Space.space2.dp

    /** The gap between the parts of one action. */
    val ItemGap: Dp = MonarchTokens.Space.space1.dp

    /** The padding above and below one action. */
    val ItemPadding: Dp = MonarchTokens.Space.space2.dp

    /** The gap between the heading and the origin label, and between tags and buttons. */
    val InlineGap: Dp = MonarchTokens.Space.space2.dp

    /** The gap between the origin icon and its text. */
    val OriginGap: Dp = MonarchTokens.Space.space1.dp

    /** The padding at the start and end of the origin label. */
    val OriginHorizontalPadding: Dp = MonarchTokens.Space.space1_5.dp

    /** The padding above and below the origin label's text. */
    val OriginVerticalPadding: Dp = MonarchTokens.Space.px.dp

    /** The status shown: the app's when it gives one, else the one the component tracked, else ready. */
    fun status(
        action: MonarchAiSuggestedAction,
        tracked: MonarchAiSuggestedActionStatus?,
    ): MonarchAiSuggestedActionStatus = action.status ?: tracked ?: MonarchAiSuggestedActionStatus.Ready

    /** The tone of a risk tag, or `null` when the risk has no tag (low). */
    fun riskTone(risk: MonarchAiToolCallRisk): MonarchTone? = when (risk) {
        MonarchAiToolCallRisk.Low -> null
        MonarchAiToolCallRisk.Medium, MonarchAiToolCallRisk.High -> MonarchTone.Warning
        MonarchAiToolCallRisk.Destructive -> MonarchTone.Danger
    }

    /** The built-in tag of a risk, or `null` for low, which has none. */
    fun riskResource(risk: MonarchAiToolCallRisk): Int? = when (risk) {
        MonarchAiToolCallRisk.Low -> null
        MonarchAiToolCallRisk.Medium -> R.string.monarch_aisuggestedactions_risk_medium
        MonarchAiToolCallRisk.High -> R.string.monarch_aisuggestedactions_risk_high
        MonarchAiToolCallRisk.Destructive -> R.string.monarch_aisuggestedactions_risk_destructive
    }

    /**
     * Where focus goes after the action at [index] was dismissed and left
     * [remaining]: the action now at that place, else the one before it, else
     * nothing.
     */
    fun focusAfterDismiss(remaining: List<MonarchAiSuggestedAction>, index: Int): String? =
        (remaining.getOrNull(index) ?: remaining.getOrNull(index - 1))?.id

    /** The origin label: only model suggestions use the assistant colours. */
    fun origin(origin: MonarchAiSuggestedActionsOrigin, colors: MonarchColors): MonarchSurfaceColors =
        when (origin) {
            MonarchAiSuggestedActionsOrigin.Ai ->
                MonarchSurfaceColors(colors.aiSubtle, colors.aiStrong, colors.aiBorder)
            MonarchAiSuggestedActionsOrigin.Rule ->
                MonarchSurfaceColors(colors.bgSurface, colors.textMuted, colors.borderDefault)
        }

    /** The action's label colour: muted once it is done. */
    fun labelColor(done: Boolean, colors: MonarchColors): Color = if (done) colors.textMuted else colors.textDefault

    /** The colour of descriptions and the empty text. */
    fun mutedColor(colors: MonarchColors): Color = colors.textMuted

    /** The colour of the "Done" text. */
    fun doneColor(colors: MonarchColors): Color = colors.successStrong

    /** The colour of the failed line. */
    fun failedColor(colors: MonarchColors): Color = colors.dangerStrong
}
