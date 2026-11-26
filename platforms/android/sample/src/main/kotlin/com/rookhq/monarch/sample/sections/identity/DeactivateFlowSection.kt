package com.rookhq.monarch.sample.sections.identity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.feedback.consequencelist.MonarchConsequence
import com.rookhq.monarch.components.feedback.consequencelist.MonarchConsequenceKind
import com.rookhq.monarch.components.feedback.consequencelist.MonarchConsequenceList
import com.rookhq.monarch.components.feedback.inlinenotice.MonarchInlineNotice
import com.rookhq.monarch.components.forms.textfield.MonarchTextField
import com.rookhq.monarch.components.overlays.bottomsheet.MonarchBottomSheet
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val SimulatedRequestMillis = 800L

/** The catalogue page of DeactivateFlow: a recipe of a sheet, MonarchConsequenceList and MonarchTextField. */
object DeactivateFlowSection : CataloguePage {
    override val title = "DeactivateFlow"

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val scope = rememberCoroutineScope()
        var open by rememberSaveable { mutableStateOf(false) }
        var deactivated by rememberSaveable { mutableStateOf(false) }
        var reason by rememberSaveable { mutableStateOf("") }
        var working by remember { mutableStateOf(false) }
        var failure by remember { mutableStateOf<String?>(null) }
        var attempts by rememberSaveable { mutableIntStateOf(0) }
        val action = if (deactivated) "Reactivate" else "Deactivate"
        CatalogueCaption("The first request fails, so the error shows in the sheet")
        MonarchText(
            text = if (deactivated) "Northwind partnership: deactivated" else "Northwind partnership: active",
            color = MonarchTheme.colors.textMuted,
        )
        MonarchButton(text = action, onClick = { open = true })
        if (open) {
            MonarchBottomSheet(
                onDismissRequest = { open = false },
                title = "$action the Northwind partnership?",
                description = if (deactivated) {
                    "It starts working again straight away."
                } else {
                    "It stops working until you turn it back on. Nothing is deleted."
                },
                dismissible = !working,
            ) {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space4),
                ) {
                    if (!deactivated) {
                        MonarchText(
                            text = "You can reactivate it at any time from this page.",
                            color = MonarchTheme.colors.textMuted,
                        )
                    }
                    MonarchConsequenceList(
                        items = if (deactivated) {
                            listOf(MonarchConsequence(MonarchConsequenceKind.Changed, "Shared reports update again"))
                        } else {
                            listOf(
                                MonarchConsequence(MonarchConsequenceKind.Changed, "Shared reports stop updating"),
                                MonarchConsequence(MonarchConsequenceKind.Changed, "Partner users can’t sign in"),
                                MonarchConsequence(MonarchConsequenceKind.Kept, "Contracts and history are kept"),
                            )
                        },
                        heading = if (deactivated) "Once it is reactivated" else "While it is deactivated",
                    )
                    if (!deactivated) {
                        MonarchTextField(
                            value = reason,
                            onValueChange = { reason = it },
                            label = "Reason (optional)",
                            helperText = "Kept with the change history.",
                            enabled = !working,
                            singleLine = false,
                        )
                    }
                    failure?.let { MonarchInlineNotice(message = it, tone = MonarchTone.Danger) }
                    MonarchButton(
                        text = action,
                        onClick = {
                            working = true
                            failure = null
                            scope.launch {
                                delay(SimulatedRequestMillis)
                                working = false
                                if (attempts++ == 0) {
                                    failure = "Couldn’t update the partnership. Try again."
                                } else {
                                    deactivated = !deactivated
                                    reason = ""
                                    this@MonarchBottomSheet.dismiss()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        variant = MonarchButtonVariant.Primary,
                        loading = working,
                    )
                    MonarchButton(
                        text = "Cancel",
                        onClick = { this@MonarchBottomSheet.dismiss() },
                        modifier = Modifier.fillMaxWidth(),
                        variant = MonarchButtonVariant.Ghost,
                        enabled = !working,
                    )
                }
            }
        }
    }
}
