package dev.monarchui.components.datadisplay

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.monarchui.components.datadisplay.statusbadge.LocalMonarchStatusRegistry
import dev.monarchui.components.datadisplay.statusbadge.MonarchStatusBadge
import dev.monarchui.components.datadisplay.statusbadge.MonarchStatusBadgeSize
import dev.monarchui.components.datadisplay.statusbadge.MonarchStatusBadgeTokens
import dev.monarchui.components.datadisplay.statusbadge.MonarchStatusBadgeVariant
import dev.monarchui.components.datadisplay.statusbadge.MonarchStatusDefinition
import dev.monarchui.components.datadisplay.statusbadge.MonarchStatusRegistry
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.foundation.icon.MonarchIconSize
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

private enum class TicketStatus { Open, Waiting }

internal object MonarchStatusBadgeFixtures : ComponentFixtures {
    override val id = "status-badge"
    override val fixtures = MonarchStatusBadgeVariant.entries.map { variant ->
        fixture("built-in statuses, ${variant.name}") {
            FlowRow {
                listOf("operational", "degraded", "partial", "down", "maintenance", "unknown").forEach {
                    MonarchStatusBadge(status = it, variant = variant)
                }
            }
        }
    } + listOf(
        fixture("generic set, small") {
            FlowRow {
                listOf("success", "warning", "danger", "info", "neutral").forEach {
                    MonarchStatusBadge(status = it, size = MonarchStatusBadgeSize.Sm)
                }
            }
        },
        fixture("unknown key and null") {
            Column {
                MonarchStatusBadge(status = "pending_delete")
                MonarchStatusBadge(status = null)
            }
        },
        fixture("custom definition with an icon") {
            MonarchStatusBadge(
                definition = MonarchStatusDefinition(
                    label = "Waiting on customer",
                    tone = MonarchTone.Warning,
                    icon = { MonarchIcon(TestIcon, null, size = MonarchIconSize.Sm) },
                ),
            )
        },
        fixture("long text") {
            MonarchStatusBadge(status = "down", label = "Down in three regions since the last deployment of the API")
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchStatusBadgeTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun aBuiltInKeyShowsItsLabelAndIsNotActionable() {
        rule.setMonarchContent { MonarchStatusBadge(status = "partial") }
        rule.onNodeWithText("Partial outage")
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.OnClick))
    }

    @Test
    fun anUnknownKeyIsMadeReadableAndNullReadsUnknown() {
        rule.setMonarchContent {
            Column {
                MonarchStatusBadge(status = "pending_delete")
                MonarchStatusBadge(status = null)
                MonarchStatusBadge(status = "__")
            }
        }
        rule.onNodeWithText("Pending delete").assertExists()
        rule.onAllNodesWithText("Unknown").assertCountEquals(2)
        assertEquals("Pending delete", MonarchStatusBadgeTokens.formatLabel("PENDING-delete"))
        assertNull(MonarchStatusBadgeTokens.formatLabel("__"))
    }

    @Test
    fun theBadgeRegistryWinsOverTheProvidedOneWhichWinsOverTheBuiltInSet() {
        val provided = MonarchStatusRegistry(mapOf("down" to MonarchStatusDefinition("Offline", MonarchTone.Danger)))
        val own = MonarchStatusRegistry.of(
            mapOf(TicketStatus.Open to MonarchStatusDefinition("Open ticket", MonarchTone.Info)),
        )
        rule.setMonarchContent {
            CompositionLocalProvider(LocalMonarchStatusRegistry provides provided) {
                Column {
                    MonarchStatusBadge(status = "down")
                    MonarchStatusBadge(status = "Open", registry = own)
                    MonarchStatusBadge(status = "operational")
                }
            }
        }
        rule.onNodeWithText("Offline").assertExists()
        rule.onNodeWithText("Open ticket").assertExists()
        rule.onNodeWithText("Operational").assertExists()
    }

    @Test
    fun mergingKeepsBothSetsAndTheOtherWins() {
        val first = MonarchStatusRegistry(
            mapOf(
                "a" to MonarchStatusDefinition("A", MonarchTone.Info),
                "b" to MonarchStatusDefinition("B", MonarchTone.Info),
            ),
        )
        val second = MonarchStatusRegistry(mapOf("b" to MonarchStatusDefinition("B2", MonarchTone.Danger)))
        val merged = first.merging(second)
        assertEquals("A", merged.resolve("a")?.label)
        assertEquals("B2", merged.resolve("b")?.label)
        assertNull(merged.resolve(null))
    }

