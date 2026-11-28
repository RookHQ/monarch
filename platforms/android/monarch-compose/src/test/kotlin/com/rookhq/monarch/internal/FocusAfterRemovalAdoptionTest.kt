package com.rookhq.monarch.internal

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.components.files.attachmentchip.MonarchUploadStatus
import com.rookhq.monarch.components.files.documentrelations.MonarchDocumentRelation
import com.rookhq.monarch.components.files.documentrelations.MonarchDocumentRelationTarget
import com.rookhq.monarch.components.files.documentrelations.MonarchDocumentRelations
import com.rookhq.monarch.components.files.uploadqueue.MonarchUploadQueue
import com.rookhq.monarch.components.files.uploadqueue.MonarchUploadQueueItem
import com.rookhq.monarch.components.forms.textfield.MonarchTextField
import com.rookhq.monarch.components.intelligence.aiattachmenttray.MonarchAiAttachmentTray
import com.rookhq.monarch.components.intelligence.aiattachmenttray.MonarchAiAttachmentTrayFile
import com.rookhq.monarch.components.intelligence.aicontextchips.MonarchAiContextChips
import com.rookhq.monarch.components.intelligence.aicontextchips.MonarchAiContextItem
import com.rookhq.monarch.components.intelligence.aicontextchips.MonarchAiContextKind
import com.rookhq.monarch.components.navigation.favoriteslist.MonarchFavoriteItem
import com.rookhq.monarch.components.navigation.favoriteslist.MonarchFavoritesList
import com.rookhq.monarch.components.scheduling.businesshourseditor.MonarchBusinessHoursEditor
import com.rookhq.monarch.components.scheduling.businesshourseditor.MonarchBusinessHoursException
import com.rookhq.monarch.components.scheduling.businesshourseditor.MonarchBusinessHoursValue
import com.rookhq.monarch.components.scheduling.timeoffpanel.MonarchTimeOffEntry
import com.rookhq.monarch.components.scheduling.timeoffpanel.MonarchTimeOffPanel
import com.rookhq.monarch.components.scheduling.timeoffpanel.MonarchTimeOffPanelLabels
import com.rookhq.monarch.testing.setMonarchContent
import kotlin.test.Test
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Focus after a removal, where the web moves it: to the next item, else the previous one, else the list's
 * fallback. One test per component that adopts `MonarchFocusAfterRemoval`.
 */
@OptIn(ExperimentalMonarchApi::class)
@RunWith(RobolectricTestRunner::class)
class FocusAfterRemovalAdoptionTest {
    @get:Rule
    val rule = createComposeRule()

    // Buttons take input focus with a keyboard (a tablet keyboard, switch access), not in touch mode, as on a
    // device, so each test runs in keyboard mode and activates buttons with the click action.
    private fun SemanticsNodeInteraction.activate() = performSemanticsAction(SemanticsActions.OnClick)

    private fun keyboardContent(content: @Composable () -> Unit) {
        lateinit var inputMode: InputModeManager
        rule.setMonarchContent {
            inputMode = LocalInputModeManager.current
            content()
        }
        rule.runOnIdle { inputMode.requestInputMode(InputMode.Keyboard) }
    }

    private fun removeAndExpect(remove: String, focused: String) {
        rule.onNodeWithContentDescription(remove).activate()
        rule.waitForIdle()
        rule.onNodeWithContentDescription(focused).assertIsFocused()
    }

    @Test
    fun contextChipsFocusTheNextChipThenTheFallback() {
        keyboardContent {
            var items by remember {
                mutableStateOf(
                    listOf(
                        MonarchAiContextItem("a", MonarchAiContextKind.File, "a.pdf"),
                        MonarchAiContextItem("b", MonarchAiContextKind.File, "b.pdf"),
                        MonarchAiContextItem("c", MonarchAiContextKind.File, "c.pdf"),
                    ),
                )
            }
            val prompt = remember { FocusRequester() }
            Column {
                MonarchAiContextChips(
                    items = items,
                    onRemove = { id -> items = items.filterNot { it.id == id } },
                    fallbackFocusRequester = prompt,
                )
                MonarchTextField(
                    value = "",
                    onValueChange = {},
                    label = "Prompt",
                    modifier = Modifier.focusRequester(prompt),
                )
            }
        }
        rule.onNodeWithContentDescription("File: b.pdf").activate()
        rule.waitForIdle()
        // The chip's own semantics are replaced, so its focus is proved with the keyboard: Enter removes the
        // focused chip, which must be c.pdf, the next one.
        rule.onRoot().performKeyInput { pressKey(Key.Enter) }
        rule.waitForIdle()
        rule.onNodeWithContentDescription("File: c.pdf").assertDoesNotExist()
        rule.onNodeWithContentDescription("File: a.pdf").assertExists()
        // Then the previous chip, a.pdf, has focus; removing it, the last one, focuses the prompt field.
        rule.onRoot().performKeyInput { pressKey(Key.Enter) }
        rule.waitForIdle()
        rule.onNodeWithContentDescription("File: a.pdf").assertDoesNotExist()
        rule.onNode(hasSetTextAction()).assertIsFocused()
    }

