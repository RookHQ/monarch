@file:OptIn(ExperimentalMonarchApi::class)

package com.rookhq.monarch.components.geo.addressautocomplete

import androidx.compose.runtime.Composable
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
import java.util.Locale

@MonarchPreviews
@Composable
internal fun MonarchAddressAutocompletePreview() {
    PreviewFrame {
        MonarchAddressAutocomplete(
            address = MonarchAddressAutocompleteAddress(),
            onAddressChange = {},
            search = { emptyList() },
            helperText = "Where we send the order.",
        )
        MonarchAddressAutocomplete(
            address = MonarchAddressAutocompleteAddress(),
            onAddressChange = {},
            search = { emptyList() },
            errorText = "Enter an address.",
        )
    }
}

@MonarchPreviews
@Composable
internal fun MonarchAddressAutocompleteFormPreview() {
    PreviewFrame {
        MonarchAddressAutocomplete(
            address = MonarchAddressAutocompleteAddress(
                line1 = "10 Rua Augusta",
                city = "Lisbon",
                postalCode = "1100-053",
                countryCode = "PT",
            ),
            onAddressChange = {},
            search = { emptyList() },
            locale = Locale.US,
        )
    }
}
