package dev.monarchui.components.feedback.snackbarhost

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.ui.graphics.vector.ImageVector
import dev.monarchui.foundation.MonarchTone

/**
 * Shows a snackbar with a tone in the `MonarchSnackbarHost` of this state and
 * suspends until it is dismissed or its action is performed. Snackbars are
 * shown one at a time, in the order they are requested.
 *
 * @param message the message. Say the outcome in words; the tone is a colour.
 * @param tone the colour of the snackbar.
 * @param actionLabel the label of the one action, for example "Undo".
 * @param withDismissAction whether a dismiss button is shown.
 * @param duration how long the snackbar stays. By default `Long` with an action or the danger tone, `Short`
 * otherwise.
 * @param icon an outlined icon at the start, in the tone colour; `null` for the status mark of the tone.
 * @param description a second, quieter line under the message; `null` for none.
 * @return `ActionPerformed` when the user activated the action, `Dismissed` otherwise.
 */
public suspend fun SnackbarHostState.showMonarchSnackbar(
    message: String,
    tone: MonarchTone = MonarchTone.Neutral,
    actionLabel: String? = null,
    withDismissAction: Boolean = false,
    duration: SnackbarDuration = MonarchSnackbarHostTokens.defaultDuration(tone, actionLabel),
    icon: ImageVector? = null,
    description: String? = null,
): SnackbarResult =
    showSnackbar(MonarchSnackbarVisuals(message, tone, actionLabel, withDismissAction, duration, icon, description))
