package com.rookhq.monarch.sample.sections.identity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
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
import com.rookhq.monarch.components.feedback.snackbarhost.MonarchSnackbarHost
import com.rookhq.monarch.components.forms.checkboxrow.MonarchCheckboxRow
import com.rookhq.monarch.components.identity.assigneepicker.MonarchAssigneePerson
import com.rookhq.monarch.components.identity.assigneepicker.MonarchAssigneePicker
import com.rookhq.monarch.components.identity.assigneepicker.MonarchAssigneePickerLabels
import com.rookhq.monarch.components.identity.memberrow.MonarchMemberRow
import com.rookhq.monarch.components.overlays.bottomsheet.MonarchBottomSheet
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val RemoveMillis = 800L

/** The catalogue page of RemoveMemberFlow: hand over open work, confirm in a sheet, then Undo in a snackbar. */
object RemoveMemberFlowSection : CataloguePage {
    override val title = "RemoveMemberFlow"

    private class OpenWork(val id: String, val label: String, val count: Int)

    private val work = listOf(OpenWork("issues", "open issues", 12), OpenWork("docs", "draft docs", 3))
    private val people = listOf(MonarchAssigneePerson("ada", "Ada Moss"), MonarchAssigneePerson("lee", "Lee Park"))

    @OptIn(ExperimentalMaterial3Api::class)
    @Suppress("LongMethod", "CyclomaticComplexMethod")
    @Composable
    override fun Content() {
        val snackbars = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()
        var open by rememberSaveable { mutableStateOf(false) }
        var step by rememberSaveable { mutableIntStateOf(0) }
        var allTo by rememberSaveable { mutableStateOf<String?>(null) }
        var perItem by rememberSaveable { mutableStateOf(false) }
        val each = remember { mutableStateMapOf<String, String?>() }
        var removing by remember { mutableStateOf(false) }
        var removed by rememberSaveable { mutableStateOf(false) }
        fun assigneeOf(workId: String): String? = if (perItem) each[workId] else allTo

        CatalogueCaption("Hand over open work, confirm, then Undo")
        if (removed) MonarchText(text = "Ben Okafor was removed.", color = MonarchTheme.colors.textMuted)
        MonarchButton(
            text = "Remove Ben Okafor",
            onClick = {
                step = 0
                allTo = null
                perItem = false
                each.clear()
                open = true
            },
            variant = MonarchButtonVariant.Danger,
            enabled = !removed,
        )
        MonarchSnackbarHost(hostState = snackbars)
        if (!open) return
        MonarchBottomSheet(onDismissRequest = { open = false }, title = "Remove Ben Okafor", dismissible = !removing) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space4),
            ) {
                if (step == 0) {
                    MonarchText(text = "Hand over their open work", style = MonarchTextStyle.Heading)
                    MonarchText(text = "Choose who takes over what Ben still owns, or leave it unassigned.")
                    if (!perItem) {
                        MonarchAssigneePicker(
                            people = people,
                            selected = allTo,
                            onSelectedChange = { allTo = it },
                            labels = MonarchAssigneePickerLabels(
                                label = "Reassign all open work to",
                                unassigned = "Leave unassigned",
                            ),
                        )
                        work.forEach { MonarchText(text = "${it.count} ${it.label}") }
                    }
                    MonarchCheckboxRow(
                        checked = perItem,
                        onCheckedChange = { on ->
                            if (on) work.forEach { each[it.id] = allTo }
                            perItem = on
                        },
                        label = "Choose a person for each kind of work",
                    )
                    if (perItem) {
                        work.forEach { w ->
                            MonarchAssigneePicker(
                                people = people,
                                selected = each[w.id],
                                onSelectedChange = { each[w.id] = it },
                                labels = MonarchAssigneePickerLabels(
                                    label = "${w.count} ${w.label}",
                                    unassigned = "Leave unassigned",
                                ),
                            )
                        }
                    }
                    MonarchButton(
                        text = "Continue",
                        onClick = { step = 1 },
                        modifier = Modifier.fillMaxWidth(),
                        variant = MonarchButtonVariant.Primary,
                    )
                } else {
                    MonarchText(text = "Confirm removal", style = MonarchTextStyle.Heading)
                    MonarchMemberRow(name = "Ben Okafor", email = "ben@example.com")
                    MonarchConsequenceList(
                        items = listOf(
                            MonarchConsequence(
                                MonarchConsequenceKind.Removed,
                                "Ben Okafor loses access to the workspace",
                            ),
                        ) + work.map { w ->
                            val to = people.firstOrNull { it.id == assigneeOf(w.id) }?.name
                            val what = "${w.count} ${w.label}"
                            if (to != null) {
                                MonarchConsequence(MonarchConsequenceKind.Changed, "$what go to $to")
                            } else {
                                MonarchConsequence(MonarchConsequenceKind.Warning, "$what are left unassigned")
                            }
                        } + MonarchConsequence(MonarchConsequenceKind.Kept, "Their past activity and comments stay"),
                    )
                    MonarchButton(
                        text = "Remove member",
                        onClick = {
                            removing = true
                            scope.launch {
                                delay(RemoveMillis)
                                removing = false
                                this@MonarchBottomSheet.dismiss()
                                removed = true
                                val result = snackbars.showSnackbar(
                                    message = "Ben Okafor removed",
                                    actionLabel = "Undo",
                                    duration = SnackbarDuration.Long,
                                )
                                if (result == SnackbarResult.ActionPerformed) removed = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        variant = MonarchButtonVariant.Danger,
                        loading = removing,
                    )
                    MonarchButton(
                        text = "Back",
                        onClick = { step = 0 },
                        modifier = Modifier.fillMaxWidth(),
                        variant = MonarchButtonVariant.Ghost,
                        enabled = !removing,
                    )
                }
            }
        }
    }
}
