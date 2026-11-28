package com.rookhq.monarch.components.overlays

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performTouchInput
import com.rookhq.monarch.components.actions.iconbutton.MonarchIconButton
import com.rookhq.monarch.components.overlays.tooltip.MonarchTooltip
import com.rookhq.monarch.components.overlays.tooltip.MonarchTooltipBox
import com.rookhq.monarch.foundation.icon.MonarchIcon
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

internal object MonarchTooltipFixtures : ComponentFixtures {
    override val id = "tooltip"
    @OptIn(ExperimentalMaterial3Api::class)
    override val fixtures = listOf(
        fixture("shown above an icon button") {
            val state = rememberTooltipState(isPersistent = true)
            LaunchedEffect(state) { state.show() }
            MonarchTooltipBox(text = "Add to favourites", state = state) {
                MonarchIconButton(label = "Add to favourites", onClick = {}) {
                    MonarchIcon(Icons.Outlined.Star, contentDescription = null)
                }
            }
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchTooltipTest {
    @get:Rule
    val rule = createComposeRule()

    private val label = "Add to favourites"

    @Test
    fun aLongPressShowsTheLabelInAPopupAndTheControlKeepsItsName() {
        rule.setMonarchContent {
            MonarchTooltip(text = label) {
                MonarchIconButton(label = label, onClick = {}) {
                    MonarchIcon(Icons.Outlined.Star, contentDescription = null)
                }
            }
        }
        rule.onNode(hasText(label) and hasAnyAncestor(isPopup())).assertDoesNotExist()
        rule.onNodeWithContentDescription(label).assertTouchTargetAtLeast48dp(rule.density)
            .performTouchInput { longClick() }
        rule.waitForIdle()
        rule.onNode(hasText(label) and hasAnyAncestor(isPopup())).assertIsDisplayed()
    }

    @Test
    fun aDisabledTooltipStaysHidden() {
        rule.setMonarchContent {
            MonarchTooltip(text = label, enabled = false) {
                MonarchIconButton(label = label, onClick = {}) {
                    MonarchIcon(Icons.Outlined.Star, contentDescription = null)
                }
            }
        }
        rule.onNodeWithContentDescription(label).performTouchInput { longClick() }
        rule.waitForIdle()
        rule.onNode(hasText(label) and hasAnyAncestor(isPopup())).assertDoesNotExist()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun aLongLabelWrapsAtTheLargestFontSize() {
        val text = "Add this report to your favourites"
        rule.setMonarchContent(fontScale = LargestFontScale) {
            val state = rememberTooltipState(isPersistent = true)
            LaunchedEffect(state) { state.show() }
            MonarchTooltipBox(text = text, state = state) {
                MonarchIconButton(label = text, onClick = {}) {
                    MonarchIcon(Icons.Outlined.Star, contentDescription = null)
                }
            }
        }
        rule.onNode(hasText(text) and hasAnyAncestor(isPopup())).assertTextFullyVisible()
    }
}
