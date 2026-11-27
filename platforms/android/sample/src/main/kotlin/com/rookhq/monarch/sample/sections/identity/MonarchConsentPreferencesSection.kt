package com.rookhq.monarch.sample.sections.identity

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.forms.switchrow.MonarchSwitchRow
import com.rookhq.monarch.components.identity.consenthistory.MonarchConsentRecord
import com.rookhq.monarch.components.identity.consenthistory.MonarchConsentRegime
import com.rookhq.monarch.components.identity.consentpreferences.MonarchConsentPreferences
import com.rookhq.monarch.components.identity.consentpreferences.MonarchConsentPreferencesCategory
import com.rookhq.monarch.components.identity.consentpreferences.MonarchConsentPreferencesMode
import com.rookhq.monarch.components.overlays.bottomsheet.MonarchBottomSheet
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage

private const val PolicyVersion = "2026-03"

private val Categories = listOf(
    MonarchConsentPreferencesCategory(
        "necessary",
        "Strictly necessary",
        "Keeps you signed in and remembers your settings.",
        required = true,
    ),
    MonarchConsentPreferencesCategory("analytics", "Analytics", "Helps us see which screens people use."),
    MonarchConsentPreferencesCategory(
        "ads",
        "Advertising",
        "Shares what you look at with advertising partners.",
        saleOrSharing = true,
    ),
)

/** The catalogue page of ConsentPreferences. */
object MonarchConsentPreferencesSection : CataloguePage {
    override val title = "ConsentPreferences"

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        var open by rememberSaveable { mutableStateOf(false) }
        var signal by rememberSaveable { mutableStateOf(false) }
        var choices by rememberSaveable { mutableStateOf(mapOf<String, Boolean>()) }
        var record by remember { mutableStateOf<MonarchConsentRecord?>(null) }
        CatalogueCaption("In a sheet, as an app shows it")
        MonarchSwitchRow(
            checked = signal,
            onCheckedChange = { signal = it },
            label = "Simulate a Global Privacy Control signal",
        )
        MonarchButton(text = "Privacy choices", onClick = { open = true })
        if (open) {
            MonarchBottomSheet(onDismissRequest = { open = false }) {
                MonarchConsentPreferences(
                    categories = Categories,
                    onDecide = { method, decided ->
                        choices = decided
                        record = MonarchConsentRecord(
                            id = System.currentTimeMillis().toString(),
                            dateMillis = System.currentTimeMillis(),
                            method = method,
                            policyVersion = PolicyVersion,
                            regime = MonarchConsentRegime.OptIn,
                            categories = decided,
                            globalPrivacyControl = signal,
                        )
                        dismiss()
                    },
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    choices = choices,
                    globalPrivacyControl = signal,
                    record = record,
                    onPolicyClick = {},
                )
            }
        }
        CatalogueCaption("Choice mode, for a single optional category")
        MonarchConsentPreferences(
            categories = Categories.take(2),
            onDecide = { _, decided -> choices = decided },
            choices = choices,
            mode = MonarchConsentPreferencesMode.Choice,
        )
    }
}
