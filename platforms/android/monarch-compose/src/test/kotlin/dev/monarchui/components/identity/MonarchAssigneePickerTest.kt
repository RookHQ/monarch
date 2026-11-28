package dev.monarchui.components.identity

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import dev.monarchui.components.identity.assigneepicker.MonarchAssigneePerson
import dev.monarchui.components.identity.assigneepicker.MonarchAssigneePicker
import dev.monarchui.components.identity.assigneepicker.MonarchAssigneePickerLabels
import dev.monarchui.components.identity.assigneepicker.MonarchAssigneePickerTokens
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.testing.stateDescription
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val Ada = MonarchAssigneePerson("ada", "Ada Moss", email = "ada@example.com")
private val Ben = MonarchAssigneePerson("ben", "Ben Okafor", email = "ben@example.com", hint = "Away")
private val Chloe = MonarchAssigneePerson("chloe", "Chloé Martin", enabled = false)
private val Team = listOf(Ben, Ada, Chloe)

internal object MonarchAssigneePickerFixtures : ComponentFixtures {
    override val id = "assignee-picker"
    override val fixtures = listOf(
        fixture("unassigned, with Assign to me") {
            MonarchAssigneePicker(people = Team, selected = null, onSelectedChange = {}, currentUserId = "ada")
        },
        fixture("assigned") {
            MonarchAssigneePicker(people = Team, selected = "ben", onSelectedChange = {}, currentUserId = "ada")
        },
        fixture("assigned to me") {
            MonarchAssigneePicker(people = Team, selected = "ada", onSelectedChange = {}, currentUserId = "ada")
        },
        fixture("several") {
            MonarchAssigneePicker(people = Team, selected = listOf("ada", "ben"), onSelectedChange = {})
        },
        fixture("required with an error") {
            MonarchAssigneePicker(
                people = Team,
                selected = null,
                onSelectedChange = {},
                required = true,
                allowUnassigned = false,
                errorText = "Choose someone to own this task.",
            )
        },
        fixture("disabled") {
            MonarchAssigneePicker(
                people = Team,
                selected = "ben",
                onSelectedChange = {},
                currentUserId = "ada",
                enabled = false,
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchAssigneePickerTest {
    @get:Rule
    val rule = createComposeRule()

    private val field get() = rule.onNodeWithContentDescription("Assignee")

    private fun row(text: String) = rule.onNode(
        hasText(text) and (
            SemanticsMatcher.keyIsDefined(SemanticsProperties.Selected) or
                SemanticsMatcher.keyIsDefined(SemanticsProperties.ToggleableState)
            ),
    )

    @Test
    fun theCurrentUserComesFirstAndReadsAsYou() {
        val ordered = MonarchAssigneePickerTokens.meFirst(Team, "ada")
        assertEquals(listOf("ada", "ben", "chloe"), ordered.map { it.id })
        assertEquals(Team, MonarchAssigneePickerTokens.meFirst(Team, "nobody"))
        val me = MonarchAssigneePickerTokens.entity(Ada, "ada") { "$it (you)" }
        assertEquals("Ada Moss (you)", me.name)
        assertEquals("ada@example.com", me.description)
        assertEquals("Ada Moss ada@example.com", me.keywords)
        assertFalse(MonarchAssigneePickerTokens.entity(Chloe, "ada") { it }.enabled)
    }

    @Test
    fun assignToMeIsOfferedOnlyWhileTheCurrentUserIsNotChosen() {
        assertTrue(MonarchAssigneePickerTokens.offersAssignToMe("ada", show = true, enabled = true, chosen = listOf()))
        assertFalse(MonarchAssigneePickerTokens.offersAssignToMe("ada", show = true, enabled = true, listOf("ada")))
        assertFalse(MonarchAssigneePickerTokens.offersAssignToMe(null, show = true, enabled = true, chosen = listOf()))
        assertFalse(MonarchAssigneePickerTokens.offersAssignToMe("ada", show = false, enabled = true, listOf()))
        assertFalse(MonarchAssigneePickerTokens.offersAssignToMe("ada", show = true, enabled = false, listOf()))
    }

    @Test
    fun aPersonIsPickedFromTheSheetWithTheCurrentUserFirst() {
        var chosen by mutableStateOf<String?>(null)
        rule.setMonarchContent {
            MonarchAssigneePicker(
                people = Team,
                selected = chosen,
                onSelectedChange = { chosen = it },
                currentUserId = "ada",
            )
        }
        assertEquals("Unassigned", field.stateDescription())
        field.assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.waitForIdle()
        row("Ada Moss (you)").assertExists()
        row("Chloé Martin").assertIsNotEnabled()
        row("Ben Okafor").performClick()
        rule.waitForIdle()
        assertEquals("ben", chosen)
        assertEquals("Ben Okafor", field.stateDescription())
    }

    @Test
    fun unassignedClearsTheChoice() {
        var chosen by mutableStateOf<String?>("ben")
        rule.setMonarchContent {
            MonarchAssigneePicker(people = Team, selected = chosen, onSelectedChange = { chosen = it })
        }
        field.performClick()
        rule.waitForIdle()
        row("Unassigned").performClick()
        rule.waitForIdle()
        assertNull(chosen)
    }

    @Test
    fun assignToMeAssignsTheCurrentUserAndThenGoesAway() {
        var chosen by mutableStateOf<String?>("ben")
        rule.setMonarchContent {
            MonarchAssigneePicker(
                people = Team,
                selected = chosen,
                onSelectedChange = { chosen = it },
                currentUserId = "ada",
            )
        }
        rule.onNodeWithText("Assign to me")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        rule.waitForIdle()
        assertEquals("ada", chosen)
        rule.onNodeWithText("Assign to me").assertDoesNotExist()
    }

    @Test
    fun assignToMeAddsToSeveralAndTheFieldReadsThem() {
        var chosen by mutableStateOf(listOf("ben"))
        rule.setMonarchContent {
            MonarchAssigneePicker(
                people = Team,
                selected = chosen,
                onSelectedChange = { chosen = it },
                currentUserId = "ada",
            )
        }
        rule.onNodeWithText("Assign to me").performClick()
        rule.waitForIdle()
        assertEquals(listOf("ben", "ada"), chosen)
        assertEquals("Ben Okafor, Ada Moss (you)", field.stateDescription())
    }

    @Test
    fun aDisabledPickerOffersNoShortcut() {
        rule.setMonarchContent {
            MonarchAssigneePicker(
                people = Team,
                selected = null,
                onSelectedChange = {},
                currentUserId = "ada",
                enabled = false,
            )
        }
        field.assertIsNotEnabled()
        rule.onNodeWithText("Assign to me").assertDoesNotExist()
    }

    @Test
    fun theAppSearchKeepsTheCurrentUserFirst() {
        val queries = mutableListOf<String>()
        rule.setMonarchContent {
            MonarchAssigneePicker(
                people = emptyList(),
                selected = null,
                onSelectedChange = {},
                currentUserId = "ada",
                search = { text ->
                    queries += text
                    listOf(Ben, Ada)
                },
            )
        }
        field.performClick()
        rule.waitForIdle()
        rule.onNode(hasSetTextAction()).performTextInput("o")
        rule.waitForIdle()
        rule.mainClock.advanceTimeBy(SearchWaitMillis)
        rule.waitForIdle()
        assertEquals("o", queries.last())
        row("Ada Moss (you)").assertExists()
    }

    @Test
    fun theStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchAssigneePicker(
                people = Team,
                selected = null,
                onSelectedChange = {},
                currentUserId = "ada",
                labels = MonarchAssigneePickerLabels(
                    label = "Owner",
                    unassigned = "Nobody",
                    assignToMe = "Take it",
                ),
            )
        }
        assertEquals("Nobody", rule.onNodeWithContentDescription("Owner").stateDescription())
        rule.onNodeWithText("Take it").assertExists()
    }

    @Test
    fun theFieldGrowsAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchAssigneePicker(people = Team, selected = null, onSelectedChange = {}, currentUserId = "ada")
        }
        rule.onNodeWithText("Assign to me").assertTextFullyVisible()
    }

    private companion object {
        const val SearchWaitMillis = 300L
    }
}
