package dev.monarchui.components.intelligence.aierrormessage

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchAiErrorMessage`.
 * Every `null` keeps the built-in string `monarch_aierrormessage_<key>`.
 * The title and the message of the kind are replaced with the `title` and
 * `message` parameters.
 *
 * @property tone read by TalkBack before the title: "Warning" or "Error".
 * @property retry the retry action, "Try again".
 * @property retryIn the retry action while it counts down, from the seconds left: "Try again in 12s".
 * @property retryReady announced when the countdown ends, "You can try again now."
 * @property editPrompt the action that puts the failed prompt back in the input, "Edit message".
 * @property switchModel the action that switches the model, "Switch model".
 * @property newConversation the action that starts a new conversation, "New conversation".
 * @property summarize the action offered for a conversation that is too long, "Summarize and continue".
 * @property details the title of the technical details disclosure, "Technical details".
 */
@Immutable
public class MonarchAiErrorMessageLabels(
    public val tone: String? = null,
    public val retry: String? = null,
    public val retryIn: ((seconds: Int) -> String)? = null,
    public val retryReady: String? = null,
    public val editPrompt: String? = null,
    public val switchModel: String? = null,
    public val newConversation: String? = null,
    public val summarize: String? = null,
    public val details: String? = null,
)
