package dev.monarchui.components.intelligence.aifeedback

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchAiFeedback`. Every
 * `null` keeps the built-in string `monarch_aifeedback_<key>`.
 *
 * @property group the name of the thumbs and the title of the sheet, "Rate this response".
 * @property good the name of thumbs up, "Good response".
 * @property bad the name of thumbs down, "Bad response".
 * @property reasonsDown the question over the reasons after a thumbs down, "What went wrong?".
 * @property reasonsUp the question over the reasons after a thumbs up, "What went well?".
 * @property comment the label of the comment field, "Tell us more (optional)".
 * @property commentPlaceholder the placeholder of the comment field.
 * @property submit the send button, "Send feedback".
 * @property cancel the button that closes the sheet without sending.
 * @property thanks shown and announced after sending, "Thanks for your feedback.".
 * @property error shown and announced when sending failed.
 */
@Immutable
public class MonarchAiFeedbackLabels(
    public val group: String? = null,
    public val good: String? = null,
    public val bad: String? = null,
    public val reasonsDown: String? = null,
    public val reasonsUp: String? = null,
    public val comment: String? = null,
    public val commentPlaceholder: String? = null,
    public val submit: String? = null,
    public val cancel: String? = null,
    public val thanks: String? = null,
    public val error: String? = null,
)
