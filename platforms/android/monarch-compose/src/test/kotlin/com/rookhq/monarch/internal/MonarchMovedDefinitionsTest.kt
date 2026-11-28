package com.rookhq.monarch.internal

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.rookhq.monarch.components.workflow.approvalqueue.MonarchApprovalQueueItem
import com.rookhq.monarch.components.workflow.approvaltimeline.MonarchApprovalPerson
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.setMonarchContent
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Definitions promoted to internal/ from one component folder (C-042). */
@RunWith(RobolectricTestRunner::class)
class MonarchMovedDefinitionsTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun searchTextIgnoresCaseAndAccents() {
        assertEquals("zoe", MonarchSearchText.normalize("Zoë"))
        assertTrue(MonarchSearchText.contains("Ärzte ohne Grenzen", "arzte"))
        assertEquals(
            com.rookhq.monarch.components.forms.combobox.ComboboxFilter.normalize("Zoë"),
            MonarchSearchText.normalize("Zoë"),
        )
    }

    @Test
    fun theRemoveButtonIsAnIconButtonOrATextButtonWithTheSameName() {
        var removed = 0
        rule.setMonarchContent {
            Column {
                MonarchRemoveButton("Remove step 2", "Remove", onClick = { removed++ }, enabled = true, icon = null)
                MonarchRemoveButton("Remove step 3", "Remove", onClick = {}, enabled = false, icon = { Text("x") })
            }
        }
        rule.onNodeWithContentDescription("Remove step 2").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(1, removed)
        rule.onNodeWithContentDescription("Remove step 3").assertIsNotEnabled()
    }

    @Test
    fun theDatePickerDialogConfirmsTheShownDate() {
        val march4 = 1_772_582_400_000L
        var picked: Long? = null
        var closed = false
        rule.setMonarchContent {
            MonarchDatePickerDialog(
                initial = march4,
                earliest = null,
                confirmLabel = "Use date",
                cancelLabel = "Cancel",
                onPick = { picked = it },
                onClose = { closed = true },
            )
        }
        rule.onNode(hasText("Use date") and hasClickAction()).performClick()
        assertEquals(march4, picked)
        assertTrue(closed)
    }

    @Test
    fun cancellingTheDatePickerPicksNothing() {
        var picked: Long? = null
        rule.setMonarchContent {
            MonarchDatePickerDialog(null, null, "Use date", "Cancel", onPick = { picked = it }, onClose = {})
        }
        rule.onNodeWithText("Cancel").performClick()
        assertNull(picked)
    }

    @Test
    fun anApprovalRequestCanBeBuiltFromAPerson() {
        val item = MonarchApprovalQueueItem(
            id = "1",
            requester = MonarchApprovalPerson(name = "Ada Lovelace", seed = "ada"),
            title = "Expense claim",
        )
        assertEquals("Ada Lovelace", item.requesterName)
        assertEquals("ada", item.requester.seed)
        assertEquals("Ada Lovelace", item.requester.name)
    }
}
