package dev.monarchui.components.billing

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.billing.plancard.MonarchPlanCard
import dev.monarchui.components.billing.plancard.MonarchPlanCardFeature
import dev.monarchui.components.billing.plancard.MonarchPlanCardTokens
import dev.monarchui.components.datadisplay.badge.MonarchBadgeVariant
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.foundation.icon.MonarchIconSize
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

private val TeamFeatures = listOf(
    MonarchPlanCardFeature("Unlimited projects"),
    MonarchPlanCardFeature("Seats", hint = "up to 10"),
    MonarchPlanCardFeature("Single sign-on", included = false),
)

internal object MonarchPlanCardFixtures : ComponentFixtures {
    override val id = "plan-card"
    override val fixtures = listOf(
        fixture("highlighted with every part") {
            MonarchPlanCard(
                name = "Team",
                price = "$12",
                period = "per seat / month",
                priceNote = "Billed yearly",
                description = "For teams that ship together.",
                featuresLabel = "Everything in Starter, plus",
                features = TeamFeatures,
                badge = "Recommended",
                footer = "Taxes extra.",
                highlighted = true,
                includedIcon = {
                    MonarchIcon(imageVector = TestIcon, contentDescription = null, size = MonarchIconSize.Sm)
                },
            ) {
                MonarchButton(text = "Choose Team", onClick = {}, modifier = Modifier.fillMaxWidth())
            }
        },
        fixture("current plan, no icon") {
            MonarchPlanCard(
                name = "Starter",
                price = "$0",
                period = "per month",
                features = TeamFeatures,
                current = true,
            )
        },
        fixture("name only") { MonarchPlanCard(name = "Enterprise") },
        fixture("price without a period") {
            MonarchPlanCard(name = "Enterprise", price = "Contact us", badge = "Custom")
        },
        fixture("long name and badges in a narrow width") {
            MonarchPlanCard(
                name = "Professional services and support",
                modifier = Modifier.width(220.dp),
                badge = "Recommended",
                current = true,
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchPlanCardTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theNameIsAHeadingAndThePriceIsOneElement() {
        rule.setMonarchContent { MonarchPlanCard(name = "Team", price = "$12", period = "per seat / month") }
        rule.onNodeWithText("Team").assert(isHeading())
        rule.onNodeWithText("$12").assert(hasText("per seat / month"))
    }

    @Test
    fun aFeatureThatIsNotIncludedIsReadWithItsPrefix() {
        rule.setMonarchContent { MonarchPlanCard(name = "Team", features = TeamFeatures) }
        rule.onNodeWithText("Single sign-on").assert(hasText("Not included:"))
        rule.onNodeWithText("Seats", substring = true).assert(hasText("up to 10", substring = true))
        rule.onNodeWithText("Unlimited projects").assert(!hasText("Not included:"))
    }

    @Test
    fun theCurrentBadgeAndThePrefixCanBeReplaced() {
        rule.setMonarchContent {
            MonarchPlanCard(
                name = "Team",
                features = TeamFeatures,
                current = true,
                currentPlanLabel = "Votre offre",
                notIncludedLabel = "Non inclus :",
            )
        }
        rule.onNodeWithText("Votre offre").assertExists()
        rule.onNodeWithText("Single sign-on").assert(hasText("Non inclus :"))
    }

    @Test
    fun theDefaultCurrentBadgeIsShown() {
        rule.setMonarchContent { MonarchPlanCard(name = "Starter", current = true) }
        rule.onNodeWithText("Current plan").assertExists()
    }

    @Test
    fun theActionStaysItsOwnButton() {
        var chosen = 0
        rule.setMonarchContent {
            MonarchPlanCard(name = "Team", price = "$12") {
                MonarchButton(text = "Choose Team", onClick = { chosen++ }, variant = MonarchButtonVariant.Primary)
            }
        }
        rule.onNodeWithText("Choose Team").assertHasClickAction().assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        assertEquals(1, chosen)
    }

    @Test
    fun theBadgeFollowsTheNameInBothLayoutDirections() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) { Box { MonarchPlanCard(name = "Team", badge = "Recommended") } }
        listOf(LayoutDirection.Ltr, LayoutDirection.Rtl).forEach { direction ->
            environment.layoutDirection = direction
            rule.waitForIdle()
            rule.onNodeWithText("Team").assertIsBefore(rule.onNodeWithText("Recommended"), direction)
        }
    }

    @Test
    fun longTextWrapsAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchPlanCard(
                name = "Professional",
                modifier = Modifier.width(240.dp),
                description = "For agencies that run many client projects.",
                features = TeamFeatures,
            )
        }
        rule.onNodeWithText("Professional", useUnmergedTree = true).assertTextFullyVisible()
        rule.onNodeWithText("For agencies that run many client projects.", useUnmergedTree = true)
            .assertTextFullyVisible()
        rule.onNodeWithText("Unlimited projects", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun highlightingIsABorderAndASolidBadgeFromTheThemeRoles() {
        assertEquals(MonarchBadgeVariant.Subtle, MonarchPlanCardTokens.BadgeVariant)
        assertEquals(MonarchTone.Accent, MonarchPlanCardTokens.badgeTone(highlighted = true))
        assertEquals(MonarchTone.Neutral, MonarchPlanCardTokens.badgeTone(highlighted = false))
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.accentBorder, MonarchPlanCardTokens.highlightBorder(colors), theme.id)
            assertEquals(colors.textDefault, MonarchPlanCardTokens.featureColor(true, colors), theme.id)
            assertEquals(colors.textMuted, MonarchPlanCardTokens.featureColor(false, colors), theme.id)
        }
    }
}
