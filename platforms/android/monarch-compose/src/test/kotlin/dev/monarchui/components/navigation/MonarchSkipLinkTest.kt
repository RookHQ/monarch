package dev.monarchui.components.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.navigation.skiplink.MonarchSkipLink
import dev.monarchui.components.navigation.skiplink.MonarchSkipLinkTokens
import dev.monarchui.components.navigation.skiplink.monarchSkipLinkTarget
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals

internal object MonarchSkipLinkFixtures : ComponentFixtures {
    override val id = "skip-link"
    override val fixtures = listOf(
        fixture("default label") {
            val main = remember { FocusRequester() }
            MonarchSkipLink(target = main)
            MonarchText("Main content starts here.", Modifier.monarchSkipLinkTarget(main))
        },
        fixture("custom label") {
            val search = remember { FocusRequester() }
            MonarchSkipLink(target = search, label = "Skip to search")
            MonarchText("Search", Modifier.monarchSkipLinkTarget(search))
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchSkipLinkTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun activatingItMovesFocusToTheTarget() {
        var skipped = 0
        rule.setMonarchContent {
            val main = remember { FocusRequester() }
            Column {
                MonarchSkipLink(target = main, onSkip = { skipped++ })
                MonarchText("Main content", Modifier.testTag("main").monarchSkipLinkTarget(main))
            }
        }
        rule.onNodeWithTag("main").assertIsNotFocused()
        rule.onNodeWithText("Skip to main content").assertHasClickAction().assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        rule.onNodeWithTag("main").assertIsFocused()
        assertEquals(1, skipped)
    }

    @Test
    fun itIsReadFirstInItsGroup() {
        rule.setMonarchContent { MonarchSkipLink(target = remember { FocusRequester() }) }
        rule.onNodeWithText("Skip to main content")
            .assert(
                SemanticsMatcher.expectValue(SemanticsProperties.TraversalIndex, MonarchSkipLinkTokens.TRAVERSAL_INDEX),
            )
    }

    @Test
    fun theLabelCanBeReplacedAndWrapsAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchSkipLink(target = remember { FocusRequester() }, label = "Saltar al contenido principal")
        }
        rule.onNodeWithText("Saltar al contenido principal", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun itIsAQuietSecondaryButton() {
        assertEquals(MonarchButtonVariant.Secondary, MonarchSkipLinkTokens.Variant)
    }
}
