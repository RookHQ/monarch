package com.rookhq.monarch.components.actions

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonTokens
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.MonarchTestEnvironment
import com.rookhq.monarch.testing.TestIcon
import com.rookhq.monarch.testing.assertIsBefore
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.testing.stateDescription
import com.rookhq.monarch.testing.textLayout
import com.rookhq.monarch.theme.MonarchColors
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

internal object MonarchButtonFixtures : ComponentFixtures {
    override val id = "button"
    override val fixtures = MonarchButtonVariant.entries.flatMap { variant ->
        listOf(
            fixture("${variant.name}, every size") {
                Column {
                    MonarchButtonSize.entries.forEach { size ->
                        MonarchButton(text = "Save", onClick = {}, variant = variant, size = size)
                    }
                }
            },
            fixture("${variant.name}, disabled") {
                MonarchButton(text = "Save", onClick = {}, variant = variant, enabled = false)
            },
            fixture("${variant.name}, loading") {
                MonarchButton(text = "Save", onClick = {}, variant = variant, loading = true)
            },
        )
    } + listOf(
        fixture("icons") {
            MonarchButton(
                text = "Add",
                onClick = {},
                leadingIcon = { MonarchIcon(imageVector = TestIcon, contentDescription = null) },
                trailingIcon = { MonarchIcon(imageVector = TestIcon, contentDescription = null) },
            )
        },
        fixture("long label in a narrow width") {
            MonarchButton(text = "Save and continue to the next step", onClick = {}, modifier = Modifier.width(160.dp))
        },
        fixture("in a horizontally scrolling row") {
            Row(Modifier.horizontalScroll(rememberScrollState())) {
                MonarchButton(text = "Archive", onClick = {})
                MonarchButton(
                    text = "Export",
                    onClick = {},
                    trailingIcon = { MonarchIcon(imageVector = TestIcon, contentDescription = null) },
                )
            }
        },
        fixture("custom shape") {
            MonarchButton(text = "Left", onClick = {}, shape = RoundedCornerShape(topStartPercent = 50))
        },
        fixture("content slot, full width") {
            MonarchButton(onClick = {}, modifier = Modifier.fillMaxWidth()) {
                MonarchText(text = "Inbox")
                MonarchText(text = "3")
            }
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchButtonTest {
    @get:Rule
    val rule = createComposeRule()

    private val isButton = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)

    @Test
    fun exposesTheButtonRoleLabelAndClickAction() {
        var clicks = 0
        rule.setMonarchContent { MonarchButton(text = "Save", onClick = { clicks++ }) }
        rule.onNodeWithText("Save").assert(isButton).assertHasClickAction().assertIsEnabled().performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun aDisabledButtonIsAnnouncedAsDisabledAndIgnoresTaps() {
        var clicks = 0
        rule.setMonarchContent { MonarchButton(text = "Save", onClick = { clicks++ }, enabled = false) }
        rule.onNodeWithText("Save").assert(isButton).assertIsNotEnabled().performClick()
        assertEquals(0, clicks)
    }

    @Test
    fun aLoadingButtonAnnouncesItsStateAndIgnoresTaps() {
        var clicks = 0
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent { MonarchButton(text = "Save", onClick = { clicks++ }, loading = true) }
        rule.mainClock.advanceTimeByFrame()
        val button = rule.onNodeWithText("Save")
        button.assert(isButton).assertIsNotEnabled()
        assertEquals("Loading", button.stateDescription())
        button.performClick()
        rule.mainClock.advanceTimeByFrame()
        assertEquals(0, clicks)
    }

    @Test
    fun theLoadingLabelCanBeOverridden() {
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent {
            MonarchButton(text = "Save", onClick = {}, loading = true, loadingLabel = "Saving changes")
        }
        rule.mainClock.advanceTimeByFrame()
        assertEquals("Saving changes", rule.onNodeWithText("Save").stateDescription())
    }

    @Test
    fun aButtonThatIsNotLoadingHasNoStateDescription() {
        rule.setMonarchContent { MonarchButton(text = "Save", onClick = {}) }
        assertNull(rule.onNodeWithText("Save").stateDescription())
    }

    @Test
    fun theLoadingIndicatorReplacesTheLeadingIcon() {
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent {
            MonarchButton(
                text = "Save",
                onClick = {},
                loading = true,
                leadingIcon = { Box(Modifier.testTag("leading")) },
                trailingIcon = { Box(Modifier.testTag("trailing")) },
            )
        }
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithTag("leading", useUnmergedTree = true).assertDoesNotExist()
        rule.onNodeWithTag("trailing", useUnmergedTree = true).assertExists()
    }

    @Test
    fun everySizeHasATouchTargetOfAtLeast48dp() {
        rule.setMonarchContent {
            Column {
                MonarchButtonSize.entries.forEach { size ->
                    MonarchButton(text = "A", onClick = {}, size = size, modifier = Modifier.testTag(size.name))
                }
            }
        }
        MonarchButtonSize.entries.forEach { size ->
            rule.onNodeWithTag(size.name).assertTouchTargetAtLeast48dp(rule.density)
        }
        // The visible button keeps the height of its size.
        rule.onNodeWithTag(MonarchButtonSize.Sm.name).assertHeightIsEqualTo(48.dp)
        rule.onNodeWithTag(MonarchButtonSize.Md.name).assertHeightIsEqualTo(48.dp)
        rule.onNodeWithTag(MonarchButtonSize.Lg.name).assertHeightIsEqualTo(48.dp)
        assertEquals(32.dp, MonarchButtonTokens.minHeight(MonarchButtonSize.Sm))
        assertEquals(36.dp, MonarchButtonTokens.minHeight(MonarchButtonSize.Md))
        assertEquals(40.dp, MonarchButtonTokens.minHeight(MonarchButtonSize.Lg))
        // Each button reserves 48dp in the layout, so neighbouring targets do not overlap.
        val small = rule.onNodeWithTag(MonarchButtonSize.Sm.name).getUnclippedBoundsInRoot()
        val medium = rule.onNodeWithTag(MonarchButtonSize.Md.name).getUnclippedBoundsInRoot()
        val large = rule.onNodeWithTag(MonarchButtonSize.Lg.name).getUnclippedBoundsInRoot()
        assertEquals(48.dp, (medium.top + medium.height / 2) - (small.top + small.height / 2))
        assertEquals(48.dp, (large.top + large.height / 2) - (medium.top + medium.height / 2))
    }

    @Test
    fun everyButtonSizeKeepsTheFourteenSpLabel() {
        val typography = com.rookhq.monarch.foundation.monarchTypography()
        MonarchButtonVariant.entries.forEach { variant ->
            MonarchButtonSize.entries.forEach { size ->
                assertEquals(
                    typography.label.fontSize,
                    MonarchButtonTokens.textStyle(size, typography, variant).fontSize,
                )
            }
        }
    }

    @Test
    fun theLeadingIconIsAtTheStartInBothLayoutDirections() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) {
            MonarchButton(
                text = "Save",
                onClick = {},
                leadingIcon = { Box(Modifier.testTag("leading").width(16.dp)) },
            )
        }
        fun assertIconFirst(direction: LayoutDirection) {
            rule.onNodeWithTag("leading", useUnmergedTree = true)
                .assertIsBefore(rule.onNodeWithText("Save", useUnmergedTree = true), direction)
        }
        assertIconFirst(LayoutDirection.Ltr)
        environment.layoutDirection = LayoutDirection.Rtl
        rule.waitForIdle()
        assertIconFirst(LayoutDirection.Rtl)
    }

    @Test
    fun aOneWordLabelIsNotCutOffAtTheLargestFontSizeInARow() {
        // Two batches found the text overload laying out a single word 1px
        // narrower than the text at font scale 2.0 when the button sits in a row.
        rule.setMonarchContent(fontScale = LargestFontScale) {
            Row {
                MonarchButton(text = "Discard", onClick = {})
                MonarchButton(text = "Merge", onClick = {}, variant = MonarchButtonVariant.Primary)
            }
        }
        rule.onNodeWithText("Discard", useUnmergedTree = true).assertTextFullyVisible()
        rule.onNodeWithText("Merge", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun theLabelWrapsInsteadOfBeingCutOffAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchButton(
                text = "Save and continue",
                onClick = {},
                modifier = Modifier.testTag("button").width(160.dp),
            )
        }
        val label = rule.onNodeWithText("Save and continue", useUnmergedTree = true).assertTextFullyVisible()
        assertTrue(label.textLayout().lineCount > 1)
        assertEquals(androidx.compose.ui.text.style.TextAlign.Center, label.textLayout().layoutInput.style.textAlign)
        rule.onNodeWithTag("button").assertHeightIsAtLeast(MonarchButtonTokens.minHeight(MonarchButtonSize.Md))
    }

    @Test
    fun theContentOverloadKeepsTheButtonSemantics() {
        var clicks = 0
        rule.setMonarchContent {
            MonarchButton(onClick = { clicks++ }, variant = MonarchButtonVariant.Primary) {
                MonarchText(text = "Inbox")
                MonarchText(text = "3")
            }
        }
        rule.onNodeWithText("Inbox").assert(isButton).assertHasClickAction().performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun theLabelKeepsItsWidthInAHorizontallyScrollingRow() {
        // Regression: the label used Modifier.weight(1f, fill = false), which
        // gets no width when the row around the button has an unbounded width.
        rule.setMonarchContent(fontScale = LargestFontScale) {
            Row(Modifier.horizontalScroll(rememberScrollState())) {
                MonarchButton(text = "Archive", onClick = {})
                MonarchButton(
                    text = "Export",
                    onClick = {},
                    leadingIcon = { MonarchIcon(imageVector = TestIcon, contentDescription = null) },
                    trailingIcon = {
                        MonarchIcon(
                            imageVector = TestIcon,
                            contentDescription = null,
                            modifier = Modifier.testTag("trailing"),
                        )
                    },
                )
            }
        }
        listOf("Archive", "Export").forEach { label ->
            val node = rule.onNodeWithText(label, useUnmergedTree = true).assertTextFullyVisible()
            assertEquals(1, node.textLayout().lineCount, label)
            val bounds = node.getUnclippedBoundsInRoot()
            assertTrue(bounds.right - bounds.left > 0.dp, label)
        }
        rule.onNodeWithText("Export", useUnmergedTree = true)
            .assertIsBefore(rule.onNodeWithTag("trailing", useUnmergedTree = true), LayoutDirection.Ltr)
    }

    @Test
    fun aLongLabelWrapsAndKeepsTheTrailingIconVisibleInANarrowWidth() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchButton(
                text = "Save and continue",
                onClick = {},
                modifier = Modifier.testTag("button").width(160.dp),
                trailingIcon = {
                    MonarchIcon(
                        imageVector = TestIcon,
                        contentDescription = null,
                        modifier = Modifier.testTag("trailing"),
                    )
                },
            )
        }
        val label = rule.onNodeWithText("Save and continue", useUnmergedTree = true).assertTextFullyVisible()
        assertTrue(label.textLayout().lineCount > 1)
        val button = rule.onNodeWithTag("button").getUnclippedBoundsInRoot()
        val trailing = rule.onNodeWithTag("trailing", useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertTrue(trailing.right - trailing.left > 0.dp)
        assertTrue(trailing.right <= button.right, "The trailing icon is inside the button")
    }

    @Test
    fun theSuccessVariantUsesTheSuccessRoles() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val success = MonarchButtonTokens.colors(MonarchButtonVariant.Success, true, colors)
            assertEquals(colors.successSolid, success.container, theme.id)
            assertEquals(colors.successOnSolid, success.content, theme.id)
            assertNull(success.border, theme.id)
        }
    }

    @Test
    fun aCustomShapeKeepsTheButtonSemanticsAndTarget() {
        var clicks = 0
        rule.setMonarchContent {
            MonarchButton(
                text = "First",
                onClick = { clicks++ },
                modifier = Modifier.testTag("button"),
                shape = RoundedCornerShape(topStartPercent = 50, bottomStartPercent = 50),
            )
        }
        rule.onNodeWithTag("button").assert(isButton).assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun theDefaultVariantIsSecondary() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val secondary = MonarchButtonTokens.colors(MonarchButtonVariant.Secondary, true, colors)
            assertEquals(colors.bgSurface, secondary.container, theme.id)
            assertEquals(colors.borderDefault, secondary.border, theme.id)
        }
    }

    @Test
    fun variantsTakeTheirColoursFromTheThemeRoles() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val primary = MonarchButtonTokens.colors(MonarchButtonVariant.Primary, true, colors)
            assertEquals(colors.accentSolid, primary.container, theme.id)
            assertEquals(colors.accentOnSolid, primary.content, theme.id)
            val danger = MonarchButtonTokens.colors(MonarchButtonVariant.Danger, true, colors)
            assertEquals(colors.dangerSolid, danger.container, theme.id)
            assertEquals(colors.dangerOnSolid, danger.content, theme.id)
            val ghost = MonarchButtonTokens.colors(MonarchButtonVariant.Ghost, true, colors)
            assertEquals(Color.Transparent, ghost.container, theme.id)
            assertNull(ghost.border, theme.id)
        }
    }

    @Test
    fun aDisabledButtonIsMutedTextOnASubtleFillNotAFadedVariant() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            MonarchButtonVariant.entries.forEach { variant ->
                val look = MonarchButtonTokens.look(variant, colors)
                val flat = MonarchButtonTokens.colors(variant, false, colors)
                if (variant == MonarchButtonVariant.Primary) {
                    // The web's disabled primary is the accent paled; its text keeps the on-solid colour.
                    assertEquals(MonarchButtonTokens.disabledPrimaryFill(colors), flat.container, theme.id)
                    assertEquals(colors.accentOnSolid, flat.content, theme.id)
                    return@forEach
                }
                if (variant == MonarchButtonVariant.Danger) {
                    // Likewise the disabled danger button is the red paled; its text keeps the on-solid colour.
                    assertEquals(colors.dangerOnSolid, flat.content, theme.id)
                    return@forEach
                }
                assertEquals(colors.textDisabled, flat.content, "${theme.id} $variant")
                assertNotEquals(look.content, flat.content, "${theme.id} $variant")
                assertTrue(
                    flat.container == colors.bgMuted ||
                        flat.container == colors.bgSurface ||
                        flat.container == Color.Transparent,
                    "${theme.id} $variant",
                )
            }
        }
    }

    @Test
    fun theInverseAndAiVariantsUseTheirOwnRolesAndStepOnPress() {
        AllThemes.forEach { theme ->
            val c = MonarchColors(theme.colors)
            val inverse = MonarchButtonTokens.look(MonarchButtonVariant.Inverse, c)
            assertEquals(c.bgInverse, inverse.container, theme.id)
            assertEquals(c.textInverse, inverse.content, theme.id)
            assertNotEquals(inverse.container, inverse.hover, theme.id)
            assertNotEquals(inverse.hover, inverse.pressed, theme.id)
            val ai = MonarchButtonTokens.look(MonarchButtonVariant.Ai, c)
            assertEquals(c.aiSolid, ai.container, theme.id)
            assertEquals(c.aiOnSolid, ai.content, theme.id)
            assertEquals(c.aiSolidHover, ai.hover, theme.id)
            assertEquals(c.aiSolidActive, ai.pressed, theme.id)
        }
    }

    @Test
    fun noButtonDrawsAShadow() {
        assertFalse(MonarchButtonTokens.HasShadow)
    }

    @Test
    fun everyVariantHasHoverAndPressedFillsFromTheThemeRoles() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val primary = MonarchButtonTokens.look(MonarchButtonVariant.Primary, colors)
            assertEquals(colors.accentSolidHover, primary.hover, theme.id)
            assertEquals(colors.accentSolidActive, primary.pressed, theme.id)
            val secondary = MonarchButtonTokens.look(MonarchButtonVariant.Secondary, colors)
            assertEquals(colors.bgMuted, secondary.hover, theme.id)
            // Pressed is one surface step from the resting fill, not two.
            assertEquals(colors.bgMuted, secondary.pressed, theme.id)
            val subtle = MonarchButtonTokens.look(MonarchButtonVariant.DangerSubtle, colors)
            assertEquals(colors.dangerSubtle, subtle.container, theme.id)
            assertEquals(colors.dangerStrong, subtle.content, theme.id)
            assertEquals(colors.dangerBorder, subtle.border, theme.id)
            val warning = MonarchButtonTokens.look(MonarchButtonVariant.Warning, colors)
            assertEquals(colors.warningSolid, warning.container, theme.id)
            assertEquals(colors.warningOnSolid, warning.content, theme.id)
            val link = MonarchButtonTokens.look(MonarchButtonVariant.Link, colors)
            assertEquals(colors.textLink, link.content, theme.id)
            assertEquals(Color.Transparent, link.container, theme.id)
        }
    }

    @Test
    fun aTapInTheReservedSpaceAroundASmallButtonPressesIt() {
        var clicks = 0
        rule.setMonarchContent {
            MonarchButton(
                text = "Save",
                onClick = { clicks++ },
                size = MonarchButtonSize.Sm,
                modifier = Modifier.testTag("button"),
            )
        }
        // 32dp visible in 48dp: 4dp from the top edge of the reserved space is outside the visible button.
        rule.onNodeWithTag("button").performTouchInput { click(Offset(width / 2f, 4.dp.toPx())) }
        assertEquals(1, clicks)
    }

    @Test
    fun aLoadingButtonKeepsItsSize() {
        var loading by mutableStateOf(false)
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent {
            Column {
                MonarchButton(
                    text = "Save changes",
                    onClick = {},
                    loading = loading,
                    modifier = Modifier.testTag("plain"),
                )
                MonarchButton(
                    text = "Upload",
                    onClick = {},
                    loading = loading,
                    leadingIcon = { MonarchIcon(imageVector = TestIcon, contentDescription = null) },
                    modifier = Modifier.testTag("withIcon"),
                )
            }
        }
        rule.mainClock.advanceTimeByFrame()
        val before = listOf("plain", "withIcon").map { rule.onNodeWithTag(it).fetchSemanticsNode().size }
        loading = true
        rule.mainClock.advanceTimeByFrame()
        val after = listOf("plain", "withIcon").map { rule.onNodeWithTag(it).fetchSemanticsNode().size }
        assertEquals(before, after)
    }

    @Test
    fun aButtonWithAFocusInteractionStaysAButton() {
        val source = MutableInteractionSource()
        rule.setMonarchContent { MonarchButton(text = "Save", onClick = {}, interactionSource = source) }
        rule.onNodeWithText("Save").assert(isButton).assertHasClickAction()
    }
}
