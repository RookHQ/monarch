package dev.monarchui.components.search

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.components.search.querybuilder.MonarchQueryBuilder
import dev.monarchui.components.search.querybuilder.MonarchQueryBuilderLabels
import dev.monarchui.components.search.querybuilder.MonarchQueryBuilderTokens
import dev.monarchui.components.search.querybuilder.MonarchQueryCombinator as Comb
import dev.monarchui.components.search.querybuilder.MonarchQueryGroup
import dev.monarchui.components.workflow.conditioneditor.MonarchCondition
import dev.monarchui.components.workflow.conditioneditor.MonarchConditionField
import dev.monarchui.components.workflow.conditioneditor.MonarchConditionFieldType
import dev.monarchui.components.workflow.conditioneditor.MonarchConditionOperator as Op
import dev.monarchui.components.workflow.conditioneditor.MonarchConditionProblem
import dev.monarchui.components.workflow.conditioneditor.MonarchConditionValue as Cv
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.assertIsBefore
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

private val Flagged =
    MonarchConditionField(key = "flagged", label = "Flagged", type = MonarchConditionFieldType.Boolean)
private val Amount = MonarchConditionField(key = "amount", label = "Amount", type = MonarchConditionFieldType.Number)
private val Fields = listOf(Flagged, Amount)

private fun flagged(id: String, yes: Boolean = true) =
    MonarchCondition(id, "flagged", if (yes) Op.IsTrue else Op.IsFalse)

internal object MonarchQueryBuilderFixtures : ComponentFixtures {
    override val id = "query-builder"
    override val fixtures = listOf(
        fixture("empty top group, flat") {
            MonarchQueryBuilder(
                value = MonarchQueryGroup(id = "root"),
                onValueChange = {},
                fields = Fields,
                maxDepth = 1,
            )
        },
        fixture("empty top group with Add group") {
            MonarchQueryBuilder(value = MonarchQueryGroup(id = "root"), onValueChange = {}, fields = Fields)
        },
        fixture("disabled") {
            MonarchQueryBuilder(
                value = MonarchQueryGroup(id = "root"),
                onValueChange = {},
                fields = Fields,
                enabled = false,
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchQueryBuilderTest {
    @get:Rule
    val rule = createComposeRule()

    private val nested = MonarchQueryGroup(
        id = "root",
        rules = listOf(flagged("r1")),
        groups = listOf(MonarchQueryGroup(id = "g1", combinator = Comb.Or, rules = listOf(flagged("r2", false)))),
    )

    private fun show(value: MonarchQueryGroup, onChange: (MonarchQueryGroup) -> Unit = {}, maxDepth: Int = 3) {
        rule.setMonarchContent {
            MonarchQueryBuilder(
                value = value,
                onValueChange = onChange,
                fields = Fields,
                modifier = Modifier.verticalScroll(rememberScrollState()),
                maxDepth = maxDepth,
            )
        }
    }

    @Test
    fun groupsAndRulesAreHeadingsAndTheBuilderIsNamed() {
        show(nested)
        rule.onNodeWithContentDescription("Query").assertExists()
        rule.onNode(hasText("Top group") and isHeading()).assertExists()
        rule.onNode(hasText("Group 1") and isHeading()).assertExists()
        rule.onAllNodes(hasText("Rule 1") and isHeading()).fetchSemanticsNodes().let { assertEquals(2, it.size) }
        rule.onNodeWithContentDescription("Remove rule 1 of Group 1").assertExists()
        rule.onNodeWithContentDescription("Remove Group 1").assertExists()
    }

    @Test
    fun theCombinatorSwitchIsNamedAndReportsTheChoice() {
        var result: MonarchQueryGroup? = null
        show(nested, { result = it })
        rule.onNode(hasContentDescription("Top group combinator")).assertExists()
        rule.onAllNodes(hasText("AND"))[0].assertIsSelected()
        rule.onAllNodes(hasText("OR"))[0].assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(Comb.Or, result!!.combinator)
        assertEquals(nested.groups, result.groups)
    }

    @Test
    fun addGroupNestsAGroupWithTheOppositeCombinatorAndAFreshId() {
        var result: MonarchQueryGroup? = null
        show(MonarchQueryGroup(id = "root"), { result = it })
        rule.onNodeWithText("Add group").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(listOf(MonarchQueryGroup(id = "q-1", combinator = Comb.Or)), result!!.groups)
    }

    @Test
    fun addRuleInANestedGroupChangesOnlyThatGroup() {
        var result: MonarchQueryGroup? = null
        show(nested, { result = it })
        rule.onAllNodes(hasText("Add rule"))[1].performScrollTo().performClick()
        val group = result!!.groups.single()
        assertEquals(listOf("r2", "q-1"), group.rules.map { it.id })
        assertEquals(nested.rules, result.rules)
    }

    @Test
    fun removingANestedGroupKeepsTheRest() {
        var result: MonarchQueryGroup? = null
        show(nested, { result = it })
        rule.onNodeWithContentDescription("Remove Group 1").performScrollTo().performClick()
        assertEquals(nested.copy(groups = emptyList()), result)
    }

    @Test
    fun theDeepestLevelShowsANoteInsteadOfAddGroup() {
        show(nested, maxDepth = 2)
        rule.onNodeWithText("Groups cannot be nested any deeper.").assertExists()
        rule.onAllNodes(hasText("Add group")).fetchSemanticsNodes().let { assertEquals(1, it.size) }
    }

    @Test
    fun aFlatBuilderHasNoGroupsAtAll() {
        show(MonarchQueryGroup(id = "root"), maxDepth = 1)
        rule.onNodeWithText("Add group").assertDoesNotExist()
        rule.onNodeWithText("Groups cannot be nested any deeper.").assertDoesNotExist()
        rule.onNodeWithText("No rules in this group.").assertExists()
    }

    @Test
    fun disabledBuilderCannotChange() {
        rule.setMonarchContent {
            MonarchQueryBuilder(value = nested, onValueChange = { error("changed") }, fields = Fields, enabled = false,
                modifier = Modifier.verticalScroll(rememberScrollState()))
        }
        rule.onAllNodes(hasText("Add group"))[0].assertIsNotEnabled()
        rule.onNodeWithContentDescription("Remove Group 1").assertIsNotEnabled()
    }

    @Test
    fun labelsReplaceTheBuiltInStrings() {
        rule.setMonarchContent {
            MonarchQueryBuilder(
                value = nested,
                onValueChange = {},
                fields = Fields,
                modifier = Modifier.verticalScroll(rememberScrollState()),
                labels = MonarchQueryBuilderLabels(
                    group = { if (it.size == 1) "All tickets" else "Subset ${it.last()}" },
                    addGroup = "Add subset",
                    and = "ALL",
                ),
            )
        }
        rule.onNodeWithText("All tickets").assertExists()
        rule.onNodeWithText("Subset 1").assertExists()
        rule.onAllNodes(hasText("Add subset"))[0].assertExists()
        rule.onAllNodes(hasText("ALL"))[0].assertExists()
    }

    @Test
    fun theGroupNameComesBeforeItsRemoveButtonRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchQueryBuilder(value = nested, onValueChange = {}, fields = Fields,
                modifier = Modifier.verticalScroll(rememberScrollState()))
        }
        rule.onNodeWithText("Group 1")
            .assertIsBefore(rule.onNodeWithContentDescription("Remove Group 1"), LayoutDirection.Rtl)
    }

    @Test
    fun theJoinTextIsNotCutAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchQueryBuilder(value = MonarchQueryGroup(id = "root"), onValueChange = {}, fields = Fields,
                modifier = Modifier.verticalScroll(rememberScrollState()))
        }
        rule.onNodeWithText("Join rules with").assertTextFullyVisible()
    }

