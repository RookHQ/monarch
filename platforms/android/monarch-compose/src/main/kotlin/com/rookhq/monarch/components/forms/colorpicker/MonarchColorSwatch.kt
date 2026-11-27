package com.rookhq.monarch.components.forms.colorpicker

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * A preset colour of a `MonarchColorPicker`: a hex value and a name that
 * TalkBack reads, for example "Blue".
 *
 * Create one with [of], which returns `null` for text that is not a hex
 * colour, so a list built with `listOfNotNull` skips it.
 *
 * @property hex the colour as lower-case `#rrggbb`.
 * @property name the name of the colour, already localised.
 */
@Immutable
public class MonarchColorSwatch private constructor(public val hex: String, public val name: String) {
    /** The colour to draw. Swatch colours are the app's data, not theme roles. */
    public val color: Color get() = checkNotNull(color(hex))

    override fun equals(other: Any?): Boolean = other is MonarchColorSwatch && other.hex == hex && other.name == name

    override fun hashCode(): Int = java.util.Objects.hash(hex, name)

    override fun toString(): String = "MonarchColorSwatch($hex, $name)"

    /** Creates swatches and reads hex colours. */
    public companion object {
        private val shortHex = Regex("^[0-9a-fA-F]{3}$")
        private val longHex = Regex("^[0-9a-fA-F]{6}$")
        private const val CHANNEL_BITS = 8
        private const val CHANNEL_MASK = 0xFF
        private const val HEX_RADIX = 16

        /**
         * A swatch, or `null` when [hex] is not a hex colour.
         *
         * @param hex `#rgb`, `#rrggbb`, `rgb` or `rrggbb`, in any case.
         * @param name the name TalkBack reads, already localised.
         */
        public fun of(hex: String, name: String): MonarchColorSwatch? =
            normalizedHex(hex)?.let { MonarchColorSwatch(it, name) }

        /**
         * Reads `#rgb`, `#rrggbb`, `rgb` or `rrggbb` (any case, surrounding
         * spaces) and returns lower-case `#rrggbb`, or `null` when the text
         * is not a hex colour. The same rule as the web `normalizeHex`.
         */
        public fun normalizedHex(text: String): String? {
            val digits = text.trim().removePrefix("#")
            return when {
                shortHex.matches(digits) -> "#" + digits.map { "$it$it" }.joinToString("").lowercase()
                longHex.matches(digits) -> "#" + digits.lowercase()
                else -> null
            }
        }

        /** The colour of a hex text, or `null` when it is not a hex colour. */
        public fun color(hex: String): Color? {
            val value = normalizedHex(hex)?.substring(1)?.toInt(HEX_RADIX) ?: return null
            return Color(
                red = value shr (CHANNEL_BITS * 2) and CHANNEL_MASK,
                green = value shr CHANNEL_BITS and CHANNEL_MASK,
                blue = value and CHANNEL_MASK,
            )
        }
    }
}