    @Test
    fun theAttachmentTrayFocusesTheNextFile() {
        keyboardContent {
            var files by remember {
                mutableStateOf(
                    listOf(
                        MonarchAiAttachmentTrayFile("1", "one.pdf"),
                        MonarchAiAttachmentTrayFile("2", "two.pdf"),
                        MonarchAiAttachmentTrayFile("3", "three.pdf"),
                    ),
                )
            }
            MonarchAiAttachmentTray(files = files, onRemove = { id -> files = files.filterNot { it.id == id } })
        }
        removeAndExpect("Remove two.pdf", "Remove three.pdf")
    }

    @Test
    fun theFavoritesListFocusesTheNextRemoveButtonThenDone() {
        keyboardContent {
            var items by remember {
                mutableStateOf(
                    listOf(
                        MonarchFavoriteItem("Roadmap", id = "r"),
                        MonarchFavoriteItem("Planning", id = "p"),
                    ),
                )
            }
            MonarchFavoritesList(
                items = items,
                onSelect = {},
                editing = true,
                onRemove = { item -> items = items - item },
            )
        }
        removeAndExpect("Remove Roadmap from favorites", "Remove Planning from favorites")
    }

    @Test
    fun documentRelationsFocusTheNextRemoveButtonThenAdd() {
        val targets = listOf("Plan", "Pricing", "Brief").map { MonarchDocumentRelationTarget(it.lowercase(), it) }
        keyboardContent {
            var relations by remember {
                mutableStateOf(targets.map { MonarchDocumentRelation("r-${it.id}", "related-to", it) })
            }
            MonarchDocumentRelations(
                relations = relations,
                onAdd = { _, _ -> },
                onRemove = { id -> relations = relations.filterNot { it.id == id } },
            )
        }
        removeAndExpect("Remove relation to Brief", "Remove relation to Pricing")
        rule.onNodeWithContentDescription("Remove relation to Pricing").activate()
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Remove relation to Plan").activate()
        rule.waitForIdle()
        rule.onNodeWithText("Add relation", substring = true, ignoreCase = true).assertIsFocused()
    }

    @Test
    fun theUploadQueueFocusesTheNextRowsFirstButton() {
        keyboardContent {
            var items by remember {
                mutableStateOf(
                    listOf("a", "b", "c").map {
                        MonarchUploadQueueItem(id = it, name = "$it.pdf", status = MonarchUploadStatus.Done)
                    },
                )
            }
            MonarchUploadQueue(items = items, onRemove = { id -> items = items.filterNot { it.id == id } })
        }
        removeAndExpect("Remove a.pdf from the list", "Remove b.pdf from the list")
    }

    @Test
    fun theTimeOffPanelFocusesTheNextEntryAfterTheConfirmedRemoval() {
        val start = 1_794_182_400_000L
        val entries = listOf("Alex", "Blake", "Casey").mapIndexed { index, name ->
            MonarchTimeOffEntry(
                id = "$index",
                personId = "p$index",
                allDay = true,
                start = start,
                end = start,
                personName = name,
            )
        }
        keyboardContent {
            var shown by remember { mutableStateOf(entries) }
            MonarchTimeOffPanel(
                entries = shown,
                onRemove = { entry -> shown = shown - entry },
                now = start - 86_400_000L,
                labels = MonarchTimeOffPanelLabels(removeEntry = { who, _ -> "Remove $who" }),
            )
        }
        rule.onNodeWithContentDescription("Remove Blake").activate()
        rule.onAllNodesWithText("Remove").filterToOne(hasAnyAncestor(isDialog())).activate()
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Remove Casey").assertIsFocused()
    }

    @Test
    fun theBusinessHoursDatesFocusTheNextRemoveButton() {
        keyboardContent {
            var value by remember {
                mutableStateOf(
                    MonarchBusinessHoursValue(
                        timeZone = "UTC",
                        exceptions = listOf(
                            MonarchBusinessHoursException(date = 1_798_675_200_000L, name = "New Year"),
                            MonarchBusinessHoursException(date = 1_798_761_600_000L, name = "Second day"),
                        ),
                    ),
                )
            }
            MonarchBusinessHoursEditor(
                value = value,
                onValueChange = { value = it },
                modifier = Modifier.verticalScroll(rememberScrollState()),
                is24Hour = false,
            )
        }
        removeAndExpect("Remove New Year", "Remove Second day")
    }
}
