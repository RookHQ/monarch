package com.rookhq.monarch.components.forms

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.forms.switch.MonarchSwitch
import com.rookhq.monarch.components.forms.switchrow.MonarchSwitchRow
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.MonarchTestEnvironment
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals

internal object MonarchSwitchRowFixtures : ComponentFixtures {
    override val id = "switch"
    override val fixtures = listOf(
        fixture("row, on") { MonarchSwitchRow(checked = true, onCheckedChange = {}, label = "Notifications") },
        fixture("row, off with description") {
            MonarchSwitchRow(
                checked = false,
                onCheckedChange = {},
                label = "Sync over mobile data",
                description = "May use your data allowance.",
            )
        },
        fixture("row, disabled") {
            MonarchSwitchRow(checked = true, onCheckedChange = {}, label = "Managed by your admin", enabled = false)
        },
        fixture("bare switches: on, off, disabled") {
            Row {
                MonarchSwitch(
                    checked = true,
                    onCheckedChange = {},
                    modifier = Modifier.semantics { contentDescription = "Dark theme" },
                )
                MonarchSwitch(
                    checked = false,
                    onCheckedChange = {},
                    modifier = Modifier.semantics { contentDescription = "Compact view" },
                )
                MonarchSwitch(
                    checked = true,
                    onCheckedChange = {},
                    enabled = false,
                    modifier = Modifier.semantics { contentDescription = "Locked" },
                )
            }
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchSwitchRowTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theRowHasTheSwitchRoleAndTogglesAsAWhole() {
        var checked by mutableStateOf(true)
        rule.setMonarchContent {
            MonarchSwitchRow(checked = checked, onCheckedChange = { checked = it }, label = "Notifications")
        }
        val row = rule.onNodeWithText("Notifications")
        row.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch))
            .assertIsOn()
            .assertHeightIsAtLeast(48.dp)
        row.performClick()
        assertEquals(false, checked)
        row.assertIsOff()
    }

    @Test
    fun aDisabledRowIsAnnouncedAsDisabledAndIgnoresTaps() {
        var changes = 0
        rule.setMonarchContent {
            MonarchSwitchRow(checked = false, onCheckedChange = { changes++ }, label = "Switch", enabled = false)
        }
        rule.onNodeWithText("Switch").assertIsNotEnabled().performClick()
        assertEquals(0, changes)
    }

    @Test
    fun theLabelIsAtTheStartAndTheSwitchAtTheEndInBothLayoutDirections() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) {
            MonarchSwitchRow(checked = false, onCheckedChange = {}, label = "Switch")
        }
        fun label() = rule.onNodeWithText("Switch", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        fun row() = rule.onNodeWithText("Switch").fetchSemanticsNode().boundsInRoot
        assertEquals(row().left, label().left)
        environment.layoutDirection = LayoutDirection.Rtl
        rule.waitForIdle()
        assertEquals(row().right, label().right)
    }
}
