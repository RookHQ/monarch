package dev.monarchui.sample.sections.identity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.feedback.inlinenotice.MonarchInlineNotice
import dev.monarchui.components.forms.radiocardgroup.MonarchRadioCardGroup
import dev.monarchui.components.forms.radiocardgroup.MonarchRadioCardOption
import dev.monarchui.components.forms.textfield.MonarchTextField
import dev.monarchui.components.identity.usernameavailabilityinput.MonarchUsernameAvailabilityInput
import dev.monarchui.components.identity.usernameavailabilityinput.MonarchUsernameAvailabilityResult
import dev.monarchui.components.identity.usernameavailabilityinput.MonarchUsernameAvailabilityStatus
import dev.monarchui.components.overlays.bottomsheet.MonarchBottomSheet
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val SimulatedCreateMillis = 1_200L
private val NameRange = 4..25
private val UsernameRange = 5..30

/** The catalogue page of CreateWorkspaceDialog: the onboarding form in a sheet that locks while creating. */
object CreateWorkspaceDialogSection : CataloguePage {
    override val title = "CreateWorkspaceDialog"

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val scope = rememberCoroutineScope()
        var open by rememberSaveable { mutableStateOf(false) }
        var name by rememberSaveable { mutableStateOf("") }
        var username by rememberSaveable { mutableStateOf("") }
        var type by rememberSaveable { mutableStateOf<String?>(null) }
        var status by remember { mutableStateOf(MonarchUsernameAvailabilityStatus.Idle) }
        var submitted by rememberSaveable { mutableStateOf(false) }
        var creating by remember { mutableStateOf(false) }
        var created by rememberSaveable { mutableStateOf<String?>(null) }
        val nameError = if (submitted && name.trim().length !in NameRange) "Use 4 to 25 characters" else null
        CatalogueCaption("The sheet cannot be closed while the workspace is being created")
        created?.let { MonarchInlineNotice(message = "Workspace $it created.", tone = MonarchTone.Success) }
        MonarchButton(text = "New workspace", onClick = { open = true }, variant = MonarchButtonVariant.Primary)
        if (open) {
            MonarchBottomSheet(
                onDismissRequest = { open = false },
                title = "Create new workspace",
                dismissible = !creating,
            ) {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space4),
                ) {
                    MonarchTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = "Workspace name",
                        errorText = nameError,
                    )
                    MonarchUsernameAvailabilityInput(
                        value = username,
                        label = "Workspace username",
                        onValueChange = { username = it },
                        validate = { MonarchUsernameAvailabilityResult(valid = it.length in UsernameRange) },
                        prefix = "@",
                        onStatusChange = { next, _ -> status = next },
                    )
                    MonarchRadioCardGroup(
                        options = listOf(
                            MonarchRadioCardOption("creator", title = "Creator", description = "For personal projects"),
                            MonarchRadioCardOption("business", title = "Business", description = "For teams"),
                        ),
                        selected = type,
                        onSelect = { type = it },
                        label = "Type",
                    )
                    MonarchButton(
                        text = "Create workspace",
                        onClick = {
                            submitted = true
                            val valid = name.trim().length in NameRange && type != null
                            if (valid && status == MonarchUsernameAvailabilityStatus.Valid) {
                                creating = true
                                scope.launch {
                                    delay(SimulatedCreateMillis)
                                    creating = false
                                    created = name.trim()
                                    this@MonarchBottomSheet.dismiss()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        variant = MonarchButtonVariant.Primary,
                        loading = creating,
                    )
                    MonarchButton(
                        text = "Cancel",
                        onClick = { this@MonarchBottomSheet.dismiss() },
                        modifier = Modifier.fillMaxWidth(),
                        variant = MonarchButtonVariant.Ghost,
                        enabled = !creating,
                    )
                }
            }
        }
    }
}
