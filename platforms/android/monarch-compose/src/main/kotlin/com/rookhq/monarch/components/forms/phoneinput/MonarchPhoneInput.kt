package com.rookhq.monarch.components.forms.phoneinput

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import com.rookhq.monarch.components.forms.textfield.MonarchTextFieldLabelVisibility
import com.rookhq.monarch.components.forms.formfield.MonarchFieldFrame
import com.rookhq.monarch.components.forms.formfield.monarchFieldSemantics
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.rookhq.monarch.R
import com.rookhq.monarch.components.forms.textfield.MonarchTextField
import com.rookhq.monarch.components.geo.countryselect.CountrySelectData
import com.rookhq.monarch.components.geo.phonecountryselect.MonarchPhoneCountrySelect
import com.rookhq.monarch.components.geo.phonecountryselect.PhoneCountrySelectData
import com.rookhq.monarch.internal.monarchString
import java.util.Locale

/**
 * A phone number as a country calling code and the national number, with one
 * E.164 value out ("+442079460958").
 *
 * Built from `MonarchPhoneCountrySelect` (the Material 3 exposed dropdown with
 * a searchable sheet) above `MonarchTextField` (the Material 3
 * `OutlinedTextField`) with the phone keyboard and phone-number autofill. The
 * calling code is shown at the start of the number. The number is read when
 * the field loses focus, on Done and when the country changes, never on every
 * keystroke. Typing or pasting a full international number ("+44 20 7946
 * 0958", "0044…") picks its country and keeps the national digits. A number
 * that is too short or too long is kept on screen, flagged and explained, and
 * never passed to [onValueChange]. Only the length is checked (8 to 15 digits):
 * validate on the server before sending messages to it.
 *
 * @param value the number in E.164 ("+442079460958"), or `null` while there is none. Other international forms
 * are read too.
 * @param onValueChange called with the new E.164 number, or `null` when the number is cleared. Not called while
 * the number is incomplete.
 * @param modifier the modifier of the whole input.
 * @param defaultCountry the ISO code of the country shown while [value] does not say, such as the person's own.
 * @param onCountryChange called with the ISO code when the country changes, by the picker or by a pasted number.
 * @param label the visible label of the number.
 * @param placeholder an example shown while the number is empty and focused.
 * @param helperText guidance under the number.
 * @param errorText an error from the app. It replaces the input's own message while present.
 * @param countries limits the picker to these ISO codes. `null` lists every country with a calling code.
 * @param preferred ISO codes listed first in the picker, such as the person's own country.
 * @param locale the language of the country names. `null` uses the first locale of the configuration.
 * @param enabled whether the country and the number can be changed.
 * @param readOnly whether the number can be selected and copied but not changed. The picker is disabled.
 * @param labels replacements for the built-in strings.
 * @param interactionSource the source of the focus interactions of the number field.
 */
@Composable
@Suppress("LongParameterList", "LongMethod")
public fun MonarchPhoneInput(
    value: String?,
    onValueChange: (String?) -> Unit,
    modifier: Modifier = Modifier,
    defaultCountry: String = "US",
    onCountryChange: ((String) -> Unit)? = null,
    label: String? = null,
    placeholder: String? = null,
    helperText: String? = null,
    errorText: String? = null,
    countries: List<String>? = null,
    preferred: List<String> = emptyList(),
    locale: Locale? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    labels: MonarchPhoneInputLabels = MonarchPhoneInputLabels(),
    interactionSource: MutableInteractionSource? = null,
) {
    val country = rememberSaveable { mutableStateOf(PhoneInputEditor.split(value, defaultCountry).country) }
    val shown = PhoneInputEditor.split(value, country.value)
    val countryChanged by rememberUpdatedState(onCountryChange)
    // A new value from outside shows its own country.
    LaunchedEffect(value) {
        val owner = PhoneInputEditor.split(value, country.value).country
        if (owner != country.value) {
            country.value = owner
            countryChanged?.invoke(owner)
        }
    }
    val draft = remember(value) { mutableStateOf(shown.national) }
    val problem = remember(value) { mutableStateOf<String?>(null) }
    var focused by remember { mutableStateOf(false) }
    val editor = PhoneInputEditor(
        value = value,
        country = country,
        draft = draft,
        problem = problem,
        invalid = monarchString(labels.invalid, R.string.monarch_phoneinput_invalid),
        onValueChange = onValueChange,
        onCountryChange = onCountryChange,
    )
    // As the web: one label over the country and the number side by side, then the helper or error.
    MonarchFieldFrame(
        modifier = modifier,
        label = label,
        enabled = enabled,
        helperText = helperText,
        errorText = errorText ?: problem.value,
    ) {
        // Side by side as the web; stacked at a large font size, where half the width would cut the country name.
        val stacked = LocalDensity.current.fontScale >= MonarchPhoneInputTokens.StackedFontScale
        val countryPart: @Composable (Modifier) -> Unit = { part ->
            MonarchPhoneCountrySelect(
                selected = country.value,
                onSelectedChange = { code, _ -> if (code != null) editor.changeCountry(code) },
                modifier = part,
                locale = locale,
                countries = countries,
                preferred = preferred,
                // The picker has no read-only state, so a read-only number keeps its country too.
                enabled = enabled && !readOnly,
                labels = labels.country,
                labelVisible = false,
            )
        }
        val numberPart: @Composable (Modifier) -> Unit = { part ->
            MonarchTextField(
                value = draft.value,
                onValueChange = editor::edit,
                modifier = part
                    .onFocusChanged { state ->
                        if (focused && !state.isFocused) editor.commit()
                        focused = state.isFocused
                    }
                    .monarchFieldSemantics(null, errorText ?: problem.value)
                    .semantics { contentType = ContentType.PhoneNumberNational },
                label = label ?: monarchString(labels.country.label, R.string.monarch_phonecountryselect_label),
                labelVisibility = MonarchTextFieldLabelVisibility.Hidden,
                placeholder = placeholder,
                invalid = (errorText ?: problem.value) != null,
                enabled = enabled,
                readOnly = readOnly,
                visualTransformation = MonarchPhoneInputTokens.LeftToRight,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { editor.commit() }),
                interactionSource = interactionSource,
            )
        }
        if (stacked) {
            Column(verticalArrangement = Arrangement.spacedBy(MonarchPhoneInputTokens.Gap)) {
                countryPart(Modifier.fillMaxWidth())
                numberPart(Modifier.fillMaxWidth())
            }
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(MonarchPhoneInputTokens.Gap),
                verticalAlignment = Alignment.Top,
            ) {
                countryPart(Modifier.weight(1f))
                numberPart(Modifier.weight(1f))
            }
        }
    }
}

