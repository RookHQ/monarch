package dev.monarchui.components.forms.colorpicker

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.monarchui.R

/** Default values of `MonarchColorPicker`. */
public object MonarchColorPickerDefaults {
    private val presets = listOf(
        "#64748b" to R.string.monarch_colorpicker_swatch_slate,
        "#dc2626" to R.string.monarch_colorpicker_swatch_red,
        "#ea580c" to R.string.monarch_colorpicker_swatch_orange,
        "#ca8a04" to R.string.monarch_colorpicker_swatch_yellow,
        "#16a34a" to R.string.monarch_colorpicker_swatch_green,
        "#0d9488" to R.string.monarch_colorpicker_swatch_teal,
        "#2563eb" to R.string.monarch_colorpicker_swatch_blue,
        "#4f46e5" to R.string.monarch_colorpicker_swatch_indigo,
        "#9333ea" to R.string.monarch_colorpicker_swatch_purple,
        "#db2777" to R.string.monarch_colorpicker_swatch_pink,
    )

    /**
     * The ten presets of every platform, from slate to pink, that read well
     * on light and dark surfaces. Their names are built-in strings.
     */
    @Composable
    public fun swatches(): List<MonarchColorSwatch> =
        presets.mapNotNull { (hex, name) -> MonarchColorSwatch.of(hex, stringResource(name)) }
}
