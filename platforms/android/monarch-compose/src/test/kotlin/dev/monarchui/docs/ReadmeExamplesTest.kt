package dev.monarchui.docs

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import dev.monarchui.tokens.MonarchTokens
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Keeps the Kotlin in README.md equal to source that is compiled and composed here. */
@RunWith(RobolectricTestRunner::class)
class ReadmeExamplesTest {
    @get:Rule
    val rule = createComposeRule()

    private val root = File(checkNotNull(System.getProperty("monarch.rootDir")) { "monarch.rootDir is not set" })

    @Test
    fun everyKotlinBlockInTheReadmeIsCompiledSource() {
        val source = File("src/test/kotlin/dev/monarchui/docs/ReadmeExamples.kt").readText()
        val compiled = Regex("""// readme: (\S+)\n(.*?)\n// end""", RegexOption.DOT_MATCHES_ALL)
            .findAll(source).map { it.groupValues[2] }.toSet()
        val blocks = Regex("""```kotlin\n(.*?)\n```""", RegexOption.DOT_MATCHES_ALL)
            .findAll(File(root, "README.md").readText()).map { it.groupValues[1] }.toList()
        assertTrue(blocks.isNotEmpty(), "README.md has no Kotlin examples")
        blocks.forEach { block ->
            assertTrue(block in compiled, "README.md shows Kotlin that is not in ReadmeExamples.kt:\n$block")
        }
        assertEquals(compiled.size, blocks.toSet().size, "A region of ReadmeExamples.kt is not shown in README.md")
    }

    @Test
    fun theReadmeExamplesCompose() {
        rule.setContent {
            AtlasTheme { InvoicesScreen(onCreate = {}) }
            DarkAtlasPreview { }
        }
        rule.onNodeWithText("New invoice").assertExists()
        assertEquals(MonarchTokens.light.colors.bgCanvas, canvasArgb)
        assertEquals(16.dp, gutter)
        assertEquals(MonarchTokens.Accents.atlas.hcDark.solid, highContrastDark.colors.accentSolid)
        assertEquals(MonarchTokens.FontSize.base, bodySize.value)
    }
}
