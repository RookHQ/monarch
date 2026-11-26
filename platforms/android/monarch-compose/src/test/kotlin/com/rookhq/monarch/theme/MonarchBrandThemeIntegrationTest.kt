package com.rookhq.monarch.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.rookhq.monarch.tokens.MonarchBrandTheme
import com.rookhq.monarch.tokens.MonarchTokens
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class MonarchBrandThemeIntegrationTest {
    @get:Rule
    val rule = createComposeRule()

    private class Observed {
        var themeId = ""
        var accentSolid = Color.Unspecified
        var materialPrimary = Color.Unspecified
    }

    private val cases = listOf(
        Triple(MonarchScheme.Light, MonarchContrast.Standard, "light"),
        Triple(MonarchScheme.Dark, MonarchContrast.Standard, "dark"),
        Triple(MonarchScheme.Light, MonarchContrast.More, "hc-light"),
        Triple(MonarchScheme.Dark, MonarchContrast.More, "hc-dark"),
    )

    private fun observeAll(brand: String): List<Observed> {
        val observed = cases.map { Observed() }
        rule.setContent {
            cases.forEachIndexed { i, (scheme, contrast) ->
                MonarchTheme(brand = brand, scheme = scheme, contrast = contrast) {
                    observed[i].themeId = LocalMonarchThemeTokens.current.id
                    observed[i].accentSolid = MonarchTheme.colors.accentSolid
                    observed[i].materialPrimary = MaterialTheme.colorScheme.primary
                }
            }
        }
        rule.waitForIdle()
        return observed
    }

    @Test
    fun aComponentUnderBrandUsesTheDerivedAccentInAllFourThemes() {
        val brand = MonarchBrandTheme.derive("#0A7CFF")
        observeAll("#0A7CFF").forEachIndexed { i, o ->
            val id = cases[i].third
            val scale = brand.mode(id).accent
            assertEquals(id, o.themeId)
            assertEquals(Color(scale.solid), o.accentSolid, id)
            assertEquals(Color(scale.strong), o.materialPrimary, id)
        }
    }

    @Test
    fun invalidBrandFallsBackToTheDefaultAccent() {
        observeAll("not a color").forEachIndexed { i, o ->
            val id = cases[i].third
            assertEquals(Color(MonarchTokens.Accents.rook.scale(id).solid), o.accentSolid, id)
        }
    }
}
