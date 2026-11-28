package dev.monarchui.components.datadisplay

import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.monarchui.components.datadisplay.keyvalue.MonarchKeyValue
import dev.monarchui.components.datadisplay.keyvalue.MonarchKeyValueItem
import dev.monarchui.components.datadisplay.keyvalue.MonarchKeyValueLayout
import dev.monarchui.components.datadisplay.keyvalue.MonarchKeyValueSeparator
import dev.monarchui.components.datadisplay.keyvalue.MonarchKeyValueSize
import dev.monarchui.components.datadisplay.keyvalue.MonarchKeyValueTokens
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.foundation.icon.MonarchIconSize
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.TestIcon
import dev.monarchui.testing.assertIsBefore
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

private val pairs = listOf(
    MonarchKeyValueItem("Region", "eu-west"),
    MonarchKeyValueItem("Plan", "Team"),
    MonarchKeyValueItem("Owner", null),
)

internal object MonarchKeyValueFixtures : ComponentFixtures {
    override val id = "key-value"
    override val fixtures = MonarchKeyValueSeparator.entries.map { separator ->
        fixture("wrap, ${separator.name}") { MonarchKeyValue(items = pairs, separator = separator) }
    } + listOf(
        fixture("body size") { MonarchKeyValue(items = pairs, size = MonarchKeyValueSize.Md) },
        fixture("stack") { MonarchKeyValue(items = pairs, layout = MonarchKeyValueLayout.Stack) },
        fixture("copy as text and monospace") {
            MonarchKeyValue(
                items = listOf(MonarchKeyValueItem("ID", "4f2a…", copyValue = "4f2a9c1e", monospace = true)),
            )
        },
        fixture("copy as icon") {
            MonarchKeyValue(
                items = listOf(MonarchKeyValueItem("Key", "pk_live_1", copyable = true)),
                copyIcon = { MonarchIcon(TestIcon, null, size = MonarchIconSize.Sm) },
            )
        },
        fixture("long text") {
            MonarchKeyValue(
                items = listOf(
                    MonarchKeyValueItem(
                        "Billing address",
                        "14 Harbour Street, Floor 3, Edinburgh EH6 6QW, United Kingdom",
                    ),
                    MonarchKeyValueItem("Note", "Renews automatically at the end of every annual period"),
                ),
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchKeyValueTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun eachLabelAndValueAreOneElement() {
        rule.setMonarchContent { MonarchKeyValue(items = pairs) }
        rule.onNodeWithText("Region").assertTextEquals("Region", "eu-west")
        rule.onNodeWithText("Plan").assertTextEquals("Plan", "Team")
    }

    @Test
    fun anEmptyValueShowsADashReadAsNotSet() {
        rule.setMonarchContent { MonarchKeyValue(items = pairs) }
        rule.onNodeWithText("Owner").assertContentDescriptionEquals("Not set")
        rule.onAllNodesWithText(MonarchKeyValueTokens.EMPTY_MARK, substring = true).assertCountEquals(0)
    }

    @Test
    fun theEmptyLabelReplacesNotSet() {
        rule.setMonarchContent { MonarchKeyValue(items = pairs, emptyLabel = "Nobody") }
        rule.onNodeWithText("Owner").assertContentDescriptionEquals("Nobody")
    }

    @Test
    fun separatorsAreHiddenFromTalkBack() {
        rule.setMonarchContent { MonarchKeyValue(items = pairs) }
        rule.onAllNodesWithText(MonarchKeyValueTokens.DOT).assertCountEquals(0)
    }

    @Test
    fun aCopyButtonCopiesTheCopyValueAndIsNamedAfterTheLabel() {
        val copied = mutableListOf<String>()
        rule.setMonarchContent {
            MonarchKeyValue(
                items = listOf(
                    MonarchKeyValueItem("ID", "4f2a…", copyValue = "4f2a9c1e"),
                    MonarchKeyValueItem("Region", "eu-west"),
                    MonarchKeyValueItem("Empty", "", copyable = true),
                ),
                onCopy = { copied += it },
            )
        }
        rule.onAllNodesWithContentDescription("Copy", substring = true).assertCountEquals(1)
        rule.onNodeWithContentDescription("Copy ID").assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.waitForIdle()
        assertEquals(listOf("4f2a9c1e"), copied)
    }

    @Test
    fun theIconCopyButtonIsNamedToo() {
        rule.setMonarchContent {
            MonarchKeyValue(
                items = listOf(MonarchKeyValueItem("Key", "pk_live_1", copyable = true)),
                copyIcon = { MonarchIcon(TestIcon, null, size = MonarchIconSize.Sm) },
            )
        }
        rule.onNodeWithContentDescription("Copy Key").assertTouchTargetAtLeast48dp(rule.density)
    }

    @Test
    fun theStackPutsEachPairOnItsOwnLine() {
        rule.setMonarchContent { MonarchKeyValue(items = pairs, layout = MonarchKeyValueLayout.Stack) }
        val first = rule.onNodeWithText("Region").getUnclippedBoundsInRoot()
        val second = rule.onNodeWithText("Plan").getUnclippedBoundsInRoot()
        assertTrue(second.top >= first.bottom)
    }

    @Test
    fun theValueFollowsTheLabelInRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) { MonarchKeyValue(items = pairs) }
        rule.onNodeWithText("Region", useUnmergedTree = true)
            .assertIsBefore(rule.onNodeWithText("eu-west", useUnmergedTree = true), LayoutDirection.Rtl)
        rule.onNodeWithText("eu-west", useUnmergedTree = true)
            .assertIsBefore(rule.onNodeWithText("Plan", useUnmergedTree = true), LayoutDirection.Rtl)
    }

    @Test
    fun longValuesWrapAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchKeyValue(
                items = listOf(MonarchKeyValueItem("Address", "14 Harbour Street, Floor 3, Edinburgh")),
                modifier = Modifier.width(240.dp),
            )
        }
        rule.onNodeWithText("14 Harbour", substring = true, useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun coloursComeFromTheRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val keyValue = MonarchKeyValueTokens.colors(colors)
            assertEquals(colors.textMuted, keyValue.label, theme.id)
            assertEquals(colors.textDefault, keyValue.value, theme.id)
            assertEquals(colors.textSubtle, keyValue.dot, theme.id)
            assertEquals(colors.borderDefault, keyValue.divider, theme.id)
        }
    }
}
