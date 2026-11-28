package dev.monarchui.screenshots

import android.graphics.Bitmap
import android.graphics.Color
import dev.monarchui.generated.AllComponentFixtures
import dev.monarchui.generated.examples.AllInventoryExamples
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Tests of the screenshot harness itself: what it covers, and the helpers that keep the images small. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScreenshotHarnessTest {
    private val store = ScreenshotStore.fromSystemProperties()
    private val targets = ScreenshotTargets.all.associateBy { it.id }

    @Test
    fun everyFixtureIsPickedUp() {
        val fixtureIds = AllComponentFixtures.map { it.id }.toSet()
        val shown = fixtureIds - ScreenshotTargets.excluded.keys
        assertTrue(shown.all { it in targets }, "a fixture object is not in the screenshot targets")
        for (id in fixtureIds - ScreenshotTargets.excluded.keys) {
            val expected = AllComponentFixtures.filter { it.id == id }.sumOf { it.fixtures.size }
            assertEquals(expected, targets.getValue(id).cells.size, "$id: one cell per fixture")
            assertEquals(ScreenshotTarget.Source.Fixtures, targets.getValue(id).source)
        }
    }

    @Test
    fun everyInventoryExampleIsPickedUpWhereThereAreNoFixtures() {
        val fixtureIds = AllComponentFixtures.map { it.id }.toSet()
        val exampleIds = AllInventoryExamples.map { it.id }.toSet() - fixtureIds - ScreenshotTargets.excluded.keys
        assertTrue(exampleIds.all { it in targets }, "an inventory example is not in the screenshot targets")
        for (id in exampleIds) {
            assertEquals(AllInventoryExamples.count { it.id == id }, targets.getValue(id).cells.size, id)
        }
    }

    @Test
    fun everyExclusionIsARealComponentWithAReason() {
        val inventory = File(System.getProperty("monarch.rootDir"), "inventory")
        ScreenshotTargets.excluded.forEach { (id, reason) ->
            assertTrue(File(inventory, "$id.json").exists(), "$id is not an inventory id")
            assertTrue(reason.length > 20, "$id: say why it is excluded")
            assertTrue(id !in store.core, "$id is excluded, so it cannot be in the core set")
        }
    }

    @Test
    fun everyIntentionallyBlankCellIsARealCellWithAReason() {
        ScreenshotTargets.intentionallyBlank.forEach { (key, reason) ->
            val (id, label) = key.split("/", limit = 2)
            assertTrue(targets.getValue(id).cells.any { it.label == label }, "$key is not a cell")
            assertTrue(reason.length > 20, "$key: say why it is blank")
        }
    }

    @Test
    fun theCoreListHasOnlyRealComponents() {
        val missing = store.core.filter { it !in targets }
        assertTrue(missing.isEmpty(), "core-components.txt names ids without fixtures or examples: $missing")
        assertTrue(store.core.size in 28..34, "about 30 core components, found ${store.core.size}")
        val inventory = File(System.getProperty("monarch.rootDir"), "inventory")
        assertTrue(store.core.all { File(inventory, "$it.json").exists() }, "a core id has no inventory file")
    }

    @Test
    fun theMatrixFollowsTheSizeDecision() {
        val id = targets.keys.first { it !in store.core }
        assertEquals(listOf("light", "dark"), ScreenshotTargets.variantsFor(id, store.core).map { it.suffix })
        val core = ScreenshotTargets.variantsFor(store.core.first(), store.core)
        assertEquals(16, core.size)
        assertEquals(16, core.map { it.suffix }.toSet().size)
        assertEquals(listOf("light", "dark"), core.take(2).map { it.suffix }, "the two preview images come first")
        assertEquals(4, core.map { it.theme.id }.toSet().size)
    }

    @Test
    fun everyBaselineIsStoredNextToItsComponentAndNeverInThePreviews() {
        val variants = ScreenshotTargets.variantsFor(store.core.first(), store.core)
        val files = variants.associate { it.suffix to store.fileFor("x", it) }
        assertEquals("x.light.png", files.getValue("light").name)
        assertEquals("x", files.getValue("light").parentFile?.name)
        assertEquals("x", files.getValue("dark").parentFile?.name)
        assertEquals("x", files.getValue("hc-dark.x2.rtl").parentFile?.name)
        assertTrue(files.values.none { it.parentFile?.name == "previews" }, "previews/ holds the docs previews only")
    }

    @Test
    fun reducingKeepsAtMost256ColoursAndIsStable() {
        val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        for (x in 0 until 64) for (y in 0 until 64) bitmap.setPixel(x, y, Color.rgb(x * 4, y * 4, (x + y) * 2))
        val reduced = ScreenshotPixels.reduce(bitmap)
        val colours = (0 until 64).flatMap { x -> (0 until 64).map { y -> reduced.getPixel(x, y) } }.toSet()
        assertTrue(colours.size <= 256, "${colours.size} colours")
        val again = ScreenshotPixels.reduce(reduced)
        for (x in 0 until 64) for (y in 0 until 64) assertEquals(reduced.getPixel(x, y), again.getPixel(x, y))
    }

    @Test
    fun theTestEnvironmentCanSetTheTextInPoppinsAndKeepsTheSystemFontByDefault() {
        val poppins = dev.monarchui.testing.MonarchTestEnvironment(
            typography = dev.monarchui.fonts.poppins.monarchPoppinsTypography(),
        )
        assertEquals(dev.monarchui.fonts.poppins.MonarchPoppins, poppins.typography.body.fontFamily)
        val system = dev.monarchui.testing.MonarchTestEnvironment()
        assertTrue(system.typography.body.fontFamily != dev.monarchui.fonts.poppins.MonarchPoppins)
    }

    @Test
    fun largeFlatFillsKeepTheirExactColourWhenTheColourCountGrows() {
        // A white page, a pale pill, a dark card, and enough anti-aliasing noise to exceed 256 colours.
        val pale = Color.rgb(0xFD, 0xF2, 0xF2)
        val dark = Color.rgb(0x16, 0x1B, 0x22)
        val bitmap = Bitmap.createBitmap(120, 80, Bitmap.Config.ARGB_8888).also { it.eraseColor(Color.WHITE) }
        for (x in 10 until 50) for (y in 10 until 30) bitmap.setPixel(x, y, pale)
        for (x in 60 until 110) for (y in 40 until 70) bitmap.setPixel(x, y, dark)
        for (i in 0 until 600) {
            bitmap.setPixel(i % 120, 75 + i / 120, Color.rgb(i % 256, (i * 7) % 256, (i * 13) % 256))
        }
        val reduced = ScreenshotPixels.reduce(bitmap)
        assertEquals(pale, reduced.getPixel(20, 20), "the pale pill stays pale")
        assertEquals(dark, reduced.getPixel(80, 55), "the dark card stays dark")
        assertEquals(Color.WHITE, reduced.getPixel(0, 0))
        val colours = (0 until 120).flatMap { x -> (0 until 80).map { y -> reduced.getPixel(x, y) } }.toSet()
        assertTrue(colours.size <= 256, "${colours.size} colours")
    }

    @Test
    fun anImageWithFewColoursIsKeptExactly() {
        val bitmap = Bitmap.createBitmap(30, 30, Bitmap.Config.ARGB_8888).also { it.eraseColor(Color.WHITE) }
        for (x in 0 until 30) for (y in 0 until 30) {
            if ((x + y) % 7 == 0) bitmap.setPixel(x, y, Color.rgb(3 * x, y, 201))
        }
        val reduced = ScreenshotPixels.reduce(bitmap)
        for (x in 0 until 30) for (y in 0 until 30) assertEquals(bitmap.getPixel(x, y), reduced.getPixel(x, y))
    }

    @Test
    fun longFlatRunsOfRowsAreShortened() {
        val bitmap = Bitmap.createBitmap(4, 100, Bitmap.Config.ARGB_8888).also { it.eraseColor(Color.GRAY) }
        bitmap.setPixel(1, 50, Color.RED)
        val collapsed = ScreenshotPixels.collapseFlatRows(bitmap, keep = 10)
        assertEquals(10 + 1 + 10, collapsed.height)
    }
}