    @Test
    fun queriesFlattenValidateAndEvaluate() {
        val query = MonarchQueryGroup(
            id = "root",
            rules = listOf(MonarchCondition("a", "amount", Op.Gte, Cv.Number("500"))),
            groups = listOf(
                MonarchQueryGroup(
                    id = "g",
                    combinator = Comb.Or,
                    rules = listOf(flagged("b"), MonarchCondition("c", "amount", Op.Eq, Cv.Number(""))),
                ),
            ),
        )
        assertEquals(listOf("a", "b", "c"), query.allRules().map { it.id })
        assertEquals(mapOf("c" to MonarchConditionProblem.Value), query.problems(Fields))
        assertTrue(query.matches(mapOf("amount" to 600, "flagged" to true)))
        assertFalse(query.matches(mapOf("amount" to 120, "flagged" to true)))
        assertTrue(query.copy(combinator = Comb.Or).matches(mapOf("amount" to 120, "flagged" to true)))
        assertTrue(MonarchQueryGroup(id = "empty").matches(emptyMap()))
        assertEquals("q-1", MonarchQueryBuilderTokens.freshId(query))
        assertEquals(Comb.And, MonarchQueryBuilderTokens.childCombinator(Comb.Or))
    }

    @Test
    fun nestedLevelsAlternateTheirFillInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val top = MonarchQueryBuilderTokens.groupColors(colors, 1)
            val second = MonarchQueryBuilderTokens.groupColors(colors, 2)
            assertEquals(colors.bgSubtle, top.container)
            assertEquals(colors.bgMuted, second.container)
            assertNotEquals(top.container, second.container)
            assertEquals(colors.borderDefault, top.border)
        }
    }
}
