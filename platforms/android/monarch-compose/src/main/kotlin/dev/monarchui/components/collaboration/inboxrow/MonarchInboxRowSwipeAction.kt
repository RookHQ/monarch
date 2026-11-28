package dev.monarchui.components.collaboration.inboxrow

/**
 * An action of a `MonarchInboxRow`, offered by swiping the row and as a
 * TalkBack action. Swiping toward the end runs [ToggleRead]; swiping toward
 * the start runs [Archive], or [Delete] when there is no Archive.
 *
 * @property onAction runs the action. The row springs back afterwards; the app updates or removes it.
 */
public sealed class MonarchInboxRowSwipeAction {
    /** Runs the action. */
    public abstract val onAction: () -> Unit

    /** Marks the conversation read or unread, depending on the row's unread state. */
    public class ToggleRead(override val onAction: () -> Unit) : MonarchInboxRowSwipeAction()

    /** Archives the conversation. */
    public class Archive(override val onAction: () -> Unit) : MonarchInboxRowSwipeAction()

    /** Deletes the conversation. */
    public class Delete(override val onAction: () -> Unit) : MonarchInboxRowSwipeAction()
}
