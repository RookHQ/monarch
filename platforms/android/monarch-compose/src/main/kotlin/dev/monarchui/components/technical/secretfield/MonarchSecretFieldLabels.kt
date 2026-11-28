package dev.monarchui.components.technical.secretfield

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchSecretField`. Every
 * `null` uses the built-in string named after it in `strings_secretfield.xml`.
 *
 * @property show writes the spoken name of the reveal toggle from the field's label: "Show API key".
 * @property showText the visible text of the toggle while the secret is masked: "Show".
 * @property hideText the visible text of the toggle while the secret is shown: "Hide".
 * @property copy the visible label of the copy button: "Copy". It is spoken with the field's label:
 * "Copy API key".
 * @property copied the label of the copy button just after a copy: "Copied".
 * @property copiedAnnouncement the state TalkBack reads after a copy: "Copied to clipboard".
 * @property savedPlaceholder the placeholder while a saved secret is not shown:
 * "Saved. Enter a new value to replace it".
 * @property savedDescription the supporting text while a saved secret is not shown:
 * "A value is saved and hidden. Leave blank to keep it."
 */
@Immutable
public class MonarchSecretFieldLabels(
    public val show: ((label: String) -> String)? = null,
    public val showText: String? = null,
    public val hideText: String? = null,
    public val copy: String? = null,
    public val copied: String? = null,
    public val copiedAnnouncement: String? = null,
    public val savedPlaceholder: String? = null,
    public val savedDescription: String? = null,
)
