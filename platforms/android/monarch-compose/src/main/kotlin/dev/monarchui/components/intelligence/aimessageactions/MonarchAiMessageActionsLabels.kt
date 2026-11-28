package dev.monarchui.components.intelligence.aimessageactions

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchAiMessageActions`.
 * Every `null` keeps the built-in string `monarch_aimessageactions_<key>`.
 *
 * @property toolbar the spoken name of the row, "Message actions".
 * @property copy the Copy button, "Copy".
 * @property copied the Copy button after copying, "Copied".
 * @property regenerate the Regenerate button, "Regenerate".
 * @property edit the Edit button, "Edit".
 * @property good the thumbs-up toggle, "Good response".
 * @property bad the thumbs-down toggle, "Bad response".
 * @property share the Share button, "Share".
 * @property readAloud the Read aloud button, "Read aloud".
 * @property stopReading the Read aloud button while speaking, "Stop reading".
 * @property more the button that opens the menu of further actions, "More actions".
 */
@Immutable
public class MonarchAiMessageActionsLabels(
    public val toolbar: String? = null,
    public val copy: String? = null,
    public val copied: String? = null,
    public val regenerate: String? = null,
    public val edit: String? = null,
    public val good: String? = null,
    public val bad: String? = null,
    public val share: String? = null,
    public val readAloud: String? = null,
    public val stopReading: String? = null,
    public val more: String? = null,
)
