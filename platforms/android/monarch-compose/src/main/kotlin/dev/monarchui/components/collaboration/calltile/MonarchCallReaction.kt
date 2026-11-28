package dev.monarchui.components.collaboration.calltile

import androidx.compose.runtime.Immutable

/**
 * A short-lived reaction on a `MonarchCallTile`. It shows for three seconds
 * from [sentAt]; a reaction with a new [sentAt] plays again.
 *
 * @property emoji the reaction, for example "👏".
 * @property sentAt when it was sent, in epoch milliseconds (UTC).
 */
@Immutable
public class MonarchCallReaction(public val emoji: String, public val sentAt: Long) {
    override fun equals(other: Any?): Boolean =
        other is MonarchCallReaction && other.emoji == emoji && other.sentAt == sentAt

    override fun hashCode(): Int = 31 * emoji.hashCode() + sentAt.hashCode()
}
