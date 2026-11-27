package com.rookhq.monarch.sample.sections.forms

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.forms.ratingpicker.MonarchRatingOption
import com.rookhq.monarch.components.forms.ratingpicker.MonarchRatingPicker
import com.rookhq.monarch.components.forms.ratingpicker.MonarchRatingPickerSize
import com.rookhq.monarch.components.forms.ratingpicker.MonarchRatingPickerVariant
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage

/** The catalogue page of RatingPicker. */
object MonarchRatingPickerSection : CataloguePage {
    override val title = "RatingPicker"

    private const val READ_ONLY_VALUE = 4

    @Composable
    override fun Content() {
        var score by rememberSaveable { mutableStateOf<Int?>(null) }
        var stars by rememberSaveable { mutableStateOf<Int?>(null) }
        var review by rememberSaveable { mutableStateOf<Int?>(null) }
        CatalogueCaption("A 1 to 5 scale, clearable")
        MonarchRatingPicker(
            value = score,
            onValueChange = { score = it },
            label = "How was the call?",
            clearable = true,
        )
        CatalogueCaption("Stars")
        MonarchRatingPicker(
            value = stars,
            onValueChange = { stars = it },
            label = "Rate this article",
            variant = MonarchRatingPickerVariant.Stars,
        )
        CatalogueCaption("A labelled scale, small")
        MonarchRatingPicker(
            value = review,
            onValueChange = { review = it },
            label = "Performance",
            options = listOf("Below", "Meets", "Exceeds").mapIndexed { index, text ->
                MonarchRatingOption(value = index + 1, label = text)
            },
            size = MonarchRatingPickerSize.Sm,
        )
        CatalogueCaption("Read only")
        MonarchRatingPicker(
            value = READ_ONLY_VALUE,
            onValueChange = {},
            label = "Average rating",
            variant = MonarchRatingPickerVariant.Stars,
            readOnly = true,
        )
    }
}
