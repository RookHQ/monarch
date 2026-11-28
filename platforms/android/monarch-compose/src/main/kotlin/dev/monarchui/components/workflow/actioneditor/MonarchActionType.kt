package dev.monarchui.components.workflow.actioneditor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable

/**
 * A kind of action, such as "Set priority" or "Notify agents".
 *
 * @property key the stable value stored on the action.
 * @property label the visible name, already localised.
 * @property description one line on what it does, shown under the type menu.
 * @property group the heading the type is listed under in the menu, such as "Assignment".
 * @property parameters the inputs of the type, in order.
 * @property defaults the parameters an action of this type starts with.
 * @property enabled whether new actions can use it. A disabled type stays listed, so existing actions keep it,
 * and its actions show a warning.
 * @property disabledReason why it is not available; shown in the menu and in the warning.
 * @property validate an extra check of the parameters; returns a message when they are not usable.
 * @property editor a custom editor for parameters that inputs cannot express. It replaces the [parameters] inputs
 * and is given the parameters and a callback for new ones.
 */
@Immutable
public class MonarchActionType(
    public val key: String,
    public val label: String,
    public val description: String? = null,
    public val group: String? = null,
    public val parameters: List<MonarchActionParameter> = emptyList(),
    public val defaults: Map<String, MonarchActionValue> = emptyMap(),
    public val enabled: Boolean = true,
    public val disabledReason: String? = null,
    public val validate: ((parameters: Map<String, MonarchActionValue>) -> String?)? = null,
    public val editor: (
        @Composable (
            parameters: Map<String, MonarchActionValue>,
            onParamsChange: (Map<String, MonarchActionValue>) -> Unit,
        ) -> Unit
    )? = null,
)
