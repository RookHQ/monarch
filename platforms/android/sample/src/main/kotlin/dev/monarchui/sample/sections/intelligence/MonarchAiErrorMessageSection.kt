package dev.monarchui.sample.sections.intelligence

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.intelligence.aierrormessage.MonarchAiErrorKind
import dev.monarchui.components.intelligence.aierrormessage.MonarchAiErrorMessage
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage

private const val RetryAfterMillis = 12_000L

/** The catalogue page of AiErrorMessage. */
object MonarchAiErrorMessageSection : CataloguePage {
    override val title = "AiErrorMessage"

    @Composable
    override fun Content() {
        var attempts by rememberSaveable { mutableIntStateOf(0) }
        var choice by rememberSaveable { mutableStateOf("") }
        CatalogueCaption("A lost connection, with retry and edit")
        MonarchAiErrorMessage(
            kind = MonarchAiErrorKind.fromCode("503"),
            announce = false,
            onRetry = { attempts++ },
            onEditPrompt = { choice = "edit" },
        )
        MonarchText(text = "Retries: $attempts")
        CatalogueCaption("A rate limit with a countdown and details")
        MonarchAiErrorMessage(
            kind = MonarchAiErrorKind.RateLimit,
            details = "request_id: req_8f3a2c",
            retryAfterMillis = RetryAfterMillis,
            announce = false,
            onRetry = { attempts++ },
        )
        CatalogueCaption("A conversation that is too long, with an extra action")
        MonarchAiErrorMessage(
            kind = MonarchAiErrorKind.ContextOverflow,
            announce = false,
            onNewConversation = { choice = "new" },
            onSummarize = { choice = "summarize" },
        ) {
            MonarchButton(
                text = "Learn about limits",
                onClick = { choice = "learn" },
                variant = MonarchButtonVariant.Ghost,
                size = MonarchButtonSize.Sm,
            )
        }
        if (choice.isNotEmpty()) MonarchText(text = "Chosen: $choice")
    }
}