    @Test
    fun theLabelReplacesTheRegistryLabelAndTheIconIsHidden() {
        rule.setMonarchContent {
            MonarchStatusBadge(
                status = "down",
                label = "Hors service",
                icon = { MonarchIcon(TestIcon, contentDescription = "should not be read") },
            )
        }
        rule.onNodeWithText("Hors service").assertTextEquals("Hors service")
        rule.onNodeWithText("Hors service").assert(
            SemanticsMatcher("has no content description") {
                SemanticsProperties.ContentDescription !in it.config
            },
        )
    }

    @Test
    fun theIconComesBeforeTheLabelInRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchStatusBadge(
                status = "down",
                modifier = Modifier.testTag("badge"),
                icon = { MonarchIcon(TestIcon, null, size = MonarchIconSize.Sm) },
            )
        }
        val badge = rule.onNodeWithTag("badge").getUnclippedBoundsInRoot()
        val text = rule.onNodeWithText("Down", useUnmergedTree = true).getUnclippedBoundsInRoot()
        // The icon takes the start, which is the right side in right to left.
        assertTrue(badge.right - text.right > badge.left.let { text.left - it })
    }

    @Test
    fun aLongLabelWrapsAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchStatusBadge(
                status = "down",
                label = "Down in three regions since the last deployment",
                modifier = Modifier.width(200.dp),
            )
        }
        rule.onNodeWithText("Down in three", substring = true, useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun coloursComeFromTheToneRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val subtle = MonarchStatusBadgeTokens.colors(MonarchTone.Success, MonarchStatusBadgeVariant.Subtle, colors)
            assertEquals(colors.successSubtle, subtle.container, theme.id)
            assertEquals(colors.successStrong, subtle.content, theme.id)
            val outline = MonarchStatusBadgeTokens.colors(MonarchTone.Danger, MonarchStatusBadgeVariant.Outline, colors)
            assertEquals(Color.Transparent, outline.container, theme.id)
            assertEquals(colors.dangerBorder, outline.border, theme.id)
            val solid = MonarchStatusBadgeTokens.colors(MonarchTone.Info, MonarchStatusBadgeVariant.Solid, colors)
            assertEquals(colors.infoSolid, solid.container, theme.id)
            assertEquals(colors.infoOnSolid, solid.content, theme.id)
            val dot = MonarchStatusBadgeTokens.colors(MonarchTone.Warning, MonarchStatusBadgeVariant.Dot, colors)
            assertEquals(Color.Transparent, dot.container, theme.id)
            assertEquals(colors.textDefault, dot.content, theme.id)
            assertEquals(colors.warningSolid, MonarchStatusBadgeTokens.dotColor(MonarchTone.Warning, colors), theme.id)
            assertEquals(colors.textSubtle, MonarchStatusBadgeTokens.dotColor(MonarchTone.Neutral, colors), theme.id)
        }
    }

    @Test
    fun theBuiltInSetMapsEachKeyToItsTone() {
        assertEquals(MonarchTone.Success, MonarchStatusBadgeTokens.BuiltIn.getValue("operational").tone)
        assertEquals(MonarchTone.Warning, MonarchStatusBadgeTokens.BuiltIn.getValue("partial").tone)
        assertEquals(MonarchTone.Danger, MonarchStatusBadgeTokens.BuiltIn.getValue("down").tone)
        assertEquals(MonarchTone.Info, MonarchStatusBadgeTokens.BuiltIn.getValue("maintenance").tone)
        assertEquals(MonarchTone.Neutral, MonarchStatusBadgeTokens.BuiltIn.getValue("unknown").tone)
        assertEquals(11, MonarchStatusBadgeTokens.BuiltIn.size)
    }

    @Test
    fun aBadgeInARowThatMergesIsReadWithTheRow() {
        rule.setMonarchContent {
            Row(Modifier.semantics(mergeDescendants = true) { }) {
                Text("API")
                MonarchStatusBadge(status = "partial", mergeSemantics = false)
            }
        }
        rule.onNode(hasText("API") and hasText("Partial outage")).assertExists()
    }

    @Test
    fun aBadgeMergesItsOwnContentByDefault() {
        rule.setMonarchContent { MonarchStatusBadge(status = "partial") }
        rule.onAllNodesWithText("Partial outage").assertCountEquals(1)
    }
}
