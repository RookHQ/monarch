package com.rookhq.monarch.components.collaboration.messagecomposer

import androidx.compose.ui.input.key.Key

/** What a hardware key does in the composer. */
internal enum class MessageComposerKeyAction { Send, CancelEdit, EditLast, None }

/** A key press, reduced to what the composer needs. */
internal data class MessageComposerKey(
    val key: Key,
    val shift: Boolean,
    val command: Boolean,
    val hardware: Boolean,
)

/** The hardware keyboard rules of MonarchMessageComposer. */
internal object MonarchMessageComposerKeys {
    /**
     * What a key press does. Only a hardware keyboard sends: Enter (without
     * Shift) with [MonarchMessageComposerSendShortcut.Enter], Ctrl or Meta
     * with Enter with [MonarchMessageComposerSendShortcut.CtrlEnter]. Escape
     * leaves editing; Up in an empty box edits the last message.
     */
    fun keyAction(
        press: MessageComposerKey,
        shortcut: MonarchMessageComposerSendShortcut,
        editing: Boolean,
        empty: Boolean,
        canEditLast: Boolean,
    ): MessageComposerKeyAction {
        if (!press.hardware) return MessageComposerKeyAction.None
        return when (press.key) {
            Key.Enter, Key.NumPadEnter -> if (sendsOn(press, shortcut)) {
                MessageComposerKeyAction.Send
            } else {
                MessageComposerKeyAction.None
            }
            Key.Escape -> if (editing) MessageComposerKeyAction.CancelEdit else MessageComposerKeyAction.None
            Key.DirectionUp -> if (empty && canEditLast) {
                MessageComposerKeyAction.EditLast
            } else {
                MessageComposerKeyAction.None
            }
            else -> MessageComposerKeyAction.None
        }
    }

    private fun sendsOn(press: MessageComposerKey, shortcut: MonarchMessageComposerSendShortcut): Boolean =
        when (shortcut) {
            MonarchMessageComposerSendShortcut.Enter -> !press.shift
            MonarchMessageComposerSendShortcut.CtrlEnter -> press.command
        }
}
