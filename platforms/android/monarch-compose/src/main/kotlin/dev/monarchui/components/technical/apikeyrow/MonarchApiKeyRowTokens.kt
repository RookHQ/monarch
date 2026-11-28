package dev.monarchui.components.technical.apikeyrow

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.components.forms.typetoconfirminput.MonarchTypeToConfirmInputDefaults
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.internal.MonarchRelativeTime
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens
import java.util.Locale

/** An action of a MonarchApiKeyRow that asks for confirmation and then runs. */
internal enum class MonarchApiKeyRowAction { Rotate, Revoke }

/** Component tokens and the pure logic of MonarchApiKeyRow. */
internal object MonarchApiKeyRowTokens {
    /** The characters drawn after the prefix in place of the secret. */
    const val MASK: String = "••••••••"

    /** The separator between the times. */
    const val SEPARATOR: String = " · "

    /** The space between the lines of the row and between badges. */
    val Gap: Dp = MonarchTokens.Space.space1.dp

    /** The space between the badges of the scope list, and between the spinner and its text. */
    val BadgeGap: Dp = MonarchTokens.Space.space2.dp

    /** The size of the spinner while an action runs. */
    val SpinnerSize: Dp = MonarchTokens.Space.space4.dp

    /** The tone of the status badge. */
    fun tone(status: MonarchApiKeyStatus): MonarchTone = when (status) {
        MonarchApiKeyStatus.Active -> MonarchTone.Success
        MonarchApiKeyStatus.Expiring -> MonarchTone.Warning
        MonarchApiKeyStatus.Expired -> MonarchTone.Neutral
        MonarchApiKeyStatus.Revoked -> MonarchTone.Danger
    }

    /** Whether the key can still be rotated or revoked: only while it works. */
    fun live(status: MonarchApiKeyStatus): Boolean =
        status == MonarchApiKeyStatus.Active || status == MonarchApiKeyStatus.Expiring

    /** Whether the row has the actions menu: the key still works and there is something to put in it. */
    fun hasActions(status: MonarchApiKeyStatus, anyAction: Boolean): Boolean = live(status) && anyAction

    /** The scopes shown as badges; a negative limit shows none. */
    fun shownScopes(scopes: List<String>, max: Int): List<String> = scopes.take(max.coerceAtLeast(0))

    /** The scopes behind the +N badge. */
    fun hiddenScopes(scopes: List<String>, max: Int): List<String> = scopes.drop(max.coerceAtLeast(0))

    /** Whether the typed phrase confirms the revocation: case-sensitive, ignoring surrounding spaces. */
    fun phraseMatches(phrase: String?, typed: String): Boolean =
        phrase == null || MonarchTypeToConfirmInputDefaults.matches(typed, phrase)

    /**
     * How far [date] is from [now] by the shared Monarch rule (the web rule), starting in lower case because it
     * follows a word: "3 days ago" or "in 5 days", in [locale].
     */
    fun relativeTime(date: Long, now: Long, locale: Locale): String =
        MonarchRelativeTime.format(date, now, locale, case = MonarchRelativeTime.Case.MidSentence)

    /** The times line: the parts joined with the separator. */
    fun timesLine(parts: List<String>): String = parts.joinToString(SEPARATOR)

    /** The colour of the prefix and the times. */
    fun mutedColor(colors: MonarchColors): Color = colors.textMuted
}
