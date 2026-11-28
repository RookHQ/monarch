package com.rookhq.monarch.components.forms

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.forms.radio.MonarchRadio
import com.rookhq.monarch.components.forms.radiogroup.MonarchRadioGroup
import com.rookhq.monarch.components.forms.radiorow.MonarchRadioRow
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals

internal object MonarchRadioRowFixtures : ComponentFixtures {
    override val id = "radio-group"
    override val fixtures = listOf(
        fixture("group of rows") {
            Column(Modifier.selectableGroup()) {
                MonarchRadioRow(selected = true, onClick = {}, label = "Daily")
                MonarchRadioRow(selected = false, onClick = {}, label = "Weekly", description = "On Mondays.")
                MonarchRadioRow(selected = false, onClick = {}, label = "Never", enabled = false)
            }
        },
        fixture("bare radios: selected, unselected, invalid, disabled") {
            MonarchRadioGroup(label = "Delivery") {
                Row {
                    MonarchRadio(
                        selected = true,
                        onClick = {},
                        modifier = Modifier.semantics { contentDescription = "Standard delivery" },
                    )
                    MonarchRadio(
                        selected = false,
                        onClick = {},
                        modifier = Modifier.semantics { contentDescription = "Express delivery" },
                    )
                    MonarchRadio(
                        selected = false,
                        onClick = {},
                        invalid = true,
                        modifier = Modifier.semantics { contentDescription = "Invalid" },
                    )
                    MonarchRadio(
                        selected = true,
                        onClick = {},
                        enabled = false,
                        modifier = Modifier.semantics { contentDescription = "Disabled" },
                    )
                }
            }
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchRadioRowTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun rowsSelectOneOptionOfAGroup() {
        var selected by mutableIntStateOf(0)
        val options = listOf("Daily", "Weekly", "Never")
        rule.setMonarchContent {
            Column(Modifier.selectableGroup()) {
                options.forEachIndexed { index, option ->
                    MonarchRadioRow(selected = selected == index, onClick = { selected = index }, label = option)
                }
            }
        }
        rule.onNodeWithText("Daily")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
            .assertIsSelected()
        rule.onNodeWithText("Weekly").assertIsNotSelected().assertHeightIsAtLeast(48.dp).performClick()
        assertEquals(1, selected)
        rule.onNodeWithText("Weekly").assertIsSelected()
        rule.onNodeWithText("Daily").assertIsNotSelected()
    }

    @Test
    fun aDisabledRowIsAnnouncedAsDisabledAndIgnoresTaps() {
        var changes = 0
        rule.setMonarchContent {
            MonarchRadioRow(selected = false, onClick = { changes++ }, label = "Radio", enabled = false)
        }
        rule.onNodeWithText("Radio").assertIsNotEnabled().performClick()
        assertEquals(0, changes)
    }
}
