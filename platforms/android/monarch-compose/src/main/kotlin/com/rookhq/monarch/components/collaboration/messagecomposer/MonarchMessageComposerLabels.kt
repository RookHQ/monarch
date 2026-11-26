package com.rookhq.monarch.components.collaboration.messagecomposer

import androidx.compose.runtime.Immutable
import com.rookhq.monarch.components.collaboration.mentionpicker.MonarchMentionPickerLabels

/**
 * Replacements for the built-in strings of a `MonarchMessageComposer`.
 * Every `null` keeps the built-in string `monarch_messagecomposer_<key>`.
 *
 * @property send the send button: "Send".
 * @property save the send button while editing: "Save".
 * @property sending what the send button reads while sending: "Sending…".
 * @property cancel the button that leaves editing: "Cancel".
 * @property attach the attach button: "Attach files".
 * @property emoji the emoji button: "Add emoji".
 * @property modes the name of the mode buttons: "Message type".
 * @property replyingTo the reply context, given the author: "Replying to Ana Ruiz".
 * @property dismissReply the button that removes the reply context: "Stop replying".
 * @property editing the edit context: "Editing message".
 * @property remaining what TalkBack reads for the count, given the characters left (negative when over the
 * limit): "12 characters left", "3 characters over the limit".
 * @property draftSaving the draft status while saving: "Saving draft…".
 * @property draftSaved the draft status once saved: "Draft saved".
 * @property draftError the draft status when saving failed: "Draft not saved".
 * @property mentionPicker the strings of the mention suggestions.
 */
@Immutable
public class MonarchMessageComposerLabels(
    public val send: String? = null,
    public val save: String? = null,
    public val sending: String? = null,
    public val cancel: String? = null,
    public val attach: String? = null,
    public val emoji: String? = null,
    public val modes: String? = null,
    public val replyingTo: ((String) -> String)? = null,
    public val dismissReply: String? = null,
    public val editing: String? = null,
    public val remaining: ((Int) -> String)? = null,
    public val draftSaving: String? = null,
    public val draftSaved: String? = null,
    public val draftError: String? = null,
    public val mentionPicker: MonarchMentionPickerLabels = MonarchMentionPickerLabels(),
)
