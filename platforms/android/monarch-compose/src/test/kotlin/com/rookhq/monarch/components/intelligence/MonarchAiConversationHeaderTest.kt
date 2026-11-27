@file:OptIn(ExperimentalMonarchApi::class)

package com.rookhq.monarch.components.intelligence

import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiAgentRef
import com.rookhq.monarch.components.intelligence.aiconversationheader.MonarchAiConversationHeader
import com.rookhq.monarch.components.intelligence.aiconversationheader.MonarchAiConversationHeaderLabels
import com.rookhq.monarch.components.intelligence.aiconversationheader.MonarchAiConversationHeaderTokens
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.TestIcon
import com.rookhq.monarch.testing.assertIsBefore
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.testing.stateDescription
import com.rookhq.monarch.theme.MonarchColors
import kotlinx.coroutines.CompletableDeferred
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private val Dossier = MonarchAiAgentRef(name = "Dossier", id = "dossier")

internal object MonarchAiConversationHeaderFixtures : ComponentFixtures {
    override val id = "ai-conversation-header"
    override val fixtures = listOf(
        fixture("titled, with agent, model and actions") {
            MonarchAiConversationHeader(
                title = "Q3 revenue summary",
                agent = Dossier,
                onRename = {},
                navigationIcon = {
                    IconButton(onClick = {}) { MonarchIcon(TestIcon, contentDescription = "Back") }
                },
                renameIcon = { MonarchIcon(TestIcon, contentDescription = null) },
                model = { Text("Fast model") },
            ) {
                IconButton(onClick = {}) { MonarchIcon(TestIcon, contentDescription = "Share") }
            }
        },
        fixture("untitled") {
            MonarchAiConversationHeader(title = "", onRename = {})
        },
        fixture("naming") {
            MonarchAiConversationHeader(title = "", naming = true, agent = Dossier)
        },
        fixture("long title, read only") {
            MonarchAiConversationHeader(
                title = "A comparison of the quarterly revenue of every region with last year's forecast",
                agent = Dossier,
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchAiConversationHeaderTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theTitleIsAHeadingAndAnEmptyTitleSaysNewConversation() {
        rule.setMonarchContent { MonarchAiConversationHeader(title = "  ") }
        rule.onNode(hasText("New conversation") and isHeading())
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Role))
        rule.onNode(hasText("New conversation") and hasClickAction()).assertDoesNotExist()
    }

    @Test
    fun whileNamingThePlaceholderShowsAndRenameIsNotOffered() {
        rule.setMonarchContent { MonarchAiConversationHeader(title = "", naming = true, onRename = {}) }
        val title = rule.onNode(hasText("Naming conversation…") and isHeading())
        assertEquals("Naming conversation…", title.stateDescription())
        rule.onNode(hasText("Naming conversation…") and hasClickAction()).assertDoesNotExist()
    }

    @Test
    fun tappingTheTitleRenamesItWithTheTrimmedText() {
        val renamed = mutableListOf<String>()
        rule.setMonarchContent {
            MonarchAiConversationHeader(title = "Draft", onRename = { renamed += it })
        }
        rule.onNode(hasText("Draft") and hasClickAction())
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, androidx.compose.ui.semantics.Role.Button))
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        rule.waitForIdle()
        rule.onNode(hasSetTextAction()).performTextReplacement("  Q3 plan  ")
        rule.onNodeWithText("Save").performClick()
        rule.waitForIdle()
        assertEquals(listOf("Q3 plan"), renamed)
        rule.onNode(hasSetTextAction()).assertDoesNotExist()
    }

    @Test
    fun anEmptyRenameClosesWithoutCallingTheApp() {
        var calls = 0
        rule.setMonarchContent { MonarchAiConversationHeader(title = "Draft", onRename = { calls++ }) }
        rule.onNode(hasText("Draft") and hasClickAction()).performClick()
        rule.waitForIdle()
        rule.onNode(hasSetTextAction()).performTextReplacement("   ")
        rule.onNodeWithText("Save").performClick()
        rule.waitForIdle()
        assertEquals(0, calls)
        rule.onNode(hasSetTextAction()).assertDoesNotExist()
    }

    @Test
    fun aFailedRenameKeepsTheEditorOpenWithTheMessage() {
        val gate = CompletableDeferred<Unit>()
        rule.setMonarchContent {
            MonarchAiConversationHeader(
                title = "Draft",
                onRename = {
                    gate.await()
                    error("conflict")
                },
            )
        }
        rule.onNode(hasText("Draft") and hasClickAction()).performClick()
        rule.waitForIdle()
        rule.onNode(hasSetTextAction()).performTextReplacement("Renamed")
        rule.onNodeWithText("Save").performClick()
        rule.waitForIdle()
        gate.complete(Unit)
        rule.waitForIdle()
        rule.onNodeWithText("Couldn’t rename. Try again.", useUnmergedTree = true).assertExists()
        rule.onNode(hasSetTextAction()).assertExists()
    }

    @Test
    fun theAgentIsReadWithItsRoleAndTheActionsAreAGroup() {
        rule.setMonarchContent {
            MonarchAiConversationHeader(title = "Draft", agent = Dossier, model = { Text("Fast model") }) {
                IconButton(onClick = {}) { MonarchIcon(TestIcon, contentDescription = "Share") }
            }
        }
        rule.onNodeWithContentDescription("AI assistant: Dossier").assertExists()
        rule.onNodeWithText("Fast model").assertExists()
        rule.onNodeWithContentDescription("Conversation actions").assertExists()
        rule.onNodeWithContentDescription("Share").assertTouchTargetAtLeast48dp(rule.density)
    }

    @Test
    fun theStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchAiConversationHeader(
                title = "",
                agent = Dossier,
                labels = MonarchAiConversationHeaderLabels(
                    untitled = "Neue Unterhaltung",
                    agent = { "Assistent: $it" },
                ),
            )
        }
        rule.onNodeWithText("Neue Unterhaltung").assertExists()
        rule.onNodeWithContentDescription("Assistent: Dossier").assertExists()
    }

    @Test
    fun theNavigationIconComesFirstRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchAiConversationHeader(
                title = "Draft",
                navigationIcon = {
                    IconButton(onClick = {}, modifier = Modifier.testTag("back")) {
                        MonarchIcon(TestIcon, contentDescription = "Back")
                    }
                },
            ) {
                IconButton(onClick = {}, modifier = Modifier.testTag("share")) {
                    MonarchIcon(TestIcon, contentDescription = "Share")
                }
            }
        }
        val title = rule.onNodeWithText("Draft", useUnmergedTree = true)
        rule.onNodeWithTag("back").assertIsBefore(title, LayoutDirection.Rtl)
        title.assertIsBefore(rule.onNodeWithTag("share"), LayoutDirection.Rtl)
    }

    @Test
    fun aLongTitleWrapsAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchAiConversationHeader(title = "Quarterly revenue by region", agent = Dossier)
        }
        rule.onNodeWithText("Quarterly revenue by region", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun theColoursComeFromTheTheme() {
        val t = MonarchAiConversationHeaderTokens
        AllThemes.forEach { theme ->
            val c = MonarchColors(theme.colors)
            assertEquals(c.bgSurface, t.container(c), theme.id)
            assertEquals(c.borderSubtle, t.border(c), theme.id)
            assertEquals(c.textMuted, t.titleColor(placeholder = true, colors = c), theme.id)
            assertEquals(c.textDefault, t.titleColor(placeholder = false, colors = c), theme.id)
            assertEquals(c.textMuted, t.metaColor(c), theme.id)
        }
        assertEquals("Untitled", t.shownTitle(" ", "Untitled"))
        assertEquals("Plan", t.shownTitle(" Plan ", "Untitled"))
        assertTrue(t.renamable(hasRename = true, naming = false))
        assertFalse(t.renamable(hasRename = true, naming = true))
        assertFalse(t.renamable(hasRename = false, naming = false))
    }
}
