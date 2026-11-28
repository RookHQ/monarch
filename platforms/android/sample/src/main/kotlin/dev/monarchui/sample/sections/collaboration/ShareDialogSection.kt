package dev.monarchui.sample.sections.collaboration

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.actions.copyfield.MonarchCopyField
import dev.monarchui.components.forms.entitypicker.MonarchEntityPicker
import dev.monarchui.components.forms.entitypicker.MonarchEntityPickerEntity
import dev.monarchui.components.identity.memberrow.MonarchMemberRow
import dev.monarchui.components.identity.rolepicker.MonarchRoleOption
import dev.monarchui.components.identity.rolepicker.MonarchRolePicker
import dev.monarchui.components.identity.rolepicker.MonarchRolePickerLayout
import dev.monarchui.components.overlays.bottomsheet.MonarchBottomSheet
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme

/**
 * The catalogue page of ShareDialog: a recipe in a MonarchBottomSheet
 * built from MonarchEntityPicker, MonarchRolePicker, MonarchMemberRow and
 * MonarchCopyField.
 */
object ShareDialogSection : CataloguePage {
    override val title = "ShareDialog"

    private val roles = listOf(
        MonarchRoleOption("viewer", "Viewer", description = "Can view"),
        MonarchRoleOption("editor", "Editor", description = "Can edit and comment"),
    )

    private val access = listOf(
        MonarchRoleOption("restricted", "Restricted", description = "Only people with access can open the link"),
        MonarchRoleOption("anyone", "Anyone with the link", description = "Can view"),
    )

    private val people = listOf(
        MonarchEntityPickerEntity("kat", "Katherine Johnson", description = "kat@example.com"),
        MonarchEntityPickerEntity("dor", "Dorothy Vaughan", description = "dorothy@example.com"),
        MonarchEntityPickerEntity("mel", "Melba Roy", description = "melba@example.com"),
    )

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        var open by rememberSaveable { mutableStateOf(false) }
        CatalogueCaption("Share a record with people")
        MonarchButton(text = "Share “Q3 plan”", onClick = { open = true }, variant = MonarchButtonVariant.Primary)
        if (open) {
            MonarchBottomSheet(onDismissRequest = { open = false }, title = "Share “Q3 plan”") {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space4),
                ) {
                    ShareContent()
                    MonarchButton(
                        text = "Done",
                        onClick = { this@MonarchBottomSheet.dismiss() },
                        modifier = Modifier.fillMaxWidth(),
                        variant = MonarchButtonVariant.Primary,
                    )
                }
            }
        }
    }

    @Composable
    private fun ShareContent() {
        var invitees by rememberSaveable { mutableStateOf(listOf<String>()) }
        var inviteRole by rememberSaveable { mutableStateOf<String?>("editor") }
        var general by rememberSaveable { mutableStateOf<String?>("restricted") }
        val members = remember { mutableStateListOf("Mary Jackson" to "editor") }
        MonarchEntityPicker(
            label = "Invite people",
            entities = people,
            selected = invitees,
            onSelectedChange = { invitees = it },
        )
        MonarchRolePicker(
            roles = roles,
            selected = inviteRole,
            onSelect = { inviteRole = it },
            layout = MonarchRolePickerLayout.Compact,
            label = "Role for new people",
        )
        MonarchButton(
            text = "Invite",
            onClick = {
                invitees.forEach { id -> members += people.first { it.id == id }.name to (inviteRole ?: "viewer") }
                invitees = emptyList()
            },
            enabled = invitees.isNotEmpty(),
        )
        MonarchText(
            text = "People with access",
            modifier = Modifier.semantics { heading() },
            style = MonarchTextStyle.Subheading,
        )
        MonarchMemberRow(name = "You", email = "you@example.com", role = "Owner", self = true)
        members.forEachIndexed { index, (name, role) ->
            MonarchMemberRow(
                name = name,
                role = role,
                roles = roles,
                onRoleChange = { next -> members[index] = name to next },
                onRemove = { members.removeAt(index) },
            )
        }
        MonarchRolePicker(
            roles = access,
            selected = general,
            onSelect = { general = it },
            layout = MonarchRolePickerLayout.Compact,
            label = "General access",
        )
        MonarchCopyField(value = "https://example.com/d/q3-plan", label = "Link")
    }
}
