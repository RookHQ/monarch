@file:OptIn(ExperimentalMonarchApi::class)

package com.rookhq.monarch.sample.sections.intelligence

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.components.intelligence.aiclaimsources.MonarchAiClaim
import com.rookhq.monarch.components.intelligence.aiclaimsources.MonarchAiClaimEvidence
import com.rookhq.monarch.components.intelligence.aiclaimsources.MonarchAiClaimEvidenceStance
import com.rookhq.monarch.components.intelligence.aiclaimsources.MonarchAiClaimSource
import com.rookhq.monarch.components.intelligence.aiclaimsources.MonarchAiClaimSources
import com.rookhq.monarch.components.intelligence.aiclaimsources.MonarchAiClaimSupport
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiSourceKind
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.sampleicons.SampleIcons

private const val PublishedMillis = 1_772_582_400_000L

private val SampleSources = listOf(
    MonarchAiClaimSource(
        id = "report",
        title = "Q3 retention report",
        kind = MonarchAiSourceKind.Doc,
        publishedMillis = PublishedMillis,
    ),
    MonarchAiClaimSource(
        id = "blog",
        title = "Pricing update",
        url = "https://example.com/pricing",
        kind = MonarchAiSourceKind.Web,
    ),
)

private val SampleClaims = listOf(
    MonarchAiClaim(
        id = "churn",
        text = "Churn fell to 2.1% in the third quarter.",
        support = MonarchAiClaimSupport.Supported,
        evidence = listOf(
            MonarchAiClaimEvidence(
                id = "e1",
                sourceId = "report",
                quote = "Monthly churn averaged 2.1% across July, August and September.",
                location = "Page 4",
                stance = MonarchAiClaimEvidenceStance.Supports,
            ),
        ),
    ),
    MonarchAiClaim(
        id = "cause",
        text = "The new pricing caused the drop.",
        support = MonarchAiClaimSupport.Conflicting,
        evidence = listOf(
            MonarchAiClaimEvidence(
                id = "e2",
                sourceId = "blog",
                quote = "Annual plans became cheaper in June.",
                stance = MonarchAiClaimEvidenceStance.Supports,
            ),
            MonarchAiClaimEvidence(
                id = "e3",
                sourceId = "report",
                quote = "Most of the change came from fewer failed payments.",
                location = "Page 6",
                stance = MonarchAiClaimEvidenceStance.Contradicts,
            ),
        ),
    ),
    MonarchAiClaim(id = "q4", text = "Q4 will be lower still.", support = MonarchAiClaimSupport.Unchecked),
)

/** The catalogue page of AiClaimSources. */
object MonarchAiClaimSourcesSection : CataloguePage {
    override val title = "AiClaimSources"

    @Composable
    override fun Content() {
        var event by rememberSaveable { mutableStateOf<String?>(null) }
        CatalogueCaption("Open a claim to see its passages")
        MonarchAiClaimSources(
            claims = SampleClaims,
            sources = SampleSources,
            onSelectClaim = { event = "Selected $it" },
            onOpenSource = { source, _ -> event = "Opened ${source.title}" },
            onFlag = { event = "Reported a problem with $it" },
            onVerify = { event = "Checking $it again" },
            expandIcon = { MonarchIcon(imageVector = SampleIcons.ChevronRight, contentDescription = null) },
        )
        event?.let { MonarchText(text = it) }
        CatalogueCaption("No claims")
        MonarchAiClaimSources(claims = emptyList(), sources = emptyList())
    }
}
