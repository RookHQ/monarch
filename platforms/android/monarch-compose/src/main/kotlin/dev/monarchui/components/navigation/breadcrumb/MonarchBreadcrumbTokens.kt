package dev.monarchui.components.navigation.breadcrumb

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** Component tokens and the pure logic of MonarchBreadcrumb. */
internal object MonarchBreadcrumbTokens {
    /** Stands for the collapsed levels in [visible]. */
    const val MORE: Int = -1

    /** The fewest levels shown: the first, the menu and the last. */
    const val MIN_VISIBLE: Int = 3

    /** The visible text of the collapsed-levels button; TalkBack reads its name instead. */
    const val ELLIPSIS: String = "\u2026"

    /**
     * The default separator, a single angle quotation mark. It is a mirrored character, so the text layout
     * draws it pointing the other way in right-to-left text.
     */
    const val SEPARATOR: String = "\u203A"

    /** How much of the ellipsis button's 48dp reserve each side gives back to the row. */
    val MoreOverhang: Dp = MonarchTokens.Space.space3.dp

    /** The gap between a level and a separator. */
    val Gap: Dp = MonarchTokens.Space.space1.dp

    /**
     * The indices of the levels shown, in order, with [MORE] where the collapsed levels are. Above
     * [maxVisible] the first level stays, the middle ones collapse and the last `maxVisible - 2` stay.
     */
    fun visible(count: Int, maxVisible: Int): List<Int> {
        val max = maxVisible.coerceAtLeast(MIN_VISIBLE)
        if (count <= max) return List(count) { it }
        val tailStart = count - (max - 2)
        return listOf(0, MORE) + (tailStart until count)
    }

    /** The indices of the collapsed levels: empty when nothing collapses. */
    fun hidden(count: Int, maxVisible: Int): List<Int> {
        val max = maxVisible.coerceAtLeast(MIN_VISIBLE)
        if (count <= max) return emptyList()
        return (1 until count - (max - 2)).toList()
    }

    /** Whether the level at [index] is a link: not the last one, and only when it is linked. */
    fun isLink(index: Int, count: Int, linked: Boolean): Boolean = index != count - 1 && linked

    /** The colour of a level that is not a link: the default text for the current level, muted otherwise. */
    fun textColor(current: Boolean, enabled: Boolean, colors: MonarchColors): Color = when {
        !enabled -> colors.textDisabled
        current -> colors.textDefault
        else -> colors.textMuted
    }

    /** The colour of the separators. */
    fun separatorColor(colors: MonarchColors): Color = colors.textSubtle
}
