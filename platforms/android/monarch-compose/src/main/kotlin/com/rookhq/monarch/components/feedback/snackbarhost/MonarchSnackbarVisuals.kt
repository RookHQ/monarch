package com.rookhq.monarch.components.feedback.snackbarhost

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.ui.graphics.vector.ImageVector
import com.rookhq.monarch.foundation.MonarchTone

/**
 * The content of a Monarch snackbar: the Material 3 `SnackbarVisuals` with a
 * tone. Show it with `SnackbarHostState.showSnackbar(visuals)` or
 * [showMonarchSnackbar], inside a `MonarchSnackbarHost`.
 *
 * @property message the message. Say the outcome in words; the tone is a colour.
 * @property tone the colour of the snackbar.
 * @property actionLabel the label of the one action, for example "Undo". `null` for none.
 * @property withDismissAction whether a dismiss button is shown.
 * @property duration how long the snackbar stays. By default `Long` with an action or the danger tone,
 * `Short` otherwise. TalkBack users get more time, as the system decides.
 * @property icon an outlined icon at the start, in the tone colour. `null` shows the status mark of the info,
 * success, warning and danger tones, and nothing for the others.
 * @property description a second, quieter line under the message, as the web toast's description. `null` for none.
 */
public class MonarchSnackbarVisuals(
    override val message: String,
    public val tone: MonarchTone = MonarchTone.Neutral,
    override val actionLabel: String? = null,
    override val withDismissAction: Boolean = false,
    override val duration: SnackbarDuration = MonarchSnackbarHostTokens.defaultDuration(tone, actionLabel),
    public val icon: ImageVector? = null,
    public val description: String? = null,
) : SnackbarVisuals {
    override fun equals(other: Any?): Boolean =
        other is MonarchSnackbarVisuals &&
            other.message == message &&
            other.tone == tone &&
            other.actionLabel == actionLabel &&
            other.withDismissAction == withDismissAction &&
            other.duration == duration &&
            other.icon == icon &&
            other.description == description

    override fun hashCode(): Int {
        var result = message.hashCode()
        result = HASH_FACTOR * result + tone.hashCode()
        result = HASH_FACTOR * result + actionLabel.hashCode()
        result = HASH_FACTOR * result + withDismissAction.hashCode()
        result = HASH_FACTOR * result + duration.hashCode()
        result = HASH_FACTOR * result + icon.hashCode()
        result = HASH_FACTOR * result + description.hashCode()
        return result
    }

    private companion object {
        const val HASH_FACTOR = 31
    }
}
