package dev.monarchui.sample.sections.collaboration

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.collaboration.decisioncard.MonarchDecision
import dev.monarchui.components.collaboration.decisioncard.MonarchDecisionCard
import dev.monarchui.components.collaboration.decisioncard.MonarchDecisionOption
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme
import kotlinx.coroutines.delay

/** The catalogue page of DecisionCard. */
object MonarchDecisionCardSection : CataloguePage {
    override val title = "DecisionCard"

    private const val SaveDelayMillis = 800L

    private val options = listOf(
        MonarchDecisionOption(
            label = "Keep",
            value = "keep",
            description = "Renew for another year.",
            tone = MonarchTone.Success,
        ),
        MonarchDecisionOption(
            label = "Review",
            value = "review",
            description = "Decide next month.",
            tone = MonarchTone.Warning,
        ),
        MonarchDecisionOption(
            label = "Let lapse",
            value = "lapse",
            description = "Release the domain when it expires.",
            note = "Email for this domain stops working.",
            tone = MonarchTone.Danger,
            reasonRequired = true,
        ),
    )

    @Composable
    override fun Content() {
        var value by rememberSaveable { mutableStateOf<String?>("keep") }
        var reason by rememberSaveable { mutableStateOf<String?>(null) }
        var decidedAt by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }
        var pending by rememberSaveable { mutableStateOf<String?>(null) }
        var pendingReason by rememberSaveable { mutableStateOf<String?>(null) }
        // Stands in for the app's request: stores the decision after a short wait.
        LaunchedEffect(pending, pendingReason) {
            val next = pending ?: return@LaunchedEffect
            delay(SaveDelayMillis)
            value = next.ifEmpty { null }
            reason = pendingReason
            decidedAt = System.currentTimeMillis()
            pending = null
        }
        CatalogueCaption("Editable")
        MonarchDecisionCard(
            options = options,
            value = value,
            title = "Renewal decision",
            reason = reason,
            decidedAt = decidedAt,
            decidedBy = "Ana",
            saving = pending != null,
            onDecide = { decision: MonarchDecision ->
                pendingReason = decision.reason
                pending = decision.value.orEmpty()
            },
        ) {
            MonarchText(
                text = "example.com, renews on 1 March, used by 12 people.",
                style = MonarchTextStyle.Caption,
                color = MonarchTheme.colors.textMuted,
            )
        }
        CatalogueCaption("Read-only")
        MonarchDecisionCard(
            options = options,
            value = value,
            reason = reason,
            decidedAt = decidedAt,
            decidedBy = "Ana",
            readOnly = true,
        )
    }
}
