package com.rookhq.monarch.components.geo.locationfields

import androidx.compose.runtime.Composable
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchLocationFieldsPreview() {
    PreviewFrame {
        MonarchLocationFields(
            value = MonarchLocationValue(
                country = "US",
                region = "US-CA",
                regionName = "California",
                city = "Oakland",
                postalCode = "94612",
            ),
            onValueChange = { _, _ -> },
            legend = "Shipping address",
        )
    }
}

@MonarchPreviews
@Composable
internal fun MonarchLocationFieldsErrorsPreview() {
    PreviewFrame {
        MonarchLocationFields(
            value = MonarchLocationValue(country = "PT"),
            onValueChange = { _, _ -> },
            fields = listOf(MonarchLocationField.City, MonarchLocationField.PostalCode),
            errors = mapOf(MonarchLocationField.PostalCode to "Enter a postal code."),
        )
    }
}
