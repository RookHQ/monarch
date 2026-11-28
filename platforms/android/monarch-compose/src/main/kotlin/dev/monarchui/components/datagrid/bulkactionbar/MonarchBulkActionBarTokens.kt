package dev.monarchui.components.datagrid.bulkactionbar

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** The actions shown as buttons and those in the More menu. */
internal data class BulkActionBarSplit(val visible: List<MonarchBulkAction>, val overflow: List<MonarchBulkAction>)

/** Component tokens and rules of MonarchBulkActionBar, built from primitives and roles only. */
internal object MonarchBulkActionBarTokens {
    val Gap: Dp = MonarchTokens.Space.space2.dp
    val HorizontalPadding: Dp = MonarchTokens.Space.space3.dp
    val VerticalPadding: Dp = MonarchTokens.Space.space2.dp
    val BorderWidth: Dp = MonarchTokens.Space.px.dp

    /** The first [maxVisible] actions are buttons, the rest go into the More menu. A negative value counts as 0. */
    fun split(actions: List<MonarchBulkAction>, maxVisible: Int): BulkActionBarSplit {
        val visible = maxVisible.coerceAtLeast(0)
        return BulkActionBarSplit(actions.take(visible), actions.drop(visible))
    }

    /** Whether something runs, so that every other control is blocked. */
    fun blocked(busy: Boolean, busyActionId: String?): Boolean = busy || busyActionId != null

    /** Whether [action] can be pressed: its own state, and nothing else runs. The running action stays enabled. */
    fun actionEnabled(action: MonarchBulkAction, busy: Boolean, busyActionId: String?): Boolean = when {
        busy -> false
        busyActionId == null -> action.enabled
        else -> action.id == busyActionId && action.enabled
    }

    /** The button variant of an action. */
    fun variant(action: MonarchBulkAction): MonarchButtonVariant =
        if (action.destructive) MonarchButtonVariant.Danger else MonarchButtonVariant.Secondary

    /** What to announce when the count goes from [previous] to [current]: the count, "cleared", or nothing. */
    fun announcement(previous: Int, current: Int): BulkActionBarAnnouncement = when {
        current == previous -> BulkActionBarAnnouncement.None
        current > 0 -> BulkActionBarAnnouncement.Count
        previous > 0 -> BulkActionBarAnnouncement.Cleared
        else -> BulkActionBarAnnouncement.None
    }

    /** The raised surface of the bar. */
    fun container(colors: MonarchColors): Color = colors.bgRaised

    /** The outline of the bar. */
    fun borderColor(colors: MonarchColors): Color = colors.borderSubtle

    /** The count is strong default text. */
    fun countText(colors: MonarchColors): Color = colors.textDefault
}

/** What a change of the count announces. */
internal enum class BulkActionBarAnnouncement { None, Count, Cleared }
