package com.rookhq.monarch.components.collaboration.messagecomposer

import androidx.compose.runtime.Immutable

/**
 * One message type of a `MonarchMessageComposer`, such as a public reply or
 * an internal note, offered as a segmented button above the box.
 *
 * @property value the value passed to `onModeChange` and sent with the message.
 * @property label the segment's text, already localized.
 * @property note gives the box the warning look of a private note. The segment's label says it too.
 * @property placeholder the placeholder while this mode is selected.
 * @property sendLabel the send button's text while this mode is selected, such as "Add note".
 */
@Immutable
public class MonarchMessageComposerMode(
    public val value: String,
    public val label: String,
    public val note: Boolean = false,
    public val placeholder: String? = null,
    public val sendLabel: String? = null,
)
