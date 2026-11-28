package dev.monarchui.components.forms.colorpicker

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchColorPicker`. Every
 * `null` uses the built-in string named after it in `strings_colorpicker.xml`.
 *
 * @property swatches the spoken name of the group of presets: "Preset colors".
 * @property swatch writes the spoken name of a preset from its name and hex value: "Blue, #2563eb".
 * @property hex the label of the hex text field: "Hex color".
 * @property invalid the message when the hex text is not a color: "Enter a hex color like #2563eb.".
 */
@Immutable
public class MonarchColorPickerLabels(
    public val swatches: String? = null,
    public val swatch: ((name: String, hex: String) -> String)? = null,
    public val hex: String? = null,
    public val invalid: String? = null,
)
