package com.rookhq.monarch.sample

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.rookhq.monarch.sample.generated.CatalogueFamilies
import com.rookhq.monarch.theme.MonarchTheme
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Composes every page of the catalogue in the four themes. The pages come
 * from the generated `CatalogueFamilies`; one test per family.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
class CataloguePagesTest(private val familyTitle: String) {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun everyPageOfTheFamilyComposesInEveryTheme() {
        val family = CatalogueFamilies.first { it.title == familyTitle }
        assertTrue(family.pages.isNotEmpty())
        assertTrue(family.pages.all { it.title.isNotBlank() })
        var page by mutableStateOf(family.pages.first())
        var theme by mutableStateOf(CatalogueTheme.Light)
        rule.mainClock.autoAdvance = false
        rule.setContent {
            MonarchTheme(tokens = theme.tokens) {
                Column(Modifier.verticalScroll(rememberScrollState())) { page.Content() }
            }
        }
        for (candidate in family.pages) {
            for (option in CatalogueTheme.entries) {
                page = candidate
                theme = option
                rule.mainClock.advanceTimeByFrame()
            }
        }
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun families(): List<Array<String>> = CatalogueFamilies.map { arrayOf(it.title) }
    }
}
