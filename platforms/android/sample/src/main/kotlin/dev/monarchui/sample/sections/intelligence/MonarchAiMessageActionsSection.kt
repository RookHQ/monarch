package dev.monarchui.sample.sections.intelligence

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.intelligence.aiconversation.MonarchAiMessageFeedback
import dev.monarchui.components.intelligence.aimessageactions.MonarchAiMessageActions
import dev.monarchui.components.overlays.menu.MonarchMenuItem
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.sample.aiActionIcon
import dev.monarchui.sampleicons.SampleIcons

private const val SampleAnswer = "The invoice is due on 4 November. I added a reminder for 1 November."

/** The catalogue page of AiMessageActions. */
object MonarchAiMessageActionsSection : CataloguePage {
    override val title = "AiMessageActions"

    @Composable
    override fun Content() {
        var feedback by rememberSaveable { mutableStateOf<MonarchAiMessageFeedback?>(null) }
        var speaking by rememberSaveable { mutableStateOf(false) }
        var last by rememberSaveable { mutableStateOf("") }
        CatalogueCaption("With icons (long-press an icon for its name)")
        MonarchAiMessageActions(
            messageText = SampleAnswer,
            onCopy = { last = "Copied" },
            onRegenerate = { last = "Regenerate" },
            feedback = feedback,
            onFeedback = { feedback = it },
            onShare = { last = "Share" },
            speaking = speaking,
            onReadAloud = { speaking = it },
            icon = { action, _ -> MonarchIcon(aiActionIcon(action), contentDescription = null) },
            moreIcon = { MonarchIcon(SampleIcons.More, contentDescription = null) },
            more = { close ->
                MonarchMenuItem(
                    text = "Report a problem",
                    onClick = {
                        last = "Report a problem"
                        close()
                    },
                )
            },
        )
        CatalogueCaption("As text, for a person's own message")
        MonarchAiMessageActions(messageText = "Draft the reply", onCopy = {}, onEdit = { last = "Edit" })
        if (last.isNotEmpty()) MonarchText(text = "Last action: $last")
    }
}
