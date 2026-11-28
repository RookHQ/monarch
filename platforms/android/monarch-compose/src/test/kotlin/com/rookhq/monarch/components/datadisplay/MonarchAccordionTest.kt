package com.rookhq.monarch.components.datadisplay

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.datadisplay.accordion.MonarchAccordion
import com.rookhq.monarch.components.datadisplay.accordion.MonarchAccordionItem
import com.rookhq.monarch.components.datadisplay.accordion.MonarchAccordionTokens
import com.rookhq.monarch.components.layout.divider.MonarchDividerTokens
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.MonarchTestEnvironment
import com.rookhq.monarch.testing.TestIcon
import com.rookhq.monarch.testing.assertIsBefore
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.testing.stateDescription
import com.rookhq.monarch.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private val faq = listOf(
    MonarchAccordionItem("billing", "How does billing work?"),
    MonarchAccordionItem("cancel", "Can I cancel at any time?"),
    MonarchAccordionItem("legacy", "Legacy plans", enabled = false),
)

internal object MonarchAccordionFixtures : ComponentFixtures {
    override val id = "accordion"
    override val fixtures = listOf(
        fixture("all closed") {
            MonarchAccordion(items = faq, expanded = emptySet(), onExpandedChange = {}) { Text("") }
        },
        fixture("one open with an icon") {
            MonarchAccordion(
                items = faq,
                expanded = setOf("billing"),
                onExpandedChange = {},
                trailingIcon = { MonarchIcon(imageVector = TestIcon, contentDescription = null) },
            ) { Text("Invoices are sent on the first of the month.") }
        },
        fixture("multiple open") {
            MonarchAccordion(
                items = faq.take(2),
                expanded = setOf("billing", "cancel"),
                onExpandedChange = {},
                multiple = true,
            ) { item -> Text("Answer: ${item.title}") }
        },
        fixture("disabled") {
            MonarchAccordion(items = faq.take(2), expanded = emptySet(), onExpandedChange = {}, enabled = false) {}
        },
        fixture("long title in a narrow width") {
            MonarchAccordion(
                items = listOf(
                    MonarchAccordionItem("long", "What happens to my data when my trial ends and I do not pay?"),
                ),
                expanded = emptySet(),
                onExpandedChange = {},
                modifier = Modifier.width(240.dp),
            ) {}
        },
        fixture("subtitles and trailing content") {
            MonarchAccordion(
                items = listOf(
                    MonarchAccordionItem("rules", "Rules", subtitle = "3 rules, 1 off"),
                    MonarchAccordionItem("alerts", "Alerts", subtitle = "None set"),
                ),
                expanded = emptySet(),
                onExpandedChange = {},
                headerTrailing = { Text(if (it.id == "rules") "3" else "0") },
            ) { Text("") }
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchAccordionTest {
    @get:Rule
    val rule = createComposeRule()

    private fun header(title: String) = rule.onNode(hasText(title) and hasClickAction())

    @Test
    fun eachHeaderIsAHeadingButtonWithItsState() {
        rule.setMonarchContent {
            MonarchAccordion(items = faq, expanded = setOf("billing"), onExpandedChange = {}) { Text("Answer") }
        }
        header("How does billing work?")
            .assert(isHeading())
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertTouchTargetAtLeast48dp(rule.density)
        assertEquals("Expanded", header("How does billing work?").stateDescription())
        assertEquals("Collapsed", header("Can I cancel at any time?").stateDescription())
        rule.onNodeWithText("Answer").assertExists()
    }

    @Test
    fun openingOneClosesTheOtherByDefault() {
        var expanded by mutableStateOf(setOf("billing"))
        rule.setMonarchContent {
            MonarchAccordion(items = faq, expanded = expanded, onExpandedChange = { expanded = it }) { item ->
                Text("Answer to ${item.id}")
            }
        }
        header("Can I cancel at any time?").performClick()
        rule.waitForIdle()
        assertEquals(setOf("cancel"), expanded)
        rule.onNodeWithText("Answer to billing").assertDoesNotExist()
        rule.onNodeWithText("Answer to cancel").assertExists()
        header("Can I cancel at any time?").performClick()
        rule.waitForIdle()
        assertEquals(emptySet(), expanded)
    }

    @Test
    fun withMultipleSeveralStayOpen() {
        var expanded by mutableStateOf(setOf("billing"))
        rule.setMonarchContent {
            MonarchAccordion(items = faq, expanded = expanded, onExpandedChange = { expanded = it }, multiple = true) {}
        }
        header("Can I cancel at any time?").performSemanticsAction(SemanticsActions.Expand)
        rule.waitForIdle()
        assertEquals(setOf("billing", "cancel"), expanded)
        header("How does billing work?").performSemanticsAction(SemanticsActions.Collapse)
        rule.waitForIdle()
        assertEquals(setOf("cancel"), expanded)
    }

    @Test
    fun withoutCollapsibleTheOpenSectionStaysOpen() {
        val changes = mutableListOf<Set<String>>()
        rule.setMonarchContent {
            MonarchAccordion(
                items = faq,
                expanded = setOf("billing"),
                onExpandedChange = { changes += it },
                collapsible = false,
            ) {}
        }
        header("How does billing work?")
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.Collapse))
            .performClick()
        assertTrue(changes.isEmpty())
    }

    @Test
    fun aDisabledSectionDoesNotOpen() {
        val changes = mutableListOf<Set<String>>()
        rule.setMonarchContent {
            MonarchAccordion(items = faq, expanded = emptySet(), onExpandedChange = { changes += it }) {}
        }
        rule.onNode(hasText("Legacy plans")).assertIsNotEnabled().performClick()
        assertTrue(changes.isEmpty())
    }

    @Test
    fun theBuiltInStatesCanBeReplaced() {
        rule.setMonarchContent {
            MonarchAccordion(
                items = faq.take(2),
                expanded = setOf("billing"),
                onExpandedChange = {},
                expandedLabel = "Ouvert",
                collapsedLabel = "Fermé",
            ) {}
        }
        assertEquals("Ouvert", header("How does billing work?").stateDescription())
        assertEquals("Fermé", header("Can I cancel at any time?").stateDescription())
    }

    @Test
    fun theIconIsAtTheEndInBothDirections() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) {
            MonarchAccordion(
                items = faq.take(1),
                expanded = emptySet(),
                onExpandedChange = {},
                trailingIcon = { Box(Modifier.testTag("icon").width(16.dp)) },
            ) {}
        }
        listOf(LayoutDirection.Ltr, LayoutDirection.Rtl).forEach { direction ->
            environment.layoutDirection = direction
            rule.waitForIdle()
            rule.onNodeWithText("How does billing work?", useUnmergedTree = true)
                .assertIsBefore(rule.onNodeWithTag("icon", useUnmergedTree = true), direction)
        }
    }

    @Test
    fun titlesWrapAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchAccordion(items = faq.take(1), expanded = emptySet(), onExpandedChange = {}) {}
        }
        rule.onNodeWithText("How does billing work?", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun theToggleRuleFollowsTheMode() {
        assertEquals(setOf("b"), MonarchAccordionTokens.toggle(setOf("a"), "b", multiple = false, collapsible = true))
        assertEquals(
            setOf("a", "b"),
            MonarchAccordionTokens.toggle(setOf("a"), "b", multiple = true, collapsible = true),
        )
        assertEquals(emptySet(), MonarchAccordionTokens.toggle(setOf("a"), "a", multiple = false, collapsible = true))
        assertEquals(setOf("a"), MonarchAccordionTokens.toggle(setOf("a"), "a", multiple = false, collapsible = false))
        assertEquals(emptySet(), MonarchAccordionTokens.toggle(setOf("a"), "a", multiple = true, collapsible = false))
        assertFalse(MonarchAccordionTokens.canCollapse(multiple = false, collapsible = false))
    }

    @Test
    fun coloursComeFromTheThemeRoles() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.textDefault, MonarchAccordionTokens.titleColor(true, colors), theme.id)
            assertEquals(colors.textDisabled, MonarchAccordionTokens.titleColor(false, colors), theme.id)
            assertEquals(colors.textMuted, MonarchAccordionTokens.iconColor(true, colors), theme.id)
            assertEquals(colors.textDisabled, MonarchAccordionTokens.iconColor(false, colors), theme.id)
            assertEquals(colors.textMuted, MonarchAccordionTokens.contentColor(colors), theme.id)
            assertEquals(
                colors.borderDefault,
                MonarchDividerTokens.color(MonarchAccordionTokens.DividerVariant, colors),
                theme.id,
            )
        }
    }

    @Test
    fun aHeaderReadsItsTitleSubtitleAndTrailingContentAsOneHeading() {
        rule.setMonarchContent {
            MonarchAccordion(
                items = listOf(MonarchAccordionItem("rules", "Rules", subtitle = "3 rules, 1 off")),
                expanded = emptySet(),
                onExpandedChange = {},
                headerTrailing = { Text("New") },
            ) { Text("") }
        }
        rule.onNode(hasText("Rules") and hasText("3 rules, 1 off") and hasText("New") and isHeading())
            .assertTouchTargetAtLeast48dp(rule.density)
    }

    @Test
    fun theSubtitleUsesTheMutedAndDisabledRoles() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.textMuted, MonarchAccordionTokens.subtitleColor(true, colors), theme.id)
            assertEquals(colors.textDisabled, MonarchAccordionTokens.subtitleColor(false, colors), theme.id)
        }
    }
}
