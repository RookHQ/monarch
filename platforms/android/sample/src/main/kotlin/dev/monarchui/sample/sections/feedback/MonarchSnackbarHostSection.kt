package dev.monarchui.sample.sections.feedback

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.feedback.snackbarhost.MonarchSnackbarHost
import dev.monarchui.components.feedback.snackbarhost.showMonarchSnackbar
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme
import kotlinx.coroutines.launch

/** The catalogue page of Toast: snackbars in a MonarchSnackbarHost. */
object MonarchSnackbarHostSection : CataloguePage {
    override val title = "Toast"

    @Composable
    override fun Content() {
        val state = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()
        var undone by rememberSaveable { mutableIntStateOf(0) }
        CatalogueCaption("Show a snackbar")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2)) {
            MonarchTone.entries.forEach { tone ->
                MonarchButton(
                    text = tone.name,
                    onClick = {
                        scope.launch {
                            val result = state.showMonarchSnackbar(
                                message = "Ticket archived.",
                                tone = tone,
                                actionLabel = "Undo",
                                withDismissAction = true,
                            )
                            if (result == SnackbarResult.ActionPerformed) undone++
                        }
                    },
                    size = MonarchButtonSize.Sm,
                )
            }
        }
        MonarchText(text = "Undone $undone times.", color = MonarchTheme.colors.textMuted)
        // In an app the host sits in the snackbarHost slot of a Scaffold; here it follows the buttons.
        MonarchSnackbarHost(hostState = state)
    }
}
