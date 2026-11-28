package dev.monarchui.components.intelligence.aitoolapproval

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.R
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.intelligence.aiconversation.MonarchAiJSON
import dev.monarchui.components.intelligence.aiconversation.MonarchAiToolCallRisk
import dev.monarchui.components.intelligence.aiconversation.MonarchAiToolCallState
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.internal.MonarchTonePalette
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** What the person decided in this card. */
internal enum class AiToolApprovalDecision { Approved, Denied }

/** How a call that is no longer waiting ended. */
internal enum class AiToolApprovalOutcome { Approved, Denied, Cancelled }

/** The edited arguments when approving. */
internal sealed interface AiToolApprovalEdit {
    /** Not edited, or edited back to the original: approve without new input. */
    data object Unchanged : AiToolApprovalEdit

    /** Edited into valid JSON. */
    data class Edited(val input: MonarchAiJSON) : AiToolApprovalEdit

    /** Edited into text that is not JSON: approving is refused. */
    data object Invalid : AiToolApprovalEdit
}

/** Component tokens and the pure logic of MonarchAiToolApproval. */
internal object MonarchAiToolApprovalTokens {
    /** The gap between the blocks of the card. */
    val Gap: Dp = MonarchTokens.Space.space3.dp

    /** The gap inside a block. */
    val InnerGap: Dp = MonarchTokens.Space.space1.dp

    /** The gap between the buttons. */
    val ButtonGap: Dp = MonarchTokens.Space.space2.dp

    /** The tone of a risk. */
    fun tone(risk: MonarchAiToolCallRisk): MonarchTone = when (risk) {
        MonarchAiToolCallRisk.Low -> MonarchTone.Neutral
        MonarchAiToolCallRisk.Medium, MonarchAiToolCallRisk.High -> MonarchTone.Warning
        MonarchAiToolCallRisk.Destructive -> MonarchTone.Danger
    }

    /** The approve button: danger for a destructive call, primary otherwise. */
    fun approveVariant(risk: MonarchAiToolCallRisk): MonarchButtonVariant =
        if (risk == MonarchAiToolCallRisk.Destructive) MonarchButtonVariant.Danger else MonarchButtonVariant.Primary

    /** Always allow is never offered for a destructive call. */
    fun offersAlwaysAllow(hasCallback: Boolean, risk: MonarchAiToolCallRisk): Boolean =
        hasCallback && risk != MonarchAiToolCallRisk.Destructive

    /** The built-in text of a risk. */
    @StringRes
    fun riskText(risk: MonarchAiToolCallRisk): Int = when (risk) {
        MonarchAiToolCallRisk.Low -> R.string.monarch_aitoolapproval_risk_low
        MonarchAiToolCallRisk.Medium -> R.string.monarch_aitoolapproval_risk_medium
        MonarchAiToolCallRisk.High -> R.string.monarch_aitoolapproval_risk_high
        MonarchAiToolCallRisk.Destructive -> R.string.monarch_aitoolapproval_risk_destructive
    }

    /** The built-in consequence of a risk, or `null` for none. */
    @StringRes
    fun consequenceText(risk: MonarchAiToolCallRisk): Int? =
        if (risk == MonarchAiToolCallRisk.Destructive) R.string.monarch_aitoolapproval_consequence_destructive else null

    /** The colour of the consequence line: the strong colour of the risk's tone. */
    fun consequenceColor(risk: MonarchAiToolCallRisk, colors: MonarchColors): Color =
        MonarchTonePalette.of(tone(risk), colors).strong

    /** The colour of the outcome line. */
    fun outcomeColor(outcome: AiToolApprovalOutcome, colors: MonarchColors): Color = when (outcome) {
        AiToolApprovalOutcome.Approved -> colors.successStrong
        AiToolApprovalOutcome.Denied -> colors.dangerStrong
        AiToolApprovalOutcome.Cancelled -> colors.textMuted
    }

    /** The outcome, or `null` while the call waits for this card's decision. */
    fun outcome(state: MonarchAiToolCallState, decision: AiToolApprovalDecision?): AiToolApprovalOutcome? = when {
        decision == AiToolApprovalDecision.Approved -> AiToolApprovalOutcome.Approved
        decision == AiToolApprovalDecision.Denied -> AiToolApprovalOutcome.Denied
        state == MonarchAiToolCallState.PendingApproval -> null
        state == MonarchAiToolCallState.Denied -> AiToolApprovalOutcome.Denied
        state == MonarchAiToolCallState.Cancelled -> AiToolApprovalOutcome.Cancelled
        else -> AiToolApprovalOutcome.Approved
    }
}
