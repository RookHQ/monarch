package dev.monarchui.components.collaboration.presenceavatars

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchPresenceAvatars`.
 * Every `null` keeps the built-in string `monarch_presenceavatars_<key>`.
 *
 * @property label names the stack, given the number of people: "5 people here".
 * @property showAll what the stack does when it opens the list: "Show everyone here".
 * @property list the title of the list: "Everyone here".
 * @property done the button that closes the list.
 * @property more what TalkBack reads for the "+N" tile, given the number hidden: "2 more".
 * @property person the text of one person, given the name, the presence (with the custom status) and the
 * activity or `null`: "Ana Silva, Online, Editing".
 */
@Immutable
public class MonarchPresenceAvatarsLabels(
    public val label: ((Int) -> String)? = null,
    public val showAll: String? = null,
    public val list: String? = null,
    public val done: String? = null,
    public val more: ((Int) -> String)? = null,
    public val person: ((name: String, presence: String, activity: String?) -> String)? = null,
)
