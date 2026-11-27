package com.rookhq.monarch.components.workflow

import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.components.workflow.dependencypicker.MonarchDependencyItem
import com.rookhq.monarch.components.workflow.dependencypicker.MonarchDependencyLink
import com.rookhq.monarch.components.workflow.dependencypicker.MonarchDependencyPicker
import com.rookhq.monarch.components.workflow.dependencypicker.MonarchDependencyPickerLabels
import com.rookhq.monarch.components.workflow.dependencypicker.MonarchDependencyPickerTokens
import com.rookhq.monarch.components.workflow.dependencypicker.MonarchDependencyRelation
import com.rookhq.monarch.components.workflow.statuspicker.MonarchStatusCategory
import com.rookhq.monarch.components.workflow.statuspicker.MonarchStatusOption
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.assertIsBefore
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private val Started = MonarchStatusOption("doing", "In progress", MonarchStatusCategory.Started)
private val Login = MonarchDependencyItem("i1", "Fix the login redirect", key = "STR-12", status = Started)
private val Billing = MonarchDependencyItem("i2", "Move billing to the new API", key = "STR-30")
private val Docs = MonarchDependencyItem("i3", "Write the release notes")
private val Self = MonarchDependencyItem("self", "This issue", key = "STR-1")
private val BlockedByLogin = MonarchDependencyLink("l1", MonarchDependencyRelation.BlockedBy, Login)
private val RelatesDocs = MonarchDependencyLink("l2", MonarchDependencyRelation.Relates, Docs)

