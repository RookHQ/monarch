package dev.monarchui.components.intelligence.aithread

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchAiThread`. Every `null`
 * keeps the built-in string `monarch_aithread_<key>`.
 *
 * @property label the TalkBack name of the list, "Conversation".
 * @property jumpToLatest the button that scrolls to the newest message, "Jump to latest".
 * @property newMessages the count on the jump button while messages arrived below, "2 new messages".
 * @property loadEarlier the button above the messages that loads older ones, "Load earlier messages".
 * @property loadingEarlier the state of that button while they load, "Loading earlier messages…".
 * @property thinking the thinking indicator's label while a reply is submitted, "Thinking…".
 * @property responseComplete announced when a reply finishes, "Response complete".
 * @property responseStopped announced when the person stops a reply, "Response stopped".
 * @property error announced for a conversation error that no message shows, given its message, "Error: …".
 * @property codeBlock read in place of a code block's contents, "Code block".
 * @property today the day label of messages sent today, "Today".
 * @property yesterday the day label of messages sent yesterday, "Yesterday".
 * @property offline the notice while the device is offline and nothing is queued.
 * @property offlineQueued the notice while offline with messages waiting, given how many.
 * @property sendingQueued the notice once the connection is back and queued messages are sent, given how many.
 * @property slow the notice while a running reply has sent nothing for a while.
 */
@Immutable
public class MonarchAiThreadLabels(
    public val label: String? = null,
    public val jumpToLatest: String? = null,
    public val newMessages: ((count: Int) -> String)? = null,
    public val loadEarlier: String? = null,
    public val loadingEarlier: String? = null,
    public val thinking: String? = null,
    public val responseComplete: String? = null,
    public val responseStopped: String? = null,
    public val error: ((message: String) -> String)? = null,
    public val codeBlock: String? = null,
    public val today: String? = null,
    public val yesterday: String? = null,
    public val offline: String? = null,
    public val offlineQueued: ((count: Int) -> String)? = null,
    public val sendingQueued: ((count: Int) -> String)? = null,
    public val slow: String? = null,
)
