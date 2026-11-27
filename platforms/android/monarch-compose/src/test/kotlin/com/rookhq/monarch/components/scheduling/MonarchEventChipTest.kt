package com.rookhq.monarch.components.scheduling

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.scheduling.eventchip.MonarchEventChip
import com.rookhq.monarch.components.scheduling.eventchip.MonarchEventChipStatus
import com.rookhq.monarch.components.scheduling.eventchip.MonarchEventChipTokens
import com.rookhq.monarch.components.scheduling.eventchip.MonarchEventChipVariant
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.MonarchTestEnvironment
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.theme.MonarchTheme
import com.rookhq.monarch.tokens.MonarchTokens
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal object MonarchEventChipFixtures : ComponentFixtures {
    override val id = "event-chip"
    override val fixtures = listOf(
        fixture("dot, every status, as buttons") {
            Column {
                MonarchEventChipStatus.entries.forEach { status ->
                    MonarchEventChip(title = "Stand-up", time = "09:30", status = status, onClick = {})
                }
            }
        },
        fixture("dot, past, own color, long title, label") {
            Column {
                MonarchEventChip(title = "Retro", time = "16:00", past = true, color = MonarchTheme.colors.infoSolid)
                MonarchEventChip(title = "Quarterly planning with the finance and operations teams", time = "11:00")
                MonarchEventChip(title = "Disabled", time = "08:00", enabled = false, onClick = {})
            }
        },
        fixture("bar, every status, as buttons") {
            Column {
                MonarchEventChipStatus.entries.forEach { status ->
                    MonarchEventChip(
                        title = "Design review",
                        variant = MonarchEventChipVariant.Bar,
                        status = status,
                        time = "14:00 – 15:00",
                        detail = "Room 4",
                        onClick = {},
                    )
                }
            }
        },
        fixture("block, regular and compact") {
            Column {
                MonarchEventChip(
                    title = "Workshop",
                    modifier = Modifier.height(MonarchTokens.Space.space24.dp),
                    variant = MonarchEventChipVariant.Block,
                    status = MonarchEventChipStatus.Tentative,
                    time = "10:00 – 12:00",
                    detail = "Lab",
                    onClick = {},
                )
                MonarchEventChip(
                    title = "Check-in",
                    modifier = Modifier.height(MonarchTokens.Space.space12.dp),
                    variant = MonarchEventChipVariant.Block,
                    compact = true,
                    time = "12:00",
                    onClick = {},
                )
            }
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchEventChipTest {
    @get:Rule
    val rule = createComposeRule()

    private val isButton = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)

    @Test
    fun aChipWithAnActionIsOneButtonReadAsTitleAndTime() {
        var opened = 0
        rule.setMonarchContent {
            MonarchEventChip(
                title = "Stand-up",
                modifier = Modifier.testTag("chip"),
                time = "09:30",
                onClick = { opened++ },
            )
        }
        rule.onNodeWithTag("chip")
            .assertContentDescriptionEquals("Stand-up, 09:30")
            .assert(isButton)
            .assertHasClickAction()
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        assertEquals(1, opened)
    }

    @Test
    fun aDisabledChipDoesNotOpenAndAChipWithoutActionIsALabel() {
        var opened = 0
        rule.setMonarchContent {
            Column {
                MonarchEventChip(
                    title = "Off",
                    modifier = Modifier.testTag("disabled"),
                    enabled = false,
                    onClick = { opened++ },
                )
                MonarchEventChip(title = "Label", modifier = Modifier.testTag("label"))
            }
        }
        rule.onNodeWithTag("disabled").assertIsNotEnabled().performClick()
        assertEquals(0, opened)
        rule.onNodeWithTag("label").assertHasNoClickAction().assertContentDescriptionEquals("Label")
    }

    @Test
    fun theStatusIsReadUnlessConfirmedAndTheDetailOnlyInBarAndBlock() {
        rule.setMonarchContent {
            Column {
                MonarchEventChip(
                    title = "Lunch",
                    modifier = Modifier.testTag("dot"),
                    status = MonarchEventChipStatus.Tentative,
                    time = "12:00",
                    detail = "Cafe",
                )
                MonarchEventChip(
                    title = "Review",
                    modifier = Modifier.testTag("bar"),
                    variant = MonarchEventChipVariant.Bar,
                    status = MonarchEventChipStatus.Cancelled,
                    time = "14:00",
                    timeLabel = "2 in the afternoon",
                    detail = "Room 4",
                )
                MonarchEventChip(
                    title = "Sync",
                    modifier = Modifier.testTag("own"),
                    status = MonarchEventChipStatus.Declined,
                    statusLabel = "Abgelehnt",
                )
            }
        }
        rule.onNodeWithTag("dot").assertContentDescriptionEquals("Lunch, 12:00, Tentative")
        rule.onNodeWithTag("bar").assertContentDescriptionEquals("Review, 2 in the afternoon, Cancelled, Room 4")
        rule.onNodeWithTag("own").assertContentDescriptionEquals("Sync, Abgelehnt")
    }

    @Test
    fun aLongDotTitleWrapsAtTheLargestFontSizeInsteadOfBeingCut() {
        val environment = MonarchTestEnvironment()
        val title = "Quarterly planning with the finance and operations teams"
        rule.setMonarchContent(environment) {
            MonarchEventChip(title = title, modifier = Modifier.width(200.dp).testTag("chip"), time = "11:00")
        }
        val oneLine = rule.onNodeWithTag("chip").fetchSemanticsNode().size.height
        environment.fontScale = LargestFontScale
        rule.waitForIdle()
        val wrapped = rule.onNodeWithTag("chip").fetchSemanticsNode().size.height
        // Twice the font size on one line would be twice as high; wrapping makes it higher still.
        assertTrue(wrapped > oneLine * 2, "Expected the title to wrap: $oneLine then $wrapped")
    }

    @Test
    fun theBarIsAtTheStartEdgeInBothDirections() {
        assertEquals(0f, MonarchEventChipTokens.barStart(200f, 3f, LayoutDirection.Ltr))
        assertEquals(197f, MonarchEventChipTokens.barStart(200f, 3f, LayoutDirection.Rtl))
    }

    @Test
    fun spokenNameSkipsEmptyParts() {
        assertEquals(
            "Title",
            MonarchEventChipTokens.spokenName("Title", "", null, "Detail", MonarchEventChipVariant.Dot),
        )
        assertEquals(
            "11:00, Detail",
            MonarchEventChipTokens.spokenName("", "11:00", null, "Detail", MonarchEventChipVariant.Block),
        )
    }

    @Test
    fun theEventColourIsOnlyTheMarkerTintAndBorderInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val event = colors.successSolid
            val bar = colors(colors, event, MonarchEventChipVariant.Bar, MonarchEventChipStatus.Confirmed)
            assertEquals(event, bar.marker, theme.id)
            assertEquals(lerp(colors.bgSurface, event, 0.14f), bar.container, theme.id)
            assertEquals(colors.textDefault, bar.title, theme.id)
            assertEquals(colors.textMuted, bar.secondary, theme.id)
            assertNull(bar.border, theme.id)

            val tentative = colors(colors, event, MonarchEventChipVariant.Block, MonarchEventChipStatus.Tentative)
            assertEquals(event, tentative.border, theme.id)
            assertEquals(lerp(colors.bgSurface, event, 0.06f), tentative.container, theme.id)

            val cancelled = colors(colors, event, MonarchEventChipVariant.Bar, MonarchEventChipStatus.Cancelled)
            assertEquals(colors.bgMuted, cancelled.container, theme.id)
            assertEquals(colors.borderStrong, cancelled.marker, theme.id)
            assertEquals(colors.textMuted, cancelled.title, theme.id)

            val dot = colors(colors, Color.Unspecified, MonarchEventChipVariant.Dot, MonarchEventChipStatus.Confirmed)
            assertEquals(colors.accentSolid, dot.marker, theme.id)
            assertEquals(Color.Transparent, dot.container, theme.id)
            val pressed = MonarchEventChipTokens.colors(
                colors,
                event,
                MonarchEventChipVariant.Dot,
                MonarchEventChipStatus.Confirmed,
                past = false,
                pressed = true,
            )
            assertEquals(colors.bgMuted, pressed.container, theme.id)

            val past = MonarchEventChipTokens.colors(
                colors,
                event,
                MonarchEventChipVariant.Dot,
                MonarchEventChipStatus.Confirmed,
                past = true,
                pressed = false,
            )
            assertEquals(lerp(event, colors.bgSurface, 0.45f), past.marker, theme.id)
            assertEquals(colors.textMuted, past.title, theme.id)
        }
    }

    @Test
    fun onlyTentativeAndInactiveDotsAreHollow() {
        assertEquals(false, MonarchEventChipTokens.isHollow(MonarchEventChipStatus.Confirmed))
        assertEquals(true, MonarchEventChipTokens.isHollow(MonarchEventChipStatus.Tentative))
        assertEquals(true, MonarchEventChipTokens.isHollow(MonarchEventChipStatus.Cancelled))
        assertEquals(true, MonarchEventChipTokens.isHollow(MonarchEventChipStatus.Declined))
    }

    private fun colors(
        colors: MonarchColors,
        event: Color,
        variant: MonarchEventChipVariant,
        status: MonarchEventChipStatus,
    ) = MonarchEventChipTokens.colors(colors, event, variant, status, past = false, pressed = false)
}
