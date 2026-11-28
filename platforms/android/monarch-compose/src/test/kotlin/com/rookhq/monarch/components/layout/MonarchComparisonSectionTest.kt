package com.rookhq.monarch.components.layout

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.billing.pricingtable.MonarchPricingFeatureValue
import com.rookhq.monarch.components.layout.comparisonsection.MonarchComparisonBestFor
import com.rookhq.monarch.components.layout.comparisonsection.MonarchComparisonColumn
import com.rookhq.monarch.components.layout.comparisonsection.MonarchComparisonRow
import com.rookhq.monarch.components.layout.comparisonsection.MonarchComparisonSection
import com.rookhq.monarch.components.layout.comparisonsection.MonarchComparisonSectionTokens
import com.rookhq.monarch.components.layout.marketingsection.MonarchMarketingAlign
import com.rookhq.monarch.components.layout.marketingsection.MonarchMarketingSectionTone
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.MonarchTestEnvironment
import com.rookhq.monarch.testing.assertIsBefore
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals

private val Columns = listOf(
    MonarchComparisonColumn("product", "Product A", highlighted = true, badge = "This product"),
    MonarchComparisonColumn("sheets", "Spreadsheets"),
)
private val Rows = listOf(
    MonarchComparisonRow(
        "history",
        "Change history",
        mapOf(
            "product" to MonarchPricingFeatureValue.Included,
            "sheets" to MonarchPricingFeatureValue.Text("Per file"),
        ),
    ),
    MonarchComparisonRow(
        "export",
        "Export to CSV",
        mapOf("product" to MonarchPricingFeatureValue.Included),
        group = "Access",
    ),
)
private val BestFor = listOf(
    MonarchComparisonBestFor("product", "Product A suits", "Teams that share and hand over work every day.", true),
    MonarchComparisonBestFor("sheets", "Spreadsheets suit", "One-off lists kept by one person."),
)

internal object MonarchComparisonSectionFixtures : ComponentFixtures {
    override val id = "comparison-section"
    override val fixtures = listOf(
        fixture("table with header", tall = true) {
            MonarchComparisonSection(Columns, Rows, title = "How it compares", eyebrow = "Compare")
        },
        fixture("best for, note and action", tall = true) {
            MonarchComparisonSection(
                Columns.take(1),
                Rows.take(1),
                bestFor = BestFor,
                note = "Checked against public sources.",
            ) {
                MonarchButton("Try Product A", {}, variant = MonarchButtonVariant.Primary)
            }
        },
        fixture("centred, subtle", tall = true) {
            MonarchComparisonSection(
                Columns,
                Rows.take(1),
                title = "Before and after",
                align = MonarchMarketingAlign.Center,
                tone = MonarchMarketingSectionTone.Subtle,
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchComparisonSectionTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun cellsAreReadAsOptionCapabilityAndValue() {
        rule.setMonarchContent { MonarchComparisonSection(Columns, Rows, title = "How it compares") }
        rule.onNodeWithText("How it compares").assert(isHeading())
        rule.onNodeWithContentDescription("How it compares").assertExists()
        rule.onNode(hasContentDescription("Spreadsheets, Change history: Per file")).assertExists()
        rule.onNodeWithText("Access").assert(isHeading())
    }

    @Test
    fun theCaptionCanBeReplaced() {
        rule.setMonarchContent { MonarchComparisonSection(
            Columns,
            Rows,
            title = "How it compares",
            caption = "Comparación",
        ) }
        rule.onNodeWithContentDescription("Comparación").assertExists()
    }

    @Test
    fun aBestForCardIsOneElementWithAHeading() {
        rule.setMonarchContent { MonarchComparisonSection(Columns, Rows, bestFor = BestFor) }
        rule.onNodeWithText("Product A suits", substring = true)
            .assert(hasText("Teams that share and hand over work every day.", substring = true))
    }

    @Test
    fun actionsCallBack() {
        var tried = false
        rule.setMonarchContent { MonarchComparisonSection(
            Columns,
            Rows,
        ) { MonarchButton("Try Product A", { tried = true }) } }
        rule.onNodeWithText("Try Product A").performClick()
        assertEquals(true, tried)
    }

    @Test
    fun bestForCardsFollowTheLayoutDirection() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) {
            MonarchComparisonSection(Columns, Rows, Modifier.requiredWidth(400.dp), bestFor = BestFor)
        }
        listOf(LayoutDirection.Ltr, LayoutDirection.Rtl).forEach { direction ->
            environment.layoutDirection = direction
            rule.waitForIdle()
            rule.onNodeWithText("Product A suits", useUnmergedTree = true)
                .assertIsBefore(rule.onNodeWithText("Spreadsheets suit", useUnmergedTree = true), direction)
        }
    }

    @Test
    fun theNoteWrapsAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                MonarchComparisonSection(Columns, Rows, note = "Sample note: say when the comparison was checked.")
            }
        }
        rule.onNodeWithText("Sample note: say when the comparison was checked.").assertTextFullyVisible()
    }

    @Test
    fun coloursComeFromTheThemeRoles() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.accentSubtle, MonarchComparisonSectionTokens.cardFill(true, colors), theme.id)
            assertEquals(colors.bgSurface, MonarchComparisonSectionTokens.cardFill(false, colors), theme.id)
            assertEquals(colors.accentBorder, MonarchComparisonSectionTokens.cardBorder(true, colors), theme.id)
            assertEquals(colors.borderDefault, MonarchComparisonSectionTokens.cardBorder(false, colors), theme.id)
            assertEquals(colors.bgSubtle, MonarchComparisonSectionTokens.noteFill(colors), theme.id)
        }
    }
}
