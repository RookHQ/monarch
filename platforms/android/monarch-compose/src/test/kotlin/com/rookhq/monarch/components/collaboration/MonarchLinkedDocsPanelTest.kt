@file:OptIn(ExperimentalMonarchApi::class)

package com.rookhq.monarch.components.collaboration

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.components.collaboration.linkeddocspanel.MonarchLinkedDocsPanel
import com.rookhq.monarch.components.collaboration.linkeddocspanel.MonarchLinkedDocsPanelItem
import com.rookhq.monarch.components.collaboration.linkeddocspanel.MonarchLinkedDocsPanelKind
import com.rookhq.monarch.components.collaboration.linkeddocspanel.MonarchLinkedDocsPanelLabels
import com.rookhq.monarch.components.collaboration.linkeddocspanel.MonarchLinkedDocsPanelOption
import com.rookhq.monarch.components.collaboration.linkeddocspanel.MonarchLinkedDocsPanelTokens
import com.rookhq.monarch.components.files.filetypeicon.MonarchFileTypeFamily
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.MonarchTestEnvironment
import com.rookhq.monarch.testing.assertIsBefore
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.theme.MonarchColors
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private val Runbook = MonarchLinkedDocsPanelItem(
    id = "l1",
    title = "Release runbook",
    docId = "d1",
    meta = "Updated 2 hours ago",
    status = "Published",
    spaceId = "eng",
    spaceName = "Engineering",
)
private val Brief = MonarchLinkedDocsPanelItem(
    id = "l2",
    title = "Launch brief",
    docId = "d2",
    status = "Draft",
    spaceId = "mkt",
    spaceName = "Marketing",
)
private val DesignFile = MonarchLinkedDocsPanelItem(
    id = "l3",
    title = "Design file",
    kind = MonarchLinkedDocsPanelKind.Link,
    url = "https://www.design.example.com/file/123",
)
private val Docs = listOf(
    MonarchLinkedDocsPanelOption("d1", "Release runbook", "Engineering"),
    MonarchLinkedDocsPanelOption("d3", "Incident review template", "Engineering"),
    MonarchLinkedDocsPanelOption("d4", "Pricing notes", "Sales"),
)

