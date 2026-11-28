package dev.monarchui.components.intelligence

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.intelligence.aimodelpicker.MonarchAiModelCapability
import dev.monarchui.components.intelligence.aimodelpicker.MonarchAiModelOption
import dev.monarchui.components.intelligence.aimodelpicker.MonarchAiModelPicker
import dev.monarchui.components.intelligence.aimodelpicker.MonarchAiModelPickerLabels
import dev.monarchui.components.intelligence.aimodelpicker.MonarchAiModelPickerList
import dev.monarchui.components.intelligence.aimodelpicker.MonarchAiModelPickerOptionState
import dev.monarchui.components.intelligence.aimodelpicker.MonarchAiModelPickerTokens
import dev.monarchui.components.intelligence.aimodelpicker.MonarchAiModelSpeed
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.TestIcon
import dev.monarchui.testing.assertIsBefore
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.testing.stateDescription
import dev.monarchui.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private val Large = MonarchAiModelOption(
    id = "large",
    provider = "Rook",
    name = "Rook Large",
    description = "Best for long documents",
    capabilities = listOf(MonarchAiModelCapability.Vision, MonarchAiModelCapability.Tools),
    contextTokens = 200_000,
    speed = MonarchAiModelSpeed.Balanced,
    costTier = 3,
)
private val Fast = MonarchAiModelOption(id = "fast", provider = "Rook", name = "Rook Fast", costTier = 1)
private val Hosted = MonarchAiModelOption(id = "hosted", provider = "Workspace", name = "Hosted", requiresKey = true)
private val Own = MonarchAiModelOption(
    id = "own",
    provider = "Workspace",
    name = "Own",
    requiresKey = true,
    keyConfigured = true,
)
private val Legacy = MonarchAiModelOption(id = "legacy", provider = "Workspace", name = "Legacy", enabled = false)
private val Models = listOf(Large, Fast, Hosted, Own, Legacy)

