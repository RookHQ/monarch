package dev.monarchui.components.actions

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.actions.buttongroup.MonarchButtonGroup
import dev.monarchui.components.actions.buttongroup.MonarchButtonGroupTokens
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.MonarchTestEnvironment
import dev.monarchui.testing.TestIcon
import dev.monarchui.testing.assertIsBefore
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal object MonarchButtonGroupFixtures : ComponentFixtures {
    override val id = "button-group"
    override val fixtures = MonarchButtonVariant.entries.flatMap { variant ->
        listOf(
            fixture("${variant.name}, attached, every size") {
                Column {
                    MonarchButtonSize.entries.forEach { size ->
                        MonarchButtonGroup(variant = variant, size = size, label = "Pages") {
                            button(text = "Previous", onClick = {})
                            button(text = "Today", onClick = {})
                            button(text = "Next", onClick = {})
                        }
                    }
                }
            },
            fixture("${variant.name}, one disabled") {
                MonarchButtonGroup(variant = variant) {
                    button(text = "Previous", onClick = {}, enabled = false)
                    button(text = "Next", onClick = {})
                }
            },
        )
    } + listOf(
        fixture("vertical icon buttons") {
            MonarchButtonGroup(orientation = Orientation.Vertical, label = "Zoom") {
                iconButton(contentDescription = "Zoom in", onClick = {}) {
                    MonarchIcon(imageVector = TestIcon, contentDescription = null)
                }
                iconButton(contentDescription = "Zoom out", onClick = {}) {
                    MonarchIcon(imageVector = TestIcon, contentDescription = null)
                }
            }
        },
        fixture("small icon buttons in a row") {
            MonarchButtonGroup(size = MonarchButtonSize.Sm) {
                iconButton(contentDescription = "Bold", onClick = {}) {
                    MonarchIcon(imageVector = TestIcon, contentDescription = null)
                }
                iconButton(contentDescription = "Italic", onClick = {}) {
                    MonarchIcon(imageVector = TestIcon, contentDescription = null)
                }
            }
        },
        fixture("detached, full width") {
            MonarchButtonGroup(attached = false, fullWidth = true) {
                button(text = "Cancel", onClick = {})
                button(text = "Save", onClick = {})
            }
        },
        fixture("long labels in a narrow width") {
            MonarchButtonGroup(modifier = Modifier.width(220.dp), fullWidth = true) {
                button(text = "Keep editing", onClick = {})
                button(text = "Save and continue to the next step", onClick = {})
            }
        },
        fixture("one button") { MonarchButtonGroup { button(text = "Alone", onClick = {}) } },
        fixture("vertical, full width, with icons") {
            MonarchButtonGroup(
                orientation = Orientation.Vertical,
                variant = MonarchButtonVariant.Primary,
                fullWidth = true,
            ) {
                button(
                    text = "Approve",
                    onClick = {},
                    leadingIcon = { MonarchIcon(imageVector = TestIcon, contentDescription = null) },
                )
                button(text = "Approve and notify the team", onClick = {})
            }
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchButtonGroupTest {
    @get:Rule
    val rule = createComposeRule()

    private val isButton = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)

    @Test
    fun everyButtonStaysItsOwnButtonAndTheGroupIsLabelled() {
        val clicks = mutableListOf<String>()
        rule.setMonarchContent {
            MonarchButtonGroup(modifier = Modifier.testTag("group"), label = "Pages") {
                button(text = "Previous", onClick = { clicks += "previous" })
                button(text = "Next", onClick = { clicks += "next" })
            }
        }
        rule.onNodeWithTag("group").assertContentDescriptionEquals("Pages")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.IsTraversalGroup, true))
        rule.onNodeWithText("Previous").assert(isButton).assertHasClickAction().performClick()
        rule.onNodeWithText("Next").assert(isButton).performClick()
        assertEquals(listOf("previous", "next"), clicks)
    }

    @Test
    fun aDisabledButtonIgnoresTapsAndTheOthersStayEnabled() {
        var clicks = 0
        rule.setMonarchContent {
            MonarchButtonGroup {
                button(text = "Previous", onClick = { clicks++ }, enabled = false)
                button(text = "Next", onClick = {})
            }
        }
        rule.onNodeWithText("Previous").assertIsNotEnabled().performClick()
        rule.onNodeWithText("Next").assertIsEnabled()
        assertEquals(0, clicks)
    }

    @Test
    fun iconButtonsAreNamedByTheirDescription() {
        var zoom = 0
        rule.setMonarchContent {
            MonarchButtonGroup(orientation = Orientation.Vertical) {
                iconButton(contentDescription = "Zoom in", onClick = { zoom++ }) {
                    MonarchIcon(imageVector = TestIcon, contentDescription = null)
                }
            }
        }
        rule.onNodeWithContentDescription("Zoom in").assert(isButton).performClick()
        assertEquals(1, zoom)
    }

    @Test
    fun everyButtonHasATouchTargetOfAtLeast48dpInBothOrientations() {
        rule.setMonarchContent {
            Column {
                Orientation.entries.forEach { orientation ->
                    MonarchButtonGroup(orientation = orientation, size = MonarchButtonSize.Sm) {
                        iconButton(contentDescription = "A ${orientation.name}", onClick = {}) {
                            MonarchIcon(imageVector = TestIcon, contentDescription = null)
                        }
                        button(text = "B ${orientation.name}", onClick = {})
                    }
                }
            }
        }
        Orientation.entries.forEach { orientation ->
            rule.onNodeWithContentDescription("A ${orientation.name}").assertTouchTargetAtLeast48dp(rule.density)
            rule.onNodeWithText("B ${orientation.name}").assertTouchTargetAtLeast48dp(rule.density)
        }
    }

    @Test
    fun attachedSecondaryButtonsShareTheirBorderAndRowsHaveEqualHeights() {
        rule.setMonarchContent {
            MonarchButtonGroup(modifier = Modifier.width(240.dp), fullWidth = true) {
                button(text = "Keep", onClick = {})
                button(text = "Save and continue to the next step", onClick = {})
            }
        }
        val first = rule.onNodeWithText("Keep").getUnclippedBoundsInRoot()
        val second = rule.onNodeWithText("Save and continue to the next step").getUnclippedBoundsInRoot()
        // The second button starts one border width before the first ends (within a pixel of rounding).
        val overlap = first.right - second.left
        assertTrue(overlap > 0.5.dp && overlap < 1.5.dp, "Overlap is $overlap")
        // The short label takes the height of the wrapped one, so the joined shape stays straight.
        assertEquals(first.bottom - first.top, second.bottom - second.top)
        assertTrue(second.bottom - second.top > 48.dp)
    }

    @Test
    fun theOuterCornersAreRoundAndTheInnerOnesSquare() {
        val density = Density(1f)
        val size = Size(100f, 40f)
        fun corners(index: Int, count: Int, orientation: Orientation, attached: Boolean = true): List<Float> {
            val shape = MonarchButtonGroupTokens.shape(index, count, orientation, attached)
            return listOf(shape.topStart, shape.topEnd, shape.bottomEnd, shape.bottomStart)
                .map { corner: CornerSize -> corner.toPx(size, density) }
        }
        assertEquals(listOf(8f, 0f, 0f, 8f), corners(0, 3, Orientation.Horizontal))
        assertEquals(listOf(0f, 0f, 0f, 0f), corners(1, 3, Orientation.Horizontal))
        assertEquals(listOf(0f, 8f, 8f, 0f), corners(2, 3, Orientation.Horizontal))
        assertEquals(listOf(8f, 8f, 0f, 0f), corners(0, 2, Orientation.Vertical))
        assertEquals(listOf(0f, 0f, 8f, 8f), corners(1, 2, Orientation.Vertical))
        assertEquals(listOf(8f, 8f, 8f, 8f), corners(0, 1, Orientation.Horizontal))
        assertEquals(listOf(8f, 8f, 8f, 8f), corners(1, 3, Orientation.Horizontal, attached = false))
    }

    @Test
    fun rowWidthsAreNaturalUnlessTheyMustBeShared() {
        assertEquals(listOf(100, 60), MonarchButtonGroupTokens.rowWidths(listOf(100, 60), 400, -1, fill = false))
        assertEquals(listOf(200, 200), MonarchButtonGroupTokens.rowWidths(listOf(100, 60), 399, -1, fill = true))
        assertEquals(listOf(96, 96), MonarchButtonGroupTokens.rowWidths(listOf(300, 60), 200, 8, fill = false))
    }

    @Test
    fun spacingDependsOnTheBorderAndOnAttachment() {
        assertEquals((-1).dp, MonarchButtonGroupTokens.spacing(MonarchButtonVariant.Secondary, attached = true))
        assertEquals(1.dp, MonarchButtonGroupTokens.spacing(MonarchButtonVariant.Primary, attached = true))
        assertEquals(8.dp, MonarchButtonGroupTokens.spacing(MonarchButtonVariant.Secondary, attached = false))
    }

    @Test
    fun theFirstButtonIsAtTheStartInBothLayoutDirections() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) {
            MonarchButtonGroup {
                button(text = "Previous", onClick = {})
                button(text = "Next", onClick = {})
            }
        }
        listOf(LayoutDirection.Ltr, LayoutDirection.Rtl).forEach { direction ->
            environment.layoutDirection = direction
            rule.waitForIdle()
            rule.onNodeWithText("Previous").assertIsBefore(rule.onNodeWithText("Next"), direction)
        }
    }

    @Test
    fun labelsWrapInsteadOfBeingCutOffAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchButtonGroup(modifier = Modifier.width(240.dp), fullWidth = true) {
                button(text = "Previous page", onClick = {})
                button(text = "Next page", onClick = {})
            }
        }
        rule.onNodeWithText("Previous page", useUnmergedTree = true).assertTextFullyVisible()
        rule.onNodeWithText("Next page", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun aGroupWithoutButtonsDrawsNothing() {
        rule.setMonarchContent { MonarchButtonGroup(modifier = Modifier.testTag("empty")) { } }
        rule.onNodeWithTag("empty").assertDoesNotExist()
    }
}
