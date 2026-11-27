package com.rookhq.monarch.components.search.savedviews

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** Why a name cannot be used. */
internal enum class SavedViewsNameError { Required, Taken }

/** A group of views in the menu: its heading, or `null` for views without a group, and its views in order. */
internal data class SavedViewsGroup(val name: String?, val views: List<MonarchSavedView>)

/** The actions the menu offers for the current state, each only when its callback is given. */
internal data class SavedViewsActions(
    val saveChanges: Boolean,
    val saveAsNew: Boolean,
    val discard: Boolean,
    val rename: Boolean,
    val setDefault: Boolean,
    val delete: Boolean,
) {
    val any: Boolean get() = saveChanges || saveAsNew || discard || rename || setDefault || delete
}

/** Component tokens and logic of MonarchSavedViews, built from primitives and roles only. */
internal object MonarchSavedViewsTokens {
    val Gap: Dp = MonarchTokens.Space.space1_5.dp

    /** Views grouped in the order their groups first appear. */
    fun groups(views: List<MonarchSavedView>): List<SavedViewsGroup> =
        views.groupBy { it.group }.map { (name, members) -> SavedViewsGroup(name, members) }

    /**
     * Checks a name typed into the naming dialog: it must not be empty after
     * trimming, and no other view may have it, ignoring case and surrounding
     * spaces. Renaming a view to its own name in another case is allowed.
     */
    fun validate(name: String, views: List<MonarchSavedView>, renamingId: String?): SavedViewsNameError? {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return SavedViewsNameError.Required
        val taken = views.any { it.id != renamingId && it.name.trim().equals(trimmed, ignoreCase = true) }
        return if (taken) SavedViewsNameError.Taken else null
    }

    /** The actions offered for [current], given which callbacks the app passed. */
    @Suppress("LongParameterList")
    fun actions(
        current: MonarchSavedView?,
        modified: Boolean,
        canSaveChanges: Boolean,
        canSaveNew: Boolean,
        canDiscard: Boolean,
        canRename: Boolean,
        canSetDefault: Boolean,
        canDelete: Boolean,
    ): SavedViewsActions {
        val editable = current != null && !current.readOnly
        return SavedViewsActions(
            saveChanges = modified && editable && canSaveChanges,
            saveAsNew = canSaveNew,
            discard = modified && canDiscard,
            rename = editable && canRename,
            setDefault = current != null && !current.default && canSetDefault,
            delete = editable && canDelete,
        )
    }

    /** The second line of a view in the menu: its count, Shared and Default view. */
    fun details(view: MonarchSavedView, shared: String, defaultView: String): String? =
        listOfNotNull(view.count?.toString(), shared.takeIf { view.shared }, defaultView.takeIf { view.default })
            .joinToString(" · ")
            .ifEmpty { null }

    /** What TalkBack reads as the button's state: the view's name, then "Edited" when modified. */
    fun state(name: String, modified: String?): String = listOfNotNull(name, modified).joinToString(", ")

    /** The colour of the destructive Delete item. */
    fun dangerText(colors: MonarchColors): Color = colors.dangerStrong

    /** The colour of the count and of a view's second line. */
    fun secondaryText(colors: MonarchColors): Color = colors.textMuted
}
