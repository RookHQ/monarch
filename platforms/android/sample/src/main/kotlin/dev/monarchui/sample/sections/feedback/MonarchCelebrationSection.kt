package dev.monarchui.sample.sections.feedback

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.feedback.celebration.MonarchCelebration
import dev.monarchui.components.feedback.celebration.MonarchCelebrationDecoration
import dev.monarchui.components.overlays.dialog.MonarchDialog
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.sampleicons.SampleIcons

/** The catalogue page of Celebration: inline with a replay, without confetti, and in a dialog. */
object MonarchCelebrationSection : CataloguePage {
    override val title = "Celebration"

    @Composable
    override fun Content() {
        var runs by rememberSaveable { mutableIntStateOf(0) }
        var dialog by rememberSaveable { mutableStateOf(false) }
        CatalogueCaption("Inline, with the next actions")
        key(runs) {
            MonarchCelebration(
                title = "Workspace ready",
                message = "Invite your team to start working together.",
                announces = runs > 0,
                icon = { MonarchIcon(SampleIcons.Success, contentDescription = null) },
            ) {
                MonarchButton(text = "Invite people", onClick = {}, variant = MonarchButtonVariant.Primary)
                MonarchButton(text = "Play again", onClick = { runs++ }, variant = MonarchButtonVariant.Ghost)
            }
        }
        CatalogueCaption("Info tone, no confetti")
        MonarchCelebration(
            title = "Import finished",
            message = "1,204 contacts were added.",
            tone = MonarchTone.Info,
            decoration = MonarchCelebrationDecoration.None,
            announces = false,
        )
        CatalogueCaption("In a dialog")
        MonarchButton(text = "Verify domain", onClick = { dialog = true })
        if (dialog) {
            // The celebration brings its own heading, so the dialog has none; TalkBack names it by its pane title.
            MonarchDialog(
                onDismissRequest = { dialog = false },
                title = null,
                paneTitle = "Domain verified",
                content = {
                    MonarchCelebration(
                        title = "Domain verified",
                        message = "Email from acme.com is now signed.",
                        icon = { MonarchIcon(SampleIcons.Success, contentDescription = null) },
                    ) {
                        MonarchButton(
                            text = "Continue",
                            onClick = { dialog = false },
                            variant = MonarchButtonVariant.Primary,
                        )
                    }
                },
            )
        }
    }
}
