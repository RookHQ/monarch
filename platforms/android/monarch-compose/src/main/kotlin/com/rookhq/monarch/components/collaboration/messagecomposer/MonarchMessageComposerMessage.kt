package com.rookhq.monarch.components.collaboration.messagecomposer

import androidx.compose.runtime.Immutable
import com.rookhq.monarch.components.collaboration.mentionpicker.MonarchMentionItem

/**
 * What `MonarchMessageComposer` sends.
 *
 * @property text the trimmed plain text, with mentions written as @Label. Empty when only attachments are sent.
 * @property mentions the picked mentions whose @Label is still in [text], each once.
 * @property mode the value of the selected mode, when the composer has modes.
 */
@Immutable
public class MonarchMessageComposerMessage(
    public val text: String,
    public val mentions: List<MonarchMentionItem> = emptyList(),
    public val mode: String? = null,
)
