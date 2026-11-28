package dev.monarchui.testing

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.monarchui.accessibility.monarchMinimumTouchTarget
import dev.monarchui.internal.MonarchCentredText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** The test helpers themselves: each must pass on a correct layout and fail on a wrong one. */
@RunWith(RobolectricTestRunner::class)
class MonarchAssertionsTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theTouchTargetAssertionAcceptsAReservedTargetAndRejectsASmallElementWithoutOne() {
        rule.setMonarchContent {
            Row {
                // 20dp with no reserved space: Compose still extends its tap area, over its neighbour.
                Box(Modifier.size(20.dp).testTag("small").clickable { })
                Box(Modifier.monarchMinimumTouchTarget().size(20.dp).testTag("reserved").clickable { })
            }
        }
        rule.onNodeWithTag("reserved").assertTouchTargetAtLeast48dp(rule.density)
        assertFailsWith<AssertionError> { rule.onNodeWithTag("small").assertTouchTargetAtLeast48dp(rule.density) }
    }

    @Test
    fun theTextAssertionAcceptsWrappedTextAndRejectsEllipsizedText() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            Row {
                Text("A sentence that wraps", Modifier.width(120.dp))
                Text("A sentence that is cut", Modifier.width(120.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        rule.onNodeWithText("A sentence that wraps").assertTextFullyVisible()
        assertFailsWith<AssertionError> { rule.onNodeWithText("A sentence that is cut").assertTextFullyVisible() }
    }

    @Test
    fun theTextAssertionCatchesTheCutOfASingleCentredLineInABoxThatDoesNotFillItsWidth() {
        // C-052 re-check: a single line measured with centre alignment is laid out narrower than its text at large
        // font sizes, so its last glyph is cut. The assertion reports it; MonarchCentredText avoids it, and centred
        // text that wraps is fine.
        rule.setMonarchContent(fontScale = LargestFontScale) {
            Column {
                Box(Modifier.width(200.dp), contentAlignment = Alignment.Center) {
                    Text("Short", textAlign = TextAlign.Center)
                }
                Box(Modifier.width(200.dp), contentAlignment = Alignment.Center) {
                    MonarchCentredText("Brief")
                }
                Box(Modifier.width(140.dp), contentAlignment = Alignment.Center) {
                    Text("A centered sentence that wraps", textAlign = TextAlign.Center)
                }
            }
        }
        assertFailsWith<AssertionError> { rule.onNodeWithText("Short").assertTextFullyVisible() }
        rule.onNodeWithText("Brief").assertTextFullyVisible()
        rule.onNodeWithText("A centered sentence that wraps").assertTextFullyVisible()
    }

    @Test
    fun plainSpacesReplaceTheNoBreakSpacesOfIcu() {
        assertEquals("9:00 AM", "9:00\u202FAM".withPlainSpaces())
        assertEquals("10 Mar", "10\u00A0Mar".withPlainSpaces())
    }

    @Test
    fun theOrderAssertionFollowsTheLayoutDirection() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) {
            Row {
                Text("first")
                Text("second")
            }
        }
        val first = rule.onNodeWithText("first")
        val second = rule.onNodeWithText("second")
        first.assertIsBefore(second, LayoutDirection.Ltr)
        assertFailsWith<AssertionError> { second.assertIsBefore(first, LayoutDirection.Ltr) }
        environment.layoutDirection = LayoutDirection.Rtl
        rule.waitForIdle()
        first.assertIsBefore(second, LayoutDirection.Rtl)
        assertFailsWith<AssertionError> { second.assertIsBefore(first, LayoutDirection.Rtl) }
    }

    @Test
    fun aLinkInRunningTextIsFoundByComposesLinkMarker() {
        rule.setMonarchContent {
            Column {
                Text(
                    buildAnnotatedString {
                        append("Read ")
                        withLink(LinkAnnotation.Url("https://example.com")) { append("the report") }
                        append(" first.")
                    },
                )
                Box(Modifier.size(48.dp).clickable {}.testTag("button"))
            }
        }
        val clickable = rule.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.OnClick)).fetchSemanticsNodes()
        assertEquals(1, clickable.count { it.isTextLink() }, "Compose still marks link spans with $TextLinkMarker")
        assertEquals(false, rule.onNodeWithTag("button").fetchSemanticsNode().isTextLink())
    }
}

