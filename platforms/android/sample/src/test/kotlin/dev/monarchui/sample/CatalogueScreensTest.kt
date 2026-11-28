package dev.monarchui.sample

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import dev.monarchui.sample.generated.CatalogueFamilies
import dev.monarchui.theme.MonarchTheme
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import kotlin.test.Test

/**
 * Pictures of the catalogue for review: the home screen in light and dark, and a few pages. It writes PNGs only
 * when the environment variable `MONARCH_SAMPLE_SHOTS` names a folder, and does nothing otherwise.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "+w392dp-h1600dp-xxhdpi")
class CatalogueScreensTest {
    @get:Rule
    val rule = createComposeRule()

    private val folder: File? = System.getenv("MONARCH_SAMPLE_SHOTS")?.takeIf { it.isNotBlank() }?.let(::File)

    private fun save(name: String, bitmap: Bitmap) {
        val dir = folder ?: return
        dir.mkdirs()
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test
    fun theHomeScreen() {
        if (folder == null) return
        rule.setContent { CatalogueApp() }
        rule.waitForIdle()
        save("sample-home.light", rule.onRoot().captureToImage().asAndroidBitmap())
    }

    @Test
    fun pages() {
        if (folder == null) return
        val pages = CatalogueFamilies.flatMap { it.pages }
        val wanted = listOf("ViewSwitcher", "CallControls", "Toolbar", "Sheet", "ToggleGroup")
        var current by mutableStateOf(pages.first())
        var theme by mutableStateOf(CatalogueTheme.Light)
        rule.setContent {
            MonarchTheme(tokens = theme.tokens) {
                Column(
                    modifier = Modifier
                        .testTag("page")
                        .width(392.dp)
                        .background(MonarchTheme.colors.bgCanvas)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space3),
                ) {
                    current.Content()
                }
            }
        }
        for (title in wanted) {
            val page = pages.firstOrNull { it.title == title } ?: continue
            for (option in listOf(CatalogueTheme.Light, CatalogueTheme.Dark)) {
                current = page
                theme = option
                rule.waitForIdle()
                val shot = rule.onNodeWithTag("page").captureToImage().asAndroidBitmap()
                save("sample-$title.${option.name.lowercase()}", shot)
            }
        }
    }
}