internal object MonarchDependencyPickerFixtures : ComponentFixtures {
    override val id = "dependency-picker"
    override val fixtures = listOf(
        fixture("empty with add controls") {
            MonarchDependencyPicker(value = emptyList(), onAdd = { _, _ -> }, items = listOf(Login, Billing))
        },
        fixture("links with remove and open") {
            MonarchDependencyPicker(value = listOf(BlockedByLogin, RelatesDocs), onRemove = {}, onOpen = {})
        },
        fixture("read only") {
            MonarchDependencyPicker(value = listOf(BlockedByLogin), onAdd = { _, _ -> }, onRemove = {}, readOnly = true)
        },
        fixture("disabled") {
            MonarchDependencyPicker(value = listOf(RelatesDocs), onAdd = { _, _ -> }, onRemove = {}, enabled = false)
        },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchDependencyPickerTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun linksAreGroupedUnderRelationHeadingsAndReadWithTheirStatus() {
        rule.setMonarchContent { MonarchDependencyPicker(value = listOf(RelatesDocs, BlockedByLogin)) }
        val first = rule.onNode(hasText("Blocked by") and isHeading()).getUnclippedBoundsInRoot()
        val second = rule.onNode(hasText("Related to") and isHeading()).getUnclippedBoundsInRoot()
        assertTrue(first.top < second.top)
        rule.onNodeWithText("Fix the login redirect").assertExists()
        rule.onNodeWithContentDescription("Status: In progress", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("No linked items.").assertDoesNotExist()
    }

    @Test
    fun relationGroupsFollowTheOrderOfRelations() {
        val groups = MonarchDependencyPickerTokens.groups(
            listOf(RelatesDocs, BlockedByLogin),
            listOf(
                MonarchDependencyRelation.Relates,
                MonarchDependencyRelation.BlockedBy,
                MonarchDependencyRelation.Blocks,
            ),
        )
        val order = groups.map { it.first }
        assertEquals(listOf(MonarchDependencyRelation.Relates, MonarchDependencyRelation.BlockedBy), order)
    }

    @Test
    fun removeIsNamedAndAnnounced() {
        var removed: MonarchDependencyLink? = null
        rule.setMonarchContent { MonarchDependencyPicker(value = listOf(BlockedByLogin), onRemove = { removed = it }) }
        rule.onNodeWithContentDescription("Remove “Blocked by” link to STR-12 Fix the login redirect")
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        assertEquals(BlockedByLogin, removed)
        rule.onNodeWithContentDescription("Blocked by STR-12 Fix the login redirect removed.")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
    }

    @Test
    fun aRowOpensTheItemWhenOnOpenIsGiven() {
        var opened: MonarchDependencyItem? = null
        rule.setMonarchContent { MonarchDependencyPicker(value = listOf(RelatesDocs), onOpen = { opened = it }) }
        rule.onNode(hasText("Write the release notes") and hasClickAction())
            .assertHasClickAction()
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        assertEquals(Docs, opened)
    }

    @Test
    fun readOnlyHidesAddAndRemove() {
        rule.setMonarchContent {
            MonarchDependencyPicker(value = listOf(BlockedByLogin), onAdd = { _, _ -> }, onRemove = {}, readOnly = true)
        }
        rule.onNodeWithText("Relation").assertDoesNotExist()
        rule.onNodeWithText("Remove").assertDoesNotExist()
    }

    @Test
    fun disabledRemoveDoesNothing() {
        rule.setMonarchContent {
            MonarchDependencyPicker(value = listOf(BlockedByLogin), onRemove = { error("removed") }, enabled = false)
        }
        rule.onNodeWithContentDescription("Remove “Blocked by” link to STR-12 Fix the login redirect")
            .assertIsNotEnabled()
    }

    @Test
    fun theSearchLeavesOutLinkedAndExcludedItems() {
        val candidates = MonarchDependencyPickerTokens.candidates(
            items = listOf(Login, Billing, Docs, Self),
            links = listOf(BlockedByLogin),
            excludeIds = setOf("self"),
        )
        assertEquals(listOf(Billing, Docs), candidates)
        val option = MonarchDependencyPickerTokens.option(Login)
        assertEquals("STR-12", option.hint)
        assertEquals("In progress", option.description)
        assertEquals("STR-12 Fix the login redirect", MonarchDependencyPickerTokens.itemLabel(Login))
        assertEquals("Write the release notes", MonarchDependencyPickerTokens.itemLabel(Docs))
    }

    @Test
    fun theAddControlsShowTheRelationAndTheSearch() {
        rule.setMonarchContent {
            MonarchDependencyPicker(
                value = emptyList(),
                onAdd = { _, _ -> },
                items = listOf(Billing),
                defaultRelation = MonarchDependencyRelation.Duplicates,
            )
        }
        rule.onNodeWithText("No linked items.").assertExists()
        rule.onNodeWithText("Relation", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Duplicates", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Find an item to link", useUnmergedTree = true).assertExists()
    }

    @Test
    fun labelsReplaceTheBuiltInStrings() {
        rule.setMonarchContent {
            MonarchDependencyPicker(
                value = listOf(BlockedByLogin),
                labels = MonarchDependencyPickerLabels(
                    relations = mapOf(MonarchDependencyRelation.BlockedBy to "Waiting on"),
                    status = { "State $it" },
                ),
            )
        }
        rule.onNodeWithText("Waiting on").assertExists()
        rule.onNodeWithContentDescription("State In progress", useUnmergedTree = true).assertExists()
    }

    @Test
    fun theTitleComesBeforeTheRemoveButtonRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchDependencyPicker(value = listOf(RelatesDocs), onRemove = {})
        }
        rule.onNodeWithText("Write the release notes", useUnmergedTree = true).assertIsBefore(
            rule.onNodeWithContentDescription("Remove “Related to” link to Write the release notes"),
            LayoutDirection.Rtl,
        )
    }

    @Test
    fun aLongTitleWrapsAtTheLargestFontScale() {
        val long = MonarchDependencyItem("x", "Investigate the intermittent timeouts on checkout", key = "OPS-7")
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchDependencyPicker(value = listOf(MonarchDependencyLink("l", MonarchDependencyRelation.Blocks, long)))
        }
        rule.onNodeWithText("Investigate the intermittent timeouts on checkout", useUnmergedTree = true)
            .assertTextFullyVisible()
    }
}
