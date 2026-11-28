package dev.monarchui.components.navigation

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.monarchui.components.navigation.tableofcontents.MonarchTableOfContents
import dev.monarchui.components.navigation.tableofcontents.MonarchTableOfContentsHeading
import dev.monarchui.components.navigation.tableofcontents.MonarchTableOfContentsItem
import dev.monarchui.components.navigation.tableofcontents.MonarchTableOfContentsTokens
import dev.monarchui.components.navigation.tableofcontents.rememberMonarchTableOfContentsSpy
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.testing.stateDescription
import dev.monarchui.theme.MonarchColors
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val Contents = listOf(
    MonarchTableOfContentsItem("Overview", "overview"),
    MonarchTableOfContentsItem("Install", "install"),
    MonarchTableOfContentsItem("With Gradle", "gradle", level = 3),
)

internal object MonarchTableOfContentsFixtures : ComponentFixtures {
    override val id = "table-of-contents"
    override val fixtures = listOf(
        fixture("current and nested") {
            MonarchTableOfContents(items = Contents, selectedId = "install", onSelect = {})
        },
        fixture("no current section, title hidden") {
            MonarchTableOfContents(items = Contents, selectedId = null, onSelect = {}, showTitle = false)
        },
        fixture("disabled") {
            MonarchTableOfContents(items = Contents, selectedId = "overview", onSelect = {}, enabled = false)
        },
        fixture("long entry") {
            MonarchTableOfContents(
                items = listOf(MonarchTableOfContentsItem("Migrating from the previous major version of the SDK", "m")),
                selectedId = "m",
                onSelect = {},
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchTableOfContentsTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theTitleIsAHeadingAndTheCurrentEntryHasItsState() {
        rule.setMonarchContent { MonarchTableOfContents(items = Contents, selectedId = "install", onSelect = {}) }
        rule.onNodeWithText("On this page").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        assertEquals("Current section", rule.onNodeWithText("Install").stateDescription())
        assertNull(rule.onNodeWithText("Overview").stateDescription())
    }

    @Test
    fun choosingAnEntryCallsBackWithItsId() {
        val chosen = mutableListOf<String>()
        rule.setMonarchContent {
            MonarchTableOfContents(items = Contents, selectedId = null, onSelect = { chosen += it })
        }
        rule.onNodeWithText("With Gradle").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(listOf("gradle"), chosen)
    }

    @Test
    fun aDisabledListIgnoresTaps() {
        rule.setMonarchContent {
            MonarchTableOfContents(
                items = Contents,
                selectedId = null,
                onSelect = { error("not called") },
                enabled = false,
            )
        }
        rule.onNodeWithText("Overview").assertIsNotEnabled()
    }

    @Test
    fun aHiddenTitleStillNamesTheList() {
        rule.setMonarchContent {
            MonarchTableOfContents(
                items = Contents,
                selectedId = null,
                onSelect = {},
                showTitle = false,
                title = "Contents",
            )
        }
        rule.onNodeWithText("Contents").assertDoesNotExist()
        rule.onNodeWithContentDescription("Contents").assertExists()
    }

    @Test
    fun theCurrentLabelCanBeReplaced() {
        rule.setMonarchContent {
            MonarchTableOfContents(
                items = Contents,
                selectedId = "overview",
                onSelect = {},
                currentLabel = "Sección actual",
            )
        }
        assertEquals("Sección actual", rule.onNodeWithText("Overview").stateDescription())
    }

    @Test
    fun nestedEntriesAreIndentedFromTheStartRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchTableOfContents(items = Contents, selectedId = null, onSelect = {})
        }
        val section = rule.onNodeWithText("Install", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val nested = rule.onNodeWithText("With Gradle", useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertTrue(nested.right < section.right, "A nested entry starts further from the right edge")
    }

    @Test
    fun longEntriesWrapAtTheLargestFontSize() {
        val title = "Migrating from the previous major version of the SDK"
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchTableOfContents(
                items = listOf(MonarchTableOfContentsItem(title, "m")),
                selectedId = null,
                onSelect = {},
            )
        }
        rule.onNodeWithText(title, useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun theSpyFollowsScrolling() {
        lateinit var state: LazyListState
        var current: String? = "unset"
        rule.setMonarchContent {
            state = rememberLazyListState()
            val spy by rememberMonarchTableOfContentsSpy(state, mapOf("a" to 2, "b" to 20))
            current = spy
            LazyColumn(Modifier.height(400.dp), state = state) {
                items(40) { Text("Row $it", Modifier.height(50.dp)) }
            }
        }
        rule.waitForIdle()
        // Heading "a" is at 100dp, inside the top 40% (160dp) of the list.
        assertEquals("a", current)
        rule.runOnIdle { runBlocking { state.scrollToItem(10) } }
        rule.waitForIdle()
        // "a" is scrolled past and "b" is at 500dp, below the zone.
        assertEquals("a", current)
        rule.runOnIdle { runBlocking { state.scrollToItem(19) } }
        rule.waitForIdle()
        assertEquals("b", current)
    }

    @Test
    fun theSpyRule() {
        fun heading(id: String, index: Int, top: Int?) = MonarchTableOfContentsHeading(id, index, top)
        val t = MonarchTableOfContentsTokens
        assertNull(t.current(listOf(heading("a", 5, 900)), firstVisibleIndex = 0, viewport = 1000, inset = 0))
        assertEquals("a", t.current(listOf(heading("a", 5, 300)), firstVisibleIndex = 0, viewport = 1000, inset = 0))
        assertEquals(
            "a",
            t.current(listOf(heading("a", 1, null), heading("b", 9, 700)), 4, viewport = 1000, inset = 0),
        )
        assertEquals(
            "b",
            t.current(listOf(heading("a", 1, -10), heading("b", 3, 200), heading("c", 4, 300)), 0, 1000, 0),
        )
        assertEquals("a", t.current(listOf(heading("a", 1, 50), heading("b", 3, 600)), 0, 1000, inset = 100))
        assertEquals(1, t.depth(3, Contents))
        assertEquals(0, t.depth(2, Contents))
    }

    @Test
    fun theColoursComeFromTheTextAndAccentRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val c = MonarchColors(theme.colors)
            val current = MonarchTableOfContentsTokens.entryColors(current = true, enabled = true, c)
            assertEquals(c.textDefault, current.text, theme.id)
            assertEquals(c.accentSolid, current.line, theme.id)
            val other = MonarchTableOfContentsTokens.entryColors(current = false, enabled = true, c)
            assertEquals(c.textMuted, other.text, theme.id)
            assertEquals(c.borderSubtle, other.line, theme.id)
            val disabled = MonarchTableOfContentsTokens.entryColors(current = true, enabled = false, c)
            assertEquals(c.textDisabled, disabled.text, theme.id)
            assertEquals(c.borderSubtle, disabled.line, theme.id)
        }
    }
}
