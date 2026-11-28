package com.rookhq.monarch.sample.sections.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.datadisplay.tag.MonarchChip
import com.rookhq.monarch.components.feedback.errorstate.MonarchErrorState
import com.rookhq.monarch.components.feedback.errorstate.MonarchErrorStateKind
import com.rookhq.monarch.components.feedback.inlinenotice.MonarchInlineNotice
import com.rookhq.monarch.components.feedback.savestatusindicator.MonarchSaveStatus
import com.rookhq.monarch.components.feedback.savestatusindicator.MonarchSaveStatusIndicator
import com.rookhq.monarch.components.forms.ratingpicker.MonarchRatingOption
import com.rookhq.monarch.components.forms.ratingpicker.MonarchRatingPicker
import com.rookhq.monarch.components.forms.textfield.MonarchTextField
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme
import kotlinx.coroutines.delay

/** The catalogue page of CSATSurvey: a recipe of MonarchRatingPicker, MonarchTextField and save status. */
object CsatSurveySection : CataloguePage {
    override val title = "CSATSurvey"

    private const val MAX_COMMENT = 500
    private const val SAVE_MILLIS = 700L

    private val scale = listOf(
        MonarchRatingOption(1, "Very poor"),
        MonarchRatingOption(2, "Poor"),
        MonarchRatingOption(3, "Okay"),
        MonarchRatingOption(4, "Good"),
        MonarchRatingOption(5, "Excellent"),
    )

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    override fun Content() {
        var answer by rememberSaveable { mutableStateOf<String?>(null) }
        var comment by rememberSaveable { mutableStateOf("") }
        var status by rememberSaveable { mutableStateOf(MonarchSaveStatus.Idle) }
        var savedAt by rememberSaveable { mutableStateOf<Long?>(null) }
        LaunchedEffect(status) {
            if (status == MonarchSaveStatus.Saving) {
                delay(SAVE_MILLIS)
                savedAt = System.currentTimeMillis()
                status = MonarchSaveStatus.Saved
            }
        }
        val gap = Arrangement.spacedBy(MonarchTheme.spacing.space3)
        CatalogueCaption("Good or bad, saved at once, with a comment")
        Column(verticalArrangement = gap) {
            MonarchText(text = "How would you rate the support you received?", style = MonarchTextStyle.Heading)
            FlowRow(horizontalArrangement = gap) {
                listOf("Good", "Bad").forEach { choice ->
                    MonarchChip(
                        text = choice,
                        selected = answer == choice,
                        onSelectedChange = {
                            answer = choice
                            status = MonarchSaveStatus.Saving
                        },
                    )
                }
            }
            if (answer == null) {
                MonarchText(
                    text = "Choose an answer above. You can add a comment afterwards.",
                    color = MonarchTheme.colors.textMuted,
                )
            } else {
                MonarchTextField(
                    value = comment,
                    onValueChange = { comment = it.take(MAX_COMMENT) },
                    label = "Anything you would like to add? (optional)",
                    helperText = "${MAX_COMMENT - comment.length} characters left",
                    singleLine = false,
                )
                MonarchButton(text = "Save comment", onClick = { status = MonarchSaveStatus.Saving })
            }
            MonarchSaveStatusIndicator(
                status = status,
                savedAt = savedAt,
                onRetry = { status = MonarchSaveStatus.Saving },
            )
        }
        CatalogueCaption("A five-point scale, locked after answering")
        MonarchRatingPicker(
            value = 4,
            onValueChange = {},
            label = "Your rating",
            options = scale,
            readOnly = true,
        )
        MonarchInlineNotice(
            message = "Thanks for your feedback. This answer can no longer be changed.",
            tone = MonarchTone.Success,
        )
        CatalogueCaption("A survey link that expired")
        MonarchErrorState(
            title = "We could not load this survey",
            kind = MonarchErrorStateKind.NotFound,
            message = "This link has expired. Ask the team for a new one.",
        )
    }
}
