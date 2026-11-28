package dev.monarchui.testing

import androidx.compose.runtime.Composable

/**
 * One state of a component, shown on its own by `ComponentMatrixTest`.
 *
 * @property name what the fixture shows, for the failure message: "disabled", "long label".
 * @property content the component in that state. No theme, no scrolling: the test adds them.
 * @property tall shows the fixture in the tall matrix window (320 by 960dp, `TallComponentMatrixTest`) instead
 * of the phone window (320 by 470dp), for a component that is taller than a phone screen at font scale 2.0 and
 * is normally placed in a scrolling screen. Everything in it is then measured, nothing is cut off.
 */
internal class ComponentFixture(val name: String, val tall: Boolean = false, val content: @Composable () -> Unit)

/**
 * The fixtures of one inventory entry.
 *
 * Declare an `object <Name>Fixtures : ComponentFixtures` in the component's
 * test file. Do not register it anywhere: Gradle finds every such object under
 * `src/test` and lists it in the generated `AllComponentFixtures`.
 * `ComponentMatrixTest` then shows each fixture in the four themes, at the
 * default and the largest font size, left to right and right to left.
 *
 * Cover every variant, size, tone and state (disabled, loading, error, empty,
 * long text). Several small fixtures are better than one large one, because
 * the failure message names the fixture.
 */
internal interface ComponentFixtures {
    /** The inventory id: the file name of `inventory/<id>.json`. */
    val id: String

    /** The fixtures. */
    val fixtures: List<ComponentFixture>
}

/** Shorthand for a [ComponentFixture]. */
internal fun fixture(name: String, content: @Composable () -> Unit): ComponentFixture =
    ComponentFixture(name, tall = false, content = content)

/** Shorthand for a [ComponentFixture] shown in the tall matrix window when [tall] is `true`. */
internal fun fixture(name: String, tall: Boolean, content: @Composable () -> Unit): ComponentFixture =
    ComponentFixture(name, tall, content)
