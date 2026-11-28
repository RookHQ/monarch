package dev.monarchui.components.identity.cookieconsentbanner

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.R
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** Component tokens and the pure logic of MonarchCookieConsentBanner. */
internal object MonarchCookieConsentBannerTokens {
    val Padding: Dp = MonarchTokens.Space.space4.dp
    val Gap: Dp = MonarchTokens.Space.space3.dp
    val TextGap: Dp = MonarchTokens.Space.space1.dp
    val ActionGap: Dp = MonarchTokens.Space.space2.dp

    /** Accept and reject always look the same, so neither choice is pushed; the web default is secondary. */
    val DecisionVariant: MonarchButtonVariant = MonarchButtonVariant.Secondary
    val CustomizeVariant: MonarchButtonVariant = MonarchButtonVariant.Ghost

    /** The raised surface of the card, with the subtle border; the description is muted. */
    fun fill(colors: MonarchColors): Color = colors.bgRaised
    fun border(colors: MonarchColors): Color = colors.borderSubtle
    fun description(colors: MonarchColors): Color = colors.textMuted
    fun link(colors: MonarchColors): Color = colors.textLink

    /** The built-in description of each regime. */
    @StringRes
    fun description(regime: MonarchCookieConsentRegime): Int =
        when (regime) {
            MonarchCookieConsentRegime.OptIn -> R.string.monarch_cookieconsentbanner_description_opt_in
            MonarchCookieConsentRegime.OptOut -> R.string.monarch_cookieconsentbanner_description_opt_out
            MonarchCookieConsentRegime.Notice -> R.string.monarch_cookieconsentbanner_description_notice
        }

    /** The choices of each regime, in display order. */
    fun choices(regime: MonarchCookieConsentRegime): List<MonarchCookieConsentChoice> =
        when (regime) {
            MonarchCookieConsentRegime.OptIn ->
                listOf(MonarchCookieConsentChoice.RejectAll, MonarchCookieConsentChoice.AcceptAll)
            MonarchCookieConsentRegime.OptOut ->
                listOf(MonarchCookieConsentChoice.DoNotSell, MonarchCookieConsentChoice.Acknowledge)
            MonarchCookieConsentRegime.Notice -> listOf(MonarchCookieConsentChoice.Acknowledge)
        }
}
