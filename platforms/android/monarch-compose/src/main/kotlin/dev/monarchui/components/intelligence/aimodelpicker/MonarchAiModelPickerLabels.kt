package dev.monarchui.components.intelligence.aimodelpicker

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchAiModelPicker`. Every
 * `null` keeps the built-in string `monarch_aimodelpicker_<key>`.
 *
 * @property label the TalkBack name of the button and the heading of the sheet, "Model".
 * @property placeholder the button text when nothing is selected, "Choose a model".
 * @property capability the badge text of a capability. Return `null` to keep the built-in text.
 * @property context the context window, from the compact number: "200K context".
 * @property speed the text of a speed. Return `null` to keep the built-in text.
 * @property cost what TalkBack reads for a cost tier from 1 to 3, such as "Cost: high".
 * @property ownKey the badge of a model on a configured workspace key, "Your key".
 * @property keyNeeded the badge of a model whose key is missing, "Key needed".
 * @property addKeyHint read on a model whose key is missing when choosing it adds the key.
 * @property unavailable read on a model that cannot be chosen, "Unavailable".
 * @property done the button that closes the sheet, "Done".
 */
@Immutable
public class MonarchAiModelPickerLabels(
    public val label: String? = null,
    public val placeholder: String? = null,
    public val capability: ((MonarchAiModelCapability) -> String?)? = null,
    public val context: ((formatted: String) -> String)? = null,
    public val speed: ((MonarchAiModelSpeed) -> String?)? = null,
    public val cost: ((tier: Int) -> String)? = null,
    public val ownKey: String? = null,
    public val keyNeeded: String? = null,
    public val addKeyHint: String? = null,
    public val unavailable: String? = null,
    public val done: String? = null,
)
