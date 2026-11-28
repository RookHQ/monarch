package dev.monarchui.components.identity.pendinginviteslist

import dev.monarchui.components.layout.divider.MonarchDividerVariant
import dev.monarchui.components.layout.sectioncard.MonarchSectionCardState
import dev.monarchui.internal.MonarchRelativeTime
import java.util.Locale

/** Component tokens and pure logic of MonarchPendingInvitesList, built from roles only. */
internal object MonarchPendingInvitesListTokens {
    /** A ready list with no invitations shows the empty state. */
    fun state(state: MonarchSectionCardState, count: Int): MonarchSectionCardState =
        if (state == MonarchSectionCardState.Ready && count == 0) MonarchSectionCardState.Empty else state

    /** Whether the invitation has expired: as given, or because [MonarchPendingInvite.expiresAt] has passed. */
    fun expired(invite: MonarchPendingInvite, now: Long): Boolean =
        invite.expired ?: (invite.expiresAt != null && invite.expiresAt <= now)

    /** The detail line from its parts, "Invited by Sam Rivera · Sent 2 days ago · Expires in 5 days". */
    fun detail(parts: List<String?>): String? =
        parts.filterNot { it.isNullOrBlank() }.joinToString(" · ").ifEmpty { null }

    /**
     * A relative time in [locale], in the past ("2 days ago") or the future ("in 5 days"), by the shared
     * Monarch rule (the web rule).
     */
    fun relativeTime(time: Long, now: Long, locale: Locale): String = MonarchRelativeTime.format(time, now, locale)

    /** The line between invitations: the subtle border, as between the rows of a list. */
    val DividerVariant: MonarchDividerVariant = MonarchDividerVariant.Subtle
}
