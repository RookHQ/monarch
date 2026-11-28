package dev.monarchui.components.forms.marketingconsentcheckbox

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** Component tokens of MonarchMarketingConsentCheckbox, built from primitives and roles only. */
internal object MonarchMarketingConsentCheckboxTokens {
    val Gap: Dp = MonarchTokens.Space.space1.dp

    /** The colour of the question when it is a heading. */
    fun question(enabled: Boolean, colors: MonarchColors): Color =
        if (enabled) colors.textDefault else colors.textDisabled

    /** The colour of the description and the notes. */
    fun note(enabled: Boolean, colors: MonarchColors): Color = if (enabled) colors.textMuted else colors.textDisabled

    /** The colour of the policy link. It is also underlined, so it does not depend on colour. */
    fun link(enabled: Boolean, colors: MonarchColors): Color = if (enabled) colors.textLink else colors.textDisabled

    /** The wording shown, in reading order, with whitespace collapsed. */
    fun wording(parts: List<String?>): String = parts.filterNotNull()
        .map { it.replace(whitespace, " ").trim() }
        .filter { it.isNotEmpty() }
        .joinToString(" ")

    private val whitespace = Regex("""\s+""")
}
