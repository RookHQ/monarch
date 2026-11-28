package dev.monarchui.components.intelligence.aimodeswitcher

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.res.stringResource
import dev.monarchui.R

/**
 * One way the assistant can work, offered by `MonarchAiModeSwitcher`: for
 * example Ask, Edit or Agent.
 *
 * @property id a stable id, the value of the switcher's selection.
 * @property label the short name, already localised. One word is best. It stays the TalkBack name when
 * [iconOnly] is true.
 * @property description what changes in this mode, already localised. Shown under the switcher while the
 * mode is selected. `null` for none.
 * @property icon an icon before the label, for example a `MonarchIcon` with a `null` content description.
 * `null` for none. It is decorative: the label names the mode.
 * @property iconOnly whether the segment shows only [icon]. Ignored when [icon] is `null`.
 * @property enabled whether the mode can be chosen.
 */
@Immutable
public class MonarchAiMode(
    public val id: String,
    public val label: String,
    public val description: String? = null,
    public val icon: (@Composable () -> Unit)? = null,
    public val iconOnly: Boolean = false,
    public val enabled: Boolean = true,
) {
    override fun equals(other: Any?): Boolean = other is MonarchAiMode &&
        other.id == id && other.label == label && other.description == description &&
        other.icon === icon && other.iconOnly == iconOnly && other.enabled == enabled

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = HASH * result + label.hashCode()
        result = HASH * result + description.hashCode()
        result = HASH * result + iconOnly.hashCode()
        return HASH * result + enabled.hashCode()
    }

    override fun toString(): String = "MonarchAiMode(id=$id, label=$label, enabled=$enabled)"

    /** The built-in modes. */
    public companion object {
        private const val HASH = 31

        /**
         * The built-in modes Ask, Edit and Agent, with ids `ask`, `edit` and `agent` and a description of
         * what each may change, in the language of the app. They have no icon.
         */
        @Composable
        public fun defaults(): List<MonarchAiMode> = listOf(
            MonarchAiMode(
                id = "ask",
                label = stringResource(R.string.monarch_aimodeswitcher_ask),
                description = stringResource(R.string.monarch_aimodeswitcher_ask_detail),
            ),
            MonarchAiMode(
                id = "edit",
                label = stringResource(R.string.monarch_aimodeswitcher_edit),
                description = stringResource(R.string.monarch_aimodeswitcher_edit_detail),
            ),
            MonarchAiMode(
                id = "agent",
                label = stringResource(R.string.monarch_aimodeswitcher_agent),
                description = stringResource(R.string.monarch_aimodeswitcher_agent_detail),
            ),
        )
    }
}
