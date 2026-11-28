package dev.monarchui.testing

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.core.app.ApplicationProvider
import dev.monarchui.generated.AllComponentFixtures
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Shows every fixture of every component in the four themes, each at the
 * default font size, at the largest font size and right to left.
 *
 * The fixtures are not listed here. Gradle finds every
 * `object … : ComponentFixtures` in the test sources and generates
 * `AllComponentFixtures`. One test per inventory id; inside it the settings
 * are changed as state, so twelve renders cost one test set-up.
 *
 * For every render it checks that composition did not fail, that something
 * was laid out, and that every enabled element that takes a tap has a touch
 * target of at least 48dp: both the area that receives taps and the space
 * reserved in the layout. A primitive that opted in through [StrictMatrix]
 * is also checked at font scale 2.0 for text that is cut off, ellipsized or
 * clipped, and in every theme (dark and high contrast included) for text
 * contrast on the rendered pixels. See [MatrixAxes].
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
class ComponentMatrixTest(private val id: String) {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun everyFixtureRendersInEveryThemeAndMode() {
        if (id == NoFixtures) return
        val fixtures = AllComponentFixtures.filter { it.id == id }.flatMap { it.fixtures }
        assertTrue(fixtures.isNotEmpty(), "$id: the fixtures object has no fixtures")
        assertEquals(fixtures.size, fixtures.map { it.name }.toSet().size, "$id: fixture names must be unique")
        val phone = fixtures.filterNot { it.tall }
        if (phone.isNotEmpty()) ComponentMatrix(rule).run(id, phone)
    }

    companion object {
        private const val NoFixtures = "(no fixtures selected)"

        /** The inventory ids that have fixtures; with `-Pcomponent=…` only the selected ones. */
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun ids(): List<Array<String>> = matrixIds { true }
    }
}

/**
 * The tall matrix window: the fixtures marked `tall`, in a window of 320 by
 * 960dp, with the same checks as [ComponentMatrixTest]. For components that
 * are taller than a phone screen at font scale 2.0, so that every part is
 * measured instead of being cut off by the 470dp window.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(qualifiers = "+w320dp-h960dp")
class TallComponentMatrixTest(private val id: String) {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun everyTallFixtureRendersInEveryThemeAndMode() {
        val tall = AllComponentFixtures.filter { it.id == id }.flatMap { it.fixtures }.filter { it.tall }
        ComponentMatrix(rule).run(id, tall)
    }

    @Test
    fun theWindowIsTall() {
        val window = ApplicationProvider.getApplicationContext<Context>().resources.configuration
        assertEquals(TallWindowHeight, window.screenHeightDp)
    }

    companion object {
        private const val TallWindowHeight = 960

        /** The inventory ids with tall fixtures; with `-Pcomponent=…` only the selected ones. */
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun ids(): List<Array<String>> = matrixIds { set -> set.fixtures.any { it.tall } }
    }
}

/** The inventory ids whose fixture sets match [filter]; with `-Pcomponent=…` only the selected ones. */
private fun matrixIds(filter: (ComponentFixtures) -> Boolean): List<Array<String>> {
    val all = AllComponentFixtures.filter(filter).map { it.id }.distinct().sorted()
    val selected = SelectedComponent.ids?.let { ids -> all.filter { it in ids } } ?: all
    return selected.map { arrayOf(it) }.ifEmpty { listOf(arrayOf("(no fixtures selected)")) }
}

/** Renders fixtures in every theme and mode and checks each render. */
private class ComponentMatrix(private val rule: ComposeContentTestRule) {
    private lateinit var checker: MatrixChecker
    private var axes = MatrixAxes()

    private class Mode(val name: String, val fontScale: Float, val layoutDirection: LayoutDirection)

    private val modes = listOf(
        Mode("default", 1f, LayoutDirection.Ltr),
        Mode("font scale 2.0", LargestFontScale, LayoutDirection.Ltr),
        Mode("right to left", 1f, LayoutDirection.Rtl),
    )

    fun run(id: String, fixtures: List<ComponentFixture>) {
        if (fixtures.isEmpty()) return
        axes = StrictMatrix.axesOf(id)
        checker = MatrixChecker(rule, FrameTag)
        val environment = MonarchTestEnvironment()
        var current by mutableStateOf(fixtures.first())
        // Indeterminate indicators animate forever, so the clock is advanced by hand.
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent(environment) {
            val view = LocalView.current
            SideEffect { checker.host = view }
            Box(Modifier.testTag(FrameTag)) {
                key(current, environment.fontScale, environment.layoutDirection) { current.content() }
            }
        }
        for (fixture in fixtures) {
            for (theme in AllThemes) {
                for (mode in modes) {
                    current = fixture
                    environment.tokens = theme
                    environment.fontScale = mode.fontScale
                    environment.layoutDirection = mode.layoutDirection
                    rule.mainClock.advanceTimeByFrame()
                    check("$id, fixture \"${fixture.name}\", theme ${theme.id}, ${mode.name}", mode.fontScale)
                }
            }
        }
    }

    private fun check(where: String, fontScale: Float) {
        val frame = rule.onNodeWithTag(FrameTag).fetchSemanticsNode()
        val roots = rule.onAllNodes(isRoot()).fetchSemanticsNodes()
        // A dialog, sheet or menu is laid out in a window of its own, so the frame may be empty then.
        if (frame.size.width == 0 || frame.size.height == 0) {
            assertTrue(roots.size > 1, "$where: nothing was laid out")
        }
        if (axes.touchTarget) checker.touchTargets(where)
        if (axes.fontScaleNoClipping && fontScale >= LargestFontScale) checker.textNotCut(where)
        if (axes.textContrast) checker.textContrast(where)
    }

    private companion object {
        const val FrameTag = "monarch-fixture-frame"
    }
}
