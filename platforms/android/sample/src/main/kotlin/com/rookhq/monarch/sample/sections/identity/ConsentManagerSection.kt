package com.rookhq.monarch.sample.sections.identity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
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
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.feedback.banner.MonarchBanner
import com.rookhq.monarch.components.identity.consenthistory.MonarchConsentMethod
import com.rookhq.monarch.components.identity.consenthistory.MonarchConsentRecord
import com.rookhq.monarch.components.identity.consenthistory.MonarchConsentRegime
import com.rookhq.monarch.components.identity.consentpreferences.MonarchConsentPreferences
import com.rookhq.monarch.components.identity.consentpreferences.MonarchConsentPreferencesCategory
import com.rookhq.monarch.components.overlays.bottomsheet.MonarchBottomSheet
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme

/** The catalogue page of ConsentProvider: the app's consent state, a first ask and the preferences sheet. */
object ConsentManagerSection : CataloguePage {
    override val title = "ConsentProvider"

    private val categories = listOf(
        MonarchConsentPreferencesCategory("necessary", "Strictly necessary", "Keeps you signed in.", required = true),
        MonarchConsentPreferencesCategory("analytics", "Analytics", "Helps us see which screens people use."),
        MonarchConsentPreferencesCategory("marketing", "Marketing", "Shows offers based on what you use."),
    )

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        var record by remember { mutableStateOf<MonarchConsentRecord?>(null) }
        var choosing by rememberSaveable { mutableStateOf(false) }
        val decide: (MonarchConsentMethod, Map<String, Boolean>) -> Unit = { method, decided ->
            val now = System.currentTimeMillis()
            record = MonarchConsentRecord(
                id = "consent-$now",
                dateMillis = now,
                method = method,
                policyVersion = "2026-01",
                regime = MonarchConsentRegime.OptIn,
                categories = decided,
            )
            choosing = false
        }
        CatalogueCaption("Ask once, change from settings")
        if (record == null) {
            MonarchBanner(
                message = "We use analytics to improve the app. You can change this at any time.",
                title = "Your privacy choices",
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2)) {
                MonarchButton(
                    text = "Reject all",
                    onClick = { decide(MonarchConsentMethod.RejectAll, categories.associate { it.id to it.required }) },
                )
                MonarchButton(
                    text = "Accept all",
                    onClick = { decide(MonarchConsentMethod.AcceptAll, categories.associate { it.id to true }) },
                )
                MonarchButton(text = "Choose", onClick = { choosing = true }, variant = MonarchButtonVariant.Ghost)
            }
        }
        val allowed = categories.filter { record?.categories?.get(it.id) == true }.joinToString { it.label }
        MonarchText(
            text = if (record == null) "Not asked yet." else "Allowed: ${allowed.ifEmpty { "nothing optional" }}",
            color = MonarchTheme.colors.textMuted,
        )
        MonarchButton(text = "Privacy choices", onClick = { choosing = true }, variant = MonarchButtonVariant.Ghost)
        if (choosing) {
            MonarchBottomSheet(onDismissRequest = { choosing = false }) {
                MonarchConsentPreferences(
                    categories = categories,
                    onDecide = decide,
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    choices = record?.categories.orEmpty(),
                    record = record,
                )
            }
        }
    }
}
