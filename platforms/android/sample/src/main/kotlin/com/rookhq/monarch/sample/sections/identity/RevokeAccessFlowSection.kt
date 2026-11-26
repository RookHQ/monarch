package com.rookhq.monarch.sample.sections.identity

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.feedback.consequencelist.MonarchConsequence
import com.rookhq.monarch.components.feedback.consequencelist.MonarchConsequenceKind
import com.rookhq.monarch.components.feedback.consequencelist.MonarchConsequenceList
import com.rookhq.monarch.components.forms.typetoconfirminput.MonarchTypeToConfirmInput
import com.rookhq.monarch.components.forms.typetoconfirminput.MonarchTypeToConfirmInputDefaults
import com.rookhq.monarch.components.overlays.bottomsheet.MonarchBottomSheet
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val KeyName = "CI deploy key"
private const val LastUsedAgoMillis = 3 * 60 * 60 * 1000L
private const val SimulatedRevokeMillis = 600L

/** The catalogue page of RevokeAccessFlow: a recipe of a sheet, a consequence list and a type-to-confirm input. */
object RevokeAccessFlowSection : CataloguePage {
    override val title = "RevokeAccessFlow"

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val scope = rememberCoroutineScope()
        var revoked by rememberSaveable { mutableStateOf(false) }
        var confirming by rememberSaveable { mutableStateOf(false) }
        var typed by remember { mutableStateOf("") }
        var revoking by remember { mutableStateOf(false) }
        val now = remember { System.currentTimeMillis() }
        val lastUsed = DateUtils.getRelativeTimeSpanString(now - LastUsedAgoMillis, now, DateUtils.MINUTE_IN_MILLIS)
        CatalogueCaption("An API key")
        MonarchText(text = KeyName, style = MonarchTextStyle.BodyStrong)
        MonarchText(
            text = if (revoked) "Revoked" else "mk_live_4f… · Last used $lastUsed",
            style = MonarchTextStyle.Caption,
        )
        MonarchButton(
            text = "Revoke",
            onClick = {
                typed = ""
                confirming = true
            },
            modifier = Modifier.semantics { contentDescription = "Revoke $KeyName" },
            variant = MonarchButtonVariant.Danger,
            size = MonarchButtonSize.Sm,
            enabled = !revoked,
        )
        if (confirming) {
            MonarchBottomSheet(
                onDismissRequest = { confirming = false },
                title = "Revoke “$KeyName”?",
                description = "Apps that use this key stop working at once. This can’t be undone.",
                dismissible = !revoking,
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space4),
                ) {
                    MonarchConsequenceList(
                        items = listOf(
                            MonarchConsequence(MonarchConsequenceKind.Removed, "The key stops working immediately"),
                            MonarchConsequence(MonarchConsequenceKind.Warning, "2 deployments use this key"),
                        ),
                        heading = "What happens",
                    )
                    MonarchTypeToConfirmInput(phrase = KeyName, value = typed, onValueChange = { typed = it })
                    MonarchButton(
                        text = "Revoke key",
                        onClick = {
                            revoking = true
                            scope.launch {
                                delay(SimulatedRevokeMillis)
                                revoking = false
                                revoked = true
                                this@MonarchBottomSheet.dismiss()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        variant = MonarchButtonVariant.Danger,
                        enabled = MonarchTypeToConfirmInputDefaults.matches(typed, KeyName),
                        loading = revoking,
                    )
                    MonarchButton(
                        text = "Cancel",
                        onClick = { this@MonarchBottomSheet.dismiss() },
                        modifier = Modifier.fillMaxWidth(),
                        variant = MonarchButtonVariant.Ghost,
                        enabled = !revoking,
                    )
                }
            }
        }
    }
}
