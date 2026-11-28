package com.rookhq.monarch.components.intelligence.aipermissionscope

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** Component tokens and the pure rules of MonarchAiPermissionScope. */
internal object MonarchAiPermissionScopeTokens {
    /** The padding above and below a row. */
    val RowPadding: Dp = MonarchTokens.Space.space3.dp

    /** The gap between the app and its access, across and, once it wraps, down. */
    val Gap: Dp = MonarchTokens.Space.space2.dp

    /** The gap between the access icon and its text, and between the favicon and the app name. */
    val InnerGap: Dp = MonarchTokens.Space.space1_5.dp

    /** The list with one row changed to [level]; `null` when the row is missing or already at that level. */
    fun changed(
        scopes: List<MonarchAiPermissionScopeItem>,
        index: Int,
        level: MonarchAiAccessLevel,
    ): List<MonarchAiPermissionScopeItem>? {
        val current = scopes.getOrNull(index) ?: return null
        if (current.access == level) return null
        return scopes.mapIndexed { i, item -> if (i == index) item.copy(access = level) else item }
    }

    /** What TalkBack reads for the app of a row: the name, then the resources when there are some. */
    fun appDescription(item: MonarchAiPermissionScopeItem): String =
        listOfNotNull(item.app, item.resources?.takeIf { it.isNotBlank() }).joinToString(", ")

    /** The colour of a level's text on a read-only row: muted for no access, default otherwise. */
    fun accessColor(level: MonarchAiAccessLevel, colors: MonarchColors): Color =
        if (level == MonarchAiAccessLevel.None) colors.textMuted else colors.textDefault

    /** The colour of the resources line and of the empty text. */
    fun mutedColor(colors: MonarchColors): Color = colors.textMuted
}
