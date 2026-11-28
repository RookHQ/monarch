package com.rookhq.monarch.components.typography

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.em
import com.rookhq.monarch.components.typography.inlinecodestyle.InlineRun
import com.rookhq.monarch.components.typography.inlinecodestyle.MonarchInlineCodeStyle
import com.rookhq.monarch.components.typography.inlinecodestyle.MonarchInlineCodeStyleTokens
import com.rookhq.monarch.components.typography.inlinecodestyle.MonarchInlineCodeVariant
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.foundation.MonarchTypography
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals

internal object MonarchInlineCodeStyleFixtures : ComponentFixtures {
    override val id = "inline-code"
    override val fixtures = listOf(
        fixture("subtle in body text") {
            MonarchText(text = MonarchInlineCodeStyle.markdown("Run `./gradlew build` before you push."))
        },
        fixture("plain in a caption") {
            MonarchText(
                text = MonarchInlineCodeStyle.markdown("Exit code `127`", MonarchInlineCodeVariant.Plain),
                style = MonarchTextStyle.Caption,
            )
        },
        fixture("long token") {
            MonarchText(text = MonarchInlineCodeStyle.markdown("Commit `9f2c4e1b7a3d5f60c8e2b4a6d8f0c2e4a6b8d0f2`"))
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchInlineCodeStyleTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun backticksMarkCodeAndAreRemoved() {
        assertEquals(
            listOf(InlineRun("Run ", false), InlineRun("build", true), InlineRun(" first.", false)),
            MonarchInlineCodeStyleTokens.runs("Run `build` first."),
        )
        assertEquals(listOf(InlineRun("a", true), InlineRun("b", true)), MonarchInlineCodeStyleTokens.runs("`a``b`"))
    }

    @Test
    fun anUnmatchedBacktickIsShownAsItIs() {
        assertEquals(
            listOf(InlineRun("Say ", false), InlineRun("hi", true), InlineRun(" and `bye", false)),
            MonarchInlineCodeStyleTokens.runs("Say `hi` and `bye"),
        )
        assertEquals(emptyList(), MonarchInlineCodeStyleTokens.runs(""))
        assertEquals(emptyList(), MonarchInlineCodeStyleTokens.runs("``"))
    }

    @Test
    fun theCodeRunCarriesTheStyleAndTheTextIsReadInPlace() {
        lateinit var text: AnnotatedString
        lateinit var style: SpanStyle
        rule.setMonarchContent {
            text = MonarchInlineCodeStyle.markdown("Run `build` first.")
            style = MonarchInlineCodeStyle.spanStyle()
            MonarchText(text = text)
        }
        rule.onNodeWithText("Run build first.").assertTextEquals("Run build first.")
        val span = text.spanStyles.single()
        assertEquals(4, span.start)
        assertEquals(9, span.end)
        assertEquals(style, span.item)
    }

    @Test
    fun aSingleFragmentIsOneCodeRun() {
        lateinit var text: AnnotatedString
        rule.setMonarchContent { text = MonarchInlineCodeStyle.annotatedString("127", MonarchInlineCodeVariant.Plain) }
        assertEquals("127", text.text)
        assertEquals(0 until 3, text.spanStyles.single().let { it.start until it.end })
    }

    @Test
    fun aLongTokenWrapsAtTheLargestFontSize() {
        val token = "9f2c4e1b7a3d5f60c8e2b4a6d8f0c2e4a6b8d0f2"
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchText(text = MonarchInlineCodeStyle.markdown("Commit `$token`"))
        }
        rule.onNodeWithText("Commit $token").assertTextFullyVisible()
    }

    @Test
    fun theStyleComesFromTheThemeRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val typography = MonarchTypography.Default
            val subtle = MonarchInlineCodeStyleTokens.spanStyle(colors, typography, MonarchInlineCodeVariant.Subtle)
            assertEquals(colors.bgMuted, subtle.background, theme.id)
            assertEquals(colors.textDefault, subtle.color, theme.id)
            assertEquals(typography.code.fontFamily, subtle.fontFamily, theme.id)
            assertEquals(0.9f.em, subtle.fontSize, theme.id)
            val plain = MonarchInlineCodeStyleTokens.spanStyle(colors, typography, MonarchInlineCodeVariant.Plain)
            assertEquals(Color.Unspecified, plain.background, theme.id)
            assertEquals(Color.Unspecified, plain.color, theme.id)
            assertEquals(typography.code.fontFamily, plain.fontFamily, theme.id)
        }
    }
}
