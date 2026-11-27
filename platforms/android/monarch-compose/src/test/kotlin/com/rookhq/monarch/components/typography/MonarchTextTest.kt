package com.rookhq.monarch.components.typography

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.foundation.MonarchTypography
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.testing.textLayout
import com.rookhq.monarch.theme.MonarchTheme
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal object MonarchTextFixtures : ComponentFixtures {
    override val id = "text"
    override val fixtures = listOf(
        fixture("every type role") {
            Column { MonarchTextStyle.entries.forEach { MonarchText(text = it.name, style = it) } }
        },
        fixture("color, truncated to one line") {
            MonarchText(
                text = "A long sentence that does not fit on one line of this narrow column.",
                modifier = Modifier.width(160.dp),
                color = MonarchTheme.colors.textMuted,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
        },
        fixture("annotated") {
            MonarchText(text = buildAnnotatedString { append("Annotated") }, style = MonarchTextStyle.Subheading)
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchTextTest {
    @get:Rule
    val rule = createComposeRule()

    private val isHeading = SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)

    @Test
    fun usesTheStyleOfItsRole() {
        rule.setMonarchContent { MonarchText(text = "Title", style = MonarchTextStyle.Title) }
        val style = rule.onNodeWithText("Title").textLayout().layoutInput.style
        assertEquals(MonarchTypography.Default.title.fontSize, style.fontSize)
        assertEquals(MonarchTypography.Default.title.fontWeight, style.fontWeight)
    }

    @Test
    fun headingRolesAreHeadingsAndBodyIsNot() {
        rule.setMonarchContent {
            Column {
                MonarchText(text = "Heading", style = MonarchTextStyle.Heading)
                MonarchText(text = "Body")
                MonarchText(text = "Forced", heading = true)
                MonarchText(text = "Plain title", style = MonarchTextStyle.Title, heading = false)
            }
        }
        rule.onNodeWithText("Heading").assert(isHeading)
        rule.onNodeWithText("Body").assert(!isHeading)
        rule.onNodeWithText("Forced").assert(isHeading)
        rule.onNodeWithText("Plain title").assert(!isHeading)
    }

    @Test
    fun scalesWithTheFontSizeSetting() {
        rule.setMonarchContent(fontScale = LargestFontScale) { MonarchText(text = "Body") }
        val input = rule.onNodeWithText("Body").textLayout().layoutInput
        val scaledPx = with(input.density) { input.style.fontSize.toPx() }
        val unscaledPx = MonarchTypography.Default.body.fontSize.value * input.density.density
        // Android scales large text less than linearly from Android 14, so 200% is not exactly double.
        assertTrue(scaledPx >= unscaledPx * 1.5f, "Scaled $scaledPx px from $unscaledPx px")
    }

    @Test
    fun acceptsAnnotatedText() {
        rule.setMonarchContent {
            MonarchText(text = buildAnnotatedString { append("Annotated") }, style = MonarchTextStyle.Subheading)
        }
        rule.onNodeWithText("Annotated").assert(isHeading)
    }
}
