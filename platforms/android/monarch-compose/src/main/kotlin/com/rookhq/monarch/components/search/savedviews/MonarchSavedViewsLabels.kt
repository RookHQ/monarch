package com.rookhq.monarch.components.search.savedviews

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchSavedViews`. Every
 * `null` keeps the built-in string `monarch_savedviews_<key>`.
 *
 * @property view the name of the menu button for TalkBack, "View".
 * @property noView the button's text when no view is selected, "All items".
 * @property modified the marker of unsaved changes, "Edited".
 * @property shared the marker of a shared view, "Shared".
 * @property defaultView the marker of the default view, "Default view".
 * @property views the menu heading above views without a group, "Views".
 * @property saveChanges the menu item that saves over the selected view: "Save changes to “Open deals”".
 * @property saveAsNew the menu item that saves a new view, "Save as new view…".
 * @property discardChanges the menu item that reapplies the selected view, "Discard changes".
 * @property rename the menu item that renames the selected view, "Rename…".
 * @property setDefault the menu item that makes the selected view the default, "Set as default".
 * @property delete the menu item that deletes the selected view, "Delete view…".
 * @property saveTitle the title of the dialog that names a new view, "Save view".
 * @property renameTitle the title of the dialog that renames a view, "Rename view".
 * @property name the label of the name field, "Name".
 * @property save the confirm button of the naming dialog, "Save".
 * @property cancel the cancel button of both dialogs, "Cancel".
 * @property nameRequired the error for an empty name, "Enter a name.".
 * @property nameTaken the error for a name another view has, "A view with this name already exists.".
 * @property saveFailed the error when saving or renaming throws, "The view could not be saved. Try again.".
 * @property deleteTitle the title of the delete confirmation: "Delete “Open deals”?".
 * @property deleteMessage the message of the delete confirmation.
 * @property deleteConfirm the confirm button of the delete confirmation, "Delete view".
 * @property deleteFailed the error when deleting throws, "The view could not be deleted. Try again.".
 */
@Immutable
public class MonarchSavedViewsLabels(
    public val view: String? = null,
    public val noView: String? = null,
    public val modified: String? = null,
    public val shared: String? = null,
    public val defaultView: String? = null,
    public val views: String? = null,
    public val saveChanges: ((String) -> String)? = null,
    public val saveAsNew: String? = null,
    public val discardChanges: String? = null,
    public val rename: String? = null,
    public val setDefault: String? = null,
    public val delete: String? = null,
    public val saveTitle: String? = null,
    public val renameTitle: String? = null,
    public val name: String? = null,
    public val save: String? = null,
    public val cancel: String? = null,
    public val nameRequired: String? = null,
    public val nameTaken: String? = null,
    public val saveFailed: String? = null,
    public val deleteTitle: ((String) -> String)? = null,
    public val deleteMessage: String? = null,
    public val deleteConfirm: String? = null,
    public val deleteFailed: String? = null,
)
