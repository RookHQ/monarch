package dev.monarchui.components.intelligence.aipromptinput

import androidx.compose.runtime.Immutable
import dev.monarchui.components.intelligence.aiattachmenttray.MonarchAiAttachmentTrayLabels

/**
 * Replacements for the built-in strings of a `MonarchAiPromptInput`.
 * Every `null` keeps the built-in string `monarch_aipromptinput_<key>`.
 *
 * @property placeholder shown while the text is empty: "Send a message…".
 * @property send the Send button: "Send".
 * @property stop the Stop button shown while a response runs: "Stop".
 * @property attach the attach button: "Attach files".
 * @property dropHint shown over the box while files are dragged onto it: "Drop files to attach".
 * @property counter the visible counter, given the count and the limit: "120 / 4000".
 * @property counterLabel what TalkBack reads for the counter: "120 of 4000 characters".
 * @property counterWarning announced once when the count reaches the warning share, given the characters
 * left: "12 characters left".
 * @property counterOver announced once when the count goes over the limit, and set as the field's error,
 * given the excess: "3 characters over the limit".
 * @property tokens the token estimate, given the count: "≈ 120 tokens".
 * @property attachments the strings of the attachment tray.
 */
@Immutable
public class MonarchAiPromptInputLabels(
    public val placeholder: String? = null,
    public val send: String? = null,
    public val stop: String? = null,
    public val attach: String? = null,
    public val dropHint: String? = null,
    public val counter: ((count: Int, max: Int) -> String)? = null,
    public val counterLabel: ((count: Int, max: Int) -> String)? = null,
    public val counterWarning: ((remaining: Int) -> String)? = null,
    public val counterOver: ((over: Int) -> String)? = null,
    public val tokens: ((count: Int) -> String)? = null,
    public val attachments: MonarchAiAttachmentTrayLabels = MonarchAiAttachmentTrayLabels(),
)
