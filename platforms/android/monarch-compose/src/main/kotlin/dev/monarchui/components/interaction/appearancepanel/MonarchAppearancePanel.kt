package dev.monarchui.components.interaction.appearancepanel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import dev.monarchui.R
import dev.monarchui.components.forms.segmentedcontrol.MonarchSegmentedControl
import dev.monarchui.components.forms.select.MonarchSelect
import dev.monarchui.components.forms.select.MonarchSelectOption
import dev.monarchui.components.interaction.themetoggle.MonarchThemeToggle
import dev.monarchui.components.interaction.themetoggle.MonarchThemeToggleLabels
import dev.monarchui.components.layout.divider.MonarchDivider
import dev.monarchui.components.layout.divider.MonarchDividerVariant
import dev.monarchui.internal.monarchString
import dev.monarchui.theme.MonarchContrast
import dev.monarchui.theme.MonarchTheme
import dev.monarchui.tokens.MonarchAccent

/**
 * A settings panel for appearance: labelled, described rows for theme,
 * contrast, accent, motion and density, bound to one
 * [MonarchAppearancePreferences] value.
 *
 * Built from `MonarchThemeToggle` for the theme, a
 * `MonarchSegmentedControl` for contrast, motion and density, and
 * `MonarchSelect` for the accent, separated by dividers. The panel holds no
 * state: the app keeps [preferences], passes `scheme`, `contrast` and `accent`
 * to `MonarchTheme`, applies motion and density itself (see
 * [MonarchAppearancePreferences.reducesMotion]) and stores the choices, for
 * example in DataStore. System stays the default for scheme, contrast and
 * motion, so the app follows the device until the person chooses.
 *
 * @param preferences every current choice.
 * @param onPreferencesChange called with all the choices after any one changes.
 * @param modifier the modifier of the panel.
 * @param title a heading above the rows.
 * @param rows the rows to show, in order, each once. The standard rows are theme, contrast, motion and density;
 * add [MonarchAppearancePanelRow.Accent] for the accent menu.
 * @param accents the accents the accent row offers, chosen by name, for example `MonarchTokens.Accents.all`. The
 * current accent is always offered.
 * @param enabled whether the choices can be changed.
 * @param labels replacements for every built-in row label, description and choice name.
 */
@Composable
public fun MonarchAppearancePanel(
    preferences: MonarchAppearancePreferences,
    onPreferencesChange: (MonarchAppearancePreferences) -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    rows: List<MonarchAppearancePanelRow> = MonarchAppearancePanelRow.Standard,
    accents: List<MonarchAccent> = emptyList(),
    enabled: Boolean = true,
    labels: MonarchAppearancePanelLabels = MonarchAppearancePanelLabels(),
) {
    val tokens = MonarchAppearancePanelTokens
    Column(modifier = modifier) {
        if (title != null) {
            Text(
                text = title,
                modifier = Modifier.padding(bottom = tokens.TitleGap).semantics { heading() },
                style = MonarchTheme.typography.subheading,
                color = tokens.label(MonarchTheme.colors),
            )
        }
        tokens.visibleRows(rows).forEachIndexed { index, row ->
            if (index > 0) MonarchDivider(variant = MonarchDividerVariant.Subtle)
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = tokens.RowPadding),
                verticalArrangement = Arrangement.spacedBy(tokens.ControlGap),
            ) {
                when (row) {
                    MonarchAppearancePanelRow.Theme -> ThemeRow(preferences, onPreferencesChange, enabled, labels)
                    MonarchAppearancePanelRow.Contrast -> ContrastRow(preferences, onPreferencesChange, enabled, labels)
                    MonarchAppearancePanelRow.Accent ->
                        AccentRow(preferences, onPreferencesChange, accents, enabled, labels)
                    MonarchAppearancePanelRow.Motion -> MotionRow(preferences, onPreferencesChange, enabled, labels)
                    MonarchAppearancePanelRow.Density -> DensityRow(preferences, onPreferencesChange, enabled, labels)
                }
            }
        }
    }
}

/** A row's label and description, read as one element before the control. */
@Composable
private fun RowText(label: String, description: String) {
    val tokens = MonarchAppearancePanelTokens
    Column(
        modifier = Modifier.semantics(mergeDescendants = true) { },
        verticalArrangement = Arrangement.spacedBy(tokens.TextGap),
    ) {
        Text(text = label, style = MonarchTheme.typography.label, color = tokens.label(MonarchTheme.colors))
        Text(
            text = description,
            style = MonarchTheme.typography.caption,
            color = tokens.description(MonarchTheme.colors),
        )
    }
}

@Composable
private fun ThemeRow(
    preferences: MonarchAppearancePreferences,
    onChange: (MonarchAppearancePreferences) -> Unit,
    enabled: Boolean,
    labels: MonarchAppearancePanelLabels,
) {
    val label = monarchString(labels.theme, R.string.monarch_themetoggle_title)
    RowText(label, monarchString(labels.themeDescription, R.string.monarch_appearancepanel_theme_description))
    MonarchThemeToggle(
        scheme = preferences.scheme,
        onSchemeChange = { onChange(preferences.copy(scheme = it)) },
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        labels = MonarchThemeToggleLabels(
            title = label,
            light = labels.light,
            dark = labels.dark,
            system = labels.system,
        ),
    )
}

