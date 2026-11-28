package dev.monarchui.components.collaboration.reactionbar

import androidx.compose.runtime.Immutable

/**
 * One emoji reaction of a `MonarchReactionBar`, with its count.
 *
 * @property emoji the emoji shown on the chip, such as "👍". Also the value passed to `onToggle`.
 * @property count how many people reacted. A chip with a count of 0 is not shown.
 * @property reacted the current person reacted: the chip is selected.
 * @property label the spoken name of the emoji, such as "thumbs up". `null` reads the emoji itself.
 * @property reactedBy who reacted, such as "Ana, Ben and 3 others", read after the chip's name.
 */
@Immutable
public class MonarchReactionBarItem(
    public val emoji: String,
    public val count: Int,
    public val reacted: Boolean = false,
    public val label: String? = null,
    public val reactedBy: String? = null,
) {
    override fun equals(other: Any?): Boolean =
        other is MonarchReactionBarItem &&
            other.emoji == emoji &&
            other.count == count &&
            other.reacted == reacted &&
            other.label == label &&
            other.reactedBy == reactedBy

    override fun hashCode(): Int {
        var result = emoji.hashCode()
        result = HASH * result + count
        result = HASH * result + reacted.hashCode()
        result = HASH * result + (label?.hashCode() ?: 0)
        result = HASH * result + (reactedBy?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String =
        "MonarchReactionBarItem(emoji=$emoji, count=$count, reacted=$reacted, label=$label, reactedBy=$reactedBy)"

    private companion object {
        const val HASH = 31
    }
}
