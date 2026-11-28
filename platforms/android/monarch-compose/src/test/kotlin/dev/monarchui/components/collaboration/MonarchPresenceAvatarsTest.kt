package dev.monarchui.components.collaboration

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.components.collaboration.presenceavatars.MonarchPresenceAvatars
import dev.monarchui.components.collaboration.presenceavatars.MonarchPresenceAvatarsLabels
import dev.monarchui.components.collaboration.presenceavatars.MonarchPresenceAvatarsTokens
import dev.monarchui.components.collaboration.presenceavatars.MonarchPresencePerson
import dev.monarchui.components.identity.avatar.MonarchAvatarSize
import dev.monarchui.components.identity.presencedot.MonarchPresenceStatus
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.MonarchTestEnvironment
import dev.monarchui.testing.assertIsBefore
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val People = listOf(
    MonarchPresencePerson(id = "1", name = "Ana Silva", activity = "Editing"),
    MonarchPresencePerson(
        id = "2",
        name = "Ben Okafor",
        status = MonarchPresenceStatus.Away,
        customStatus = "In a meeting",
    ),
    MonarchPresencePerson(id = "3", name = "Chloé Martin", status = MonarchPresenceStatus.Busy),
    MonarchPresencePerson(id = "4", name = "Dev Patel", status = MonarchPresenceStatus.DoNotDisturb),
    MonarchPresencePerson(id = "5", name = "Eun-ji Park", status = MonarchPresenceStatus.Offline, activity = "  "),
    MonarchPresencePerson(id = "6", name = "Femi Adeyemi"),
)

internal object MonarchPresenceAvatarsFixtures : ComponentFixtures {
    override val id = "presence-avatars"
    override val fixtures = listOf(
        fixture("all shown, every presence") { MonarchPresenceAvatars(people = People.take(5), maxVisible = 5) },
        fixture("overflow opens the list") { MonarchPresenceAvatars(people = People) },
        fixture("tappable, large") {
            MonarchPresenceAvatars(people = People.take(2), size = MonarchAvatarSize.Lg, onPersonClick = {})
        },
        fixture("one person, medium") { MonarchPresenceAvatars(people = People.take(1), size = MonarchAvatarSize.Md) },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchPresenceAvatarsTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun aStackThatShowsEveryoneReadsEachPersonWithPresenceAndActivity() {
        rule.setMonarchContent { MonarchPresenceAvatars(people = People.take(2)) }
        rule.onNodeWithContentDescription("2 people here").assertExists()
        rule.onNodeWithContentDescription("Ana Silva, Online, Editing")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Image))
        rule.onNodeWithContentDescription("Ben Okafor, Away · In a meeting").assertExists()
        rule.onAllNodesWithText("Everyone here").assertCountEquals(0)
    }

    @Test
    fun anOverflowingStackIsOneButtonThatOpensTheList() {
        rule.setMonarchContent { MonarchPresenceAvatars(people = People, maxVisible = 3) }
        val stack = rule.onNodeWithContentDescription("6 people here", substring = true)
        stack.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertTouchTargetAtLeast48dp(rule.density)
        val description = stack.fetchSemanticsNode().config[SemanticsProperties.ContentDescription].single()
        assertEquals(
            "6 people here: Ana Silva, Online, Editing; Ben Okafor, Away · In a meeting; Chloé Martin, Busy; 3 more",
            description,
        )
        val click = stack.fetchSemanticsNode().config.getOrNull(SemanticsActions.OnClick)
        assertEquals("Show everyone here", click?.label)
        stack.performClick()
        rule.onNode(isHeading()).assertExists()
        rule.onNodeWithText("Everyone here").assertExists()
        rule.onNodeWithContentDescription("Eun-ji Park, Offline").assertExists()
        rule.onNodeWithText("Done").performClick()
        rule.onAllNodesWithText("Everyone here").assertCountEquals(0)
    }

    @Test
    fun withACallbackEveryPersonInTheListIsAButton() {
        var chosen: String? = null
        rule.setMonarchContent { MonarchPresenceAvatars(people = People.take(2), onPersonClick = { chosen = it.id }) }
        rule.onNodeWithContentDescription("2 people here", substring = true).performClick()
        rule.onNodeWithContentDescription("Ben Okafor, Away · In a meeting")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        assertEquals("2", chosen)
        rule.onAllNodesWithText("Everyone here").assertCountEquals(0)
    }

    @Test
    fun withoutACallbackTheRowsAreNotButtons() {
        rule.setMonarchContent { MonarchPresenceAvatars(people = People, maxVisible = 2) }
        rule.onNodeWithContentDescription("6 people here", substring = true).performClick()
        val row = rule.onNodeWithContentDescription("Chloé Martin, Busy").fetchSemanticsNode()
        assertFalse(row.config.contains(SemanticsActions.OnClick))
    }

    @Test
    fun anEmptyListDrawsNothing() {
        rule.setMonarchContent { Box(Modifier.testTag("host")) { MonarchPresenceAvatars(people = emptyList()) } }
        assertEquals(0, rule.onNodeWithTag("host").fetchSemanticsNode().children.size)
    }

    @Test
    fun theStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchPresenceAvatars(
                people = People,
                maxVisible = 1,
                labels = MonarchPresenceAvatarsLabels(
                    label = { "$it personas" },
                    showAll = "Ver todos",
                    list = "Todos",
                    done = "Listo",
                    more = { "$it más" },
                    person = { name, presence, _ -> "$name ($presence)" },
                ),
            )
        }
        val stack = rule.onNodeWithContentDescription("6 personas: Ana Silva (Online); 5 más")
        assertEquals("Ver todos", stack.fetchSemanticsNode().config.getOrNull(SemanticsActions.OnClick)?.label)
        stack.performClick()
        rule.onNodeWithText("Todos").assertExists()
        rule.onNodeWithText("Listo").assertExists()
    }

    @Test
    fun theFirstPersonComesFirstInBothDirections() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) { MonarchPresenceAvatars(people = People.take(2)) }
        LayoutDirection.entries.forEach { direction ->
            environment.layoutDirection = direction
            rule.waitForIdle()
            rule.onNodeWithContentDescription("Ana Silva, Online, Editing")
                .assertIsBefore(rule.onNodeWithContentDescription("Ben Okafor, Away · In a meeting"), direction)
        }
    }

    @Test
    fun longActivitiesWrapInTheListAtTheLargestFontScale() {
        val person = MonarchPresencePerson(id = "x", name = "Ana", activity = "Reviewing the second draft of the plan")
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchPresenceAvatars(people = listOf(person), onPersonClick = {})
        }
        rule.onNodeWithContentDescription("1 person here", substring = true).performClick()
        rule.onNodeWithText("Online · Reviewing the second draft of the plan", useUnmergedTree = true)
            .assertTextFullyVisible()
    }

    @Test
    fun theStackRulesMatchTheWeb() {
        val t = MonarchPresenceAvatarsTokens
        assertEquals(5, t.visibleCount(total = 5, maxVisible = 4))
        assertEquals(4, t.visibleCount(total = 6, maxVisible = 4))
        assertEquals(0, t.visibleCount(total = 6, maxVisible = -2))
        assertTrue(t.opensList(total = 6, maxVisible = 4, clickable = false))
        assertFalse(t.opensList(total = 5, maxVisible = 4, clickable = false))
        assertTrue(t.opensList(total = 2, maxVisible = 4, clickable = true))
        assertNull(t.activityText("   "))
        assertEquals("Editing", t.activityText(" Editing "))
    }
}
