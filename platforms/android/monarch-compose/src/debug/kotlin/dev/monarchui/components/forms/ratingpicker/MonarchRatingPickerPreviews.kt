package dev.monarchui.components.forms.ratingpicker

import androidx.compose.runtime.Composable
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchRatingPickerPreview() {
    PreviewFrame {
        MonarchRatingPicker(value = 3, onValueChange = {}, label = "How was the call?")
        MonarchRatingPicker(
            value = 4,
            onValueChange = {},
            label = "Rating",
            options = listOf(
                MonarchRatingOption(1, "Poor"),
                MonarchRatingOption(2, "Fair"),
                MonarchRatingOption(3, "Good"),
                MonarchRatingOption(4, "Great"),
            ),
            variant = MonarchRatingPickerVariant.Stars,
        )
        MonarchRatingPicker(value = 2, onValueChange = {}, size = MonarchRatingPickerSize.Sm, enabled = false)
    }
}
