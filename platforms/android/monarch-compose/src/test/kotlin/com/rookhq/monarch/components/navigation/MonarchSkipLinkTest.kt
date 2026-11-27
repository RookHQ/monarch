package com.rookhq.monarch.components.navigation

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
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.navigation.skiplink.MonarchSkipLink
import com.rookhq.monarch.components.navigation.skiplink.MonarchSkipLinkTokens
import com.rookhq.monarch.components.navigation.skiplink.monarchSkipLinkTarget
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
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
