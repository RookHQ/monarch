@file:OptIn(ExperimentalMonarchApi::class, ExperimentalMaterial3Api::class)

package dev.monarchui.sample.sections.identity

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
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.feedback.inlinenotice.MonarchInlineNotice
import dev.monarchui.components.forms.taginput.MonarchTagInput
import dev.monarchui.components.forms.textfield.MonarchTextField
import dev.monarchui.components.identity.appaccesspicker.MonarchAppAccessOption
import dev.monarchui.components.identity.appaccesspicker.MonarchAppAccessPicker
import dev.monarchui.components.identity.rolepicker.MonarchRoleOption
import dev.monarchui.components.identity.rolepicker.MonarchRolePicker
import dev.monarchui.components.identity.rolepicker.MonarchRolePickerLayout
import dev.monarchui.components.overlays.bottomsheet.MonarchBottomSheet
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.sampleicons.SampleIcons
import dev.monarchui.theme.MonarchTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** The catalogue page of InviteMembersDialog: a sheet that invites by email; addresses with "taken" are refused. */
object InviteMembersDialogSection : CataloguePage {
    override val title = "InviteMembersDialog"

    private const val SimulatedSendMillis = 800L
    private const val MaxEmails = 50
    private val EmailPattern = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

    @Composable
    override fun Content() {
        val scope = rememberCoroutineScope()
        var open by rememberSaveable { mutableStateOf(false) }
        var emails by rememberSaveable { mutableStateOf(listOf<String>()) }
        var role by rememberSaveable { mutableStateOf<String?>("member") }
        var apps by rememberSaveable { mutableStateOf(listOf("crm")) }
        var note by rememberSaveable { mutableStateOf("") }
        var sending by remember { mutableStateOf(false) }
        var fieldError by remember { mutableStateOf<String?>(null) }
        var failures by remember { mutableStateOf(mapOf<String, String>()) }
        var invited by rememberSaveable { mutableIntStateOf(0) }
        CatalogueCaption("Invite people; an address with \"taken\" is already a member")
        MonarchButton(text = "Invite people", onClick = { open = true }, variant = MonarchButtonVariant.Primary)
        MonarchText(text = "Invited so far: $invited", color = MonarchTheme.colors.textMuted)
        if (!open) return
        MonarchBottomSheet(
            onDismissRequest = { open = false },
            title = "Invite people",
            description = "They’ll get an email and join once they accept the invitation.",
            dismissible = !sending,
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space4),
            ) {
                MonarchTagInput(
                    tags = emails,
                    onTagsChange = {
                        emails = it
                        fieldError = null
                        failures = failures.filterKeys { email -> email in it }
                    },
                    label = "Email addresses",
                    removeIcon = {
                        MonarchIcon(
                            imageVector = SampleIcons.Close,
                            contentDescription = null,
                        )
                    },
                    enabled = !sending,
                    placeholder = "name@example.com",
                    helperText = "Separate several addresses with commas, spaces or semicolons.",
                    errorText = fieldError,
                    separators = setOf(',', ' ', ';'),
                    maxCount = MaxEmails,
                    normalize = { it.trim().removePrefix("<").removeSuffix(">") },
                    validate = { tag, _ ->
                        if (EmailPattern.matches(tag)) null else "“$tag” is not an email address."
                    },
                )
                if (failures.isNotEmpty()) {
                    MonarchInlineNotice(
                        title = "${failures.size} addresses couldn’t be invited",
                        message = failures.entries.joinToString("\n") { (email, reason) -> "$email: $reason" },
                        tone = MonarchTone.Danger,
                    )
                }
                MonarchRolePicker(
                    roles = listOf(
                        MonarchRoleOption("member", title = "Member", description = "Uses the apps they are given."),
                        MonarchRoleOption("admin", title = "Admin", description = "Manages members, apps and billing."),
                    ),
                    selected = role,
                    onSelect = { role = it },
                    layout = MonarchRolePickerLayout.Compact,
                    enabled = !sending,
                    label = "Role",
                )
                MonarchAppAccessPicker(
                    apps = listOf(
                        MonarchAppAccessOption("crm", "CRM", description = "Contacts and deals"),
                        MonarchAppAccessOption("books", "Books", description = "Invoices"),
                    ),
                    value = apps,
                    onValueChange = { apps = it },
                    enabled = !sending,
                )
                MonarchTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = "Personal note",
                    helperText = "Optional",
                    enabled = !sending,
                    singleLine = false,
                )
                MonarchButton(
                    text = if (emails.size > 1) "Send ${emails.size} invites" else "Send invite",
                    onClick = {
                        if (emails.isEmpty()) {
                            fieldError = "Add at least one email address."
                        } else {
                            sending = true
                            scope.launch {
                                delay(SimulatedSendMillis)
                                val refused = emails.filter { "taken" in it }.associateWith { "Already a member" }
                                invited += emails.size - refused.size
                                sending = false
                                failures = refused
                                emails = emails.filter { it in refused }
                                if (refused.isEmpty()) this@MonarchBottomSheet.dismiss()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    variant = MonarchButtonVariant.Primary,
                    loading = sending,
                )
                MonarchButton(
                    text = "Cancel",
                    onClick = { this@MonarchBottomSheet.dismiss() },
                    modifier = Modifier.fillMaxWidth(),
                    variant = MonarchButtonVariant.Ghost,
                    enabled = !sending,
                )
            }
        }
    }
}
