package com.rookhq.monarch.components.actions.buttongroup

import androidx.compose.runtime.Composable

/**
 * Declares the buttons of a `MonarchButtonGroup`, in order.
 *
 * The group draws every button with its own variant, size and joined shape,
 * so the buttons are declared here rather than composed as `MonarchButton`s.
 */
public interface MonarchButtonGroupScope {
    /**
     * A button with a text label.
     *
     * @param text the label. It wraps instead of being cut off.
     * @param onClick called when the user activates the button. Not called when not [enabled].
     * @param enabled whether the user can activate this button. The others are not affected.
     * @param leadingIcon an icon before the label, for example a `MonarchIcon` with a `null` content
     * description.
     */
    public fun button(
        text: String,
        onClick: () -> Unit,
        enabled: Boolean = true,
        leadingIcon: (@Composable () -> Unit)? = null,
    )

    /**
     * A button that shows only an icon.
     *
     * @param contentDescription what the button does, read by TalkBack, such as "Zoom in".
     * @param onClick called when the user activates the button. Not called when not [enabled].
     * @param enabled whether the user can activate this button.
     * @param icon the icon, for example a `MonarchIcon` with a `null` content description.
     */
    public fun iconButton(
        contentDescription: String,
        onClick: () -> Unit,
        enabled: Boolean = true,
        icon: @Composable () -> Unit,
    )
}

/** One declared button. Either [text] or [contentDescription] is set. */
internal class MonarchButtonGroupEntry(
    val text: String?,
    val contentDescription: String?,
    val onClick: () -> Unit,
    val enabled: Boolean,
    val icon: (@Composable () -> Unit)?,
)

/** Collects the buttons declared in a group's content. */
internal class MonarchButtonGroupScopeImpl : MonarchButtonGroupScope {
    val entries: MutableList<MonarchButtonGroupEntry> = mutableListOf()

    override fun button(text: String, onClick: () -> Unit, enabled: Boolean, leadingIcon: (@Composable () -> Unit)?) {
        entries += MonarchButtonGroupEntry(text, null, onClick, enabled, leadingIcon)
    }

    override fun iconButton(
        contentDescription: String,
        onClick: () -> Unit,
        enabled: Boolean,
        icon: @Composable () -> Unit,
    ) {
        entries += MonarchButtonGroupEntry(null, contentDescription, onClick, enabled, icon)
    }
}
