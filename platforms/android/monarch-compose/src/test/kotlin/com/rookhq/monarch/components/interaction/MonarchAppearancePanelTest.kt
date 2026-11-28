package com.rookhq.monarch.components.interaction

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.components.interaction.appearancepanel.MonarchAppearanceDensity
import com.rookhq.monarch.components.interaction.appearancepanel.MonarchAppearanceMotion
import com.rookhq.monarch.components.interaction.appearancepanel.MonarchAppearancePanel
import com.rookhq.monarch.components.interaction.appearancepanel.MonarchAppearancePanelLabels
import com.rookhq.monarch.components.interaction.appearancepanel.MonarchAppearancePanelRow
import com.rookhq.monarch.components.interaction.appearancepanel.MonarchAppearancePanelTokens
import com.rookhq.monarch.components.interaction.appearancepanel.MonarchAppearancePreferences
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.assertIsBefore
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.theme.MonarchContrast
import com.rookhq.monarch.theme.MonarchScheme
import com.rookhq.monarch.tokens.MonarchTokens
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals

private val Rook = MonarchTokens.Accents.rook

internal object MonarchAppearancePanelFixtures : ComponentFixtures {
    override val id = "appearance-panel"
    override val fixtures = listOf(
        fixture("theme and contrast, titled") {
            MonarchAppearancePanel(
                preferences = MonarchAppearancePreferences(Rook),
                onPreferencesChange = {},
                title = "Appearance",
                rows = listOf(MonarchAppearancePanelRow.Theme, MonarchAppearancePanelRow.Contrast),
            )
        },
        fixture("motion and density, overridden") {
            MonarchAppearancePanel(
                preferences = MonarchAppearancePreferences(
                    Rook,
                    motion = MonarchAppearanceMotion.Reduce,
                    density = MonarchAppearanceDensity.Compact,
                ),
                onPreferencesChange = {},
                rows = listOf(MonarchAppearancePanelRow.Motion, MonarchAppearancePanelRow.Density),
            )
        },
        fixture("accent") {
            MonarchAppearancePanel(
                preferences = MonarchAppearancePreferences(accent = MonarchTokens.Accents.atlas),
                onPreferencesChange = {},
                rows = listOf(MonarchAppearancePanelRow.Accent),
            )
        },
        fixture("disabled") {
            MonarchAppearancePanel(
                preferences = MonarchAppearancePreferences(Rook),
                onPreferencesChange = {},
                rows = listOf(MonarchAppearancePanelRow.Contrast),
                enabled = false,
            )
        },
        fixture("no rows") {
            MonarchAppearancePanel(
                preferences = MonarchAppearancePreferences(Rook),
                onPreferencesChange = {},
                title = "Appearance",
                rows = emptyList(),
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchAppearancePanelTest {
    @get:Rule
    val rule = createComposeRule()

    private val tokens = MonarchAppearancePanelTokens

    @Test
    fun eachRowChangesItsOwnChoice() {
        var preferences by mutableStateOf(MonarchAppearancePreferences(Rook))
        var rows by mutableStateOf(listOf(MonarchAppearancePanelRow.Theme, MonarchAppearancePanelRow.Contrast))
        rule.setMonarchContent {
            MonarchAppearancePanel(preferences = preferences, onPreferencesChange = { preferences = it }, rows = rows)
        }
        rule.onNodeWithText("Dark").performClick()
        rule.onNode(hasText("More") and hasAnyAncestor(hasContentDescription("Contrast"))).performClick()
        // The window is short: show the other two rows on their own.
        rows = listOf(MonarchAppearancePanelRow.Motion, MonarchAppearancePanelRow.Density)
        rule.onNodeWithText("Reduce").performClick()
        rule.onNodeWithText("Compact").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(
            MonarchAppearancePreferences(
                Rook,
                scheme = MonarchScheme.Dark,
                contrast = MonarchContrast.More,
                motion = MonarchAppearanceMotion.Reduce,
                density = MonarchAppearanceDensity.Compact,
            ),
            preferences,
        )
        rule.onNodeWithText("Compact").assertIsSelected()
    }

    @Test
    fun systemIsTheDefaultAndSelected() {
        rule.setMonarchContent {
            MonarchAppearancePanel(
                preferences = MonarchAppearancePreferences(Rook),
                onPreferencesChange = {},
                rows = listOf(MonarchAppearancePanelRow.Contrast),
            )
        }
        rule.onNode(hasText("System") and isSelectable()).assertIsSelected()
        rule.onNodeWithContentDescription("Contrast").assertExists()
        rule.onNodeWithText("Stronger text and border colours.", useUnmergedTree = true).assertExists()
    }

    @Test
    fun theAccentIsChosenByName() {
        rule.setMonarchContent {
            MonarchAppearancePanel(
                preferences = MonarchAppearancePreferences(accent = MonarchTokens.Accents.atlas),
                onPreferencesChange = {},
                rows = listOf(MonarchAppearancePanelRow.Accent),
                accents = listOf(MonarchTokens.Accents.rook),
            )
        }
        rule.onNodeWithText(MonarchTokens.Accents.atlas.label, useUnmergedTree = true).assertExists()
    }

    @Test
    fun disabledRowsCannotChange() {
        rule.setMonarchContent {
            MonarchAppearancePanel(
                preferences = MonarchAppearancePreferences(Rook),
                onPreferencesChange = {},
                rows = listOf(MonarchAppearancePanelRow.Density),
                enabled = false,
            )
        }
        rule.onNodeWithText("Compact").assertIsNotEnabled()
    }

    @Test
    fun theStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchAppearancePanel(
                preferences = MonarchAppearancePreferences(Rook),
                onPreferencesChange = {},
                rows = listOf(MonarchAppearancePanelRow.Theme, MonarchAppearancePanelRow.Motion),
                labels = MonarchAppearancePanelLabels(theme = "Tema", dark = "Oscuro", motionReduce = "Reducir"),
            )
        }
        rule.onNodeWithText("Tema", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Oscuro").assertExists()
        rule.onNodeWithText("Reducir").assertExists()
    }

    @Test
    fun theSegmentsFollowTheReadingOrderRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchAppearancePanel(
                preferences = MonarchAppearancePreferences(Rook),
                onPreferencesChange = {},
                rows = listOf(MonarchAppearancePanelRow.Density),
            )
        }
        rule.onNodeWithText("Comfortable").assertIsBefore(rule.onNodeWithText("Compact"), LayoutDirection.Rtl)
    }

    @Test
    fun descriptionsWrapAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchAppearancePanel(
                preferences = MonarchAppearancePreferences(Rook),
                onPreferencesChange = {},
                rows = listOf(MonarchAppearancePanelRow.Density),
            )
        }
        rule.onNodeWithText("Tighter spacing fits more on screen.", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun rowsAccentsAndMotionLogic() {
        val theme = MonarchAppearancePanelRow.Theme
        val density = MonarchAppearancePanelRow.Density
        assertEquals(listOf(theme, density), tokens.visibleRows(listOf(theme, theme, density)))
        val rook = MonarchTokens.Accents.rook
        val atlas = MonarchTokens.Accents.atlas
        assertEquals(listOf(rook, atlas), tokens.accentChoices(listOf(rook, rook), atlas))
        assertEquals(listOf(rook), tokens.accentChoices(listOf(rook), rook))
        val reduce = MonarchAppearancePreferences(Rook, motion = MonarchAppearanceMotion.Reduce)
        assertEquals(true, reduce.reducesMotion(systemReducesMotion = false))
        assertEquals(true, MonarchAppearancePreferences(Rook).reducesMotion(systemReducesMotion = true))
        assertEquals(false, MonarchAppearancePreferences(Rook).reducesMotion(systemReducesMotion = false))
        assertEquals(MonarchScheme.System, MonarchAppearancePreferences(Rook).scheme)
    }

    @Test
    fun theTextColoursComeFromTheRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val c = MonarchColors(theme.colors)
            assertEquals(c.textDefault, tokens.label(c), theme.id)
            assertEquals(c.textMuted, tokens.description(c), theme.id)
        }
    }
}