/** The country and national digits shown for a value. */
internal class PhoneInputShown(val country: String, val national: String)

/** Reads, splits and saves the number of a phone input. */
internal class PhoneInputEditor(
    private val value: String?,
    private val country: MutableState<String>,
    private val draft: MutableState<String>,
    private val problem: MutableState<String?>,
    private val invalid: String,
    private val onValueChange: (String?) -> Unit,
    private val onCountryChange: ((String) -> Unit)?,
) {
    /** Takes typed text; a full international number picks its country at once. */
    fun edit(text: String) {
        val typed = MonarchPhoneInputDefaults.ascii(text)
        val compact = typed.replace(Separators, "")
        val parsed = compact.takeIf(International::matches)?.let { MonarchPhoneInputDefaults.parse(it, country.value) }
        if (parsed != null) {
            setCountry(parsed.country)
            draft.value = parsed.national
        } else {
            draft.value = typed
        }
        problem.value = null
    }

    /** Chooses another country and, when a number is typed, saves it with the new calling code. */
    fun changeCountry(code: String) {
        if (code == country.value) return
        setCountry(code)
        if (draft.value.isNotBlank()) commit()
    }

    /** Saves the typed number, or flags it when it is incomplete. */
    fun commit() {
        var code = country.value
        var national = draft.value
        val trimmed = national.trim()
        if (trimmed.startsWith("+") || trimmed.startsWith("00")) {
            val parsed = MonarchPhoneInputDefaults.parse(trimmed, code)
            if (parsed == null) {
                problem.value = invalid
                return
            }
            code = parsed.country
            national = parsed.national
            setCountry(code)
            draft.value = national
        }
        val dialCode = PhoneCountrySelectData.country(code)?.dialCode.orEmpty()
        val e164 = MonarchPhoneInputDefaults.toE164(dialCode, national, code)
        when {
            e164 == null -> emit(null)
            !MonarchPhoneInputDefaults.isValidE164(e164) -> problem.value = invalid
            else -> {
                // A shared calling code ("+1 242…") may belong to another country.
                val owner = MonarchPhoneInputDefaults.parse(e164, code)
                if (owner != null && owner.dialCode == dialCode) setCountry(owner.country)
                emit(e164)
            }
        }
    }

    private fun emit(next: String?) {
        problem.value = null
        if (next != value) onValueChange(next)
    }

    private fun setCountry(code: String) {
        if (code == country.value) return
        country.value = code
        onCountryChange?.invoke(code)
    }

    companion object {
        private val Separators = Regex("[\\s().\\-]")
        private val International = Regex("^(\\+|00)\\d{7,}$")

        /** The country and the national digits of [value], with [country] as the owner of a shared code. */
        fun split(value: String?, country: String): PhoneInputShown {
            val code = CountrySelectData.canonical(country)
            if (value.isNullOrBlank()) return PhoneInputShown(code, "")
            val parsed = MonarchPhoneInputDefaults.parse(value, code)
            return if (parsed != null) {
                PhoneInputShown(parsed.country, parsed.national)
            } else {
                PhoneInputShown(code, MonarchPhoneInputDefaults.digits(value))
            }
        }
    }
}
