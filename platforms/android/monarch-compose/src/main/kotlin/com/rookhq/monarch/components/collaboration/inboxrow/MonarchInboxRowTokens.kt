package com.rookhq.monarch.components.collaboration.inboxrow

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.internal.MonarchSurfaceColors
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** The colours of the text of a row. */
internal data class MonarchInboxRowTextColors(val title: Color, val age: Color, val preview: Color)

/** Component tokens of MonarchInboxRow, built from primitives and roles only. */
internal object MonarchInboxRowTokens {
    /** The space between the parts of a line. */
    val Gap: Dp = MonarchTokens.Space.space1_5.dp

    /** The width of the bar at the start of the open conversation's row. */
    val CurrentBarWidth: Dp = MonarchTokens.Space.space0_5.dp

    /** The side of the unread dot. */
    val DotSize: Dp = MonarchTokens.Space.space2.dp

    /** The horizontal padding of the unread count. */
    val CountPadding: Dp = MonarchTokens.Space.space1_5.dp

    /** The padding of the label behind a swiped row. */
    val SwipePadding: Dp = MonarchTokens.Space.space6.dp

    /** The weight of the title: semibold when unread, so unread does not depend on the dot's colour. */
    fun titleWeight(unread: Boolean): FontWeight = if (unread) FontWeight.SemiBold else FontWeight.Normal

    /** The title in the default text; the age in the default text when unread, muted otherwise. */
    fun textColors(unread: Boolean, colors: MonarchColors): MonarchInboxRowTextColors = MonarchInboxRowTextColors(
        title = colors.textDefault,
        age = if (unread) colors.textDefault else colors.textMuted,
        preview = colors.textMuted,
    )

    /** The background of the row: the surface, or the accent subtle colour while selected. */
    fun container(selected: Boolean, colors: MonarchColors): Color =
        if (selected) colors.accentSubtle else colors.bgSurface

    /** The unread dot and the count: the accent solid colour with its on-solid text. */
    fun markColors(colors: MonarchColors): MonarchSurfaceColors =
        MonarchSurfaceColors(colors.accentSolid, colors.accentOnSolid, null)

    /** Behind a swiped row: info for read, accent for archive, danger for delete. */
    fun swipeColors(action: MonarchInboxRowSwipeAction, colors: MonarchColors): MonarchSurfaceColors = when (action) {
        is MonarchInboxRowSwipeAction.ToggleRead -> MonarchSurfaceColors(colors.infoSolid, colors.infoOnSolid, null)
        is MonarchInboxRowSwipeAction.Archive -> MonarchSurfaceColors(colors.accentSolid, colors.accentOnSolid, null)
        is MonarchInboxRowSwipeAction.Delete -> MonarchSurfaceColors(colors.dangerSolid, colors.dangerOnSolid, null)
    }
}
