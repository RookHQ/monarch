package dev.monarchui.components.intelligence.aiconversationlist

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchAiConversationList`. Every `null` uses the built-in string
 * named after it in `strings_aiconversationlist.xml`.
 *
 * @property label the TalkBack name of the list: "Conversation history".
 * @property group the heading of a group: "Pinned", "Today", "Previous 7 days", "Older".
 * @property untitled the title of a conversation without one: "New conversation".
 * @property newConversation the button that calls `onNew`: "New conversation".
 * @property search the placeholder of the search field: "Search conversations".
 * @property empty the empty state without conversations: "No conversations yet".
 * @property noResults the empty state of a search, from the search text: "No conversations match “x”".
 * @property results announced when a local search changes, from the number found: "3 conversations".
 * @property rename the action that renames: "Rename".
 * @property pin the action that pins: "Pin".
 * @property unpin the action that unpins: "Unpin".
 * @property delete the action that deletes: "Delete".
 * @property moreActions the name of the menu button, from the title: "More actions for Trip plan".
 * @property renameTitle the title of the rename dialog: "Rename conversation".
 * @property renameField the label of its field: "Conversation title".
 * @property save the button that saves the title: "Save".
 * @property cancel the button that closes a dialog: "Cancel".
 * @property deleteTitle the title of the delete dialog, from the title: "Delete “Trip plan”?".
 * @property deleteMessage the text of the delete dialog.
 * @property deleteConfirm the button that deletes: "Delete conversation".
 * @property showMore the button that shows more rows, from the number hidden: "Show 12 more conversations".
 */
@Immutable
public class MonarchAiConversationListLabels(
    public val label: String? = null,
    public val group: ((MonarchAiConversationGroup) -> String)? = null,
    public val untitled: String? = null,
    public val newConversation: String? = null,
    public val search: String? = null,
    public val empty: String? = null,
    public val noResults: ((query: String) -> String)? = null,
    public val results: ((count: Int) -> String)? = null,
    public val rename: String? = null,
    public val pin: String? = null,
    public val unpin: String? = null,
    public val delete: String? = null,
    public val moreActions: ((title: String) -> String)? = null,
    public val renameTitle: String? = null,
    public val renameField: String? = null,
    public val save: String? = null,
    public val cancel: String? = null,
    public val deleteTitle: ((title: String) -> String)? = null,
    public val deleteMessage: String? = null,
    public val deleteConfirm: String? = null,
    public val showMore: ((count: Int) -> String)? = null,
)
