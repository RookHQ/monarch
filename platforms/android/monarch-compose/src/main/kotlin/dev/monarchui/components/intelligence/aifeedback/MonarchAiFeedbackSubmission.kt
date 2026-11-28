package dev.monarchui.components.intelligence.aifeedback

import androidx.compose.runtime.Immutable
import dev.monarchui.components.intelligence.aiconversation.MonarchAiMessageFeedback

/**
 * What a `MonarchAiFeedback` sends: send it to the feedback API with the message id.
 *
 * @property value the rating.
 * @property reasons the ids of the chosen reasons, in the order they are listed.
 * @property comment the comment, trimmed; empty when none.
 */
@Immutable
public data class MonarchAiFeedbackSubmission(
    public val value: MonarchAiMessageFeedback,
    public val reasons: List<String>,
    public val comment: String,
)
