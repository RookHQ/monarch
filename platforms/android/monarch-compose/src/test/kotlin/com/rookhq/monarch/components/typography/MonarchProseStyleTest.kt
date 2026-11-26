package com.rookhq.monarch.components.typography

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.rookhq.monarch.components.typography.prosestyle.MonarchProse
import com.rookhq.monarch.components.typography.prosestyle.MonarchProseSize
import com.rookhq.monarch.components.typography.prosestyle.MonarchProseStyle
import com.rookhq.monarch.components.typography.prosestyle.MonarchProseStyleTokens
import com.rookhq.monarch.components.typography.prosestyle.ProseBlock
import com.rookhq.monarch.foundation.MonarchTypography
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.assertIsBefore
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal object MonarchProseStyleFixtures : ComponentFixtures {
    override val id = "prose"
    override val fixtures = listOf(
        fixture("headings and paragraphs, md") {
            MonarchProse {
                heading("Release notes", level = 1)
                paragraph("Invoices now show the tax number.")
                heading("Fixed", level = 6)
                paragraph("Search finds archived projects.")
            }
        },
        fixture("lists, quote, sm") {
            MonarchProse(style = MonarchProseStyle(MonarchProseSize.Sm)) {
                listItem("Faster search")
                listItem("Nested", depth = 1)
                listItem("Restart the app", marker = "1.")
                quote(AnnotatedString("Faster than I expected."))
            }
        },
        fixture("code, rule, caption, lg") {
            MonarchProse(style = MonarchProseStyle(MonarchProseSize.Lg, limitLineLength = false)) {
                code("rook sync --workspace acme --since 2026-01-01 --verbose")
                rule()
                caption("Version 4.2")
            }
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchProseStyleTest {
    @get:Rule
    val rule = createComposeRule()

    private val isHeading = SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)

    @Test
    fun headingsHaveHeadingSemanticsAndParagraphsDoNot() {
        rule.setMonarchContent {
            MonarchProse {
                heading("Release notes", level = 1)
                paragraph("Invoices now show the tax number.")
            }
        }
        rule.onNodeWithText("Release notes").assert(isHeading)
        rule.onNodeWithText("Invoices now show the tax number.").assert(!isHeading)
    }

    @Test
    fun aBulletIsHiddenAndANumberIsReadWithItsItem() {
        rule.setMonarchContent {
            MonarchProse {
                listItem("Faster search")
                listItem("Restart the app", marker = "2.")
            }
        }
        rule.onAllNodes(hasText("•")).assertCountEquals(0)
        rule.onNodeWithText("Restart the app", substring = true).assert(hasText("2."))
    }

    @Test
    fun blocksAreSpacedWithTheWebRhythm() {
        val tokens = MonarchProseStyleTokens
        val heading = ProseBlock.Heading(AnnotatedString("H"), 2)
        val paragraph = ProseBlock.Paragraph(AnnotatedString("P"))
        val item = ProseBlock.ListItem(AnnotatedString("I"), null, 0)
        assertEquals(0f, tokens.spaceBefore(heading, null))
        assertEquals(0.5f, tokens.spaceBefore(paragraph, heading))
        assertEquals(1.75f, tokens.spaceBefore(heading, paragraph))
        assertEquals(1.25f, tokens.spaceBefore(paragraph, paragraph))
        assertEquals(0.375f, tokens.spaceBefore(item, item))
        assertEquals(1.25f, tokens.spaceBefore(paragraph, item))
    }

    @Test
    fun headingSizesAreMultiplesOfTheBaseAndLevelsAreClamped() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val typography = MonarchTypography.Default
            val tokens = MonarchProseStyleTokens
            val base = MonarchTokens.FontSize.base
            assertEquals((base * 1.875f).sp, tokens.heading(1, MonarchProseSize.Md, typography, colors).fontSize)
            assertEquals((base * 1.5f).sp, tokens.heading(2, MonarchProseSize.Md, typography, colors).fontSize)
            assertEquals(base.sp, tokens.heading(9, MonarchProseSize.Md, typography, colors).fontSize)
            assertEquals(colors.textMuted, tokens.heading(6, MonarchProseSize.Md, typography, colors).color)
            assertEquals(colors.textDefault, tokens.heading(0, MonarchProseSize.Md, typography, colors).color)
            assertEquals(MonarchTokens.FontSize.sm.sp, tokens.body(MonarchProseSize.Sm, typography, colors).fontSize)
            val lg = tokens.body(MonarchProseSize.Lg, typography, colors)
            assertEquals(MonarchTokens.LineHeight.relaxed.em, lg.lineHeight)
            assertEquals(colors.textMuted, tokens.caption(MonarchProseSize.Md, typography, colors).color)
            assertEquals(typography.code.fontFamily, tokens.code(MonarchProseSize.Md, typography, colors).fontFamily)
        }
    }

    @Test
    fun decorationsComeFromTheTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val decorations = MonarchProseStyleTokens.decorations(colors)
            assertEquals(colors.borderStrong, decorations.quoteBar)
            assertEquals(colors.textMuted, decorations.marker)
            assertEquals(colors.bgSubtle, decorations.codeBackground)
            assertEquals(colors.borderSubtle, decorations.codeBorder)
            assertEquals(colors.borderSubtle, decorations.rule)
        }
    }

    @Test
    fun theColumnKeepsToTheReadingMeasureUnlessTurnedOff() {
        val text = "Invoices now show the customer's tax number on every page, and exports keep the column " +
            "order you chose, the time zone of the workspace and the currency of each client."
        rule.setMonarchContent {
            Column(Modifier.requiredWidth(1000.dp)) {
                MonarchProse(style = MonarchProseStyle(MonarchProseSize.Sm)) { paragraph(text) }
                MonarchProse(style = MonarchProseStyle(MonarchProseSize.Sm, limitLineLength = false)) {
                    paragraph(text.uppercase())
                }
            }
        }
        val measure = (MonarchTokens.FontSize.sm * MonarchProseStyleTokens.MEASURE_EM).dp
        val limited = rule.onNodeWithText(text).getUnclippedBoundsInRoot()
        val free = rule.onNodeWithText(text.uppercase()).getUnclippedBoundsInRoot()
        assertTrue(limited.right - limited.left <= measure)
        assertTrue(free.right - free.left > measure)
    }

    @Test
    fun theStyleExposesItsTextStylesAndLinkStyles() {
        var body: TextStyle? = null
        var heading: TextStyle? = null
        var hasLinkStyle = false
        rule.setMonarchContent {
            val style = MonarchProseStyle(MonarchProseSize.Lg)
            body = style.bodyStyle()
            heading = style.headingStyle(3)
            hasLinkStyle = style.linkStyles().style != null
        }
        assertEquals(MonarchTokens.FontSize.lg.sp, body?.fontSize)
        assertEquals((MonarchTokens.FontSize.lg * 1.25f).sp, heading?.fontSize)
        assertTrue(hasLinkStyle)
        assertEquals(MonarchProseStyle(), MonarchProseStyle(MonarchProseSize.Md, true))
    }

    @Test
    fun theMarkerComesBeforeTheItemRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchProse { listItem("Restart the app", marker = "1.") }
        }
        rule.onNodeWithText("1.", useUnmergedTree = true)
            .assertIsBefore(rule.onNodeWithText("Restart the app", useUnmergedTree = true), LayoutDirection.Rtl)
    }

    @Test
    fun textWrapsAtTheLargestFontScaleAndCodeScrollsInstead() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchProse {
                paragraph("Invoices now show the customer's tax number on every page.")
                code("rook sync --workspace acme --since 2026-01-01")
            }
        }
        rule.onNodeWithText("Invoices now show the customer's tax number on every page.").assertTextFullyVisible()
        rule.onNodeWithText("rook sync --workspace acme --since 2026-01-01").assertExists()
    }
}
