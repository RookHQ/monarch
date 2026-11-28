package dev.monarchui.components.identity.avatar

import java.text.BreakIterator

/** The text logic of MonarchAvatar and MonarchAvatarGroup: initials, the colour seed and the visible count. */
internal object MonarchAvatarLogic {
    /** The number of fallback colour pairs. */
    const val PALETTE_COUNT: Int = 5

    private const val HASH_MULTIPLIER: Int = 31
    private const val HASH_MODULO: Int = 360

    /**
     * The palette index of a seed. The same seed gives the same colour on
     * every platform: the web's hash over UTF-16 units, modulo 360, then
     * modulo the number of pairs.
     */
    fun paletteIndex(seed: String): Int = hue(seed) % PALETTE_COUNT

    /** The web's per-name hue, 0 to 359: the hash over UTF-16 units, modulo 360. */
    fun hue(seed: String): Int {
        var hash = 0
        seed.forEach { unit -> hash = (hash * HASH_MULTIPLIER + unit.code) % HASH_MODULO }
        return hash
    }

    /**
     * The initials of a name: the first character of the first and the last
     * word, in upper case. Whole characters are kept, so an emoji or an
     * accented letter is never cut in half. Empty when the name has no words.
     */
    fun initials(name: String): String {
        val all = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        // A note in brackets after the name, such as "(you)", is not part of the name.
        val named = all.filterNot { it.startsWith("(") && it.endsWith(")") }
        val words = named.ifEmpty { all }
        if (words.isEmpty()) return ""
        val first = firstCharacter(words.first())
        val last = if (words.size > 1) firstCharacter(words.last()) else ""
        return (first + last).uppercase()
    }

    /** Whether initials can be shown: they contain a letter or a digit. */
    fun hasReadableInitials(initials: String): Boolean {
        var index = 0
        while (index < initials.length) {
            val codePoint = initials.codePointAt(index)
            if (Character.isLetterOrDigit(codePoint)) return true
            index += Character.charCount(codePoint)
        }
        return false
    }

    /**
     * How many avatars a group shows. A "+1" tile would take the place of the
     * avatar it hides, so that avatar is shown instead.
     */
    fun visibleCount(total: Int, maxVisible: Int?): Int {
        if (maxVisible == null) return total
        val limit = maxVisible.coerceAtLeast(0)
        return if (total - limit == 1) total else minOf(total, limit)
    }

    private fun firstCharacter(word: String): String {
        val iterator = BreakIterator.getCharacterInstance()
        iterator.setText(word)
        val end = iterator.next()
        return if (end == BreakIterator.DONE) word else word.substring(0, end)
    }
}
