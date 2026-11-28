package dev.monarchui.components.datadisplay

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.components.datadisplay.widget.MonarchWidget
import dev.monarchui.components.datadisplay.widget.MonarchWidgetLabels
import dev.monarchui.components.datadisplay.widget.MonarchWidgetState
import dev.monarchui.components.datadisplay.widget.MonarchWidgetTokens
import dev.monarchui.components.datadisplay.widget.MonarchWidgetVariant
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.MonarchTestEnvironment
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val Now = 1_794_657_600_000L
private const val FiveMinutes = 5 * 60_000L

internal object MonarchWidgetFixtures : ComponentFixtures {
    override val id = "widget"
    override val fixtures = listOf(
        fixture("ready with header controls and footer") {
            MonarchWidget(
                title = "Revenue",
                period = "Last 30 days",
                description = "Gross, before refunds",
                updatedAt = Now - FiveMinutes,
                now = Now,
                onViewAll = {},
                menu = { close -> DropdownMenuItem(text = { Text("Export") }, onClick = close) },
            ) { Text("128,400") }
        },
        fixture("icon menu and plain") {
            MonarchWidget(
                title = "Signups",
                variant = MonarchWidgetVariant.Plain,
                icon = { Icon(TestIcon, contentDescription = null) },
                menuIcon = { Icon(TestIcon, contentDescription = null) },
                menu = { close -> DropdownMenuItem(text = { Text("Hide") }, onClick = close) },
            ) { Text("42") }
        },
        fixture("loading") { MonarchWidget(title = "Revenue", state = MonarchWidgetState.Loading, footer = "x") {} },
        fixture("empty") { MonarchWidget(title = "Signups", state = MonarchWidgetState.Empty()) {} },
        fixture("failed with and without retry") {
            Column {
                MonarchWidget(title = "Churn", state = MonarchWidgetState.Failed(), onRetry = {}) {}
                MonarchWidget(title = "Churn", state = MonarchWidgetState.Failed("Churn is not available.")) {}
            }
        },
        fixture("retrying") {
            MonarchWidget(title = "Churn", state = MonarchWidgetState.Failed(), retrying = true, onRetry = {}) {}
        },
        fixture("long title") {
            MonarchWidget(
                title = "Median time to first response across every queue",
                period = "This quarter",
                onViewAll = {},
            ) { Text("4h 12m") }
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchWidgetTest {
    @get:Rule
    val rule = createComposeRule()

    private val isHeading = SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)
    private val polite = SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)

    @Test
    fun theTitleIsAHeadingAndTheContentIsShownWhenReady() {
        rule.setMonarchContent {
            MonarchWidget(title = "Revenue", period = "Last 30 days", description = "Gross") { Text("128,400") }
        }
        rule.onNodeWithText("Revenue").assert(isHeading)
        rule.onNodeWithText("Last 30 days").assertExists()
        rule.onNodeWithText("Gross").assertExists()
        rule.onNodeWithText("128,400").assertExists()
    }

    @Test
    fun viewAllIsNamedAfterTheWidgetAndCallsBack() {
        var opened = 0
        rule.setMonarchContent { MonarchWidget(title = "Revenue", onViewAll = { opened++ }) {} }
        rule.onNodeWithContentDescription("View all: Revenue")
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        assertEquals(1, opened)
    }

    @Test
    fun theMenuOpensFromATextButtonOrAnIconAndItsItemsCloseIt() {
        var exported = 0
        rule.setMonarchContent {
            Column {
                MonarchWidget(
                    title = "Revenue",
                    menu = { close ->
                        DropdownMenuItem(
                            text = { Text("Export") },
                            onClick = {
                                exported++
                                close()
                            },
                        )
                    },
                ) {}
                MonarchWidget(
                    title = "Signups",
                    menuIcon = { Icon(TestIcon, contentDescription = null) },
                    menu = { close -> DropdownMenuItem(text = { Text("Hide") }, onClick = close) },
                ) {}
            }
        }
        rule.onNodeWithContentDescription("More actions for Revenue").performClick()
        rule.onNodeWithText("Export").performClick()
        assertEquals(1, exported)
        rule.onNodeWithText("Export").assertDoesNotExist()
        rule.onNodeWithContentDescription("More actions for Signups")
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        rule.onNodeWithText("Hide").assertExists()
    }

    @Test
    fun loadingIsOneElementNamedAfterTheWidgetAndHidesTheFooter() {
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent {
            MonarchWidget(
                title = "Revenue",
                state = MonarchWidgetState.Loading,
                footer = "Gross",
                updatedAt = Now,
                now = Now,
            ) { Text("128,400") }
        }
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithContentDescription("Loading Revenue").assertExists()
        rule.onNodeWithText("128,400").assertDoesNotExist()
        rule.onNodeWithText("Gross").assertDoesNotExist()
    }

    @Test
    fun emptyShowsTheBuiltInOrTheGivenMessage() {
        rule.setMonarchContent {
            Column {
                MonarchWidget(title = "Signups", state = MonarchWidgetState.Empty()) {}
                MonarchWidget(title = "Churn", state = MonarchWidgetState.Empty("No churn this month")) {}
            }
        }
        rule.onNodeWithText("Nothing to show yet").assertExists()
        rule.onNodeWithText("No churn this month").assertExists()
    }

    @Test
    fun aFailureIsAnnouncedAndOffersTryAgainWhenItCanRetry() {
        var retries = 0
        rule.setMonarchContent {
            Column {
                MonarchWidget(title = "Churn", state = MonarchWidgetState.Failed(), onRetry = { retries++ }) {}
                MonarchWidget(title = "Seats", state = MonarchWidgetState.Failed("Seats are not available.")) {}
            }
        }
        rule.onNode(polite and hasText("This section could not be loaded.")).assertExists()
        rule.onNodeWithText("Try again").performClick()
        assertEquals(1, retries)
        rule.onNode(polite and hasText("Seats are not available.", substring = true)).assertExists()
    }

    @Test
    fun theFooterSaysWhenTheDataWasUpdated() {
        rule.setMonarchContent {
            MonarchWidget(title = "Revenue", footer = "Gross", updatedAt = Now - FiveMinutes, now = Now) {}
        }
        rule.onNodeWithText("Gross").assertExists()
        rule.onNodeWithText("Updated 5 minutes ago").assertExists()
    }

    @Test
    fun atTheLargestFontSizeTheControlsMoveUnderTheTitle() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchWidget(title = "Median time to first response", onViewAll = {}) {}
        }
        val title = rule.onNodeWithText("Median time to first response").assertTextFullyVisible()
        val viewAll = rule.onNodeWithContentDescription("View all: Median time to first response")
        assertTrue(viewAll.getUnclippedBoundsInRoot().top >= title.getUnclippedBoundsInRoot().bottom)
    }

    @Test
    fun theTitleComesBeforeTheControlsInBothDirections() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) { MonarchWidget(title = "Revenue", onViewAll = {}) {} }
        LayoutDirection.entries.forEach { direction ->
            environment.layoutDirection = direction
            rule.waitForIdle()
            val viewAll = rule.onNodeWithText("View all", useUnmergedTree = true)
            rule.onNodeWithText("Revenue").assertIsBefore(viewAll, direction)
        }
    }

    @Test
    fun theLabelsReplaceTheBuiltInStrings() {
        rule.setMonarchContent {
            Column {
                MonarchWidget(
                    title = "Revenue",
                    onViewAll = {},
                    updatedAt = Now - FiveMinutes,
                    now = Now,
                    labels = MonarchWidgetLabels(
                        viewAll = "See everything",
                        viewAllLabel = { "Open $it" },
                        updated = { "Refreshed $it" },
                        more = "Options",
                        menu = { "Options for $it" },
                    ),
                    menu = {},
                ) {}
                MonarchWidget(
                    title = "Churn",
                    state = MonarchWidgetState.Failed(),
                    labels = MonarchWidgetLabels(error = "Down"),
                ) {}
            }
        }
        rule.onNodeWithText("See everything").assertExists()
        rule.onNodeWithContentDescription("Open Revenue").assertExists()
        rule.onNodeWithText("Refreshed 5 minutes ago").assertExists()
        rule.onNodeWithContentDescription("Options for Revenue").assertExists()
        rule.onNodeWithText("Down", substring = true).assertExists()
    }

    @Test
    fun theRulesOfTheFrame() {
        val t = MonarchWidgetTokens
        assertFalse(t.showsFooter(MonarchWidgetState.Loading, Now, "x"))
        assertFalse(t.showsFooter(MonarchWidgetState.Ready, null, ""))
        assertTrue(t.showsFooter(MonarchWidgetState.Empty(), Now, null))
        assertTrue(t.stackedHeader(LargestFontScale))
        assertFalse(t.stackedHeader(1f))
        assertEquals("5 minutes ago", t.relativeTime(Now - FiveMinutes, Now))
    }

    @Test
    fun coloursComeFromTheRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.textDefault, MonarchWidgetTokens.titleColor(colors), theme.id)
            assertEquals(colors.textMuted, MonarchWidgetTokens.mutedColor(colors), theme.id)
        }
    }
}
