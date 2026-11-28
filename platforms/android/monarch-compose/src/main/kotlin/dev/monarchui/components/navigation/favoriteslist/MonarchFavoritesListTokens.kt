package dev.monarchui.components.navigation.favoriteslist

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** The colours of one row of a MonarchFavoritesList. */
internal class MonarchFavoritesListRowColors(val container: Color, val content: Color)

/** Component tokens and the pure logic of MonarchFavoritesList. */
internal object MonarchFavoritesListTokens {
    /** The space between the heading and the rows, and between the heading's icon and title. */
    val Gap: Dp = MonarchTokens.Space.space2.dp

    /** The space at the start and end of the heading, lined up with the text of the rows. */
    val HeadingPadding: Dp = MonarchTokens.Space.space4.dp

    /** The size of the heading's icon. */
    val HeadingIconSize: Dp = MonarchTokens.Space.space4.dp

    /** The tallest the reorderable list grows in edit mode before it scrolls inside itself. */
    val EditMaxHeight: Dp = (MonarchTokens.Space.space24 * 4).dp

    /** The colours of a row: the current one on the subtle accent fill in default text, the rest in muted text. */
    fun rowColors(current: Boolean, colors: MonarchColors): MonarchFavoritesListRowColors = if (current) {
        MonarchFavoritesListRowColors(container = colors.accentSubtle, content = colors.textDefault)
    } else {
        MonarchFavoritesListRowColors(container = Color.Transparent, content = colors.textMuted)
    }

    /** The colour of the heading and the empty message. */
    fun mutedColor(colors: MonarchColors): Color = colors.textMuted

    /** The weight of a row's name: medium for the current destination, so it is not shown by colour alone. */
    fun titleWeight(current: Boolean): FontWeight = if (current) FontWeight.Medium else FontWeight.Normal

    /** Whether the list has an edit mode: only when rows can be removed or reordered. */
    fun editable(canRemove: Boolean, canReorder: Boolean): Boolean = canRemove || canReorder

    /** Whether edit mode is on: it was asked for, the list is editable and has items. */
    fun inEdit(editable: Boolean, requested: Boolean, count: Int): Boolean = editable && requested && count > 0

    /** [items] with the item at [from] moved to [to]. */
    fun <T> moved(items: List<T>, from: Int, to: Int): List<T> {
        if (from !in items.indices || to !in items.indices || from == to) return items
        return items.toMutableList().apply { add(to, removeAt(from)) }
    }
}
