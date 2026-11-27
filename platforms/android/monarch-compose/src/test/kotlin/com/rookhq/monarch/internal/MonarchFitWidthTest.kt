package com.rookhq.monarch.internal

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.testing.setMonarchContent
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class MonarchFitWidthTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun contentBuiltForALargerWidthIsScaledToTheSpace() {
        rule.setMonarchContent {
            Box(Modifier.requiredWidth(320.dp).testTag("host")) {
                Box(Modifier.testTag("content").monarchFitWidth(360.dp)) { Box(Modifier.size(360.dp, 100.dp)) }
            }
        }
        val bounds = rule.onNodeWithTag("content").getUnclippedBoundsInRoot()
        assertEquals(320f, bounds.right.value - bounds.left.value, 0.6f)
        assertEquals(89f, bounds.bottom.value - bounds.top.value, 1f)
    }

    @Test
    fun contentIsNeverScaledUp() {
        rule.setMonarchContent {
            Box(Modifier.requiredWidth(500.dp)) {
                Box(Modifier.testTag("content").monarchFitWidth(360.dp)) { Box(Modifier.size(360.dp, 100.dp)) }
            }
        }
        val bounds = rule.onNodeWithTag("content").getUnclippedBoundsInRoot()
        assertEquals(360f, bounds.right.value - bounds.left.value, 0.6f)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun theCalendarPickerIsAsWideAsTheSpaceInsteadOfCutOff() {
        rule.setMonarchContent {
            Box(Modifier.requiredWidth(320.dp)) {
                val state = rememberDatePickerState(
                    initialSelectedDateMillis = 1_768_348_800_000L,
                    initialDisplayedMonthMillis = 1_768_348_800_000L,
                )
                DatePicker(
                    state = state,
                    modifier = Modifier.testTag("picker").monarchFitWidth(),
                    showModeToggle = false,
                )
            }
        }
        // Material draws the picker 360dp wide; unscaled, its Saturday column ends at about 348dp.
        val bounds = rule.onNodeWithTag("picker").getUnclippedBoundsInRoot()
        assertTrue(bounds.right - bounds.left <= 321.dp, "the picker is wider than its space: $bounds")
    }
}