internal object MonarchAiModelPickerFixtures : ComponentFixtures {
    override val id = "ai-model-picker"
    override val fixtures = listOf(
        fixture("selected, both sizes, with an icon") {
            MonarchAiModelPicker(
                selection = "large",
                models = Models,
                onSelectionChange = {},
                trailingIcon = { MonarchIcon(TestIcon, contentDescription = null) },
            )
            MonarchAiModelPicker(
                selection = "fast",
                models = Models,
                onSelectionChange = {},
                size = MonarchButtonSize.Sm,
            )
        },
        fixture("placeholder and disabled") {
            MonarchAiModelPicker(selection = null, models = Models, onSelectionChange = {})
            MonarchAiModelPicker(selection = "large", models = Models, onSelectionChange = {}, enabled = false)
        },
        fixture("list with every detail") {
            MonarchAiModelPickerList(
                selection = "large",
                models = listOf(Large),
                canConfigure = true,
                labels = MonarchAiModelPickerLabels(),
                onChoose = { _, _ -> },
            )
        },
        fixture("list with keys and an unavailable model") {
            MonarchAiModelPickerList(
                selection = null,
                models = listOf(Hosted, Own, Legacy),
                canConfigure = false,
                labels = MonarchAiModelPickerLabels(),
                onChoose = { _, _ -> },
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchAiModelPickerTest {
    @get:Rule
    val rule = createComposeRule()

    private fun row(name: String) =
        rule.onNode(
            hasText(name, substring = true) and
                SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton),
        )

    @Test
    fun theButtonIsNamedModelWithTheSelectionAsItsValue() {
        rule.setMonarchContent { MonarchAiModelPicker(selection = "large", models = Models, onSelectionChange = {}) }
        val button = rule.onNodeWithContentDescription("Model")
        button.assert(hasClickAction()).assertTouchTargetAtLeast48dp(rule.density)
        assertEquals("Rook Large, Rook", button.stateDescription())
    }

    @Test
    fun anUnknownSelectionShowsThePlaceholder() {
        rule.setMonarchContent { MonarchAiModelPicker(selection = "gone", models = Models, onSelectionChange = {}) }
        assertEquals("Choose a model", rule.onNodeWithContentDescription("Model").stateDescription())
    }

    @Test
    fun thePanelListsModelsUnderTheirProviders() {
        rule.setMonarchContent { MonarchAiModelPicker(selection = "large", models = Models, onSelectionChange = {}) }
        rule.onNodeWithContentDescription("Model").performClick()
        rule.waitForIdle()
        rule.onNode(hasText("Rook") and isHeading()).assertExists()
        rule.onNode(hasText("Workspace") and isHeading()).assertExists()
        row("Rook Large").assertIsSelected().assertTouchTargetAtLeast48dp(rule.density)
        row("Rook Fast").assertIsNotSelected()
    }

    @Test
    fun aRowReadsItsDetailsAndTheCostInWords() {
        rule.setMonarchContent { MonarchAiModelPicker(selection = null, models = Models, onSelectionChange = {}) }
        rule.onNodeWithContentDescription("Model").performClick()
        rule.waitForIdle()
        row("Rook Large")
            .assert(hasText("Best for long documents", substring = true))
            .assert(hasText("Vision", substring = true))
            .assert(hasText("200K context · Balanced", substring = true))
            .assert(hasContentDescription("Cost: high", substring = true))
    }

    @Test
    fun choosingAModelReportsItAndClosesThePanel() {
        var chosen: String? = null
        rule.setMonarchContent {
            MonarchAiModelPicker(selection = "large", models = Models, onSelectionChange = { chosen = it })
        }
        rule.onNodeWithContentDescription("Model").performClick()
        rule.waitForIdle()
        row("Rook Fast").performClick()
        rule.waitForIdle()
        assertEquals("fast", chosen)
        rule.onNode(hasText("Rook") and isHeading()).assertDoesNotExist()
    }

    @Test
    fun aModelWithoutItsKeyAsksForTheKey() {
        var chosen: String? = null
        var configure: MonarchAiModelOption? = null
        rule.setMonarchContent {
            MonarchAiModelPicker(
                selection = null,
                models = Models,
                onSelectionChange = { chosen = it },
                onConfigureKey = { configure = it },
            )
        }
        rule.onNodeWithContentDescription("Model").performClick()
        rule.waitForIdle()
        row("Hosted").assert(hasText("Key needed", substring = true))
            .assert(hasContentDescription("Select to add an API key.", substring = true))
            .performClick()
        rule.waitForIdle()
        assertEquals(Hosted, configure)
        assertNull(chosen)
    }

    @Test
    fun withoutConfigureKeyAndWhenDisabledARowCannotBeChosen() {
        var chosen: String? = null
        rule.setMonarchContent {
            MonarchAiModelPicker(selection = null, models = Models, onSelectionChange = { chosen = it })
        }
        rule.onNodeWithContentDescription("Model").performClick()
        rule.waitForIdle()
        row("Hosted").assertIsNotEnabled().assert(hasContentDescription("Unavailable", substring = true))
        row("Legacy").assertIsNotEnabled()
        row("Own").assert(hasText("Your key", substring = true))
        row("Legacy").performClick()
        assertNull(chosen)
    }

    @Test
    fun aDisabledButtonDoesNotOpen() {
        rule.setMonarchContent {
            MonarchAiModelPicker(selection = null, models = Models, onSelectionChange = {}, enabled = false)
        }
        rule.onNodeWithContentDescription("Model").assertIsNotEnabled()
    }

    @Test
    fun theBuiltInStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchAiModelPicker(
                selection = null,
                models = listOf(Large),
                onSelectionChange = {},
                labels = MonarchAiModelPickerLabels(
                    label = "Modelo",
                    placeholder = "Elige un modelo",
                    capability = { if (it == MonarchAiModelCapability.Vision) "Visión" else null },
                    context = { "contexto $it" },
                    speed = { "Equilibrado" },
                    cost = { "Coste $it" },
                ),
            )
        }
        assertEquals("Elige un modelo", rule.onNodeWithContentDescription("Modelo").stateDescription())
        rule.onNodeWithContentDescription("Modelo").performClick()
        rule.waitForIdle()
        row("Rook Large")
            .assert(hasText("Visión", substring = true))
            .assert(hasText("Tools", substring = true))
            .assert(hasText("contexto 200K · Equilibrado", substring = true))
            .assert(hasContentDescription("Coste 3", substring = true))
    }

    @Test
    fun theProviderFollowsTheNameRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchAiModelPicker(selection = "large", models = Models, onSelectionChange = {})
        }
        rule.onNodeWithText("Rook Large", useUnmergedTree = true)
            .assertIsBefore(rule.onNodeWithText("Rook", useUnmergedTree = true), LayoutDirection.Rtl)
    }

    @Test
    fun textFitsAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchAiModelPickerList(
                selection = null,
                models = listOf(Large),
                canConfigure = false,
                labels = MonarchAiModelPickerLabels(),
                onChoose = { _, _ -> },
            )
        }
        rule.onNodeWithText("Best for long documents", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun theRulesOfTheList() {
        val t = MonarchAiModelPickerTokens
        assertEquals(listOf("Rook", "Workspace"), t.groups(Models).map { it.first })
        assertEquals(MonarchAiModelPickerOptionState.Ok, t.state(Large, canConfigure = false))
        assertEquals(MonarchAiModelPickerOptionState.Ok, t.state(Own, canConfigure = false))
        assertEquals(MonarchAiModelPickerOptionState.Key, t.state(Hosted, canConfigure = true))
        assertEquals(MonarchAiModelPickerOptionState.Blocked, t.state(Hosted, canConfigure = false))
        assertEquals(MonarchAiModelPickerOptionState.Blocked, t.state(Legacy, canConfigure = true))
        assertEquals(3, t.tier(MonarchAiModelOption("x", "p", "n", costTier = 9)))
        assertEquals(1, t.tier(MonarchAiModelOption("x", "p", "n", costTier = 0)))
        assertNull(t.tier(Own))
    }

    @Test
    fun contextSizesAreCompactWithOneDecimal() {
        val t = MonarchAiModelPickerTokens
        assertEquals("200K", t.compact(200_000, Locale.US))
        assertEquals("32.8K", t.compact(32_768, Locale.US))
        assertEquals("1M", t.compact(1_000_000, Locale.US))
        assertEquals("1.5M", t.compact(1_500_000, Locale.US))
        assertEquals("950", t.compact(950, Locale.US))
    }

    @Test
    fun theColoursComeFromTheTheme() {
        val t = MonarchAiModelPickerTokens
        AllThemes.forEach { theme ->
            val c = MonarchColors(theme.colors)
            assertEquals(c.textDefault, t.nameColor(MonarchAiModelPickerOptionState.Ok, c), theme.id)
            assertEquals(c.textDisabled, t.nameColor(MonarchAiModelPickerOptionState.Blocked, c), theme.id)
            assertEquals(c.textMuted, t.detailColor(MonarchAiModelPickerOptionState.Key, c), theme.id)
            assertEquals(c.textDisabled, t.detailColor(MonarchAiModelPickerOptionState.Blocked, c), theme.id)
            assertEquals(c.textMuted, t.mutedColor(enabled = true, c), theme.id)
            assertEquals(c.textDisabled, t.mutedColor(enabled = false, c), theme.id)
        }
    }
}
