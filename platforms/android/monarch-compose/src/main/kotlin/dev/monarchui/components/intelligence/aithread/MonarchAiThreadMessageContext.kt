package dev.monarchui.components.intelligence.aithread

import androidx.compose.runtime.Immutable

/**
 * Where a message sits in a `MonarchAiThread`, given to the thread's `message` content.
 *
 * @property index the message's position in the messages, oldest first.
 * @property isLast whether it is the newest message.
 * @property announces whether the message should announce its own streamed text and errors. `false` while the
 * thread announces for the whole conversation, so pass it to `MonarchAiMessage` as `announces`.
 */
@Immutable
public class MonarchAiThreadMessageContext(
    public val index: Int,
    public val isLast: Boolean,
    public val announces: Boolean,
)
