package com.rookhq.monarch.sample.sections.intelligence

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.intelligence.aisafetynotice.MonarchAiSafetyNotice
import com.rookhq.monarch.components.intelligence.aisafetynotice.MonarchAiSafetyNoticeKind
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage

/** The catalogue page of AiSafetyNotice. */
object MonarchAiSafetyNoticeSection : CataloguePage {
    override val title = "AiSafetyNotice"

    @Composable
    override fun Content() {
        var edits by rememberSaveable { mutableIntStateOf(0) }
        CatalogueCaption("A blocked request with a reason, next steps and actions")
        MonarchAiSafetyNotice(
            kind = MonarchAiSafetyNoticeKind.Blocked,
            reason = "Personal data",
            nextSteps = "Remove the phone number and ask again.",
            learnMoreUrl = "https://example.com/guidelines",
            onEditPrompt = { edits++ },
        )
        MonarchText(text = "Edit prompt pressed: $edits")
        CatalogueCaption("An adjusted answer")
        MonarchAiSafetyNotice(kind = MonarchAiSafetyNoticeKind.Modified)
        CatalogueCaption("Hidden content")
        MonarchAiSafetyNotice(kind = MonarchAiSafetyNoticeKind.Filtered)
    }
}
