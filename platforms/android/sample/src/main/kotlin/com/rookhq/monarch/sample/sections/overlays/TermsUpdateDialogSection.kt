package com.rookhq.monarch.sample.sections.overlays

import android.icu.text.DateFormat
import android.icu.util.TimeZone
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.navigation.link.MonarchLink
import com.rookhq.monarch.components.navigation.link.MonarchLinkVariant
import com.rookhq.monarch.components.overlays.bottomsheet.MonarchBottomSheet
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme
import java.util.Date

/** 1 December 2026, 00:00 UTC, in epoch milliseconds. */
private const val EffectiveMillis = 1_796_083_200_000L

/** The effective date in the person's locale, such as "1 December 2026". */
private fun effectiveDate(): String {
    // The date is a calendar day in UTC; format it in UTC so it does not shift with the time zone.
    val format = DateFormat.getDateInstance(DateFormat.LONG).apply { timeZone = TimeZone.getTimeZone("UTC") }
    return format.format(Date(EffectiveMillis))
}

/** The catalogue page of TermsUpdateDialog, a recipe: a bottom sheet that cannot be dismissed until a choice. */
object TermsUpdateDialogSection : CataloguePage {
    override val title = "TermsUpdateDialog"

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        var open by rememberSaveable { mutableStateOf(false) }
        var answer by rememberSaveable { mutableStateOf("") }
        CatalogueCaption("Blocking: accept or decline")
        MonarchButton(text = "Show the terms update", onClick = { open = true })
        if (answer.isNotEmpty()) {
            MonarchText(text = answer, style = MonarchTextStyle.Caption, color = MonarchTheme.colors.textMuted)
        }
        if (open) {
            MonarchBottomSheet(
                onDismissRequest = { open = false },
                title = "We’ve updated our terms",
                description = "We now explain how long we keep invoices and how to export your data.",
                dismissible = false,
            ) {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space3),
                ) {
                    MonarchText(
                        text = "Effective from " + effectiveDate(),
                        style = MonarchTextStyle.Caption,
                        color = MonarchTheme.colors.textMuted,
                    )
                    MonarchLink(
                        text = "Terms of Service (version 7)",
                        onClick = { answer = "Opened the Terms of Service." },
                        variant = MonarchLinkVariant.Standalone,
                    )
                    MonarchLink(
                        text = "Privacy Policy (version 4)",
                        onClick = { answer = "Opened the Privacy Policy." },
                        variant = MonarchLinkVariant.Standalone,
                    )
                    MonarchButton(
                        text = "Accept",
                        onClick = {
                            answer = "Accepted."
                            this@MonarchBottomSheet.dismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        variant = MonarchButtonVariant.Primary,
                    )
                    MonarchButton(
                        text = "Decline and sign out",
                        onClick = {
                            answer = "Declined."
                            this@MonarchBottomSheet.dismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        variant = MonarchButtonVariant.Ghost,
                    )
                }
            }
        }
    }
}
