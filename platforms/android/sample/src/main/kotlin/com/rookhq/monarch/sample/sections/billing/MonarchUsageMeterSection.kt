package com.rookhq.monarch.sample.sections.billing

import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.components.billing.usagemeter.MonarchUsageMeter
import com.rookhq.monarch.components.billing.usagemeter.MonarchUsageMeterItem
import com.rookhq.monarch.components.forms.switchrow.MonarchSwitchRow
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage

/** The catalogue page of UsageMeter. The slider stands in for usage that grows. */
object MonarchUsageMeterSection : CataloguePage {
    override val title = "UsageMeter"

    private const val STORAGE_LIMIT = 50.0
    private const val START_STORAGE = 30f
    private const val STORAGE_SLIDER_END = 70f
    private const val RESETS_AT = 1_796_083_200_000L
    private val storageRange = 0f..STORAGE_SLIDER_END
    private val fixedItems = listOf(
        MonarchUsageMeterItem("Requests", used = 4200.0, limit = 10000.0),
        MonarchUsageMeterItem("Seats", used = 4.0, description = "Seats are not limited on this plan"),
    )

    @Composable
    override fun Content() {
        var storage by rememberSaveable { mutableFloatStateOf(START_STORAGE) }
        var failed by rememberSaveable { mutableStateOf(false) }
        Slider(
            value = storage,
            onValueChange = { storage = it },
            modifier = Modifier.semantics { contentDescription = "Storage used" },
            valueRange = storageRange,
        )
        MonarchSwitchRow(checked = failed, onCheckedChange = { failed = it }, label = "Show the error state")
        CatalogueCaption("Usage against limits")
        MonarchUsageMeter(
            items = fixedItems + MonarchUsageMeterItem(
                label = "Storage",
                used = storage.toDouble(),
                limit = STORAGE_LIMIT,
                unit = "GB",
            ),
            title = "Usage",
            period = "This month",
            resetsAtMillis = RESETS_AT,
            announceChanges = true,
            error = if (failed) "Usage could not be loaded." else null,
            onRetry = { failed = false },
        )
    }
}
