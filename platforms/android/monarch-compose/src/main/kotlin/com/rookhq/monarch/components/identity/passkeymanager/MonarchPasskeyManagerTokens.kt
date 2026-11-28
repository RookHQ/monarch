package com.rookhq.monarch.components.identity.passkeymanager

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.internal.MonarchRelativeTime
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens
import java.util.Locale

/** Component tokens and the pure logic of MonarchPasskeyManager. */
internal object MonarchPasskeyManagerTokens {
    /** The separator between the parts of a line. */
    const val SEPARATOR: String = " · "

    /** The gap between the header, the notices and the list, and between rows. */
    val SectionGap: Dp = MonarchTokens.Space.space4.dp

    /** The tile behind the key icon of a passkey. */
    val IconTile: Dp = MonarchTokens.Space.space8.dp

    /** The gap between the icon, the text and the actions, and between buttons. */
    val RowGap: Dp = MonarchTokens.Space.space3.dp

    /** The gap between the lines of a row. */
    val LineGap: Dp = MonarchTokens.Space.space1.dp

    /** The colour of the passkey icon. */
    fun iconColor(colors: MonarchColors): Color = colors.textMuted

    /** The colour of the warning in the removal confirmation. */
    fun warningColor(colors: MonarchColors): Color = colors.dangerStrong

    /** [date] relative to [now] by the shared Monarch rule (the web rule): "30 seconds ago", "5 minutes ago". */
    fun relativeTime(date: Long, now: Long, locale: Locale = Locale.getDefault()): String =
        MonarchRelativeTime.format(date, now, locale)

    /** Whether [draft] can be saved as the new name of a passkey called [current]: not blank and changed. */
    fun canSave(draft: String, current: String): Boolean = draft.trim().let { it.isNotEmpty() && it != current }

    /** Whether removing warns that it is the last way to sign in: only when exactly one passkey is left. */
    fun warnsLastMethod(lastSignInMethod: Boolean, count: Int): Boolean = lastSignInMethod && count == 1

    /** The parts that exist, joined with the separator, or `null` when there are none. */
    fun line(vararg parts: String?): String? =
        parts.filterNotNull().filter { it.isNotBlank() }.takeIf { it.isNotEmpty() }?.joinToString(SEPARATOR)

    /** What TalkBack reads after a passkey's name: Synced, the detail and the times. */
    fun spokenDetails(badges: List<String>, detail: String?, times: String?): String =
        (badges + listOfNotNull(detail, times)).joinToString(", ")
}
