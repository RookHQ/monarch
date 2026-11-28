package dev.monarchui.components.datadisplay

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import dev.monarchui.components.datadisplay.listrow.MonarchListRow
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.TestIcon
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MonarchListRowTest {
    @get:Rule
    val rule = createComposeRule()

    private val isButton = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)

    @Test
    fun aTappableRowIsOneButtonWithMergedTextsAndA48dpTarget() {
        var taps = 0
        rule.setMonarchContent {
            MonarchListRow(
                headlineContent = { Text("Invoice 1042") },
                supportingContent = { Text("Paid") },
                onClick = { taps++ },
                modifier = Modifier.testTag("row"),
            )
        }
        rule.onNodeWithTag("row").assert(isButton).assertHasClickAction().assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        assertEquals(1, taps)
        rule.onNodeWithTag("row").assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun theClickLabelIsWhatTalkBackSaysADoubleTapDoes() {
        rule.setMonarchContent {
            MonarchListRow(
                headlineContent = { Text("Dana Fox changed a role") },
                onClick = {},
                onClickLabel = "Details",
                modifier = Modifier.testTag("row"),
            )
            MonarchListRow(headlineContent = { Text("Plain") }, onClick = {}, modifier = Modifier.testTag("plain"))
        }
        rule.onNodeWithTag("row").assert(
            SemanticsMatcher("click label is Details") {
                it.config.getOrNull(SemanticsActions.OnClick)?.label == "Details"
            },
        )
        rule.onNodeWithTag("plain").assert(
            SemanticsMatcher("no click label") { it.config.getOrNull(SemanticsActions.OnClick)?.label == null },
        )
    }

    @Test
    fun aSelectedRowIsAnnouncedAsSelected() {
        rule.setMonarchContent {
            MonarchListRow(
                headlineContent = { Text("Current") },
                onClick = {},
                selected = true,
                modifier = Modifier.testTag("row"),
            )
        }
        rule.onNodeWithTag("row").assertIsSelected()
    }

    @Test
    fun aDisabledRowIsDisabledAndIgnoresTaps() {
        var taps = 0
        rule.setMonarchContent {
            MonarchListRow(
                headlineContent = { Text("Locked") },
                onClick = { taps++ },
                enabled = false,
                modifier = Modifier.testTag("row"),
            )
        }
        rule.onNodeWithTag("row").assertIsNotEnabled().performClick()
        assertEquals(0, taps)
    }

    @Test
    fun aLargerAvatarKeepsItsSizeAndAnIconIsCentredInTheLeadingBox() {
        rule.setMonarchContent {
            Column {
                MonarchListRow(
                    headlineContent = { Text("Dana Kim") },
                    leadingContent = { Box(Modifier.size(40.dp).testTag("avatar")) },
                )
                MonarchListRow(
                    headlineContent = { Text("Settings") },
                    leadingContent = { Box(Modifier.size(16.dp).testTag("icon")) },
                )
            }
        }
        rule.onNodeWithTag("avatar", useUnmergedTree = true).assertWidthIsEqualTo(40.dp).assertHeightIsEqualTo(40.dp)
        rule.onNodeWithTag("icon", useUnmergedTree = true).assertWidthIsEqualTo(16.dp)
    }

    @Test
    fun theTextsAreNotCutOffAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchListRow(
                headlineContent = { Text("Renewal at risk") },
                supportingContent = { Text("No activity for 14 days") },
                leadingContent = { Text("A") },
                trailingContent = { Text("3") },
            )
        }
        rule.onNodeWithText("Renewal at risk", useUnmergedTree = true).assertTextFullyVisible()
        rule.onNodeWithText("No activity for 14 days", useUnmergedTree = true).assertTextFullyVisible()
    }
}

internal object MonarchListRowFixtures : ComponentFixtures {
    override val id = "list-row"
    override val fixtures = listOf(
        fixture("rows: leading icon, supporting line, trailing value, selected, disabled") {
            Column {
                MonarchListRow(
                    headlineContent = { Text("Invoice INV-1042") },
                    supportingContent = { Text("Acme Corp, paid Mar 3") },
                    leadingContent = { MonarchIcon(imageVector = TestIcon, contentDescription = null) },
                    trailingContent = { Text("$420.00") },
                    onClick = {},
                    divider = true,
                )
                MonarchListRow(
                    overlineContent = { Text("Billing") },
                    headlineContent = { Text("Payment method") },
                    onClick = {},
                    selected = true,
                    divider = true,
                )
                MonarchListRow(headlineContent = { Text("Archived invoices") }, onClick = {}, enabled = false)
            }
        },
    )
}
