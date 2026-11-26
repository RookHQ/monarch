package com.rookhq.monarch.internal

import androidx.compose.runtime.MutableState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * The state of an edit-in-place control (`MonarchInlineEditText`,
 * `MonarchEditableCell`): whether it edits, the draft, whether a save runs
 * and the message of a refused or failed save. The components remember each
 * part themselves (the draft and editing survive a configuration change).
 */
internal class MonarchEditSessionState(
    val editing: MutableState<Boolean>,
    val draft: MutableState<String>,
    val saving: MutableState<Boolean>,
    val failure: MutableState<String?>,
)

/**
 * The edit session shared by the edit-in-place controls: opening with a
 * draft, editing it, closing, and running a save that keeps the editor open
 * with its message when it throws. What may be saved, and when, stays with
 * each component.
 *
 * @param onEditingChange the app's callback, called only when editing really changes.
 */
internal class MonarchEditSession(
    val state: MonarchEditSessionState,
    private val scope: CoroutineScope,
    private val onEditingChange: ((Boolean) -> Unit)?,
) {
    val editing: Boolean get() = state.editing.value
    val saving: Boolean get() = state.saving.value

    fun setEditing(next: Boolean) {
        if (next != state.editing.value) {
            state.editing.value = next
            onEditingChange?.invoke(next)
        }
    }

    /** Opens the editor on [draft], clearing any message. */
    fun open(draft: String) {
        state.draft.value = draft
        state.failure.value = null
        setEditing(true)
    }

    /** The person typed [text]: it becomes the draft and any message goes. */
    fun edit(text: String) {
        state.draft.value = text
        state.failure.value = null
    }

    /** Closes the editor, clearing any message. */
    fun close() {
        state.failure.value = null
        setEditing(false)
    }

    /** Refuses the draft with [message]; the editor stays open. */
    fun refuse(message: String) {
        state.failure.value = message
    }

    /**
     * Runs [save]. While it runs [saving] is true. On success the editor
     * closes; when it throws, [message] of the error is shown and, unless
     * [closeWhileSaving] already closed it, the editor stays open with the
     * draft. A cancellation is passed on.
     *
     * @param closeWhileSaving closes the editor at once, for a menu or a dialog that cannot stay open.
     */
    fun persist(closeWhileSaving: Boolean = false, message: (Throwable) -> String, save: suspend () -> Unit) {
        state.saving.value = true
        state.failure.value = null
        if (closeWhileSaving) setEditing(false)
        scope.launch {
            val error = runCatching { save() }.exceptionOrNull()
            if (error is CancellationException) throw error
            state.saving.value = false
            if (error == null) close() else state.failure.value = message(error)
        }
    }
}
