package com.rookhq.monarch.components.collaboration

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.components.collaboration.reactionbar.MonarchReactionBar
import com.rookhq.monarch.components.collaboration.reactionbar.MonarchReactionBarItem
import com.rookhq.monarch.components.collaboration.reactionbar.MonarchReactionBarLabels
import com.rookhq.monarch.components.collaboration.reactionbar.MonarchReactionBarSize
import com.rookhq.monarch.components.collaboration.reactionbar.MonarchReactionBarTokens
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.MonarchTestEnvironment
import com.rookhq.monarch.testing.TestIcon
import com.rookhq.monarch.testing.assertIsBefore
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private val Reactions = listOf(
    MonarchReactionBarItem("👍", count = 3, reacted = true, label = "thumbs up", reactedBy = "Ana, Ben and you"),
    MonarchReactionBarItem("🎉", count = 1, label = "party popper"),
    MonarchReactionBarItem("👀", count = 0, label = "eyes"),
)

internal object MonarchReactionBarFixtures : ComponentFixtures {
    override val id = "reaction-bar"
    override val fixtures = listOf(
        fixture("interactive, small") {
            MonarchReactionBar(Reactions, onToggle = { _, _ -> }, quickReactions = listOf("👍", "❤️"), addIcon = null)
        },
        fixture("interactive, medium, add icon") {
            MonarchReactionBar(
                Reactions,
                onToggle = { _, _ -> },
                size = MonarchReactionBarSize.Md,
                quickReactions = listOf("👍"),
                addIcon = { MonarchIcon(TestIcon, contentDescription = null) },
            )
        },
        fixture("read-only") { MonarchReactionBar(Reactions, addIcon = null) },
        fixture("disabled") {
            MonarchReactionBar(
                Reactions,
                onToggle = { _, _ -> },
                enabled = false,
                quickReactions = listOf("👍"),
                addIcon = null,
            )
        },
        fixture("limit reached") {
            MonarchReactionBar(
                Reactions,
                onToggle = { _, _ -> },
                maxReactions = 2,
                quickReactions = listOf("👍"),
                addIcon = null,
            )
        },
        fixture("empty, add only") {
            MonarchReactionBar(emptyList(), onToggle = { _, _ -> }, quickReactions = listOf("👍"), addIcon = null)
        },
        fixture("many reactions wrap") {
            MonarchReactionBar(
                List(8) { MonarchReactionBarItem("😀$it", count = it + 1, label = "face $it") },
                onToggle = { _, _ -> },
                addIcon = null,
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchReactionBarTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun eachChipIsNamedByItsEmojiAndCountAndShowsItsState() {
        rule.setMonarchContent { MonarchReactionBar(Reactions, onToggle = { _, _ -> }, addIcon = null) }
        rule.onNodeWithContentDescription("thumbs up, 3 reactions. Reacted by Ana, Ben and you")
            .assertIsSelected()
            .assertTouchTargetAtLeast48dp(rule.density)
        rule.onNodeWithContentDescription("party popper, 1 reaction").assertIsNotSelected()
        rule.onNodeWithContentDescription("eyes", substring = true).assertDoesNotExist()
        rule.onNodeWithContentDescription("Reactions").assertExists()
    }

    @Test
    fun tappingAChipTogglesItsReaction() {
        val toggles = mutableListOf<Pair<String, Boolean>>()
        rule.setMonarchContent { MonarchReactionBar(
            Reactions,
            onToggle = { emoji, on -> toggles += emoji to on },
            addIcon = null,
        ) }
        rule.onNodeWithContentDescription("thumbs up", substring = true).performClick()
        rule.onNodeWithContentDescription("party popper", substring = true).performClick()
        assertEquals(listOf("👍" to false, "🎉" to true), toggles)
    }

    @Test
    fun aDisabledBarDoesNotRespond() {
        val toggles = mutableListOf<Pair<String, Boolean>>()
        rule.setMonarchContent {
            MonarchReactionBar(
                Reactions,
                onToggle = { emoji, on -> toggles += emoji to on },
                enabled = false,
                quickReactions = listOf("👍"),
                addIcon = null,
            )
        }
        rule.onNodeWithContentDescription("party popper", substring = true).assertIsNotEnabled().performClick()
        rule.onNodeWithContentDescription("Add reaction").assertIsNotEnabled()
        assertEquals(emptyList(), toggles)
    }

    @Test
    fun aReadOnlyBarShowsImagesWithTheSameNamesAndNoAddButton() {
        rule.setMonarchContent { MonarchReactionBar(Reactions, quickReactions = listOf("👍"), addIcon = null) }
        val chip = rule.onNodeWithContentDescription("party popper, 1 reaction").fetchSemanticsNode()
        assertEquals(Role.Image, chip.config[SemanticsProperties.Role])
        assertFalse(chip.config.contains(SemanticsProperties.Selected))
        rule.onNodeWithContentDescription("Add reaction").assertDoesNotExist()
    }

    @Test
    fun theAddMenuPicksAQuickReactionButNotOneAlreadyGiven() {
        val toggles = mutableListOf<Pair<String, Boolean>>()
        rule.setMonarchContent {
            MonarchReactionBar(
                Reactions,
                onToggle = { emoji, on -> toggles += emoji to on },
                quickReactions = listOf("👍", "❤️"),
                addIcon = null,
            )
        }
        val add = rule.onNodeWithContentDescription("Add reaction")
        add.assertTouchTargetAtLeast48dp(rule.density)
        add.performClick()
        rule.onNodeWithText("❤️").performClick()
        add.performClick()
        rule.onNodeWithText("👍").performClick()
        assertEquals(listOf("❤️" to true), toggles)
    }

    @Test
    fun theAppPickerIsShownWhileOpenAndReportsThePick() {
        val toggles = mutableListOf<Pair<String, Boolean>>()
        rule.setMonarchContent {
            MonarchReactionBar(
                Reactions,
                onToggle = { emoji, on -> toggles += emoji to on },
                picker = { onPick, _ -> TextButton(onClick = { onPick("🚀") }) { Text("Rocket") } },
                addIcon = null,
            )
        }
        rule.onNodeWithText("Rocket").assertDoesNotExist()
        rule.onNodeWithContentDescription("Add reaction").performClick()
        rule.onNodeWithText("Rocket").performClick()
        rule.onNodeWithText("Rocket").assertDoesNotExist()
        assertEquals(listOf("🚀" to true), toggles)
    }

    @Test
    fun theAddButtonHidesAtTheLimitOrWithoutAnythingToPick() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) {
            MonarchReactionBar(
                Reactions,
                onToggle = { _, _ -> },
                maxReactions = 2,
                quickReactions = listOf("👍"),
                addIcon = null,
            )
        }
        rule.onNodeWithContentDescription("Add reaction").assertDoesNotExist()
        assertTrue(MonarchReactionBarTokens.canAdd(true, true, 1, 2, hasSource = true))
        assertFalse(MonarchReactionBarTokens.canAdd(true, true, 1, null, hasSource = false))
        assertFalse(MonarchReactionBarTokens.canAdd(false, true, 0, null, hasSource = true))
        assertFalse(MonarchReactionBarTokens.canAdd(true, false, 0, null, hasSource = true))
    }

    @Test
    fun repeatedEmojiAndZeroCountsAreDropped() {
        val shown = MonarchReactionBarTokens.shown(
            listOf(
                MonarchReactionBarItem("👍", 2),
                MonarchReactionBarItem("👍", 5),
                MonarchReactionBarItem("🎉", 0),
            ),
        )
        assertEquals(listOf("👍" to 2), shown.map { it.emoji to it.count })
    }

    @Test
    fun chipsComeBeforeTheAddButtonInBothDirections() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) {
            MonarchReactionBar(Reactions, onToggle = { _, _ -> }, quickReactions = listOf("👍"), addIcon = null)
        }
        LayoutDirection.entries.forEach { direction ->
            environment.layoutDirection = direction
            rule.waitForIdle()
            rule.onNodeWithContentDescription("party popper", substring = true)
                .assertIsBefore(rule.onNodeWithContentDescription("Add reaction"), direction)
        }
    }

    @Test
    fun chipsKeepTheirTouchTargetAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchReactionBar(Reactions, onToggle = { _, _ -> }, size = MonarchReactionBarSize.Md, addIcon = null)
        }
        rule.onNodeWithContentDescription("party popper", substring = true).assertTouchTargetAtLeast48dp(rule.density)
    }

    @Test
    fun theBuiltInStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchReactionBar(
                Reactions,
                onToggle = { _, _ -> },
                quickReactions = listOf("👍"),
                labels = MonarchReactionBarLabels(
                    label = "Réactions",
                    reaction = { label, count -> "$label : $count" },
                    reactedBy = { "par $it" },
                    add = "Ajouter une réaction",
                    addText = "Ajouter",
                ),
                addIcon = null,
            )
        }
        rule.onNodeWithContentDescription("Réactions").assertExists()
        rule.onNodeWithContentDescription("thumbs up : 3. par Ana, Ben and you").assertExists()
        rule.onNodeWithContentDescription("Ajouter une réaction").assertExists()
        rule.onNodeWithText("Ajouter", useUnmergedTree = true).assertExists()
    }

    @Test
    fun theReactedStateIsColourAndAThickerBorderInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val reacted = MonarchReactionBarTokens.colors(reacted = true, enabled = true, colors = colors)
            assertEquals(colors.accentSubtle, reacted.container)
            assertEquals(colors.accentStrong, reacted.content)
            assertEquals(colors.accentBorder, reacted.border)
            val idle = MonarchReactionBarTokens.colors(reacted = false, enabled = true, colors = colors)
            assertEquals(colors.bgSubtle, idle.container)
            assertEquals(colors.textDefault, idle.content)
            val off = MonarchReactionBarTokens.colors(reacted = true, enabled = false, colors = colors)
            assertEquals(colors.textDisabled, off.content)
            val thick = MonarchReactionBarTokens.border(true, reacted).width
            assertTrue(thick > MonarchReactionBarTokens.border(false, idle).width)
        }
    }
}
