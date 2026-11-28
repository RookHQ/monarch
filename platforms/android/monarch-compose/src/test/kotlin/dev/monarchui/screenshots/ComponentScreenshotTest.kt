package dev.monarchui.screenshots

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.RoborazziTaskType
import com.github.takahirom.roborazzi.captureScreenRoboImage
import dev.monarchui.testing.MonarchTestEnvironment
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.theme.MonarchTheme
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowLooper
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.test.Test

/**
 * The screenshot gate. For every component, in each variant D9 asks for, it renders every fixture (or, for the
 * recipes that have none, every inventory example) stacked in one image, compares it with the baseline and, with
 * `-Pmonarch.screenshots=record`, writes it. See [ScreenshotRecorder] for the compare and the failure output.
 *
 * The window is 320 by 1000dp at density 1.0 in US English: the narrowest phone shape, as the matrix tests use.
 * Animations are stopped: the compose clock is advanced by hand, by the same amount for every image.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "+w320dp-h1000dp")
@OptIn(ExperimentalRoborazziApi::class)
class ComponentScreenshotTest(private val id: String) {
    @get:Rule
    val rule = createComposeRule()

    private var current by mutableStateOf(ScreenshotCell("", tall = false) {})

    @Test
    fun everyVariantMatchesItsBaseline() {
        val store = ScreenshotStore.fromSystemProperties()
        if (store.mode == ScreenshotMode.Off) return
        val target = ScreenshotTargets.all.first { it.id == id }
        val recorder = ScreenshotRecorder(store)
        val environment = MonarchTestEnvironment()
        current = target.cells.first()
        var epoch by mutableIntStateOf(0)
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent(environment) {
            key(current, epoch, environment.fontScale, environment.layoutDirection) { CellFrame(current) }
        }
        // The first frame of a window is drawn late, and some components (text fields) start effects in it: show
        // every cell once and throw the result away, so that the first image is taken after a switch of cell, as
        // every other one is.
        target.cells.forEach { cell ->
            closeWindows()
            current = cell
            settle(cell)
            capture()
        }
        for (variant in ScreenshotTargets.variantsFor(id, store.core)) {
            environment.tokens = variant.theme
            environment.fontScale = variant.fontScale
            environment.layoutDirection = if (variant.rtl) LayoutDirection.Rtl else LayoutDirection.Ltr
            var previous: Pair<ScreenshotCell, Bitmap>? = null
            val parts = target.cells.map { cell ->
                closeWindows()
                current = cell
                var shot: Bitmap
                var tries = 0
                do {
                    settle(cell)
                    shot = capture()
                    // The window can still show the cell before this one, or nothing at all: the label strip at the
                    // top of the image shows it, because a different label cannot give the same pixels.
                    val stale = if (openedWindow()) {
                        // The window is open but not drawn yet: the screen shows only the empty frame under it.
                        isFlatBelowStrip(shot)
                    } else {
                        isBlankStrip(shot) || previous?.let { (cell0, bitmap) ->
                            cell0.label != cell.label && sameStrip(bitmap, shot)
                        } == true
                    }
                    if (stale) {
                        // Compose the cell again from scratch.
                        epoch++
                        Thread.sleep(RetryPauseMillis)
                    }
                } while (stale && ++tries < MaxSettleAttempts)
                check(tries < MaxSettleAttempts) { "$id: the cell \"${cell.label}\" was drawn as the cell before it" }
                val allowedBlank = "$id/${cell.label}" in ScreenshotTargets.intentionallyBlank
                check(!isFlatBelowStrip(shot) || allowedBlank) {
                    "$id: the cell \"${cell.label}\" is blank: it shows nothing"
                }
                shot = confirmed(shot)
                previous = cell to shot
                shot
            }
            recorder.check(store.fileFor(id, variant), stitch(parts))
        }
        recorder.assertNoFailures()
    }

    /**
     * Waits for two equal frames before the picture counts. A frame can still be the one before the last (the first
     * frame of a window is drawn late, and a recorded image once showed the first fixture with the content of a
     * later one). The frames are taken after a few frames of the clock and no more, so an animation that never
     * ends does not move on a whole second between two looks. When no two frames in a row are equal within
     * [ConfirmFrames] looks (an endless animation), [first] stands, as it did before; the virtual clock makes
     * that choice the same on every run.
     */
    private fun confirmed(first: Bitmap): Bitmap {
        var previous = first
        repeat(ConfirmFrames) {
            Snapshot.sendApplyNotifications()
            repeat(3) { rule.mainClock.advanceTimeByFrame() }
            ShadowLooper.idleMainLooper(VsyncMillis, TimeUnit.MILLISECONDS)
            rule.waitForIdle()
            val next = capture()
            if (next.sameAs(previous)) return next
            previous = next
        }
        return first
    }

    /**
     * Shows nothing and waits until the windows of the cell before are gone. A sheet that opens while the window
     * of an earlier one is still being removed stays at its hidden position, and the image shows nothing.
     */
    private fun closeWindows() {
        if (!openedWindow()) return
        current = ScreenshotCell("", tall = false) {}
        settleOnce()
        check(!openedWindow()) { "$id: a window stayed open after its cell was removed" }
    }

    /** Lets entrance animations finish by the same virtual time for every image; endless ones stop at one phase. */
    private fun settle(cell: ScreenshotCell? = null) {
        // The window sometimes shows the previous cell for a frame: wait until the label of this one is there.
        var attempts = 0
        do {
            settleOnce()
            val shown = cell == null || rule.onAllNodesWithText(cell.label).fetchSemanticsNodes().isNotEmpty()
        } while (!shown && ++attempts < MaxSettleAttempts)
        check(cell == null || attempts < MaxSettleAttempts) { "$id: the cell \"${cell?.label}\" was never shown" }
    }

    private fun settleOnce() {
        Snapshot.sendApplyNotifications()
        repeat(3) { rule.mainClock.advanceTimeByFrame() }
        rule.mainClock.advanceTimeBy(SettleMillis)
        repeat(3) { rule.mainClock.advanceTimeByFrame() }
        // Views are measured and drawn from the main looper, which Robolectric holds until it is told to run.
        // A draw is scheduled for the next vsync, so the looper's clock has to move too, not only run what is due.
        ShadowLooper.idleMainLooper(VsyncMillis, TimeUnit.MILLISECONDS)
        ShadowLooper.idleMainLooper(VsyncMillis, TimeUnit.MILLISECONDS)
        rule.waitForIdle()
        if (openedWindow()) {
            // A sheet starts its entrance animation in its own window, a frame after that window is attached.
            // One long step of the clock runs past it: step the clock and the looper together instead.
            repeat(WindowSteps) {
                rule.mainClock.advanceTimeBy(WindowStepMillis)
                ShadowLooper.idleMainLooper(VsyncMillis, TimeUnit.MILLISECONDS)
            }
            rule.waitForIdle()
        }
    }

    /**
     * The cell. A dialog, sheet or menu is a window of its own that a node capture cannot see, so when the cell
     * opened one, the whole screen is captured instead: the frame with the window over it.
     */
    private fun openedWindow(): Boolean = rule.onAllNodes(isRoot()).fetchSemanticsNodes().size > 1

    private fun capture(): Bitmap {
        if (openedWindow()) {
            val file = File.createTempFile("monarch-screen", ".png")
            try {
                captureScreenRoboImage(file, RoborazziOptions(taskType = RoborazziTaskType.Record))
                return ScreenshotPixels.collapseFlatRows(BitmapFactory.decodeFile(file.path))
            } finally {
                file.delete()
            }
        }
        // A capture can still show the frame before the last one: the first call draws the pending frame.
        rule.onNodeWithTag(FrameTag).captureToImage()
        return rule.onNodeWithTag(FrameTag).captureToImage().asAndroidBitmap()
    }

    /** The label of a cell above the cell, at a fixed small size whatever the font scale. */
    @androidx.compose.runtime.Composable
    private fun CellFrame(cell: ScreenshotCell) {
        val density = LocalDensity.current
        Column(Modifier.testTag(FrameTag).fillMaxWidth()) {
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1f)) {
                Text(
                    cell.label,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                    color = MonarchTheme.colors.textSubtle,
                    fontSize = 9.sp,
                    maxLines = 1,
                )
            }
            Box(Modifier.heightIn(max = if (cell.tall) TallHeight.dp else PhoneHeight.dp)) { cell.content() }
        }
    }

    /** The label strip at the top of a cell image. */
    private fun strip(bitmap: Bitmap): IntArray {
        val rows = LabelStripRows.coerceAtMost(bitmap.height)
        return IntArray(bitmap.width * rows).also { bitmap.getPixels(it, 0, bitmap.width, 0, 0, bitmap.width, rows) }
    }

    private fun isBlankStrip(bitmap: Bitmap): Boolean = strip(bitmap).toSet().size <= 1

    /** Whether everything under the label strip is one colour: an opened window that has not been drawn. */
    private fun isFlatBelowStrip(bitmap: Bitmap): Boolean {
        val rows = bitmap.height - LabelRows
        if (rows <= 0) return true
        val pixels = IntArray(bitmap.width * rows)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, LabelRows, bitmap.width, rows)
        return pixels.toSet().size <= 1
    }

    private fun sameStrip(a: Bitmap, b: Bitmap): Boolean = a.width == b.width && strip(a).contentEquals(strip(b))

    private fun stitch(parts: List<Bitmap>): Bitmap {
        val width = parts.maxOf { it.width }
        val height = parts.sumOf { it.height } + (parts.size - 1) * Divider
        val sheet = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(sheet)
        canvas.drawColor(parts.first().getPixel(0, parts.first().height - 1))
        val line = Paint().apply { color = AndroidColor.rgb(0x80, 0x80, 0x80) }
        var top = 0
        parts.forEachIndexed { index, part ->
            canvas.drawBitmap(part, 0f, top.toFloat(), null)
            top += part.height
            if (index < parts.size - 1) {
                canvas.drawRect(0f, top + 1f, width.toFloat(), top + 2f, line)
                top += Divider
            }
        }
        return sheet
    }

    companion object {
        private const val FrameTag = "monarch-screenshot-frame"
        private const val SettleMillis = 1000L
        private const val MaxSettleAttempts = 20
        private const val ConfirmFrames = 4
        private const val RetryPauseMillis = 50L
        private const val LabelStripRows = 11
        private const val LabelRows = 14
        private const val VsyncMillis = 17L
        private const val WindowSteps = 10
        private const val WindowStepMillis = 100L
        private const val PhoneHeight = 470
        private const val TallHeight = 960
        private const val Divider = 4

        /** One test per component; `-Pmonarch.screenshots.ids=a,b` limits the run. */
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun ids(): List<Array<String>> {
            val wanted = ScreenshotStore.fromSystemProperties().ids
            val all = ScreenshotTargets.all.map { it.id }
            return (wanted?.let { w -> all.filter { it in w } } ?: all).map { arrayOf(it) }
                .ifEmpty { listOf(arrayOf(all.first())) }
        }
    }
}
