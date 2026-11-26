package com.rookhq.monarch.components.collaboration.messagerow

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable

/**
 * One action of a `MonarchMessageRow`, such as Reply, Edit, Copy or Delete.
 * Offered in the menu that a long press opens and as a TalkBack action.
 *
 * @property label the name of the action, already localized.
 * @property onClick runs the action.
 * @property destructive draws the label in the danger colour, for Delete.
 * @property icon an optional leading icon in the menu. Decorative: the label names the action.
 */
@Immutable
public class MonarchMessageRowAction(
    public val label: String,
    public val onClick: () -> Unit,
    public val destructive: Boolean = false,
    public val icon: (@Composable () -> Unit)? = null,
)