@Composable
private fun ContrastRow(
    preferences: MonarchAppearancePreferences,
    onChange: (MonarchAppearancePreferences) -> Unit,
    enabled: Boolean,
    labels: MonarchAppearancePanelLabels,
) {
    val label = monarchString(labels.contrast, R.string.monarch_appearancepanel_contrast)
    RowText(label, monarchString(labels.contrastDescription, R.string.monarch_appearancepanel_contrast_description))
    Segments(
        name = label,
        choices = MonarchAppearancePanelTokens.ContrastOrder,
        selected = preferences.contrast,
        enabled = enabled,
        text = { contrast ->
            when (contrast) {
                MonarchContrast.Standard ->
                    monarchString(labels.contrastStandard, R.string.monarch_appearancepanel_contrast_standard)
                MonarchContrast.More ->
                    monarchString(labels.contrastMore, R.string.monarch_appearancepanel_contrast_more)
                MonarchContrast.System ->
                    monarchString(labels.contrastSystem, R.string.monarch_appearancepanel_contrast_system)
            }
        },
        onSelect = { onChange(preferences.copy(contrast = it)) },
    )
}

@Composable
private fun MotionRow(
    preferences: MonarchAppearancePreferences,
    onChange: (MonarchAppearancePreferences) -> Unit,
    enabled: Boolean,
    labels: MonarchAppearancePanelLabels,
) {
    val label = monarchString(labels.motion, R.string.monarch_appearancepanel_motion)
    RowText(label, monarchString(labels.motionDescription, R.string.monarch_appearancepanel_motion_description))
    Segments(
        name = label,
        choices = MonarchAppearanceMotion.entries,
        selected = preferences.motion,
        enabled = enabled,
        text = { motion ->
            when (motion) {
                MonarchAppearanceMotion.System ->
                    monarchString(labels.motionSystem, R.string.monarch_appearancepanel_motion_system)
                MonarchAppearanceMotion.Reduce ->
                    monarchString(labels.motionReduce, R.string.monarch_appearancepanel_motion_reduce)
            }
        },
        onSelect = { onChange(preferences.copy(motion = it)) },
    )
}

@Composable
private fun DensityRow(
    preferences: MonarchAppearancePreferences,
    onChange: (MonarchAppearancePreferences) -> Unit,
    enabled: Boolean,
    labels: MonarchAppearancePanelLabels,
) {
    val label = monarchString(labels.density, R.string.monarch_appearancepanel_density)
    RowText(label, monarchString(labels.densityDescription, R.string.monarch_appearancepanel_density_description))
    Segments(
        name = label,
        choices = MonarchAppearanceDensity.entries,
        selected = preferences.density,
        enabled = enabled,
        text = { density ->
            when (density) {
                MonarchAppearanceDensity.Comfortable ->
                    monarchString(labels.densityComfortable, R.string.monarch_appearancepanel_density_comfortable)
                MonarchAppearanceDensity.Compact ->
                    monarchString(labels.densityCompact, R.string.monarch_appearancepanel_density_compact)
            }
        },
        onSelect = { onChange(preferences.copy(density = it)) },
    )
}

@Composable
private fun AccentRow(
    preferences: MonarchAppearancePreferences,
    onChange: (MonarchAppearancePreferences) -> Unit,
    accents: List<MonarchAccent>,
    enabled: Boolean,
    labels: MonarchAppearancePanelLabels,
) {
    val choices = MonarchAppearancePanelTokens.accentChoices(accents, preferences.accent)
    MonarchSelect(
        options = choices.map { MonarchSelectOption(value = it.id, label = it.label) },
        selected = preferences.accent.id,
        onSelect = { id -> choices.firstOrNull { it.id == id }?.let { onChange(preferences.copy(accent = it)) } },
        modifier = Modifier.fillMaxWidth(),
        label = monarchString(labels.accent, R.string.monarch_appearancepanel_accent),
        helperText = monarchString(labels.accentDescription, R.string.monarch_appearancepanel_accent_description),
        enabled = enabled,
    )
}

/** Material 3 single-choice segmented buttons, named by the row's label. */
@Composable
private fun <T> Segments(
    name: String,
    choices: List<T>,
    selected: T,
    enabled: Boolean,
    text: @Composable (T) -> String,
    onSelect: (T) -> Unit,
) {
    MonarchSegmentedControl(modifier = Modifier.semantics { contentDescription = name }, fillWidth = true) {
        choices.forEach { choice ->
            Segment(
                selected = choice == selected,
                onClick = { if (choice != selected) onSelect(choice) },
                label = text(choice),
                enabled = enabled,
            )
        }
    }
}
