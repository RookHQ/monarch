package com.rookhq.monarch.testing

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.reflect.asComposableMethod
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import java.io.File
import java.lang.reflect.Modifier.isStatic
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Composes every `@Preview` function, so a preview cannot rot unnoticed.
 *
 * The previews are not listed here: the test finds every `*Previews.kt` file
 * under `src/debug` and calls the functions in it whose name ends with
 * `Preview`, by reflection. One test per file.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
class ComponentPreviewsTest(private val className: String) {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun everyPreviewInTheFileComposes() {
        if (className == NoPreviews) return
        val previews = Class.forName(className).declaredMethods
            .filter { isStatic(it.modifiers) && it.name.endsWith("Preview") }
            .mapNotNull { it.asComposableMethod() }
        assertTrue(previews.isNotEmpty(), "$className has no function whose name ends with Preview")
        rule.mainClock.autoAdvance = false
        rule.setContent {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                previews.forEach { it.invoke(currentComposer, null) }
            }
        }
        rule.mainClock.advanceTimeByFrame()
    }

    companion object {
        private const val NoPreviews = "(no previews selected)"
        private val sourceRoot = File("src/debug/kotlin")

        /** The JVM class of every `*Previews.kt` file; with `-Pcomponent=…` only the selected component's. */
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun classes(): List<Array<String>> {
            // internal/preview holds the preview frame, not previews.
            val files = sourceRoot.walkTopDown()
                .filter { it.isFile && it.name.endsWith("Previews.kt") && "/internal/" !in it.invariantSeparatorsPath }
                .toList()
            val selected = SelectedComponent.names?.let { names ->
                files.filter { it.name.removeSuffix("Previews.kt") in names }
            } ?: files
            return selected
                .map { it.relativeTo(sourceRoot).invariantSeparatorsPath.removeSuffix(".kt").replace('/', '.') + "Kt" }
                .sorted()
                .ifEmpty { listOf(NoPreviews) }
                .map { arrayOf(it) }
        }
    }
}
