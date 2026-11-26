package com.rookhq.monarch.sample.sections.feedback

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.feedback.snackbarhost.MonarchSnackbarHost
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme
import kotlinx.coroutines.launch

/** The catalogue page of UndoToast: a snackbar with Undo whose result decides between undo and commit. */
object UndoToastSection : CataloguePage {
    override val title = "UndoToast"

    @Composable
    override fun Content() {
        val snackbars = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()
        var note by rememberSaveable { mutableStateOf<String?>("Meeting notes") }
        var log by rememberSaveable { mutableStateOf("Nothing deleted yet.") }
        CatalogueCaption("Delete, then undo or let it commit")
        MonarchText(text = note?.let { "Note: $it" } ?: "No note.")
        MonarchButton(
            text = "Delete note",
            enabled = note != null,
            onClick = {
                val deleted = note
                note = null
                scope.launch {
                    val result = snackbars.showSnackbar(
                        message = "Note deleted",
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Long,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        note = deleted
                        log = "Undone."
                        snackbars.showSnackbar(message = "Restored")
                    } else {
                        log = "Committed: the delete is final."
                    }
                }
            },
        )
        MonarchText(text = log, color = MonarchTheme.colors.textMuted)
        // In an app the host sits in the snackbarHost slot of a Scaffold; here it follows the buttons.
        MonarchSnackbarHost(hostState = snackbars)
    }
}
