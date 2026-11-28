package dev.monarchui.components.intelligence

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import dev.monarchui.components.intelligence.aigeneratedbadge.MonarchAiGeneratedBadge
import dev.monarchui.components.intelligence.aigeneratedbadge.MonarchAiGeneratedBadgeKind
import dev.monarchui.components.intelligence.aigeneratedbadge.MonarchAiGeneratedBadgeSize
import dev.monarchui.components.intelligence.aigeneratedbadge.MonarchAiGeneratedBadgeTokens
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.TestIcon
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal object MonarchAiGeneratedBadgeFixtures : ComponentFixtures {
    override val id = "ai-generated-badge"
    override val fixtures = MonarchAiGeneratedBadgeSize.entries.map { size ->
        fixture("every kind, ${size.name}") {
            Column { MonarchAiGeneratedBadgeKind.entries.forEach { MonarchAiGeneratedBadge(
                kind = it,
                size = size,
                icon = null,
            ) } }
        }
    } + listOf(
        fixture("with an icon and provenance") {
            MonarchAiGeneratedBadge(
                model = "gemini-2.5-pro",
                dateMillis = 1_741_608_000_000L,
                icon = { MonarchIcon(TestIcon, contentDescription = null) },
            )
        },
        fixture("icon only") {
            MonarchAiGeneratedBadge(iconOnly = true, icon = { MonarchIcon(TestIcon, contentDescription = null) })
        },
        fixture("long label") { MonarchAiGeneratedBadge(
            label = "Drafted by the assistant and reviewed by finance",
            icon = null,
        ) },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchAiGeneratedBadgeTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theBadgeIsOneElementReadAsItsLabel() {
        rule.setMonarchContent { MonarchAiGeneratedBadge(modifier = Modifier.testTag("badge"), icon = null) }
        rule.onNodeWithContentDescription("AI generated").assertHasNoClickAction()
        rule.onAllNodesWithText("AI generated").assertCountEquals(0)
    }

    @Test
    fun theProvenanceIsReadAfterTheLabel() {
        rule.setMonarchContent {
            MonarchAiGeneratedBadge(kind = MonarchAiGeneratedBadgeKind.Assisted, model = "gemini-2.5-pro", icon = null)
        }
        rule.onNodeWithContentDescription("AI assisted, By gemini-2.5-pro").assertExists()
    }

    @Test
    fun theDetailAndLabelCanBeReplaced() {
        rule.setMonarchContent { MonarchAiGeneratedBadge(
            label = "Généré par IA",
            detail = "Par le modèle",
            icon = null,
        ) }
        rule.onNodeWithContentDescription("Généré par IA, Par le modèle").assertExists()
    }

    @Test
    fun anIconOnlyBadgeStillReadsItsLabel() {
        rule.setMonarchContent {
            MonarchAiGeneratedBadge(
                kind = MonarchAiGeneratedBadgeKind.Edited,
                iconOnly = true,
                icon = { MonarchIcon(TestIcon, contentDescription = null) },
            )
        }
        rule.onNodeWithContentDescription("Edited after AI").assertExists()
        rule.onAllNodesWithText("Edited after AI", useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    fun iconOnlyWithoutAnIconShowsTheLabel() {
        rule.setMonarchContent { MonarchAiGeneratedBadge(iconOnly = true, icon = null) }
        rule.onNodeWithText("AI generated", useUnmergedTree = true).assertExists()
    }

    @Test
    fun theLabelIsNotCutAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchAiGeneratedBadge(
                kind = MonarchAiGeneratedBadgeKind.Edited,
                size = MonarchAiGeneratedBadgeSize.Sm,
                icon = null,
            )
        }
        rule.onNodeWithText("Edited after AI", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun onlyTheEditedKindIsDashedAndABlankModelIsNoModel() {
        assertTrue(MonarchAiGeneratedBadgeTokens.dashed(MonarchAiGeneratedBadgeKind.Edited))
        assertFalse(MonarchAiGeneratedBadgeTokens.dashed(MonarchAiGeneratedBadgeKind.Generated))
        assertFalse(MonarchAiGeneratedBadgeTokens.dashed(MonarchAiGeneratedBadgeKind.Assisted))
        assertNull(MonarchAiGeneratedBadgeTokens.shownModel("  "))
        assertEquals("gpt", MonarchAiGeneratedBadgeTokens.shownModel(" gpt "))
    }

    @Test
    fun theColoursAreTheAiRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val c = MonarchColors(theme.colors)
            val colors = MonarchAiGeneratedBadgeTokens.colors(c)
            assertEquals(c.aiSubtle, colors.container, theme.id)
            assertEquals(c.aiStrong, colors.content, theme.id)
            assertEquals(c.aiBorder, colors.border, theme.id)
        }
    }
}
