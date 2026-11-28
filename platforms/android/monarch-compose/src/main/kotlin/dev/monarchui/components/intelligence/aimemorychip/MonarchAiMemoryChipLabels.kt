package dev.monarchui.components.intelligence.aimemorychip

import androidx.compose.runtime.Immutable
import dev.monarchui.components.intelligence.aiconversation.MonarchAiMemoryAction

/**
 * Replacements for the built-in strings of a `MonarchAiMemoryChip`. Every
 * `null` keeps the built-in string `monarch_aimemorychip_<key>`.
 *
 * @property prefix the prefix of each action: "Used memory:", "Remembered:", "Forgotten:".
 * @property options what TalkBack reads as the tap action of the row variant, "show options".
 * @property edit the menu item and the TalkBack action that edit the memory, "Edit".
 * @property forget the menu item and the TalkBack action that forget the memory, "Forget".
 * @property editTitle the title of the edit dialog, "Edit memory".
 * @property editField the label of the text field in the edit dialog, "Memory".
 * @property save the button that saves the edit, "Save".
 * @property cancel the button that closes the edit dialog, "Cancel".
 */
@Immutable
public class MonarchAiMemoryChipLabels(
    public val prefix: ((action: MonarchAiMemoryAction) -> String)? = null,
    public val options: String? = null,
    public val edit: String? = null,
    public val forget: String? = null,
    public val editTitle: String? = null,
    public val editField: String? = null,
    public val save: String? = null,
    public val cancel: String? = null,
)
