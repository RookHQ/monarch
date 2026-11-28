package com.rookhq.monarch.sample.sections.identity

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.identity.consenthistory.MonarchConsentCategory
import com.rookhq.monarch.components.identity.consenthistory.MonarchConsentHistory
import com.rookhq.monarch.components.identity.consenthistory.MonarchConsentMethod
import com.rookhq.monarch.components.identity.consenthistory.MonarchConsentRecord
import com.rookhq.monarch.components.identity.consenthistory.MonarchConsentRegime
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import kotlinx.coroutines.delay

private const val DayMillis = 86_400_000L
private const val SimulatedExportMillis = 800L

private val Categories = listOf(
    MonarchConsentCategory("necessary", "Necessary"),
    MonarchConsentCategory("analytics", "Analytics"),
    MonarchConsentCategory("marketing", "Marketing"),
)

private fun sampleRecords(now: Long) = listOf(
    MonarchConsentRecord(
        id = "first",
        dateMillis = now - 40 * DayMillis,
        method = MonarchConsentMethod.AcceptAll,
        policyVersion = "2026-01",
        regime = MonarchConsentRegime.OptIn,
        categories = mapOf("necessary" to true, "analytics" to true, "marketing" to true),
    ),
    MonarchConsentRecord(
        id = "second",
        dateMillis = now - 3 * DayMillis,
        method = MonarchConsentMethod.Custom,
        policyVersion = "2026-03",
        regime = MonarchConsentRegime.OptIn,
        categories = mapOf("necessary" to true, "analytics" to true, "marketing" to false),
        globalPrivacyControl = true,
    ),
)

/** The catalogue page of ConsentHistory. */
object MonarchConsentHistorySection : CataloguePage {
    override val title = "ConsentHistory"

    @Composable
    override fun Content() {
        var exports by rememberSaveable { mutableIntStateOf(0) }
        CatalogueCaption("Newest first. Every second download fails.")
        MonarchConsentHistory(
            records = sampleRecords(System.currentTimeMillis()),
            categories = Categories,
            description = "Choices about cookies and similar storage on this device.",
            showRelativeTime = true,
            exportTitle = "Download as CSV",
            onExport = {
                delay(SimulatedExportMillis)
                exports++
                if (exports % 2 == 0) error("The file could not be written")
            },
        )
        CatalogueCaption("No decisions yet")
        MonarchConsentHistory(records = emptyList())
    }
}
