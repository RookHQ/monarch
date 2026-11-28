package dev.monarchui.components.datadisplay

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import dev.monarchui.components.datadisplay.favicon.MonarchFavicon
import dev.monarchui.components.datadisplay.favicon.MonarchFaviconShape
import dev.monarchui.components.datadisplay.favicon.MonarchFaviconSize
import dev.monarchui.components.datadisplay.favicon.MonarchFaviconTokens
import dev.monarchui.foundation.MonarchShapes
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.TestIcon
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.theme.MonarchColors
import dev.monarchui.theme.MonarchTheme
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals

internal object MonarchFaviconFixtures : ComponentFixtures {
    override val id = "favicon"
    override val fixtures = MonarchFaviconShape.entries.map { shape ->
        fixture("letter tile, every size, ${shape.name}") {
            Row(horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2)) {
                MonarchFaviconSize.entries.forEach { size ->
                    MonarchFavicon(image = null, name = "example.com", size = size, shape = shape)
                }
            }
        }
    } + listOf(
        fixture("image, every size") {
            Row(horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2)) {
                MonarchFaviconSize.entries.forEach { size ->
                    MonarchFavicon(image = rememberVectorPainter(TestIcon), name = "rook", size = size)
                }
            }
        },
        fixture("with a label") { MonarchFavicon(image = null, name = "github.com", label = "GitHub") },
        fixture("no letter in the name") { MonarchFavicon(image = null, name = "", size = MonarchFaviconSize.Lg) },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchFaviconTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun withoutALabelTheFaviconIsHiddenFromTalkBack() {
        rule.setMonarchContent { MonarchFavicon(image = null, name = "example.com") }
        rule.onAllNodes(hasText("E")).assertCountEquals(0)
        rule.onAllNodesWithContentDescription("example.com").assertCountEquals(0)
    }

    @Test
    fun withALabelTheFaviconIsOneImageReadAsTheLabel() {
        rule.setMonarchContent {
            MonarchFavicon(image = rememberVectorPainter(TestIcon), name = "github.com", label = "GitHub")
        }
        rule.onNodeWithContentDescription("GitHub")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Image))
        rule.onAllNodesWithContentDescription("GitHub", useUnmergedTree = true).assertCountEquals(1)
    }

    @Test
    fun theTileKeepsItsSizeAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            Row {
                MonarchFaviconSize.entries.forEach { size ->
                    MonarchFavicon(
                        image = null,
                        name = "a",
                        modifier = Modifier.testTag(size.name),
                        label = size.name,
                        size = size,
                    )
                }
            }
        }
        MonarchFaviconSize.entries.forEach { size ->
            rule.onNodeWithTag(size.name)
                .assertWidthIsEqualTo(MonarchFaviconTokens.size(size))
                .assertHeightIsEqualTo(MonarchFaviconTokens.size(size))
        }
    }

    @Test
    fun theLetterIsTheFirstLetterOrDigitUpperCased() {
        assertEquals("E", MonarchFaviconTokens.letter("example.com"))
        assertEquals("4", MonarchFaviconTokens.letter("  42 shop"))
        assertEquals("Ñ", MonarchFaviconTokens.letter("¡ñandú!"))
        assertEquals("?", MonarchFaviconTokens.letter("!!!"))
        assertEquals("?", MonarchFaviconTokens.letter(""))
        assertEquals("𝒜", MonarchFaviconTokens.letter("𝒜bc"))
        assertEquals("İ", MonarchFaviconTokens.letter("istanbul.example", Locale.forLanguageTag("tr")))
    }

    @Test
    fun theShapeFollowsTheOption() {
        val shapes = MonarchShapes.Default
        assertEquals(shapes.xs, MonarchFaviconTokens.shape(MonarchFaviconShape.Rounded))
        assertEquals(shapes.full, MonarchFaviconTokens.shape(MonarchFaviconShape.Circle))
        assertEquals(shapes.none, MonarchFaviconTokens.shape(MonarchFaviconShape.Square))
    }

    @Test
    fun theLetterTileUsesMutedRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val tile = MonarchFaviconTokens.fallbackColors(colors)
            assertEquals(colors.bgMuted, tile.container, theme.id)
            assertEquals(colors.textMuted, tile.content, theme.id)
            assertEquals(colors.borderSubtle, tile.border, theme.id)
        }
    }
}
