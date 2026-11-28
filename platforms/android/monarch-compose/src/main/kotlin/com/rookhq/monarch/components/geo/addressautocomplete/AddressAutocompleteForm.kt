package com.rookhq.monarch.components.geo.addressautocomplete

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.rookhq.monarch.components.forms.textfield.MonarchTextField
import com.rookhq.monarch.components.geo.countryselect.MonarchCountrySelect
import com.rookhq.monarch.components.geo.countryselect.MonarchCountrySelectLabels
import java.util.Locale

/**
 * The address parts for review or manual entry: `MonarchTextField`s with the
 * autofill content types of an address, and a `MonarchCountrySelect` for
 * the country, which autofill does not fill.
 */
@Composable
@Suppress("LongParameterList")
internal fun AddressAutocompleteForm(
    address: MonarchAddressAutocompleteAddress,
    onAddressChange: (MonarchAddressAutocompleteAddress) -> Unit,
    texts: AddressAutocompleteFormTexts,
    fieldErrors: Map<MonarchAddressAutocompleteField, String>,
    enabled: Boolean,
    locale: Locale?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(MonarchAddressAutocompleteTokens.Gap)) {
        AddressAutocompleteTextField(
            value = address.line1,
            label = texts.line1,
            autofill = ContentType.AddressStreet,
            errorText = fieldErrors[MonarchAddressAutocompleteField.Line1],
            enabled = enabled,
        ) { onAddressChange(address.copy(line1 = it)) }
        AddressAutocompleteTextField(
            value = address.line2,
            label = texts.line2,
            autofill = ContentType.AddressAuxiliaryDetails,
            errorText = fieldErrors[MonarchAddressAutocompleteField.Line2],
            enabled = enabled,
            helperText = texts.optional,
        ) { onAddressChange(address.copy(line2 = it)) }
        AddressAutocompleteTextField(
            value = address.city,
            label = texts.city,
            autofill = ContentType.AddressLocality,
            errorText = fieldErrors[MonarchAddressAutocompleteField.City],
            enabled = enabled,
        ) { onAddressChange(address.copy(city = it)) }
        AddressAutocompleteTextField(
            value = address.region,
            label = texts.region,
            autofill = ContentType.AddressRegion,
            errorText = fieldErrors[MonarchAddressAutocompleteField.Region],
            enabled = enabled,
        ) { onAddressChange(address.copy(region = it)) }
        AddressAutocompleteTextField(
            value = address.postalCode,
            label = texts.postalCode,
            autofill = ContentType.PostalCode,
            errorText = fieldErrors[MonarchAddressAutocompleteField.PostalCode],
            enabled = enabled,
            capitalization = KeyboardCapitalization.Characters,
        ) { onAddressChange(address.copy(postalCode = it)) }
        MonarchCountrySelect(
            selected = address.countryCode,
            onSelectedChange = { onAddressChange(address.copy(countryCode = it)) },
            modifier = Modifier.fillMaxWidth(),
            locale = locale,
            enabled = enabled,
            errorText = fieldErrors[MonarchAddressAutocompleteField.Country],
            labels = MonarchCountrySelectLabels(label = texts.country),
        )
    }
}

/** One text field of the form, with its autofill content type. */
@Composable
@Suppress("LongParameterList")
private fun AddressAutocompleteTextField(
    value: String,
    label: String,
    autofill: ContentType,
    errorText: String?,
    enabled: Boolean,
    helperText: String? = null,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Words,
    onValueChange: (String) -> Unit,
) {
    MonarchTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentType = autofill },
        label = label,
        helperText = helperText,
        errorText = errorText,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(capitalization = capitalization, imeAction = ImeAction.Next),
    )
}
