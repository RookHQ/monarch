package com.rookhq.monarch.sample.sections.feedback

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.feedback.savestatusindicator.MonarchSaveStatus
import com.rookhq.monarch.components.feedback.savestatusindicator.MonarchSaveStatusIndicator
import com.rookhq.monarch.components.forms.textfield.MonarchTextField
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import kotlinx.coroutines.delay

/** The catalogue page of SaveStatusIndicator. */
object MonarchSaveStatusIndicatorSection : CataloguePage {
    override val title = "SaveStatusIndicator"

    private const val SAVE_MILLIS = 1200L

    @Composable
    override fun Content() {
        var note by rememberSaveable { mutableStateOf("") }
        var status by rememberSaveable { mutableStateOf(MonarchSaveStatus.Idle) }
        var savedAt by rememberSaveable { mutableLongStateOf(0L) }
        var failNext by rememberSaveable { mutableStateOf(false) }
        LaunchedEffect(status) {
            if (status == MonarchSaveStatus.Saving) {
                delay(SAVE_MILLIS)
                if (failNext) {
                    status = MonarchSaveStatus.Error
                } else {
                    savedAt = System.currentTimeMillis()
                    status = MonarchSaveStatus.Saved
                }
            }
        }
        CatalogueCaption("Edit, then save")
        MonarchTextField(
            value = note,
            onValueChange = {
                note = it
                status = MonarchSaveStatus.Unsaved
            },
            label = "Note",
        )
        MonarchSaveStatusIndicator(
            status = status,
            savedAt = savedAt.takeIf { it > 0L },
            onRetry = {
                failNext = false
                status = MonarchSaveStatus.Saving
            },
        )
        MonarchButton(text = "Save", onClick = { status = MonarchSaveStatus.Saving })
        MonarchButton(
            text = if (failNext) "Next save fails" else "Make the next save fail",
            onClick = { failNext = !failNext },
        )
    }
}
