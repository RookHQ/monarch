package dev.monarchui.components.intelligence.aiconversationheader

import androidx.compose.runtime.Immutable
import dev.monarchui.components.forms.inlineedittext.MonarchInlineEditTextLabels

/**
 * Replacements for the built-in strings of `MonarchAiConversationHeader`.
 * Every `null` keeps the built-in text.
 *
 * @property untitled shown while the title is empty. Default "New conversation".
 * @property rename the action TalkBack reads on the title. Default "Rename conversation".
 * @property titleLabel the label of the rename field. Default "Conversation title".
 * @property naming shown while the title is being generated. Default "Naming conversation…".
 * @property agent what TalkBack reads for the assistant; receives its name. Default "AI assistant: Name".
 * @property actions the name of the group of actions. Default "Conversation actions".
 * @property renameFailed shown under the rename field when renaming failed. Default "Couldn't rename. Try again.".
 * @property inlineEdit replacements for the strings of the rename editor (Save, Cancel, Saving).
 */
@Immutable
public class MonarchAiConversationHeaderLabels(
    public val untitled: String? = null,
    public val rename: String? = null,
    public val titleLabel: String? = null,
    public val naming: String? = null,
    public val agent: ((name: String) -> String)? = null,
    public val actions: String? = null,
    public val renameFailed: String? = null,
    public val inlineEdit: MonarchInlineEditTextLabels = MonarchInlineEditTextLabels(),
)
