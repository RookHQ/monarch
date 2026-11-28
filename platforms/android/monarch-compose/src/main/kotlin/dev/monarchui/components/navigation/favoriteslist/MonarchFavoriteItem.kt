package dev.monarchui.components.navigation.favoriteslist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable

/**
 * One pinned destination of a `MonarchFavoritesList`. The app resolves the
 * name before passing it in and opens the destination itself.
 *
 * Two items are equal when their [id]s are equal.
 *
 * @property title the visible name of the destination.
 * @property id a stable, unique key, such as `project:42`.
 * @property icon a small leading icon. Decorative: the row is named by [title].
 * @property current the screen the person is on. The row is selected and set in a semibold weight.
 */
@Immutable
public class MonarchFavoriteItem(
    public val title: String,
    public val id: String,
    public val icon: (@Composable () -> Unit)? = null,
    public val current: Boolean = false,
) {
    override fun equals(other: Any?): Boolean = other is MonarchFavoriteItem && other.id == id

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String = "MonarchFavoriteItem(id=$id, title=$title, current=$current)"
}
