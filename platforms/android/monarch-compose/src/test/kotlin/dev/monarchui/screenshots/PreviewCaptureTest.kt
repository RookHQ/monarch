package dev.monarchui.screenshots

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.RoborazziTaskType
import com.github.takahirom.roborazzi.captureScreenRoboImage
import dev.monarchui.fonts.poppins.monarchPoppinsTypography
import dev.monarchui.generated.AllComponentFixtures
import dev.monarchui.generated.examples.AllInventoryExamples
import dev.monarchui.review.ReviewShowcases
import dev.monarchui.screenshots.showcase.PreviewShowcases
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.MonarchTestEnvironment
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.theme.MonarchTheme
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import org.json.JSONObject
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowLooper

/**
 * The docs previews: one image per component and theme, of its PRIMARY example only (the first entry of `examples`
 * in `inventory/<id>.json`, the code the docs page shows; the first fixture when there is none). This is not the
 * regression baseline ([ComponentScreenshotTest] stacks every fixture for that).
 *
 * The window is a phone: 392dp wide at density 3.0 (xxhdpi), on the theme canvas. The example is laid out at the
 * full 392dp width with 24dp around it and a height that wraps its content, and that frame is the image: the crop
 * is measured, not fixed. A screen-level recipe (one that fills the screen) gets a 760dp tall frame. A dialog,
 * sheet or menu is a window of its own: the whole screen is captured with the scrim, then cropped to the shown
 * window plus 48dp of scrim.
 *
 * The text is set in Poppins (`monarch-fonts-poppins`), as D10.4 asks for the docs previews.
 *
 * It runs only with `-Pmonarch.previews.dir=...` (task `recordPreviews`) and writes uncompressed PNGs there;
 * `scripts/native/compress-previews.py` shrinks them. See `docs/PREVIEWS.md`.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "+en-rGB-w392dp-h2000dp-xxhdpi")
@OptIn(ExperimentalRoborazziApi::class)
class PreviewCaptureTest(private val id: String) {
    @get:Rule
    val rule = createComposeRule()

    private var epoch by mutableIntStateOf(0)

    /** The root focus manager: a still frame has no focus ring (the first field takes the initial focus in a test). */
    private var focusManager: FocusManager? = null

    @Test
    fun writesThePreviews() {
        val dir = System.getProperty("monarch.previews.dir")?.takeIf { it.isNotBlank() }?.let(::File) ?: return
        val content = primary(id) ?: return
        // The web docs render in en-GB (Monday first, 24-hour clock, d MMMM) and UTC, as the iOS PreviewHost does.
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("UTC"))
        java.util.Locale.setDefault(java.util.Locale.UK)
        val resolver = androidx.test.core.app.ApplicationProvider
            .getApplicationContext<android.content.Context>().contentResolver
        android.provider.Settings.System.putString(resolver, android.provider.Settings.System.TIME_12_24, "24")
        // A long page is taller than the 2000dp test window, which cut it: give those ids a taller window.
        if (id in TallWindowIds || id.startsWith("dashboard-")) {
            org.robolectric.RuntimeEnvironment.setQualifiers("+h3800dp")
        }
        val screen = isScreen(id)
        val core = ScreenshotStore.fromSystemProperties().core
        val themes = AllThemes.filter { it.id == "light" || it.id == "dark" || id in core }
        dir.mkdirs()
        // D10.4: the docs previews are set in Poppins (the optional font pack); the regression baselines keep the
        // system font.
        val environment = MonarchTestEnvironment(typography = monarchPoppinsTypography())
        // The web screenshots are still frames: a spinner or a shimmer in a preview is a frozen mid-turn arc.
        android.provider.Settings.Global.putFloat(
            androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>().contentResolver,
            android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
            0f,
        )
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent(environment) {
            key(epoch, environment.tokens.id) {
                Screen(environment.tokens.id.contains("dark")) { Frame(screen, content) }
            }
        }
        settle()
        capture() // the first frame of a window is drawn late: throw this one away
        for (theme in themes) {
            environment.tokens = theme
            epoch++
            val shot = if (screen) cutAtGap(stable()) else stable()
            File(dir, "$id.${theme.id}.png").outputStream().use { shot.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    /**
     * The whole window on the theme canvas, and the system night mode of the theme. Without this only the frame is
     * painted: a dialog, sheet or menu is captured with the unpainted (white) window around it, so in the dark
     * themes its scrim looked mid-grey over white. And a recipe that follows the system scheme
     * (`MonarchTheme(scheme = System)`, the theme toggle and the appearance panel) showed the light theme in the
     * dark image.
     */
    @Composable
    private fun Screen(dark: Boolean, content: @Composable () -> Unit) {
        val base = LocalConfiguration.current
        focusManager = LocalFocusManager.current
        val night = if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
        val configuration = Configuration(base).apply {
            uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or night
            // The web docs render in en-GB (d MMMM, Monday first, 24-hour clock), as the iOS PreviewHost does.
            setLocale(java.util.Locale.UK)
        }
        CompositionLocalProvider(LocalConfiguration provides configuration) {
            Box(Modifier.fillMaxSize().background(MonarchTheme.colors.bgCanvas)) { content() }
        }
    }

    @Composable
    private fun Frame(screen: Boolean, content: @Composable () -> Unit) {
        val size = if (screen) {
            val tall = id.startsWith("dashboard-") || id in TallScreens
            Modifier.height((if (tall) DashboardHeight else ScreenHeight).dp)
        } else {
            Modifier.heightIn(max = (if (id in TallWindowIds) TallWindowHeight else TallHeight).dp)
        }
        // wrapContentHeight(unbounded): a tall recipe is not cut at the height of the test window.
        Box(
            Modifier
                .wrapContentHeight(align = Alignment.Top, unbounded = true)
                .testTag(FrameTag)
                .width(PhoneWidth.dp)
                .then(size),
        ) {
            // A screen has the 16dp side margins of a phone window and no bottom padding: it is cut at a gap.
            val inset = if (screen) {
                Modifier.padding(start = ScreenMargin.dp, top = ScreenMargin.dp, end = ScreenMargin.dp)
            } else {
                Modifier.padding(Padding.dp)
            }
            // An example is a function body, often several siblings (a field and the button under it, a list and
            // its "Add" button): in a Box they were drawn on top of each other (action-editor, sequence-builder,
            // query-builder, type-to-confirm-input). A column stacks them, as the docs page does.
            Column(Modifier.fillMaxWidth().then(inset), verticalArrangement = Arrangement.spacedBy(SiblingGap.dp)) {
                content()
            }
        }
    }

    /**
     * Captures until the pictures in a row are the same, so the frame before the last one is never kept. A dialog
     * or sheet is a window that slides or fades in: two equal frames can be a stalled animation (the first frame
     * of the enter motion, drawn twice), so a window needs [WindowStillFrames] equal frames, taken after the clock
     * has run on, before it counts as still.
     */
    private fun stable(): Bitmap {
        var previous: Bitmap? = null
        var shot: Bitmap
        var tries = 0
        var still = 0
        do {
            settle()
            if (id !in FocusedIds) rule.runOnIdle { focusManager?.clearFocus(true) }
            settle()
            val window = openedWindow()
            // The enter motion of a window is on the frame clock: run it on before looking.
            if (window && tries == 0) repeat(WindowWarmUps) { settle() }
            shot = capture()
            // A frame that shows nothing yet is one flat colour: compose the content again from scratch. A window
            // that is open but not drawn yet (a sheet that is open from the first frame has no trigger on the
            // canvas, so the whole screen is flat until it appears) is given more time instead: composing it again
            // starts its first frame over, and it never appeared (thread-panel, bulk-destructive-flow).
            val blank = isFlat(shot)
            if (blank && !window) epoch++
            still = if (!blank && previous?.sameAs(shot) == true) still + 1 else 0
            previous = shot
        } while (still < (if (window) WindowStillFrames else 1) && ++tries < MaxAttempts)
        return shot
    }

    /**
     * A screen that scrolls is cut by the bottom of the phone in the middle of a line. Looks for a band of rows
     * with at most three colours (canvas, card fill, border: no text) in the last 140dp and ends the image there.
     */
    private fun cutAtGap(shot: Bitmap): Bitmap {
        val density = rule.density.density
        val width = shot.width
        val row = IntArray(width)
        val need = (GapRows * density).toInt()
        val floor = (shot.height - SearchBand * density).toInt().coerceAtLeast(shot.height / 2)
        var run = 0
        for (y in shot.height - 1 downTo floor) {
            shot.getPixels(row, 0, width, 0, y, width, 1)
            run = if (row.toHashSet().size <= 3) run + 1 else 0
            if (run >= need) return Bitmap.createBitmap(shot, 0, 0, width, y + need / 2)
        }
        return shot
    }

    private fun isFlat(shot: Bitmap): Boolean {
        val first = shot.getPixel(0, 0)
        val row = IntArray(shot.width)
        for (y in 0 until shot.height) {
            shot.getPixels(row, 0, shot.width, 0, y, shot.width, 1)
            if (row.any { it != first }) return false
        }
        return true
    }

    private fun settle() {
        Snapshot.sendApplyNotifications()
        repeat(3) { rule.mainClock.advanceTimeByFrame() }
        rule.mainClock.advanceTimeBy(SettleMillis)
        repeat(3) { rule.mainClock.advanceTimeByFrame() }
        ShadowLooper.idleMainLooper(VsyncMillis, TimeUnit.MILLISECONDS)
        ShadowLooper.idleMainLooper(VsyncMillis, TimeUnit.MILLISECONDS)
        rule.waitForIdle()
    }

    private fun openedWindow(): Boolean = rule.onAllNodes(isRoot()).fetchSemanticsNodes().size > 1

    private fun capture(): Bitmap {
        if (openedWindow()) {
            val file = File.createTempFile("monarch-preview", ".png")
            try {
                captureScreenRoboImage(file, RoborazziOptions(taskType = RoborazziTaskType.Record))
                return cropToWindow(BitmapFactory.decodeFile(file.path))
            } finally {
                file.delete()
            }
        }
        rule.onNodeWithTag(FrameTag).captureToImage()
        return rule.onNodeWithTag(FrameTag).captureToImage().asAndroidBitmap()
    }

    /**
     * The whole screen with a dialog, sheet or menu on it, cropped to the rows that are not one flat colour, plus
     * 48dp above and below (the screen edge stops it). A row of one colour is background: the scrim, or the
     * canvas or status bar band above it, whatever its colour (the top left pixel is not always the scrim).
     */
    private fun cropToWindow(screen: Bitmap): Bitmap {
        val width = screen.width
        val row = IntArray(width)
        val content = BooleanArray(screen.height)
        var first = -1
        var last = -1
        for (y in 0 until screen.height) {
            screen.getPixels(row, 0, width, 0, y, width, 1)
            val flat = row[0]
            if (row.any { it != flat }) {
                content[y] = true
                if (first < 0) first = y
                last = y
            }
        }
        if (first < 0) return screen
        val margin = (WindowMargin * rule.density.density).toInt()
        val top = (first - margin).coerceAtLeast(0)
        val bottom = (last + margin).coerceAtMost(screen.height - 1)
        // A tall window leaves a long bare band between the trigger and a bottom sheet; keep one margin of it, not all.
        val keep = ArrayList<IntRange>()
        var start = top
        var y = top
        while (y <= bottom) {
            if (!content[y]) {
                var end = y
                while (end <= bottom && !content[end]) end++
                if (end - y > 2 * margin) {
                    keep += start until (y + margin)
                    start = end - margin
                }
                y = end
            } else {
                y++
            }
        }
        keep += start..bottom
        if (keep.size == 1) return Bitmap.createBitmap(screen, 0, top, width, bottom - top + 1)
        val out = Bitmap.createBitmap(width, keep.sumOf { it.last - it.first + 1 }, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(out)
        var at = 0
        keep.forEach { range ->
            val height = range.last - range.first + 1
            canvas.drawBitmap(
                screen,
                android.graphics.Rect(0, range.first, width, range.last + 1),
                android.graphics.Rect(0, at, width, at + height),
                null,
            )
            at += height
        }
        return out
    }

    /** True when the primary example fills the screen (a recipe for a whole screen, not for a part of one). */
    private fun isScreen(id: String): Boolean {
        val file = File(System.getProperty("monarch.rootDir") ?: ".", "inventory/$id.json")
        val examples = if (!file.exists()) null else {
            JSONObject(file.readText()).optJSONArray("examples")
        }
        return examples != null && examples.length() > 0 &&
            examples.getJSONObject(0).optString("code").contains("fillMaxSize()")
    }

    companion object {
        /** Examples that show their focused state, as the web does: a suggestion appears only in a focused field. */
        /** Whole-page recipes taller than a phone screen: shown in full, not cut at the window. */
        private val TallScreens = setOf("business-hours-editor", "form-builder", "gantt-timeline", "file-list")

        private val FocusedIds = setOf("ai-ghost-text", "editable-cell")
        private const val FrameTag = "monarch-preview-frame"
        private const val PhoneWidth = 392
        private const val ScreenHeight = 760

        /** A dashboard is a whole page: every card shows, the frame is cut at the last gap. */
        private const val DashboardHeight = 2600
        private const val TallHeight = 1800
        private const val TallWindowHeight = 3400
        private val TallWindowIds = setOf(
            "business-hours-editor", "weekly-hours-editor", "rule-builder", "schema-driven-form", "data-grid",
        )
        private const val Padding = 24
        private const val ScreenMargin = 16
        private const val SiblingGap = 12
        private const val GapRows = 8
        private const val SearchBand = 140
        private const val WindowMargin = 48
        private const val SettleMillis = 1000L
        private const val VsyncMillis = 17L
        private const val MaxAttempts = 16
        private const val WindowStillFrames = 3
        private const val WindowWarmUps = 2

        /**
         * The primary example of [id] (the web's primary example, CROSS-PLATFORM.md section 1); the review showcase
         * of a foundation, the curated showcase or the first fixture only when it has no example.
         */
        private fun primary(id: String): (@Composable () -> Unit)? =
            AllInventoryExamples.firstOrNull { it.id == id }?.content
                ?: ReviewShowcases.all[id]
                ?: PreviewShowcases.all[id]
                ?: AllComponentFixtures.firstOrNull { it.id == id }?.fixtures?.firstOrNull()?.content

        /** One test per component with something to show; `-Pmonarch.screenshots.ids=a,b` limits the run. */
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun ids(): List<Array<String>> {
            if (System.getProperty("monarch.previews.dir").isNullOrBlank()) return listOf(arrayOf("(off)"))
            val wanted = System.getProperty("monarch.previews.ids")?.split(',')?.filter { it.isNotBlank() }?.toSet()
            // Review-only ids (foundations with no component) render only when asked for by name.
            val review = ReviewShowcases.all.keys.filter { wanted != null && it in wanted }
            val all = (AllInventoryExamples.map { it.id } + AllComponentFixtures.map { it.id } + review)
                .distinct().sorted()
                .filter { it !in ScreenshotTargets.excluded.keys || wanted != null }
            return (wanted?.let { w -> all.filter { it in w } } ?: all).filter { it !in PreviewWideIds.ids }
                .map { arrayOf(it) }
        }
    }
}