internal object MonarchLinkedDocsPanelFixtures : ComponentFixtures {
    override val id = "linked-docs-panel"
    override val fixtures = listOf(
        fixture("one row, every action") {
            MonarchLinkedDocsPanel(
                items = listOf(Runbook),
                onOpen = {},
                docOptions = Docs,
                onLinkDoc = {},
                onAddLink = { _, _ -> },
                onUnlink = {},
            )
        },
        fixture("Space chips") { MonarchLinkedDocsPanel(items = listOf(Runbook, Brief), onOpen = {}) },
        fixture("a link, unlinking") {
            MonarchLinkedDocsPanel(items = listOf(DesignFile), onOpen = {}, onUnlink = {}, unlinkingId = "l3")
        },
        fixture("empty") { MonarchLinkedDocsPanel(items = emptyList(), onOpen = {}, onAddLink = { _, _ -> }) },
        fixture("empty, read-only") { MonarchLinkedDocsPanel(items = emptyList(), onOpen = {}, readOnly = true) },
        fixture("loading") { MonarchLinkedDocsPanel(items = null, onOpen = {}) },
        fixture("failed") {
            MonarchLinkedDocsPanel(items = null, onOpen = {}, errorMessage = "Offline.", onRetry = {})
        },
        fixture("notice") {
            MonarchLinkedDocsPanel(items = listOf(Runbook), onOpen = {}, notice = "You have no access to docs here.")
        },
        fixture("long title") {
            MonarchLinkedDocsPanel(
                items = listOf(
                    MonarchLinkedDocsPanelItem("l9", "Quarterly business review notes for the enterprise accounts"),
                ),
                onOpen = {},
                onUnlink = {},
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchLinkedDocsPanelTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun aRowIsOneButtonReadAsItsTitleMetaAndStatus() {
        val opened = mutableListOf<String>()
        rule.setMonarchContent {
            MonarchLinkedDocsPanel(items = listOf(Runbook, DesignFile), onOpen = { opened += it.id })
        }
        rule.onNode(isHeading() and hasText("Docs", substring = true)).assertExists()
        val row = rule.onNodeWithText("Release runbook")
        val texts = row.fetchSemanticsNode().config[SemanticsProperties.Text].map { it.text }
        assertEquals(listOf("Release runbook", "Updated 2 hours ago", "Published"), texts)
        row.assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.onNodeWithText("design.example.com", substring = true).assertExists()
        assertEquals(listOf("l1"), opened)
    }

    @Test
    fun unlinkIsASeparateNamedButtonAndBusyDisablesTheOthers() {
        val unlinked = mutableListOf<String>()
        var busy by mutableStateOf<String?>(null)
        rule.setMonarchContent {
            MonarchLinkedDocsPanel(
                items = listOf(Runbook, DesignFile),
                onOpen = {},
                onUnlink = { unlinked += it.id },
                unlinkingId = busy,
            )
        }
        val unlink = rule.onNodeWithContentDescription("Unlink Release runbook")
        unlink.assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(listOf("l1"), unlinked)
        busy = "l1"
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Unlink Design file").assertIsNotEnabled()
    }

    @Test
    fun readOnlyHidesEveryAction() {
        rule.setMonarchContent {
            MonarchLinkedDocsPanel(
                items = listOf(Runbook),
                onOpen = {},
                readOnly = true,
                docOptions = Docs,
                onLinkDoc = {},
                onAddLink = { _, _ -> },
                onUnlink = {},
            )
        }
        rule.onNodeWithText("Link doc").assertDoesNotExist()
        rule.onNodeWithText("Add link").assertDoesNotExist()
        rule.onNodeWithContentDescription("Unlink Release runbook").assertDoesNotExist()
    }

    @Test
    fun spaceChipsFilterTheRowsAndShowAllClearsTheFilter() {
        rule.setMonarchContent { MonarchLinkedDocsPanel(items = listOf(Runbook, Brief, DesignFile), onOpen = {}) }
        rule.onNodeWithText("All · 3").assertIsSelected()
        rule.onNodeWithText("Marketing · 1").assertIsNotSelected().performClick()
        rule.onNodeWithText("Release runbook").assertDoesNotExist()
        rule.onNodeWithText("Launch brief").assertExists()
        rule.onNodeWithText("2 more in other Spaces.").assertExists()
        rule.onNodeWithText("Show all").performClick()
        rule.onNodeWithText("Release runbook").assertExists()
        rule.onNodeWithContentDescription("Filter by Space").assertExists()
    }

    @Test
    fun theChipsAreSortedByNameWithNoSpaceLast() {
        val chips = MonarchLinkedDocsPanelTokens.chips(listOf(DesignFile, Brief, Runbook))
        assertEquals(listOf("eng", "mkt", MonarchLinkedDocsPanelTokens.NO_SPACE), chips.map { it.key })
        assertNull(chips.last().name)
        assertEquals(MonarchLinkedDocsPanelTokens.ALL, MonarchLinkedDocsPanelTokens.activeKey("gone", chips, true))
        assertEquals("mkt", MonarchLinkedDocsPanelTokens.activeKey("mkt", chips, true))
        assertEquals(MonarchLinkedDocsPanelTokens.ALL, MonarchLinkedDocsPanelTokens.activeKey("mkt", chips, false))
    }

    @Test
    fun onlyFullWebAddressesAreAccepted() {
        val tokens = MonarchLinkedDocsPanelTokens
        assertEquals("https://example.com/a", tokens.parseLinkUrl("  https://example.com/a "))
        assertNull(tokens.parseLinkUrl("example.com"))
        assertNull(tokens.parseLinkUrl("javascript:alert(1)"))
        assertNull(tokens.parseLinkUrl("ftp://example.com"))
        assertNull(tokens.parseLinkUrl("https://"))
        assertEquals("design.example.com", tokens.hostOf("https://www.design.example.com/x"))
        assertEquals(MonarchFileTypeFamily.Document, tokens.familyOf(Runbook))
        assertEquals(MonarchFileTypeFamily.Other, tokens.familyOf(DesignFile))
        val pdf = MonarchLinkedDocsPanelItem("x", "x", fileName = "a.pdf")
        assertEquals(MonarchFileTypeFamily.Pdf, tokens.familyOf(pdf))
    }

    @Test
    fun addLinkValidatesTheAddressAndUsesTheHostAsTheDefaultTitle() {
        val added = mutableListOf<Pair<String, String>>()
        rule.setMonarchContent {
            MonarchLinkedDocsPanel(
                items = emptyList(),
                onOpen = {},
                onAddLink = { url, title -> added += url to title },
            )
        }
        rule.onNodeWithText("Add link").performClick()
        val fields = rule.onAllNodes(hasSetTextAction())
        fields[0].performTextInput("example.com")
        rule.onNodeWithText("Add").performClick()
        // The message is the address field's error; TalkBack reads it with the field.
        val invalid = "Enter a full web address that starts with https://"
        fields[0].assert(SemanticsMatcher.expectValue(SemanticsProperties.Error, invalid))
        assertEquals(emptyList(), added)
    }

    @Test
    fun aValidLinkIsAddedAndTheFormCloses() {
        val added = mutableListOf<Pair<String, String>>()
        rule.setMonarchContent {
            MonarchLinkedDocsPanel(
                items = emptyList(),
                onOpen = {},
                onAddLink = { url, title -> added += url to title },
            )
        }
        rule.onNodeWithText("Add link").performClick()
        rule.onAllNodes(hasSetTextAction())[0].performTextInput("https://www.example.com/page")
        rule.onNodeWithText("Add").performClick()
        assertEquals(listOf("https://www.example.com/page" to "example.com"), added)
        assertEquals(0, rule.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
    }

    @Test
    fun theDocMenuLeavesOutLinkedDocsFiltersAndLinks() {
        val linked = mutableListOf<String>()
        var created = 0
        rule.setMonarchContent {
            MonarchLinkedDocsPanel(
                items = listOf(Runbook),
                onOpen = {},
                docOptions = Docs,
                onLinkDoc = { linked += it },
                onCreateDoc = { created++ },
            )
        }
        rule.onNodeWithText("Link doc").performClick()
        assertEquals(1, rule.onAllNodesWithText("Release runbook").fetchSemanticsNodes().size)
        rule.onNodeWithText("Pricing notes", substring = true).assertExists()
        rule.onNode(hasSetTextAction()).performTextInput("incident")
        rule.onNodeWithText("Pricing notes", substring = true).assertDoesNotExist()
        rule.onNodeWithText("Incident review template", substring = true).performClick()
        assertEquals(listOf("d3"), linked)
        rule.onNodeWithText("Link doc").performClick()
        rule.onNodeWithText("New doc").performClick()
        assertEquals(1, created)
    }

    @Test
    fun aMenuWithoutMatchesSaysSo() {
        assertEquals(
            emptyList(),
            MonarchLinkedDocsPanelTokens.options(Docs, linked = setOf("d1"), query = "zzz"),
        )
        assertEquals(
            listOf("d4"),
            MonarchLinkedDocsPanelTokens.options(Docs, linked = setOf("d1"), query = " sales ").map { it.docId },
        )
    }

    @Test
    fun emptyLoadingAndFailedStates() {
        var items by mutableStateOf<List<MonarchLinkedDocsPanelItem>?>(emptyList())
        var error by mutableStateOf<String?>(null)
        var retries = 0
        rule.setMonarchContent {
            MonarchLinkedDocsPanel(items = items, onOpen = {}, errorMessage = error, onRetry = { retries++ })
        }
        rule.onNodeWithText("Nothing linked yet.", substring = true).assertExists()
        items = null
        rule.waitForIdle()
        rule.onNodeWithText("Nothing linked yet.", substring = true).assertDoesNotExist()
        error = "The server did not answer."
        rule.waitForIdle()
        rule.onNodeWithText("The server did not answer.", substring = true).assertExists()
        items = listOf(Runbook)
        rule.waitForIdle()
        rule.onNodeWithText("Release runbook").assertExists()
        rule.onNodeWithText("The server did not answer.", substring = true).assertExists()
        rule.onNodeWithText("Try again").performClick()
        assertEquals(1, retries)
    }

    @Test
    fun theUnlinkButtonFollowsTheRowInBothDirections() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) {
            MonarchLinkedDocsPanel(items = listOf(Runbook), onOpen = {}, onUnlink = {})
        }
        LayoutDirection.entries.forEach { direction ->
            environment.layoutDirection = direction
            rule.waitForIdle()
            rule.onNodeWithText("Release runbook")
                .assertIsBefore(rule.onNodeWithContentDescription("Unlink Release runbook"), direction)
        }
    }

    @Test
    fun aLongTitleWrapsWholeAtTheLargestFontScale() {
        val long = "Quarterly business review notes for the enterprise accounts"
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchLinkedDocsPanel(items = listOf(MonarchLinkedDocsPanelItem("l9", long)), onOpen = {}, onUnlink = {})
        }
        rule.onNodeWithText(long, useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun theBuiltInStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchLinkedDocsPanel(
                items = listOf(Runbook, Brief),
                onOpen = {},
                docOptions = Docs,
                onLinkDoc = {},
                onAddLink = { _, _ -> },
                onUnlink = {},
                labels = MonarchLinkedDocsPanelLabels(
                    title = "Documents",
                    linkDoc = "Lier",
                    addLink = "Ajouter un lien",
                    all = "Tous",
                    chip = { name, count -> "$name : $count" },
                    unlink = { "Délier $it" },
                    unlinkText = "Délier",
                ),
            )
        }
        rule.onNode(isHeading() and hasText("Documents", substring = true)).assertExists()
        rule.onNodeWithText("Lier").assertExists()
        rule.onNodeWithText("Ajouter un lien").assertExists()
        rule.onNodeWithText("Tous : 2").assertExists()
        rule.onNodeWithContentDescription("Délier Launch brief").assertExists()
    }

    @Test
    fun secondaryTextIsMutedInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.textMuted, MonarchLinkedDocsPanelTokens.mutedText(colors))
        }
    }
}
