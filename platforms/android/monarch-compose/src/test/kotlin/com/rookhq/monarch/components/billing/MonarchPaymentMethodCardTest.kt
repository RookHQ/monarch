package com.rookhq.monarch.components.billing

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.rookhq.monarch.components.billing.paymentmethodcard.MonarchPaymentMethodCard
import com.rookhq.monarch.components.billing.paymentmethodcard.MonarchPaymentMethodCardAction
import com.rookhq.monarch.components.billing.paymentmethodcard.MonarchPaymentMethodCardExpiry
import com.rookhq.monarch.components.billing.paymentmethodcard.MonarchPaymentMethodCardLabels
import com.rookhq.monarch.components.billing.paymentmethodcard.MonarchPaymentMethodCardTokens
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.testing.stateDescription
import com.rookhq.monarch.theme.MonarchColors
import kotlinx.coroutines.CompletableDeferred
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** 14 November 2026, 12:00 UTC. */
private const val NOW = 1_794_657_600_000L

internal object MonarchPaymentMethodCardFixtures : ComponentFixtures {
    override val id = "payment-method-card"
    override val fixtures = listOf(
        fixture("default, valid") {
            MonarchPaymentMethodCard(
                brand = "visa",
                last4 = "4242",
                expiryMonth = 8,
                expiryYear = 29,
                holderName = "Asha Rao",
                now = NOW,
                default = true,
                onRemove = {},
            )
        },
        fixture("expiring with both actions") {
            MonarchPaymentMethodCard(
                brand = "mastercard",
                last4 = "5100",
                expiryMonth = 12,
                expiryYear = 2026,
                now = NOW,
                onMakeDefault = {},
                onRemove = {},
            )
        },
        fixture("expired, remove disabled") {
            MonarchPaymentMethodCard(
                brand = "amex",
                last4 = "0005",
                expiry = MonarchPaymentMethodCardExpiry.Expired,
                removeEnabled = false,
                onMakeDefault = {},
                onRemove = {},
            )
        },
        fixture("display only, unknown network") {
            MonarchPaymentMethodCard(brand = "Company card", last4 = "9999")
        },
        fixture("long holder name") {
            MonarchPaymentMethodCard(
                brand = "diners",
                last4 = "3056",
                expiryMonth = 1,
                expiryYear = 30,
                holderName = "Alexandra Konstantinopoulou-Montgomery",
                now = NOW,
                onMakeDefault = {},
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchPaymentMethodCardTest {
    @get:Rule
    val rule = createComposeRule()

    private val isHeading = SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)

    @Test
    fun theStatusFollowsTheExpiryMonthInUtc() {
        val status = { month: Int, year: Int -> MonarchPaymentMethodCardExpiry.status(month, year, NOW) }
        assertEquals(MonarchPaymentMethodCardExpiry.Expired, status(10, 26))
        assertEquals(MonarchPaymentMethodCardExpiry.Expiring, status(11, 26))
        assertEquals(MonarchPaymentMethodCardExpiry.Expiring, status(12, 2026))
        assertEquals(MonarchPaymentMethodCardExpiry.Valid, status(1, 27))
        assertEquals(MonarchPaymentMethodCardExpiry.Expiring, MonarchPaymentMethodCardExpiry.status(3, 27, NOW, 4))
        assertEquals(MonarchPaymentMethodCardExpiry.Expired, status(0, 26))
    }

    @Test
    fun theHeaderIsOneHeadingWithTheNameTheBadgesAndTheExpiry() {
        rule.setMonarchContent {
            MonarchPaymentMethodCard(
                brand = "visa",
                last4 = "4242",
                expiryMonth = 12,
                expiryYear = 26,
                holderName = "Asha Rao",
                now = NOW,
                default = true,
            )
        }
        rule.onNodeWithText("Visa ending in 4242")
            .assert(isHeading)
            .assert(hasText("Default"))
            .assert(hasText("Expires soon"))
            .assert(hasText("Asha Rao · Expires 12/26"))
    }

    @Test
    fun anExpiredCardSaysSoAndCannotBecomeTheDefault() {
        rule.setMonarchContent {
            MonarchPaymentMethodCard(
                brand = "visa",
                last4 = "4242",
                expiryMonth = 1,
                expiryYear = 25,
                now = NOW,
                onMakeDefault = {},
            )
        }
        rule.onNodeWithText("Visa ending in 4242").assert(hasText("Expired")).assert(hasText("Expired 01/25"))
        rule.onNodeWithContentDescription("Make default, Visa ending in 4242").assertIsNotEnabled()
    }

    @Test
    fun theDefaultCardHasNoMakeDefaultAndAnUnknownNetworkIsShownAsGiven() {
        rule.setMonarchContent {
            Column {
                MonarchPaymentMethodCard(brand = "visa", last4 = "4242", default = true, onMakeDefault = {})
                MonarchPaymentMethodCard(brand = "Company card", last4 = "9999")
            }
        }
        rule.onNodeWithContentDescription("Make default, Visa ending in 4242").assertDoesNotExist()
        rule.onNodeWithText("Company card ending in 9999").assertExists()
    }

    @Test
    fun makeDefaultShowsProgressAndBlocksRemoveUntilItEnds() {
        val done = CompletableDeferred<Unit>()
        var calls = 0
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent {
            MonarchPaymentMethodCard(
                brand = "visa",
                last4 = "4242",
                onMakeDefault = {
                    calls++
                    done.await()
                },
                onRemove = {},
            )
        }
        rule.mainClock.advanceTimeByFrame()
        val makeDefault = rule.onNodeWithContentDescription("Make default, Visa ending in 4242")
        makeDefault.assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.mainClock.advanceTimeByFrame()
        assertEquals(1, calls)
        assertEquals("Loading", makeDefault.stateDescription())
        makeDefault.performClick()
        rule.onNodeWithContentDescription("Remove, Visa ending in 4242").assertIsNotEnabled()
        done.complete(Unit)
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()
        assertEquals(1, calls)
        rule.onNodeWithContentDescription("Remove, Visa ending in 4242").assertIsEnabled()
    }

    @Test
    fun removeAsksFirstAndCancelDoesNothing() {
        var removed = 0
        rule.setMonarchContent {
            MonarchPaymentMethodCard(brand = "visa", last4 = "4242", onRemove = { removed++ })
        }
        val remove = rule.onNodeWithContentDescription("Remove, Visa ending in 4242")
        remove.performClick()
        rule.onNode(isDialog()).assertExists()
        rule.onNodeWithText("Remove Visa ending in 4242?").assertExists()
        rule.onNodeWithText("Cancel").performClick()
        rule.onNode(isDialog()).assertDoesNotExist()
        assertEquals(0, removed)
        remove.performClick()
        rule.onNodeWithText("Remove card").performClick()
        rule.waitForIdle()
        assertEquals(1, removed)
    }

    @Test
    fun aFailureIsAnnouncedAndTryAgainRepeatsWithoutAskingAgain() {
        var attempts = 0
        rule.setMonarchContent {
            MonarchPaymentMethodCard(
                brand = "visa",
                last4 = "4242",
                onRemove = {
                    attempts++
                    if (attempts == 1) error("declined")
                },
            )
        }
        rule.onNodeWithContentDescription("Remove, Visa ending in 4242").performClick()
        rule.onNodeWithText("Remove card").performClick()
        rule.waitForIdle()
        val polite = SemanticsMatcher.expectValue(
            SemanticsProperties.LiveRegion,
            LiveRegionMode.Polite,
        )
        rule.onNode(polite and hasText("That did not work. Try again.")).assertExists()
        rule.onNodeWithText("Try again").performClick()
        rule.waitForIdle()
        rule.onNode(isDialog()).assertDoesNotExist()
        assertEquals(2, attempts)
        rule.onNodeWithText("That did not work. Try again.").assertDoesNotExist()
    }

    @Test
    fun removeCanBeDisabled() {
        rule.setMonarchContent {
            MonarchPaymentMethodCard(brand = "visa", last4 = "4242", removeEnabled = false, onRemove = {})
        }
        rule.onNodeWithContentDescription("Remove, Visa ending in 4242").assertIsNotEnabled().performClick()
        rule.onNode(isDialog()).assertDoesNotExist()
    }

    @Test
    fun theLabelsReplaceTheBuiltInStrings() {
        rule.setMonarchContent {
            MonarchPaymentMethodCard(
                brand = "visa",
                last4 = "4242",
                default = true,
                onRemove = {},
                labels = MonarchPaymentMethodCardLabels(
                    name = { brand, last4 -> "$brand •••• $last4" },
                    defaultBadge = "Primary",
                    remove = "Delete",
                    actionLabel = { action, name -> "$action $name" },
                ),
            )
        }
        rule.onNodeWithText("Visa •••• 4242").assert(hasText("Primary"))
        rule.onNodeWithContentDescription("Delete Visa •••• 4242").assertExists()
    }

    @Test
    fun aLongHolderNameWrapsAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchPaymentMethodCard(
                brand = "visa",
                last4 = "4242",
                expiryMonth = 8,
                expiryYear = 29,
                holderName = "Alexandra Konstantinopoulou-Montgomery",
                now = NOW,
            )
        }
        rule.onNodeWithText("Alexandra", substring = true, useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun theRulesOfTheActions() {
        val t = MonarchPaymentMethodCardTokens
        val expired = MonarchPaymentMethodCardExpiry.Expired
        val valid = MonarchPaymentMethodCardExpiry.Valid
        assertFalse(t.makeDefaultEnabled(expired, null))
        assertTrue(t.makeDefaultEnabled(valid, null))
        assertFalse(t.makeDefaultEnabled(valid, MonarchPaymentMethodCardAction.Remove))
        assertFalse(t.removeEnabled(true, MonarchPaymentMethodCardAction.MakeDefault))
        assertTrue(t.removeEnabled(true, MonarchPaymentMethodCardAction.Remove))
        assertFalse(t.showsMakeDefault(hasAction = true, default = true))
        assertEquals("08/29", t.expiryDate(8, 2029))
        assertNull(t.expiryDate(8, null))
        assertEquals(expired, t.status(expired, 1, 40, NOW))
        assertEquals(valid, t.status(null, null, null, NOW))
    }

    @Test
    fun coloursComeFromTheRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val badge = MonarchPaymentMethodCardTokens.defaultBadgeColors(colors)
            assertEquals(colors.accentSubtle, badge.container, theme.id)
            assertEquals(colors.accentStrong, badge.content, theme.id)
            assertEquals(colors.bgMuted, MonarchPaymentMethodCardTokens.imagePlaceholder(colors).container, theme.id)
        }
    }
}
