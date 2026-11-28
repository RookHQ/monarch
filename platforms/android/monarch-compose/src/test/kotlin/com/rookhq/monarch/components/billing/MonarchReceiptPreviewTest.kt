package com.rookhq.monarch.components.billing

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.billing.receiptpreview.MonarchReceipt
import com.rookhq.monarch.components.billing.receiptpreview.MonarchReceiptPreview
import com.rookhq.monarch.components.billing.receiptpreview.MonarchReceiptPreviewLabels
import com.rookhq.monarch.components.billing.receiptpreview.MonarchReceiptPreviewTokens
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.TestIcon
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.testing.stateDescription
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.theme.MonarchTheme
import kotlinx.coroutines.CompletableDeferred
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals

private val jpeg = MonarchReceipt(name = "receipt.jpg", contentType = "image/jpeg")

internal object MonarchReceiptPreviewFixtures : ComponentFixtures {
    override val id = "receipt-preview"
    override val fixtures = MonarchButtonVariant.entries.map { variant ->
        fixture("${variant.name}, every size") {
            Column {
                MonarchButtonSize.entries.forEach { size ->
                    MonarchReceiptPreview(receipt = jpeg, variant = variant, size = size)
                }
            }
        }
    } + listOf(
        fixture("with an icon") {
            MonarchReceiptPreview(
                receipt = jpeg,
                icon = { MonarchIcon(imageVector = TestIcon, contentDescription = null) },
            )
        },
        fixture("no receipt") { MonarchReceiptPreview(receipt = null) },
        fixture("disabled") { MonarchReceiptPreview(receipt = jpeg, enabled = false) },
        fixture("loads when pressed") { MonarchReceiptPreview(load = { jpeg }) },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchReceiptPreviewTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theButtonOpensTheReceiptFullScreenAndCloseClosesIt() {
        rule.setMonarchContent {
            val picture = ColorPainter(MonarchTheme.colors.data1)
            MonarchReceiptPreview(receipt = MonarchReceipt("receipt.jpg", "image/jpeg", picture))
        }
        rule.onNode(hasText("View receipt") and hasClickAction())
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        rule.waitForIdle()
        rule.onNode(hasText("Receipt") and isHeading()).assertExists()
        rule.onNode((hasText("Close") or hasContentDescription("Close")) and hasClickAction()).performClick()
        rule.waitForIdle()
        rule.onNode(hasText("Receipt") and isHeading()).assertDoesNotExist()
    }

    @Test
    fun withoutAReceiptItIsPlainText() {
        rule.setMonarchContent { MonarchReceiptPreview(receipt = null) }
        rule.onNodeWithText("No receipt").assertExists()
        rule.onNode(hasClickAction()).assertDoesNotExist()
    }

    @Test
    fun theLoaderRunsOnlyWhenPressedAndShowsItsState() {
        var calls = 0
        val result = CompletableDeferred<MonarchReceipt>()
        rule.setMonarchContent {
            MonarchReceiptPreview(load = {
                calls++
                result.await()
            })
        }
        rule.waitForIdle()
        assertEquals(0, calls)
        rule.onNode(hasText("View receipt") and hasClickAction()).performClick()
        rule.waitForIdle()
        assertEquals(1, calls)
        assertEquals("Loading receipt", rule.onNode(hasText("View receipt") and hasClickAction()).stateDescription())
        result.complete(jpeg)
        rule.waitForIdle()
        rule.onNode(hasText("Receipt") and isHeading()).assertExists()
    }

    @Test
    fun aFailedLoadIsShownAndCanBeRetried() {
        var calls = 0
        val errors = mutableListOf<Throwable>()
        rule.setMonarchContent {
            MonarchReceiptPreview(
                load = {
                    calls++
                    error("offline")
                },
                onLoadError = { errors += it },
            )
        }
        rule.onNode(hasText("View receipt") and hasClickAction()).performClick()
        rule.waitForIdle()
        rule.onNodeWithText("The receipt could not be loaded.", substring = true).assertExists()
        assertEquals("offline", errors.single().message)
        rule.onNode(hasText("Try again") and hasClickAction()).performClick()
        rule.waitForIdle()
        assertEquals(2, calls)
    }

    @Test
    fun aDisabledButtonDoesNotLoad() {
        var calls = 0
        rule.setMonarchContent {
            MonarchReceiptPreview(load = {
                calls++
                jpeg
            }, enabled = false)
        }
        rule.onNode(hasText("View receipt") and hasClickAction()).assertIsNotEnabled().performClick()
        rule.waitForIdle()
        assertEquals(0, calls)
    }

    @Test
    fun theBuiltInStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchReceiptPreview(
                receipt = jpeg,
                labels = MonarchReceiptPreviewLabels(view = "Voir le reçu", title = "Reçu", close = "Fermer"),
            )
        }
        rule.onNode(hasText("Voir le reçu") and hasClickAction()).performClick()
        rule.waitForIdle()
        rule.onNode(hasText("Reçu") and isHeading()).assertExists()
        rule.onNode((hasText("Fermer") or hasContentDescription("Fermer")) and hasClickAction()).assertExists()
    }

    @Test
    fun theNoReceiptLabelCanBeReplaced() {
        rule.setMonarchContent {
            MonarchReceiptPreview(receipt = null, labels = MonarchReceiptPreviewLabels(none = "Aucun reçu"))
        }
        rule.onNodeWithText("Aucun reçu").assertExists()
    }

    @Test
    fun theLabelIsNotCutAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) { MonarchReceiptPreview(receipt = jpeg) }
        rule.onNodeWithText("View receipt", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun coloursComeFromTheThemeRoles() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.textMuted, MonarchReceiptPreviewTokens.noneColor(colors), theme.id)
            assertEquals(colors.bgCanvas, MonarchReceiptPreviewTokens.screenColor(colors), theme.id)
        }
    }
}
