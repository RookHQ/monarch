package dev.monarchui.components.forms.taginput

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchTagInput`. Every
 * `null` keeps the built-in string `monarch_taginput_<key>`.
 *
 * @property duplicate the message when a tag is already there, given the tag ("Research is already added.").
 * @property maximum the message when no more tags fit, given the maximum ("You can add up to 5.").
 * @property added announced when a tag is added, given the tag ("Added Research.").
 * @property removed announced when a tag is removed, given the tag ("Removed Research.").
 * @property remove the TalkBack name of a removable tag, given the tag. `null` keeps the name of
 * `MonarchTag`, "Remove Research".
 * @property addSuggestion the TalkBack name of a suggestion button, given the suggestion ("Add Research").
 */
@Immutable
public class MonarchTagInputLabels(
    public val duplicate: ((String) -> String)? = null,
    public val maximum: ((Int) -> String)? = null,
    public val added: ((String) -> String)? = null,
    public val removed: ((String) -> String)? = null,
    public val remove: ((String) -> String)? = null,
    public val addSuggestion: ((String) -> String)? = null,
)
