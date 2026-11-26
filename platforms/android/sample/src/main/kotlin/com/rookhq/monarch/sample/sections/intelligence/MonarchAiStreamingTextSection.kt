package com.rookhq.monarch.sample.sections.intelligence

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.intelligence.aistreamingtext.MonarchAiStreamingText
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import kotlinx.coroutines.delay

private const val Reply = "## Summary\n\nThe sample project has **three open tasks** and one is overdue. " +
    "Here is the order I suggest:\n\n1. Review the *migration plan* first, since two tasks depend on it.\n" +
    "2. Update the changelog.\n3. Draft the handover notes.\n\n| Task | Due |\n| :-- | --: |\n" +
    "| Migration plan | Today |\n| Changelog | Friday |\n\n```bash\nnpm run release -- --dry-run\n```\n\n" +
    "Let me know if you want the plan as a checklist."

/** Characters added per tick of the sample stream. */
private const val Step = 4

/** Milliseconds between ticks of the sample stream. */
private const val TickMillis = 35L

/** The catalogue page of AiStreamingText. */
object MonarchAiStreamingTextSection : CataloguePage {
    override val title = "AiStreamingText"

    @Composable
    override fun Content() {
        var length by rememberSaveable { mutableIntStateOf(0) }
        var running by rememberSaveable { mutableStateOf(false) }
        LaunchedEffect(running) {
            while (running && length < Reply.length) {
                delay(TickMillis)
                length = minOf(Reply.length, length + Step)
            }
            running = false
        }
        CatalogueCaption("A sample answer streamed a few characters at a time")
        MonarchButton(
            text = if (running) "Stop" else "Stream an answer",
            onClick = {
                if (!running) length = 0
                running = !running
            },
            variant = MonarchButtonVariant.Primary,
            size = MonarchButtonSize.Sm,
        )
        if (length > 0) MonarchAiStreamingText(text = Reply.take(length), streaming = running)
        CatalogueCaption("A finished answer is not announced")
        MonarchAiStreamingText(text = "The release notes are ready. Two items need a second review.", streaming = false)
    }
}
