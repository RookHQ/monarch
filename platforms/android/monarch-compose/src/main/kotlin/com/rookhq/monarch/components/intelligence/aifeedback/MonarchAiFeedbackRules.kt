package com.rookhq.monarch.components.intelligence.aifeedback

import com.rookhq.monarch.components.forms.checkboxgroup.MonarchCheckboxGroupOption
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiMessageFeedback

/** Where the feedback flow is. */
internal enum class AiFeedbackPhase { Idle, Details, Sending, Thanks }

/** The rating and submission logic of MonarchAiFeedback, as pure functions. */
internal object MonarchAiFeedbackRules {
    /** The rating after pressing [pressed]: pressing the current thumb again clears it. */
    fun next(current: MonarchAiMessageFeedback?, pressed: MonarchAiMessageFeedback): MonarchAiMessageFeedback? =
        if (current == pressed) null else pressed

    /** Whether [value] opens the sheet. */
    fun wantsDetails(details: MonarchAiFeedbackDetails, value: MonarchAiMessageFeedback): Boolean = when (details) {
        MonarchAiFeedbackDetails.Both -> true
        MonarchAiFeedbackDetails.Down -> value == MonarchAiMessageFeedback.Down
        MonarchAiFeedbackDetails.None -> false
    }

    /** The reasons offered for [value]: the app's map, or [defaults] after a thumbs down and none after a thumbs up. */
    fun reasonsFor(
        reasons: Map<MonarchAiMessageFeedback, List<MonarchCheckboxGroupOption<String>>>?,
        value: MonarchAiMessageFeedback,
        defaults: List<MonarchCheckboxGroupOption<String>>,
    ): List<MonarchCheckboxGroupOption<String>> = when {
        reasons != null -> reasons[value].orEmpty()
        value == MonarchAiMessageFeedback.Down -> defaults
        else -> emptyList()
    }

    /** The submission: the chosen reasons in listed order, and the trimmed comment. */
    fun submission(
        value: MonarchAiMessageFeedback,
        offered: List<MonarchCheckboxGroupOption<String>>,
        chosen: List<String>,
        comment: String,
    ): MonarchAiFeedbackSubmission =
        MonarchAiFeedbackSubmission(value, offered.map { it.value }.filter { it in chosen }, comment.trim())

    /** The phase after a rating, or `null` when the rating is sent at once. */
    fun afterRating(next: MonarchAiMessageFeedback?, details: MonarchAiFeedbackDetails): AiFeedbackPhase? = when {
        next == null -> AiFeedbackPhase.Idle
        wantsDetails(details, next) -> AiFeedbackPhase.Details
        else -> null
    }

    /** The phase after sending: the thank-you, or back to the sheet or the thumbs after a failure. */
    fun afterSend(succeeded: Boolean, fromSheet: Boolean): AiFeedbackPhase = when {
        succeeded -> AiFeedbackPhase.Thanks
        fromSheet -> AiFeedbackPhase.Details
        else -> AiFeedbackPhase.Idle
    }

    /** What shows under the thumbs: `true` the error, `false` the thank-you, `null` nothing. */
    fun message(phase: AiFeedbackPhase, failed: Boolean): Boolean? = when {
        failed && phase != AiFeedbackPhase.Details -> true
        phase == AiFeedbackPhase.Thanks -> false
        else -> null
    }

    /** Whether the sheet shows: while asking for details, and while sending from it. */
    fun showsSheet(
        phase: AiFeedbackPhase,
        value: MonarchAiMessageFeedback,
        details: MonarchAiFeedbackDetails,
    ): Boolean =
        phase == AiFeedbackPhase.Details || (phase == AiFeedbackPhase.Sending && wantsDetails(details, value))
}
