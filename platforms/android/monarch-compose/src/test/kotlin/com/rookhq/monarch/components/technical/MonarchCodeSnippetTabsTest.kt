package com.rookhq.monarch.components.technical

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.core.app.ApplicationProvider
import com.rookhq.monarch.components.technical.codesnippettabs.MonarchCodeSnippet
import com.rookhq.monarch.components.technical.codesnippettabs.MonarchCodeSnippetTabs
import com.rookhq.monarch.components.technical.codesnippettabs.MonarchCodeSnippetTabsSync
import com.rookhq.monarch.components.technical.codesnippettabs.MonarchCodeSnippetTabsTokens
import com.rookhq.monarch.components.typography.codeblock.MonarchCodeBlockLabels
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.MonarchTestEnvironment
import com.rookhq.monarch.testing.assertIsBefore
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private val Curl = MonarchCodeSnippet("cURL", code = "curl https://api.example.com/v1/items", language = "bash")
private val Kotlin = MonarchCodeSnippet("Kotlin", code = "val items = client.items.list()", language = "kotlin")
private val Swift = MonarchCodeSnippet("Swift", code = "let items = try await client.items.list()", language = "swift")
private val Snippets = listOf(Curl, Kotlin, Swift)

internal object MonarchCodeSnippetTabsFixtures : ComponentFixtures {
    override val id = "code-snippet-tabs"
    override val fixtures = listOf(
        fixture("three languages") { MonarchCodeSnippetTabs(snippets = Snippets) },
        fixture("selected, line numbers, title") {
            MonarchCodeSnippetTabs(
                snippets = listOf(Curl, MonarchCodeSnippet("Kotlin", code = "val a = 1\nval b = 2", title = "Main.kt")),
                selection = "Kotlin",
                showLineNumbers = true,
            )
        },
        fixture("one snippet") { MonarchCodeSnippetTabs(snippets = listOf(Kotlin)) },
        fixture("many tabs") {
            MonarchCodeSnippetTabs(
                snippets = listOf("cURL", "JavaScript", "TypeScript", "Python", "Go", "Ruby").map {
                    MonarchCodeSnippet(it, code = "print(\"$it\")")
                },
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchCodeSnippetTabsTest {
    @get:Rule
    val rule = createComposeRule()

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val isTab = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)

    private fun tab(label: String) = rule.onNode(isTab and hasText(label))

    @Test
    fun tabsSwitchTheCodeAndReportTheChoice() {
        val chosen = mutableListOf<String>()
        rule.setMonarchContent { MonarchCodeSnippetTabs(snippets = Snippets, onSelectionChange = { chosen += it }) }
        tab("cURL").assertIsSelected()
        rule.onNodeWithText(Curl.code, substring = true, useUnmergedTree = true).assertExists()
        tab("Kotlin").assertIsNotSelected().performClick()
        tab("Kotlin").assertIsSelected()
        rule.onNodeWithText(Kotlin.code, substring = true, useUnmergedTree = true).assertExists()
        rule.onNodeWithText(Curl.code, substring = true, useUnmergedTree = true).assertDoesNotExist()
        assertEquals(listOf("Kotlin"), chosen)
        rule.onNodeWithContentDescription("Language").assertExists()
    }

    @Test
    fun theAppsSelectionWinsAndAnUnknownIdFallsBack() {
        var selection by mutableStateOf<String?>("Swift")
        rule.setMonarchContent { MonarchCodeSnippetTabs(snippets = Snippets, selection = selection) }
        tab("Swift").assertIsSelected()
        selection = "Rust"
        rule.waitForIdle()
        tab("cURL").assertIsSelected()
    }

    @Test
    fun setsWithTheSameSyncKeyFollowEachOtherAndTheChoiceIsStored() {
        val key = "sync-test-docs"
        rule.setMonarchContent {
            Column {
                MonarchCodeSnippetTabs(snippets = listOf(Curl, Kotlin), syncKey = key)
                MonarchCodeSnippetTabs(snippets = listOf(Swift, Kotlin), syncKey = key)
            }
        }
        rule.onAllNodes(isTab and hasText("Kotlin"))[0].performClick()
        rule.waitForIdle()
        rule.onAllNodes(isTab and hasText("Kotlin"))[1].assertIsSelected()
        val stored = context.getSharedPreferences(MonarchCodeSnippetTabsSync.FILE, Context.MODE_PRIVATE)
            .getString(MonarchCodeSnippetTabsSync.KEY_PREFIX + key, null)
        assertEquals("Kotlin", stored)
        // A choice a set has no snippet for is ignored by that set.
        rule.onNode(isTab and hasText("Swift")).performClick()
        rule.waitForIdle()
        rule.onAllNodes(isTab and hasText("Kotlin"))[0].assertIsSelected()
        rule.onNode(isTab and hasText("Swift")).assertIsSelected()
    }

    @Test
    fun aStoredChoiceIsRestored() {
        val key = "sync-test-restore"
        context.getSharedPreferences(MonarchCodeSnippetTabsSync.FILE, Context.MODE_PRIVATE).edit()
            .putString(MonarchCodeSnippetTabsSync.KEY_PREFIX + key, "Swift").commit()
        rule.setMonarchContent { MonarchCodeSnippetTabs(snippets = Snippets, syncKey = key) }
        tab("Swift").assertIsSelected()
    }

    @Test
    fun oneSnippetHasNoTabsAndNoneDrawsNothing() {
        var snippets by mutableStateOf(listOf(Kotlin))
        rule.setMonarchContent { MonarchCodeSnippetTabs(snippets = snippets) }
        rule.onNode(isTab).assertDoesNotExist()
        rule.onNodeWithText(Kotlin.code, substring = true, useUnmergedTree = true).assertExists()
        snippets = emptyList()
        rule.waitForIdle()
        rule.onNodeWithText(Kotlin.code, substring = true, useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun copyReportsTheShownCodeAndTheCodeBlockLabelsApply() {
        val copied = mutableListOf<String>()
        rule.setMonarchContent {
            MonarchCodeSnippetTabs(
                snippets = Snippets,
                selection = "Kotlin",
                onCopy = { copied += it },
                codeBlockLabels = MonarchCodeBlockLabels(copy = "Copier"),
                label = "Langage",
            )
        }
        rule.onNodeWithContentDescription("Langage").assertExists()
        rule.onNode(hasContentDescription("Copier", substring = true)).performClick()
        rule.waitForIdle()
        assertEquals(listOf(Kotlin.code), copied)
    }

    @Test
    fun tabsHave48dpTargetsFollowTheDirectionAndKeepTheirTextWhole() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) { MonarchCodeSnippetTabs(snippets = listOf(Curl, Kotlin)) }
        LayoutDirection.entries.forEach { direction ->
            environment.layoutDirection = direction
            rule.waitForIdle()
            tab("cURL").assertTouchTargetAtLeast48dp(rule.density).assertIsBefore(tab("Kotlin"), direction)
        }
        environment.layoutDirection = LayoutDirection.Ltr
        environment.fontScale = LargestFontScale
        rule.waitForIdle()
        rule.onAllNodesWithText("Kotlin", useUnmergedTree = true)[0].assertTextFullyVisible()
        tab("Kotlin").assert(isTab)
    }

    @Test
    fun theSelectionLogic() {
        val t = MonarchCodeSnippetTabsTokens
        val repeated = listOf(Curl, MonarchCodeSnippet("cURL", code = "other"), Kotlin)
        assertEquals(listOf("cURL", "Kotlin"), t.distinct(repeated).map { it.id })
        assertEquals("Kotlin", t.resolve(Snippets, null, "Kotlin", "Swift"))
        assertEquals("Swift", t.resolve(Snippets, "Rust", null, "Swift"))
        assertEquals("cURL", t.resolve(Snippets, "Rust", "Go"))
        assertNull(t.resolve(emptyList(), "cURL"))
        assertEquals("node", MonarchCodeSnippet("Node.js", code = "", id = "node").id)
    }
}
