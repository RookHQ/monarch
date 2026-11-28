package dev.monarchui.components.intelligence

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.monarchui.components.intelligence.aiartifactcard.MonarchAiArtifactCard
import dev.monarchui.components.intelligence.aiartifactcard.MonarchAiArtifactCardTokens
import dev.monarchui.components.intelligence.aiconversation.MonarchAiArtifactKind
import dev.monarchui.components.intelligence.aiconversation.MonarchAiArtifactPart
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.TestIcon
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal object MonarchAiArtifactCardFixtures : ComponentFixtures {
    override val id = "ai-artifact-card"
    override val fixtures = listOf(
        fixture("every kind") {
            Column {
                MonarchAiArtifactKind.entries.take(3).forEach {
                    MonarchAiArtifactCard(title = it.name, kind = it, version = 1, onClick = {})
                }
            }
        },
        fixture("more kinds with icons") {
            Column {
                MonarchAiArtifactKind.entries.drop(3).forEach {
                    MonarchAiArtifactCard(
                        title = it.name,
                        kind = it,
                        version = 2,
                        onClick = {},
                        icon = { MonarchIcon(TestIcon, contentDescription = null) },
                    )
                }
            }
        },
        fixture("active with a description") {
            MonarchAiArtifactCard(
                title = "Revenue by region",
                kind = MonarchAiArtifactKind.Chart,
                version = 4,
                onClick = {},
                description = "Updated just now",
                active = true,
            )
        },
        fixture("disabled") {
            MonarchAiArtifactCard(
                title = "Q3 summary",
                kind = MonarchAiArtifactKind.Document,
                version = 3,
                onClick = {},
                enabled = false,
            )
        },
        fixture("long title") {
            MonarchAiArtifactCard(
                title = "Quarterly revenue, costs and headcount summary for the board meeting",
                kind = MonarchAiArtifactKind.Document,
                version = 12,
                onClick = {},
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchAiArtifactCardTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theCardIsOneButtonReadAsTitleKindVersionAndAction() {
        var opened: MonarchAiArtifactPart? = null
        val part = MonarchAiArtifactPart("a1", MonarchAiArtifactKind.Document, "Q3 summary", 3)
        rule.setMonarchContent {
            MonarchAiArtifactCard(part = part, onOpen = { opened = it }, description = "Just now")
        }
        rule.onNodeWithContentDescription("Q3 summary, Document, version 3, Just now, Open")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertIsNotSelected()
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        assertEquals(part, opened)
    }

    @Test
    fun theVisibleMetaLineIsShort() {
        rule.setMonarchContent {
            MonarchAiArtifactCard(title = "Sales", kind = MonarchAiArtifactKind.Table, version = 2, onClick = {})
        }
        rule.onNodeWithText("Table · v2", useUnmergedTree = true).assertExists()
    }

    @Test
    fun theArtifactOnShowIsSelectedAndReadsShowing() {
        rule.setMonarchContent {
            MonarchAiArtifactCard(
                title = "Sales",
                kind = MonarchAiArtifactKind.Table,
                version = 2,
                onClick = {},
                active = true,
            )
        }
        rule.onNodeWithContentDescription("Sales, Table, version 2, Showing").assertIsSelected()
    }

    @Test
    fun theLabelsCanBeReplacedAndAnEmptyVersionIsHidden() {
        rule.setMonarchContent {
            MonarchAiArtifactCard(
                title = "Ventes",
                kind = MonarchAiArtifactKind.Table,
                version = 2,
                onClick = {},
                kindLabel = "Tableau",
                versionLabel = "",
                actionLabel = "Ouvrir",
            )
        }
        rule.onNodeWithContentDescription("Ventes, Tableau, Ouvrir").assertExists()
        rule.onNodeWithText("Tableau", useUnmergedTree = true).assertExists()
    }

    @Test
    fun aDisabledCardDoesNotOpen() {
        var opened = 0
        rule.setMonarchContent {
            MonarchAiArtifactCard(
                title = "Sales",
                kind = MonarchAiArtifactKind.Table,
                version = 2,
                onClick = { opened++ },
                enabled = false,
            )
        }
        rule.onNodeWithContentDescription("Sales, Table, version 2, Open").assertIsNotEnabled().performClick()
        assertEquals(0, opened)
    }

    @Test
    fun theTitleWrapsAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchAiArtifactCard(
                title = "Revenue by region",
                kind = MonarchAiArtifactKind.Chart,
                version = 1,
                onClick = {},
            )
        }
        rule.onNodeWithText("Revenue by region", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun theColoursComeFromRolesInEveryThemeAndState() {
        AllThemes.forEach { theme ->
            val c = MonarchColors(theme.colors)
            val resting = MonarchAiArtifactCardTokens.colors(active = false, enabled = true, colors = c)
            assertEquals(c.bgSurface, resting.container, theme.id)
            assertEquals(c.borderDefault, resting.border, theme.id)
            assertEquals(c.aiStrong, resting.action, theme.id)
            val active = MonarchAiArtifactCardTokens.colors(active = true, enabled = true, colors = c)
            assertEquals(c.aiSubtle, active.container, theme.id)
            assertEquals(c.aiBorder, active.border, theme.id)
            assertTrue(active.borderWidth > resting.borderWidth, theme.id)
            val disabled = MonarchAiArtifactCardTokens.colors(active = true, enabled = false, colors = c)
            assertEquals(c.textDisabled, disabled.title, theme.id)
            assertEquals(c.aiSubtle to c.aiStrong, MonarchAiArtifactCardTokens.iconTile(c), theme.id)
        }
    }
}
