package dev.monarchui.components.interaction.themetoggle

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import dev.monarchui.R
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.forms.segmentedcontrol.MonarchSegmentedControl
import dev.monarchui.components.overlays.menu.MonarchMenu
import dev.monarchui.components.overlays.menu.MonarchMenuItem
import dev.monarchui.components.overlays.menu.MonarchMenuLabel
import dev.monarchui.internal.monarchString
import dev.monarchui.theme.MonarchScheme
import dev.monarchui.theme.MonarchTheme

/**
 * Light, dark or system theme: the choice an app passes to the `scheme` of
 * `MonarchTheme`.
 *
 * The segmented variant is a `MonarchSegmentedControl`; the menu variant is a
 * ghost `MonarchButton` that opens a `MonarchMenu` with one option per choice,
 * the current one checked. The toggle holds no state:
 * the app keeps [scheme], passes it to `MonarchTheme(scheme = …)` and stores
 * it, for example in DataStore with [MonarchThemeToggleStorage]. The shared
 * web cookie does not apply to an app.
 *
 * @param scheme the current choice.
 * @param onSchemeChange called with the choice the user picks.
 * @param modifier the modifier of the toggle.
 * @param variant three segments, or one button that opens a menu.
 * @param size the text size of the segments, or the size of the menu button.
 * @param enabled whether the user can change the choice.
 * @param showLabel menu only: whether the button shows the current choice's name. Without it the name is
 * still spoken. The name is always shown when there is no [icon].
 * @param icon an icon for a choice, for example a sun, a moon or a half-filled circle as a `MonarchIcon`
 * with a `null` content description. Shown on the menu button and in the menu, and in each segment. Monarch
 * bundles no icons, so there is no default; the choices are always named in words.
 * @param labels replacements for the built-in strings Theme, Light, Dark and System.
 */
@Composable
public fun MonarchThemeToggle(
    scheme: MonarchScheme,
    onSchemeChange: (MonarchScheme) -> Unit,
    modifier: Modifier = Modifier,
    variant: MonarchThemeToggleVariant = MonarchThemeToggleVariant.Segmented,
    size: MonarchThemeToggleSize = MonarchThemeToggleSize.Md,
    enabled: Boolean = true,
    showLabel: Boolean = true,
    icon: (@Composable (MonarchScheme) -> Unit)? = null,
    labels: MonarchThemeToggleLabels = MonarchThemeToggleLabels(),
) {
    val title = monarchString(labels.title, R.string.monarch_themetoggle_title)
    val names = MonarchThemeToggleTokens.Choices.associateWith { choiceName(it, labels) }
    when (variant) {
        MonarchThemeToggleVariant.Segmented ->
            Segments(scheme, onSchemeChange, modifier, size, enabled, title, names, icon)
        MonarchThemeToggleVariant.Menu -> MenuToggle(
            scheme = scheme,
            onSchemeChange = onSchemeChange,
            modifier = modifier,
            size = size,
            enabled = enabled,
            showLabel = showLabel,
            icon = icon,
            title = title,
            names = names,
        )
    }
}

@Composable
private fun Segments(
    scheme: MonarchScheme,
    onSchemeChange: (MonarchScheme) -> Unit,
    modifier: Modifier,
    size: MonarchThemeToggleSize,
    enabled: Boolean,
    title: String,
    names: Map<MonarchScheme, String>,
    icon: (@Composable (MonarchScheme) -> Unit)?,
) {
    val choices = MonarchThemeToggleTokens.Choices
    MonarchSegmentedControl(
        modifier = modifier.semantics { contentDescription = title },
        size = MonarchThemeToggleTokens.segmentedSize(size),
    ) {
        choices.forEach { choice ->
            Segment(
                selected = choice == scheme,
                onClick = { if (choice != scheme) onSchemeChange(choice) },
                label = names.getValue(choice),
                enabled = enabled,
                icon = icon?.let { draw -> { draw(choice) } },
            )
        }
    }
}

@Composable
private fun MenuToggle(
    scheme: MonarchScheme,
    onSchemeChange: (MonarchScheme) -> Unit,
    modifier: Modifier,
    size: MonarchThemeToggleSize,
    enabled: Boolean,
    showLabel: Boolean,
    icon: (@Composable (MonarchScheme) -> Unit)?,
    title: String,
    names: Map<MonarchScheme, String>,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val current = names.getValue(scheme)
    Box(modifier = modifier) {
        MonarchButton(
            onClick = { expanded = true },
            modifier = Modifier.semantics {
                contentDescription = title
                stateDescription = current
            },
            variant = MonarchButtonVariant.Ghost,
            size = MonarchThemeToggleTokens.buttonSize(size),
            enabled = enabled,
        ) {
            icon?.let { Box(Modifier.clearAndSetSemantics { }) { it(scheme) } }
            if (showLabel || icon == null) Text(text = current, modifier = Modifier.clearAndSetSemantics { })
        }
        MonarchMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            MonarchMenuLabel(text = title)
            MonarchThemeToggleTokens.Choices.forEach { choice ->
                val selected = choice == scheme
                MonarchMenuItem(
                    onClick = {
                        expanded = false
                        if (!selected) onSchemeChange(choice)
                    },
                    modifier = Modifier.semantics { role = Role.RadioButton },
                    selected = selected,
                    leadingIcon = icon?.let { draw -> { Box(Modifier.clearAndSetSemantics { }) { draw(choice) } } },
                ) { Text(text = names.getValue(choice)) }
            }
        }
    }
}

@Composable
private fun choiceName(scheme: MonarchScheme, labels: MonarchThemeToggleLabels): String = when (scheme) {
    MonarchScheme.Light -> monarchString(labels.light, R.string.monarch_themetoggle_light)
    MonarchScheme.Dark -> monarchString(labels.dark, R.string.monarch_themetoggle_dark)
    MonarchScheme.System -> monarchString(labels.system, R.string.monarch_themetoggle_system)
}
