package dev.monarchui.components.identity

import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.components.identity.cookieconsentbanner.MonarchCookieConsentBanner
import dev.monarchui.components.identity.cookieconsentbanner.MonarchCookieConsentBannerLabels
import dev.monarchui.components.identity.cookieconsentbanner.MonarchCookieConsentBannerTokens
import dev.monarchui.components.identity.cookieconsentbanner.MonarchCookieConsentChoice
import dev.monarchui.components.identity.cookieconsentbanner.MonarchCookieConsentRegime
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.MonarchTestEnvironment
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

internal object MonarchCookieConsentBannerFixtures : ComponentFixtures {
    override val id = "cookie-consent-banner"
    override val fixtures = listOf(
        fixture("opt-in with customize and policy", tall = true) {
            MonarchCookieConsentBanner(
                MonarchCookieConsentRegime.OptIn,
                onDecide = {},
                onCustomize = {},
                onPolicyClick = {},
            )
        },
        fixture("opt-out", tall = true) {
            MonarchCookieConsentBanner(
                MonarchCookieConsentRegime.OptOut,
                onDecide = {},
                labels = MonarchCookieConsentBannerLabels(title = "Your privacy choices"),
            )
        },
        fixture("notice, announced", tall = true) {
            MonarchCookieConsentBanner(MonarchCookieConsentRegime.Notice, onDecide = {}, announce = true)
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchCookieConsentBannerTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun optInOffersCustomizeRejectAndAccept() {
        val choices = mutableListOf<MonarchCookieConsentChoice>()
        var customized = 0
        rule.setMonarchContent {
            MonarchCookieConsentBanner(
                MonarchCookieConsentRegime.OptIn,
                { choices += it },
                onCustomize = { customized++ },
            )
        }
        rule.onNodeWithText("Cookies on this site").assert(isHeading())
        rule.onNode(SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, "Cookies on this site")).assertExists()
        rule.onNodeWithText("Customize").assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.onNodeWithText("Reject all").assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.onNodeWithText("Accept all").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(1, customized)
        assertEquals(listOf(MonarchCookieConsentChoice.RejectAll, MonarchCookieConsentChoice.AcceptAll), choices)
    }

    @Test
    fun optOutAndNoticeOfferTheirChoices() {
        assertEquals(
            listOf(MonarchCookieConsentChoice.DoNotSell, MonarchCookieConsentChoice.Acknowledge),
            MonarchCookieConsentBannerTokens.choices(MonarchCookieConsentRegime.OptOut),
        )
        assertEquals(
            listOf(MonarchCookieConsentChoice.Acknowledge),
            MonarchCookieConsentBannerTokens.choices(MonarchCookieConsentRegime.Notice),
        )
        var choice: MonarchCookieConsentChoice? = null
        rule.setMonarchContent { MonarchCookieConsentBanner(MonarchCookieConsentRegime.OptOut, { choice = it }) }
        rule.onNodeWithText("Do not sell or share my personal information").performClick()
        assertEquals(MonarchCookieConsentChoice.DoNotSell, choice)
        rule.onNodeWithText("Customize").assertDoesNotExist()
    }

    @Test
    fun theNoticeTextAndLabelsCanBeReplacedAndAnnounced() {
        rule.setMonarchContent {
            MonarchCookieConsentBanner(
                MonarchCookieConsentRegime.Notice,
                {},
                announce = true,
                labels = MonarchCookieConsentBannerLabels(title = "About cookies", acknowledge = "Entendido"),
            )
        }
        rule.onNodeWithText("About cookies").assertExists()
        rule.onNodeWithText("Entendido").assertExists()
        rule.onNodeWithText("We use cookies to make this site work and to understand how it is used.").assertExists()
        rule.onNode(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)).assertExists()
    }

    @Test
    fun thePolicyLinkIsPartOfTheExplanation() {
        rule.setMonarchContent { MonarchCookieConsentBanner(MonarchCookieConsentRegime.OptIn, {}, onPolicyClick = {}) }
        rule.onNodeWithText("Cookie policy", substring = true).assertExists()
    }

    @Test
    fun rejectComesBeforeAcceptInBothDirections() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) { MonarchCookieConsentBanner(MonarchCookieConsentRegime.OptIn, {}) }
        listOf(LayoutDirection.Ltr, LayoutDirection.Rtl).forEach { direction ->
            environment.layoutDirection = direction
            rule.waitForIdle()
            rule.onNodeWithText("Reject all").assertIsBefore(rule.onNodeWithText("Accept all"), direction)
        }
    }

    @Test
    fun theExplanationWrapsAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) { MonarchCookieConsentBanner(
            MonarchCookieConsentRegime.Notice,
            {},
        ) }
        rule.onNodeWithText("We use cookies to make this site work and to understand how it is used.")
            .assertTextFullyVisible()
    }

    @Test
    fun coloursComeFromTheThemeRoles() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.bgRaised, MonarchCookieConsentBannerTokens.fill(colors), theme.id)
            assertEquals(colors.textMuted, MonarchCookieConsentBannerTokens.description(colors), theme.id)
            assertEquals(colors.textLink, MonarchCookieConsentBannerTokens.link(colors), theme.id)
        }
    }
}
