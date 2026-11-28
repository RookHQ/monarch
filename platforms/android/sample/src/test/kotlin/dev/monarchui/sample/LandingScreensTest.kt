package dev.monarchui.sample

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import dev.monarchui.sample.showcase.LandingFloat
import dev.monarchui.sample.showcase.LandingFloatCanvas
import dev.monarchui.sample.showcase.LandingTile
import dev.monarchui.sample.showcase.LandingTileCanvas
import dev.monarchui.sample.showcase.ShowcaseApp
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import kotlin.test.Test

/**
 * The screenshots of the docs site's landing and Android pages: the showcase screen on a phone and a
 * tablet, the eight category tiles and the three hero floats, each in light and dark. It writes
 * PNGs only when the environment variable `MONARCH_LANDING_SHOTS` names a folder, and does nothing
 * otherwise. `apps/docs/scripts/capture-native-screens.sh android` runs it and turns the PNGs into
 * the site's images.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LandingScreensTest {
    @get:Rule
    val rule = createComposeRule()

    private val folder: File? = System.getenv("MONARCH_LANDING_SHOTS")?.takeIf { it.isNotBlank() }?.let(::File)

    private fun save(name: String, dark: Boolean, bitmap: Bitmap) {
        val dir = folder ?: return
        dir.mkdirs()
        val theme = if (dark) "dark" else "light"
        File(dir, "android-$name.$theme.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    /** Renders [content] in light and dark and saves the node tagged [Tag], or the whole window. */
    private fun shoot(names: List<String>, whole: Boolean = false, content: @Composable (Int, Boolean) -> Unit) {
        if (folder == null) return
        var index by mutableIntStateOf(0)
        var dark by mutableStateOf(false)
        // A new key starts each picture afresh.
        rule.setContent { key(index, dark) { content(index, dark) } }
        names.forEachIndexed { i, name ->
            for (option in listOf(false, true)) {
                index = i
                dark = option
                rule.waitForIdle()
                val node = if (whole) rule.onRoot() else rule.onNodeWithTag(Tag)
                save(name, option, node.captureToImage().asAndroidBitmap())
            }
        }
    }

    @Test
    @Config(qualifiers = "w412dp-h915dp-port-xxhdpi")
    fun phone() = shoot(listOf("phone"), whole = true) { _, dark -> ShowcaseApp(dark = dark) }

    @Test
    @Config(qualifiers = "w800dp-h1280dp-port-xhdpi")
    fun tablet() = shoot(listOf("tablet"), whole = true) { _, dark -> ShowcaseApp(dark = dark) }

    @Test
    @Config(qualifiers = "w420dp-h900dp-xxhdpi")
    fun tiles() {
        val tiles = LandingTile.entries
        shoot(tiles.map { "tile-${it.id}" }) { i, dark ->
            LandingTileCanvas(tiles[i], dark = dark, modifier = Modifier.testTag(Tag))
        }
    }

    @Test
    @Config(qualifiers = "w420dp-h900dp-xxhdpi")
    fun floats() {
        val floats = LandingFloat.entries
        shoot(floats.map { "float-${it.id}" }) { i, dark ->
            LandingFloatCanvas(floats[i], dark = dark, modifier = Modifier.testTag(Tag))
        }
    }

    private companion object {
        const val Tag = "landing"
    }
}
