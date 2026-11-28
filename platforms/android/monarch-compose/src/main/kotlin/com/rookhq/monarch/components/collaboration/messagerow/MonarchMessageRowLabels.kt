package com.rookhq.monarch.components.collaboration.messagerow

import androidx.compose.runtime.Immutable
import com.rookhq.monarch.components.collaboration.reactionbar.MonarchReactionBarLabels

/**
 * Replacements for the built-in strings of a `MonarchMessageRow`. Every
 * `null` keeps the built-in string `monarch_messagerow_<key>`.
 *
 * @property sending shown under a pending message: "Sending…".
 * @property failed shown under a failed message without an error message: "Not sent".
 * @property retry the retry button of a failed message.
 * @property discard the discard button of a failed message.
 * @property edited the edited marker: "(edited)".
 * @property editHistory what TalkBack reads for the edited marker when it opens the edit history.
 * @property deleted the body of a deleted message: "This message was deleted.".
 * @property replies the thread button or text, given the count: "1 reply", "4 replies".
 * @property actions what a long press does, read by TalkBack as the action: "Message actions".
 * @property reactions the strings of the reaction bar.
 */
@Immutable
public class MonarchMessageRowLabels(
    public val sending: String? = null,
    public val failed: String? = null,
    public val retry: String? = null,
    public val discard: String? = null,
    public val edited: String? = null,
    public val editHistory: String? = null,
    public val deleted: String? = null,
    public val replies: ((Int) -> String)? = null,
    public val actions: String? = null,
    public val reactions: MonarchReactionBarLabels = MonarchReactionBarLabels(),
)
