package com.rookhq.monarch.components.forms.autosaveindicator

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.rookhq.monarch.components.feedback.savestatusindicator.MonarchSaveStatus
import com.rookhq.monarch.components.feedback.savestatusindicator.MonarchSaveStatusIndicator
import com.rookhq.monarch.components.feedback.savestatusindicator.MonarchSaveStatusIndicatorLabels

/**
 * Shows where a [MonarchAutosaveState] stands: "Unsaved changes",
 * "Saving…", "Saved · 2 minutes ago", or "Couldn't save" with a Retry button
 * that saves again.
 *
 * A `MonarchSaveStatusIndicator` fed by the state: it adds nothing to the
 * drawing and connects the status, the time of the last save, the error and
 * Retry. The outcome is announced politely; "Saving…" only with
 * [announceSaving].
 *
 * @param state the autosave state, from `rememberMonarchAutosaveState` or a view model.
 * @param modifier the modifier of the indicator.
 * @param announceSaving also announces "Saving…".
 * @param errorMessage turns the failure into the visible message. `null` shows "Couldn't save".
 * @param icon replaces the status dot, as in `MonarchSaveStatusIndicator`.
 * @param labels replacements for the built-in strings of `MonarchSaveStatusIndicator`.
 */
@Composable
public fun MonarchAutosaveIndicator(
    state: MonarchAutosaveState<*>,
    modifier: Modifier = Modifier,
    announceSaving: Boolean = false,
    errorMessage: ((Throwable) -> String)? = null,
    icon: (@Composable (MonarchSaveStatus) -> Unit)? = null,
    labels: MonarchSaveStatusIndicatorLabels = MonarchSaveStatusIndicatorLabels(),
) {
    val error = state.error
    MonarchSaveStatusIndicator(
        status = state.status,
        modifier = modifier,
        announceSaving = announceSaving,
        savedAt = state.savedAt,
        errorMessage = if (error != null && errorMessage != null) errorMessage(error) else null,
        icon = icon,
        onRetry = state::retry,
        labels = labels,
    )
}
