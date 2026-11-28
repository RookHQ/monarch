package dev.monarchui.components.typography

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import dev.monarchui.components.typography.tabularnumberstyle.MonarchTabularNumber
import dev.monarchui.components.typography.tabularnumberstyle.MonarchTabularNumberStyle
import dev.monarchui.components.typography.tabularnumberstyle.MonarchTabularNumberTokens
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.testing.textLayout
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal object MonarchTabularNumberStyleFixtures : ComponentFixtures {
    override val id = "tabular-number"
    override val fixtures = listOf(
        fixture("a column of figures with a missing value") {
            Column(horizontalAlignment = Alignment.End) {
                MonarchTabularNumber(value = 1111.11)
                MonarchTabularNumber(value = null)
                MonarchTabularNumber(value = 9048.5)
            }
        },
        fixture("percent and formatted text") {
            Column {
                MonarchTabularNumber(value = 0.125, format = NumberFormat.getPercentInstance(Locale.US))
                MonarchTabularNumber(text = "12:04", style = MonarchTextStyle.Title)
            }
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchTabularNumberStyleTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun aNumberIsFormattedWithTheFormatGiven() {
        val format = NumberFormat.getNumberInstance(Locale.US).apply { minimumFractionDigits = 2 }
        rule.setMonarchContent { MonarchTabularNumber(value = 1234.5, format = format) }
        rule.onNodeWithText("1,234.50").assertExists()
        assertEquals("1.234,5", MonarchTabularNumberTokens.format(1234.5, NumberFormat.getInstance(Locale.GERMANY)))
        assertEquals("12", MonarchTabularNumberTokens.format(BigDecimal("12"), NumberFormat.getInstance(Locale.US)))
    }

    @Test
    fun aMissingValueIsADashReadAsNoValue() {
        val format = NumberFormat.getInstance(Locale.US)
        assertNull(MonarchTabularNumberTokens.format(null, format))
        assertNull(MonarchTabularNumberTokens.format(Double.NaN, format))
        assertNull(MonarchTabularNumberTokens.format(Float.POSITIVE_INFINITY, format))
        rule.setMonarchContent {
            Column {
                MonarchTabularNumber(value = null, modifier = Modifier.testTag("default"))
                MonarchTabularNumber(
                    value = Double.NaN,
                    modifier = Modifier.testTag("custom"),
                    emptyText = "n/a",
                    emptyLabel = "Not reported",
                )
            }
        }
        rule.onNodeWithTag("default").assertContentDescriptionEquals("No value")
        rule.onNodeWithText("—", useUnmergedTree = true).assertExists()
        rule.onNodeWithTag("custom").assertContentDescriptionEquals("Not reported")
        rule.onNodeWithText("n/a", useUnmergedTree = true).assertExists()
    }

    @Test
    fun theTextHasTabularDigitsInItsRole() {
        rule.setMonarchContent { MonarchTabularNumber(text = "12:04", style = MonarchTextStyle.Title) }
        val style = rule.onNodeWithText("12:04").textLayout().layoutInput.style
        assertEquals("tnum", style.fontFeatureSettings)
        assertEquals(FontWeight.SemiBold, style.fontWeight)
    }

    @Test
    fun theStyleAddsTabularDigits() {
        val style = MonarchTabularNumberStyle.applyTo(TextStyle(fontWeight = FontWeight.Bold))
        assertEquals("tnum", style.fontFeatureSettings)
        assertEquals(FontWeight.Bold, style.fontWeight)
        assertEquals("tnum", MonarchTabularNumberStyle.spanStyle().fontFeatureSettings)
    }

    @Test
    fun aLongNumberWrapsAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchTabularNumber(value = 1234567890.12, format = NumberFormat.getInstance(Locale.US))
        }
        rule.onNodeWithText("1,234,567,890.12").assertTextFullyVisible()
    }
}
