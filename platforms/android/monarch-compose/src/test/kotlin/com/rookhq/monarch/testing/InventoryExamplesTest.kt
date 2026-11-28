package com.rookhq.monarch.testing

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.rookhq.monarch.generated.examples.AllInventoryExamples
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import kotlin.test.Test

/**
 * Composes every example of every inventory file in the four themes at the
 * largest font size. The examples are compiled from `inventory/<id>.json` by
 * the `generateComponentTestSources` task, so documentation can never show
 * code that does not compile or does not run.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
class InventoryExamplesTest(private val id: String) {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun everyExampleComposesInEveryTheme() {
        val examples = AllInventoryExamples.filter { it.id == id }
        if (examples.isEmpty()) return
        val environment = MonarchTestEnvironment(fontScale = LargestFontScale)
        var current by mutableStateOf(examples.first())
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent(environment) {
            Box { key(current) { current.content() } }
        }
        for (example in examples) {
            for (theme in AllThemes) {
                current = example
                environment.tokens = theme
                // A failure in composition is thrown here and names the test, which is the inventory id.
                rule.mainClock.advanceTimeByFrame()
            }
        }
    }

    companion object {
        private const val NoExamples = "(no examples selected)"

        /** The inventory ids that have examples; with `-Pcomponent=…` only the selected ones. */
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun ids(): List<Array<String>> {
            val all = AllInventoryExamples.map { it.id }.distinct().sorted()
            val selected = SelectedComponent.ids?.let { ids -> all.filter { it in ids } } ?: all
            return selected.ifEmpty { listOf(NoExamples) }.map { arrayOf(it) }
        }
    }
}
