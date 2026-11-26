package com.rookhq.monarch.sample.sections.workflow

import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.forms.entitypicker.MonarchEntityPickerEntity
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.components.workflow.approvalchaineditor.MonarchApprovalApproverType
import com.rookhq.monarch.components.workflow.approvalchaineditor.MonarchApprovalChainEditor
import com.rookhq.monarch.components.workflow.approvalchaineditor.MonarchApprovalChainStep
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme

/** The catalogue page of ApprovalChainEditor: the approval chain of expense claims. */
object MonarchApprovalChainEditorSection : CataloguePage {
    override val title = "ApprovalChainEditor"

    private val types = listOf(
        MonarchApprovalApproverType("manager", "Reporting manager"),
        MonarchApprovalApproverType(
            "role",
            "Anyone with a role",
            targets = listOf(
                MonarchEntityPickerEntity("finance", "Finance team"),
                MonarchEntityPickerEntity("legal", "Legal team"),
            ),
            targetLabel = "Role",
        ),
        MonarchApprovalApproverType(
            "person",
            "A named person",
            targets = listOf(
                MonarchEntityPickerEntity("u1", "Asha Rao", description = "Head of finance"),
                MonarchEntityPickerEntity("u2", "Tom Berg", description = "General counsel"),
            ),
            targetLabel = "Person",
        ),
    )

    private const val HEIGHT_STEPS = 6

    @Composable
    override fun Content() {
        CatalogueCaption("Expense approvals")
        var steps by remember {
            mutableStateOf(
                listOf(
                    MonarchApprovalChainStep("s1", "manager"),
                    MonarchApprovalChainStep("s2", "role", label = "Finance check"),
                ),
            )
        }
        var saveTried by rememberSaveable { mutableStateOf(false) }
        // The list is lazy, so the page gives it a bounded height.
        MonarchApprovalChainEditor(
            value = steps,
            onValueChange = { steps = it },
            approverTypes = types,
            modifier = Modifier.heightIn(max = MonarchTheme.spacing.space24 * HEIGHT_STEPS),
            description = "Each step must approve before the next is asked.",
            showErrors = saveTried,
        )
        MonarchButton(text = "Save", onClick = { saveTried = true }, variant = MonarchButtonVariant.Primary)
        MonarchText(text = MonarchApprovalChainStep.summarize(steps, types))
    }
}
