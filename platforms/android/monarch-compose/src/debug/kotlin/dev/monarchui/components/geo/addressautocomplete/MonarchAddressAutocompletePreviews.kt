@file:OptIn(ExperimentalMonarchApi::class)

package dev.monarchui.components.geo.addressautocomplete

import androidx.compose.runtime.Composable
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
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
