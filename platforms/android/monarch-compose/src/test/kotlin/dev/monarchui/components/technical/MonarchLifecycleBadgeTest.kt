package dev.monarchui.components.technical

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onChildAt
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import dev.monarchui.components.technical.lifecyclebadge.MonarchLifecycle
import dev.monarchui.components.technical.lifecyclebadge.MonarchLifecycleBadge
import dev.monarchui.components.technical.lifecyclebadge.MonarchLifecycleBadgeTokens
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.theme.MonarchColors
import dev.monarchui.theme.MonarchTheme
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalLayoutApi::class)
internal object MonarchLifecycleBadgeFixtures : ComponentFixtures {
    override val id = "lifecycle-badge"
    override val fixtures = listOf(
        fixture("every stage") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2)) {
                MonarchLifecycle.entries.forEach { MonarchLifecycleBadge(lifecycle = it) }
            }
        },
        fixture("custom stage, tone and no dot") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2)) {
                MonarchLifecycleBadge(stage = "early_access", tone = MonarchTone.Info)
                MonarchLifecycleBadge(stage = "sunset", tone = MonarchTone.Danger, label = "Sunset in May")
                MonarchLifecycleBadge(lifecycle = MonarchLifecycle.Stable, showDot = false)
            }
        },
        fixture("long label") {
            MonarchLifecycleBadge(
                lifecycle = MonarchLifecycle.Preview,
                label = "Preview for workspaces on the beta plan",
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchLifecycleBadgeTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theBadgeIsReadAsItsLabelOnly() {
        rule.setMonarchContent {
            MonarchLifecycleBadge(lifecycle = MonarchLifecycle.Preview, modifier = Modifier.testTag("badge"))
        }
        rule.onNodeWithText("Preview").assertHasNoClickAction()
        // The dot has no semantics: the only child TalkBack can reach is the label.
        rule.onNodeWithTag("badge").onChildren().assertCountEquals(1)
        rule.onNodeWithTag("badge").onChildAt(0).assertTextEquals("Preview")
    }

    @Test
    fun theLabelReplacesTheBuiltInString() {
        rule.setMonarchContent { MonarchLifecycleBadge(lifecycle = MonarchLifecycle.Stable, label = "Stabil") }
        rule.onNodeWithText("Stabil").assertExists()
        rule.onNodeWithText("Stable").assertDoesNotExist()
    }

    @Test
    fun aBuiltInKeyIsThatStageAndAnUnknownKeyIsReadable() {
        rule.setMonarchContent { MonarchLifecycleBadge(stage = "deprecated") }
        rule.onNodeWithText("Deprecated").assertExists()
        assertEquals(MonarchLifecycle.Deprecated, MonarchLifecycleBadgeTokens.lifecycleOf("deprecated"))
        assertNull(MonarchLifecycleBadgeTokens.lifecycleOf("Stable"))
        assertEquals("Early access", MonarchLifecycleBadgeTokens.readableStage("early_access"))
        assertEquals("General availability", MonarchLifecycleBadgeTokens.readableStage("general-availability"))
        assertEquals("", MonarchLifecycleBadgeTokens.readableStage(""))
    }

    @Test
    fun anUnknownStageIsShownAsReadableText() {
        rule.setMonarchContent { MonarchLifecycleBadge(stage = "early_access") }
        rule.onNodeWithText("Early access").assertExists()
    }

    @Test
    fun eachStageHasItsTone() {
        assertEquals(MonarchTone.Neutral, MonarchLifecycleBadgeTokens.toneOf(MonarchLifecycle.Planned))
        assertEquals(MonarchTone.Info, MonarchLifecycleBadgeTokens.toneOf(MonarchLifecycle.Preview))
        assertEquals(MonarchTone.Success, MonarchLifecycleBadgeTokens.toneOf(MonarchLifecycle.Stable))
        assertEquals(MonarchTone.Warning, MonarchLifecycleBadgeTokens.toneOf(MonarchLifecycle.Deprecated))
    }

    @Test
    fun theLabelIsNotCutAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchLifecycleBadge(lifecycle = MonarchLifecycle.Deprecated)
        }
        rule.onNodeWithText("Deprecated").assertTextFullyVisible()
    }

    @Test
    fun theToneIsOnlyInTheBorderAndTheDotInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val success = MonarchLifecycleBadgeTokens.colors(MonarchTone.Success, colors)
            assertEquals(colors.bgSurface, success.surface.container, theme.id)
            assertEquals(colors.textDefault, success.surface.content, theme.id)
            assertEquals(colors.successBorder, success.surface.border, theme.id)
            assertEquals(colors.successStrong, success.dot, theme.id)
            val neutral = MonarchLifecycleBadgeTokens.colors(MonarchTone.Neutral, colors)
            assertEquals(colors.borderDefault, neutral.surface.border, theme.id)
            assertEquals(colors.textSubtle, neutral.dot, theme.id)
        }
    }
}
