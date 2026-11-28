package dev.monarchui.components.identity.rolepicker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.monarchui.R
import dev.monarchui.components.forms.radiocardgroup.MonarchRadioCardGroup
import dev.monarchui.components.forms.radiocardgroup.MonarchRadioCardOption
import dev.monarchui.components.forms.select.MonarchSelect
import dev.monarchui.components.forms.select.MonarchSelectOption
import dev.monarchui.internal.monarchString
import dev.monarchui.theme.MonarchTheme

/**
 * Chooses a role for a person, with what each role can do in plain words, as
 * radio cards or a compact menu field. Roles that cannot be chosen stay
 * visible with the reason.
 *
 * The cards layout is `MonarchRadioCardGroup`: each role is a radio button in
 * a group named by [label], with its description, capabilities and reason as
 * the card's text. The compact layout is a `MonarchSelect` that shows
 * [placeholder] in the muted text colour until a role is chosen; each menu item shows the
 * role's description, disabled roles cannot be chosen, and the chosen role's
 * description is shown under the field. An error replaces the helper text and
 * sets the error semantics.
 *
 * @param roles the roles, from least to most access.
 * @param selected the value of the chosen role, or `null` for no choice yet.
 * @param onSelect called with the value of the role the person chooses.
 * @param modifier the modifier of the picker.
 * @param layout radio cards, or a compact menu field.
 * @param enabled whether any role can be chosen.
 * @param label replaces the built-in label "Role" (`monarch_rolepicker_label`).
 * @param required shows the required mark after the label of the compact layout.
 * @param helperText help under the control.
 * @param errorText an error under the control, in the danger colour. Replaces [helperText].
 * @param placeholder replaces the compact field's built-in "Choose a role" (`monarch_rolepicker_placeholder`),
 * shown while nothing is chosen.
 */
@Composable
public fun <T> MonarchRolePicker(
    roles: List<MonarchRoleOption<T>>,
    selected: T?,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    layout: MonarchRolePickerLayout = MonarchRolePickerLayout.Cards,
    enabled: Boolean = true,
    label: String? = null,
    helperText: String? = null,
    errorText: String? = null,
    placeholder: String? = null,
    required: Boolean = false,
) {
    val name = monarchString(label, R.string.monarch_rolepicker_label)
    val support = errorText ?: helperText
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(MonarchRolePickerTokens.SupportGap)) {
        when (layout) {
            MonarchRolePickerLayout.Cards -> MonarchRadioCardGroup(
                options = roles.map { role ->
                    MonarchRadioCardOption(
                        value = role.value,
                        title = role.title,
                        description = MonarchRolePickerTokens.details(role),
                        enabled = role.enabled,
                    )
                },
                selected = selected,
                onSelect = onSelect,
                label = name,
                enabled = enabled,
            )
            MonarchRolePickerLayout.Compact -> RoleMenu(
                roles = roles,
                selected = selected,
                onSelect = onSelect,
                name = name,
                enabled = enabled,
                helperText = helperText,
                errorText = errorText,
                placeholder = monarchString(placeholder, R.string.monarch_rolepicker_placeholder),
                required = required,
            )
        }
        // The compact field shows its helper and error itself, under the field.
        if (support != null && layout == MonarchRolePickerLayout.Cards) {
            Text(
                text = support,
                style = MonarchTheme.typography.caption,
                color = MonarchRolePickerTokens.supportColor(errorText != null, MonarchTheme.colors),
            )
        }
    }
}

/** The compact layout: a Monarch select of the roles, then the description of the chosen role. */
@Composable
@Suppress("LongParameterList")
private fun <T> RoleMenu(
    roles: List<MonarchRoleOption<T>>,
    selected: T?,
    onSelect: (T) -> Unit,
    name: String,
    enabled: Boolean,
    helperText: String?,
    errorText: String?,
    placeholder: String,
    required: Boolean,
) {
    val chosen = MonarchRolePickerTokens.chosen(roles, selected)
    Column(verticalArrangement = Arrangement.spacedBy(MonarchRolePickerTokens.SupportGap)) {
        MonarchSelect(
            options = roles.map { role ->
                MonarchSelectOption(
                    value = role.value,
                    label = role.title,
                    description = MonarchRolePickerTokens.details(role),
                    enabled = role.enabled,
                )
            },
            selected = chosen?.value,
            onSelect = onSelect,
            modifier = Modifier.fillMaxWidth(),
            label = name,
            placeholder = placeholder,
            helperText = helperText,
            errorText = errorText,
            enabled = enabled,
            required = required,
        )
    }
}
