package com.rookhq.monarch.components.identity.memberrow

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

/** The resolved words of the row's actions. */
internal class MemberRowTexts(
    val remove: RemoveTexts,
    val resendName: String,
    val resendText: String,
    val resent: String,
)

/**
 * Remove (behind its confirmation) and Resend invite, shared by the buttons,
 * the swipe actions and the TalkBack actions, so each shows the same progress
 * and the same confirmation.
 */
@Stable
internal class MemberRowControls(confirmingState: MutableState<Boolean>) {
    /** Whether the remove confirmation is open. Survives a configuration change. */
    var confirming: Boolean by confirmingState

    /** Whether the removal runs. */
    var removing: Boolean by mutableStateOf(false)
        private set

    /** Whether the invitation is being sent again. */
    var resending: Boolean by mutableStateOf(false)
        private set

    internal lateinit var actions: MemberRowActionState
    internal lateinit var texts: MemberRowTexts
    internal var onRemove: (suspend () -> Unit)? = null
    internal var onResend: (suspend () -> Unit)? = null

    /** Opens the confirmation, unless a removal already runs. */
    fun askRemove() {
        if (!removing && onRemove != null) confirming = true
    }

    /** Confirms: closes the confirmation and runs the removal. */
    fun remove() {
        confirming = false
        val block = onRemove ?: return
        actions.run(block = block, busy = { removing = it })
    }

    /** Sends the invitation again; "Invite sent again." follows success. */
    fun resend() {
        val block = onResend ?: return
        if (resending) return
        actions.run(
            block = block,
            busy = { resending = it },
            onDone = { MonarchMemberRowNotice(texts.resent, error = false) },
        )
    }
}

/** The [MemberRowControls] of one row, updated with this composition's callbacks. */
@Composable
internal fun rememberMemberRowControls(
    actions: MemberRowActionState,
    texts: MemberRowTexts,
    onRemove: (suspend () -> Unit)?,
    onResend: (suspend () -> Unit)?,
): MemberRowControls {
    val confirming = rememberSaveable { mutableStateOf(false) }
    val controls = remember { MemberRowControls(confirming) }
    controls.actions = actions
    controls.texts = texts
    controls.onRemove = onRemove
    controls.onResend = onResend
    return controls
}
