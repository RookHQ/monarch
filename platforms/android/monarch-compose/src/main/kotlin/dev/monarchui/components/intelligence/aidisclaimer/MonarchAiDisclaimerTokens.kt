package dev.monarchui.components.intelligence.aidisclaimer

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.R
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** Component tokens and the pure logic of MonarchAiDisclaimer. */
internal object MonarchAiDisclaimerTokens {
    /** The gap between the icon and the text. */
    val IconGap: Dp = MonarchTokens.Space.space1_5.dp

    /** The built-in wording of a risk. */
    fun message(risk: MonarchAiDisclaimerRisk): Int = when (risk) {
        MonarchAiDisclaimerRisk.Low -> R.string.monarch_aidisclaimer_low
        MonarchAiDisclaimerRisk.Medium -> R.string.monarch_aidisclaimer_medium
        MonarchAiDisclaimerRisk.High -> R.string.monarch_aidisclaimer_high
    }

    /** The caller's text, unless it is `null` or blank: then the built-in wording is used. */
    fun textOrNull(text: String?): String? = text?.takeIf { it.isNotBlank() }

    /** The colour of the text of the low and medium risks. The high risk is drawn by MonarchInlineNotice. */
    fun textColor(risk: MonarchAiDisclaimerRisk, colors: MonarchColors): Color = when (risk) {
        MonarchAiDisclaimerRisk.Low -> colors.textMuted
        MonarchAiDisclaimerRisk.Medium, MonarchAiDisclaimerRisk.High -> colors.textDefault
    }

    /** The colour of the icon of the low and medium risks. */
    fun iconColor(risk: MonarchAiDisclaimerRisk, colors: MonarchColors): Color = when (risk) {
        MonarchAiDisclaimerRisk.Low -> colors.aiStrong
        MonarchAiDisclaimerRisk.Medium -> colors.infoStrong
        MonarchAiDisclaimerRisk.High -> colors.warningStrong
    }
}
