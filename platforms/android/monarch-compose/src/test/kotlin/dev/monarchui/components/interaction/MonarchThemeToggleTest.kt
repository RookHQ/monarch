package dev.monarchui.components.interaction

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.width
import dev.monarchui.components.interaction.themetoggle.MonarchThemeToggle
import dev.monarchui.components.interaction.themetoggle.MonarchThemeToggleLabels
import dev.monarchui.components.interaction.themetoggle.MonarchThemeToggleSize
import dev.monarchui.components.interaction.themetoggle.MonarchThemeToggleStorage
import dev.monarchui.components.interaction.themetoggle.MonarchThemeToggleVariant
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
import dev.monarchui.testing.stateDescription
import dev.monarchui.theme.MonarchScheme
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal object MonarchThemeToggleFixtures : ComponentFixtures {
    override val id = "theme-toggle"
    override val fixtures = listOf(
        fixture("segmented, every choice") {
            MonarchScheme.entries.forEach { MonarchThemeToggle(scheme = it, onSchemeChange = {}) }
        },
        fixture("segmented, small") {
            MonarchThemeToggle(scheme = MonarchScheme.Dark, onSchemeChange = {}, size = MonarchThemeToggleSize.Sm)
        },
        fixture("segmented, disabled") {
            MonarchThemeToggle(scheme = MonarchScheme.Light, onSchemeChange = {}, enabled = false)
        },
        fixture("menu, both sizes") {
            MonarchThemeToggleSize.entries.forEach {
                MonarchThemeToggle(
                    scheme = MonarchScheme.System,
                    onSchemeChange = {},
                    variant = MonarchThemeToggleVariant.Menu,
                    size = it,
                )
            }
        },
        fixture("menu, icon only") {
            MonarchThemeToggle(
                scheme = MonarchScheme.Dark,
                onSchemeChange = {},
                variant = MonarchThemeToggleVariant.Menu,
                showLabel = false,
                icon = { MonarchIcon(imageVector = TestIcon, contentDescription = null) },
            )
        },
        fixture("menu, disabled") {
            MonarchThemeToggle(
                scheme = MonarchScheme.Light,
                onSchemeChange = {},
                variant = MonarchThemeToggleVariant.Menu,
                enabled = false,
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchThemeToggleTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theSegmentsNameEveryChoiceAndSelectTheCurrentOne() {
        rule.setMonarchContent { MonarchThemeToggle(scheme = MonarchScheme.Dark, onSchemeChange = {}) }
        rule.onNodeWithContentDescription("Theme").assertExists()
        rule.onNode(hasText("Light") and isRadio).assertIsNotSelected()
        rule.onNode(hasText("Dark") and isRadio).assertIsSelected()
        rule.onNode(hasText("System") and isRadio).assertIsNotSelected()
    }

    @Test
    fun choosingASegmentReportsTheNewScheme() {
        val chosen = mutableListOf<MonarchScheme>()
        rule.setMonarchContent { MonarchThemeToggle(scheme = MonarchScheme.Light, onSchemeChange = { chosen += it }) }
        rule.onNodeWithText("System").performClick()
        rule.onNodeWithText("Light").performClick()
        assertEquals(listOf(MonarchScheme.System), chosen)
    }

    @Test
    fun aDisabledToggleReportsNothing() {
        val chosen = mutableListOf<MonarchScheme>()
        rule.setMonarchContent {
            MonarchThemeToggle(scheme = MonarchScheme.Light, onSchemeChange = { chosen += it }, enabled = false)
        }
        rule.onNode(hasText("Dark") and isRadio).assertIsNotEnabled().performClick()
        assertEquals(emptyList(), chosen)
    }

    @Test
    fun theSegmentsHave48dpTargetsAndFollowTheReadingOrder() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) { MonarchThemeToggle(scheme = MonarchScheme.Light, onSchemeChange = {}) }
        for (direction in LayoutDirection.entries) {
            environment.layoutDirection = direction
            rule.waitForIdle()
            val light = rule.onNode(hasText("Light") and isRadio)
            val system = rule.onNode(hasText("System") and isRadio)
            light.assertTouchTargetAtLeast48dp(rule.density)
            system.assertTouchTargetAtLeast48dp(rule.density)
            light.assertIsBefore(system, direction)
        }
    }

    @Test
    fun theNamesStayWholeAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchThemeToggle(scheme = MonarchScheme.System, onSchemeChange = {})
        }
        listOf("Light", "Dark", "System").forEach {
            rule.onNodeWithText(it, useUnmergedTree = true).assertTextFullyVisible()
        }
    }

    @Test
    fun theMenuButtonIsNamedThemeWithTheChoiceAsItsState() {
        rule.setMonarchContent {
            MonarchThemeToggle(
                scheme = MonarchScheme.Dark,
                onSchemeChange = {},
                variant = MonarchThemeToggleVariant.Menu,
                showLabel = false,
                icon = { MonarchIcon(imageVector = TestIcon, contentDescription = null) },
            )
        }
        val button = rule.onNodeWithContentDescription("Theme")
        assertEquals("Dark", button.stateDescription())
        button.assertTouchTargetAtLeast48dp(rule.density)
        rule.onNodeWithText("Dark").assertDoesNotExist()
    }

    @Test
    fun theMenuButtonShowsTheNameWithoutAnIcon() {
        rule.setMonarchContent {
            Column {
                listOf(true, false).forEach { show ->
                    MonarchThemeToggle(
                        scheme = MonarchScheme.System,
                        onSchemeChange = {},
                        modifier = Modifier.testTag("name $show"),
                        variant = MonarchThemeToggleVariant.Menu,
                        showLabel = show,
                    )
                }
                MonarchThemeToggle(
                    scheme = MonarchScheme.System,
                    onSchemeChange = {},
                    modifier = Modifier.testTag("icon"),
                    variant = MonarchThemeToggleVariant.Menu,
                    showLabel = false,
                    icon = { MonarchIcon(imageVector = TestIcon, contentDescription = null) },
                )
            }
        }
        val shown = rule.onNodeWithTag("name true").getUnclippedBoundsInRoot().width
        assertEquals(shown, rule.onNodeWithTag("name false").getUnclippedBoundsInRoot().width)
        assertTrue(rule.onNodeWithTag("icon").getUnclippedBoundsInRoot().width < shown)
    }

    @Test
    fun theMenuOffersTheThreeChoicesAsRadioItemsAndClosesOnAChoice() {
        val chosen = mutableListOf<MonarchScheme>()
        rule.setMonarchContent {
            MonarchThemeToggle(
                scheme = MonarchScheme.Light,
                onSchemeChange = { chosen += it },
                variant = MonarchThemeToggleVariant.Menu,
            )
        }
        rule.onNodeWithContentDescription("Theme").performClick()
        rule.onNode(hasText("Light") and isRadio).assertIsSelected()
        rule.onNode(hasText("Dark") and isRadio).assertIsNotSelected()
        rule.onNode(hasText("Theme") and isHeading).assertExists()
        rule.onNode(hasText("Dark") and isRadio).performClick()
        assertEquals(listOf(MonarchScheme.Dark), chosen)
        rule.onNode(hasText("Dark") and isRadio).assertDoesNotExist()
    }

    @Test
    fun theBuiltInStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchThemeToggle(
                scheme = MonarchScheme.System,
                onSchemeChange = {},
                labels = MonarchThemeToggleLabels(
                    title = "Thème",
                    light = "Clair",
                    dark = "Sombre",
                    system = "Système",
                ),
            )
        }
        rule.onNodeWithContentDescription("Thème").assertExists()
        listOf("Clair", "Sombre").forEach { rule.onNode(hasText(it) and isRadio).assertIsNotSelected() }
        rule.onNode(hasText("Système") and isRadio).assertIsSelected()
    }

    @Test
    fun theStoredValuesMatchTheWebAndIos() {
        val stored = MonarchScheme.entries.map(MonarchThemeToggleStorage::storageValue)
        assertEquals(listOf("system", "light", "dark"), stored)
        MonarchScheme.entries.forEach {
            assertEquals(it, MonarchThemeToggleStorage.scheme(MonarchThemeToggleStorage.storageValue(it)))
        }
        assertNull(MonarchThemeToggleStorage.scheme(null))
        assertNull(MonarchThemeToggleStorage.scheme("Dark"))
    }

    private val isRadio = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton)
    private val isHeading = SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)
}
