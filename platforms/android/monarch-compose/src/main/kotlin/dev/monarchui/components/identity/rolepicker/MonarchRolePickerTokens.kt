package dev.monarchui.components.identity.rolepicker

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** Component tokens and the pure logic of MonarchRolePicker. */
internal object MonarchRolePickerTokens {
    /** The gap between the control and the text under it. */
    val SupportGap: Dp = MonarchTokens.Space.space1_5.dp

    /** The text of a card or a menu item under the title: description, capabilities, and the reason if disabled. */
    fun details(role: MonarchRoleOption<*>): String? = listOfNotNull(
        role.description?.takeIf { it.isNotBlank() },
        role.capabilities.filter { it.isNotBlank() }.takeIf { it.isNotEmpty() }?.joinToString("\n") { "• $it" },
        role.disabledReason?.takeIf { !role.enabled && it.isNotBlank() },
    ).takeIf { it.isNotEmpty() }?.joinToString("\n")

    /** The chosen role, or `null` when nothing or a value that is not in [roles] is chosen. */
    fun <T> chosen(roles: List<MonarchRoleOption<T>>, selected: T?): MonarchRoleOption<T>? =
        if (selected == null) null else roles.firstOrNull { it.value == selected }

    /** The colour of the text under the control: the error in the danger colour, help in the muted colour. */
    fun supportColor(error: Boolean, colors: MonarchColors): Color =
        if (error) colors.dangerStrong else colors.textMuted
}
