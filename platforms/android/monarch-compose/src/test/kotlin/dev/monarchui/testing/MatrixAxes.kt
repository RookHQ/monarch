package dev.monarchui.testing

import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.TextLayoutResult
import dev.monarchui.accessibility.MonarchTouchTarget
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min
import kotlin.test.fail
import org.robolectric.shadows.ShadowLooper

/**
 * The checks `ComponentMatrixTest` can run on a component, switched per inventory id.
 *
 * Every component gets the baseline checks: composition does not fail, something is laid out, and every
 * enabled element that takes a tap has a 48dp target. The axes below are stricter and are switched on per
 * primitive, as each primitive is restyled to the Monarch layer (visual spec, plan.md gate 3b). A component
 * that has not landed yet keeps the default and is not held to a standard it was not built for.
 *
 * @property touchTarget every enabled tap element reserves and receives 48dp by 48dp. On for everything; the
 * flag exists so the axis is named and a test of the checker can turn it off.
 * @property fontScaleNoClipping at font scale 2.0 no text is cut off, ellipsized or clipped by the window:
 * every label reads in full.
 * @property textContrast in every theme, dark and high contrast included, every enabled text reaches the
 * WCAG 1.4.3 ratio (4.5, or 3.0 for text of 24sp and larger) against what is drawn behind it, measured on
 * the rendered pixels.
 */
internal data class MatrixAxes(
    val touchTarget: Boolean = true,
    val fontScaleNoClipping: Boolean = false,
    val textContrast: Boolean = false,
) {
    companion object {
        /** Every axis on: what a restyled primitive is held to. */
        val Strict = MatrixAxes(touchTarget = true, fontScaleNoClipping = true, textContrast = true)
    }
}

/**
 * Which inventory ids opted in to which axes. A primitive's stream adds its ids here when its row on the
 * review sheet has no open item, for example `"text-field" to MatrixAxes.Strict`. The environment variable
 * `MONARCH_MATRIX_STRICT=all` runs every id strict, to see where the library stands today.
 */
internal object StrictMatrix {
    private val optedIn: Map<String, MatrixAxes> = mapOf(
        // W3 (containers, navigation, buttons)
        "button" to MatrixAxes.Strict,
        "icon-button" to MatrixAxes.Strict,
        "tabs" to MatrixAxes.Strict,
        "card" to MatrixAxes.Strict,
        "spinner" to MatrixAxes.Strict,
        // W1 (fields)
        "currency-input" to MatrixAxes.Strict,
        "form-field" to MatrixAxes.Strict,
        "number-input" to MatrixAxes.Strict,
        "password-input" to MatrixAxes.Strict,
        "text-field" to MatrixAxes.Strict,
        // W2: selection controls.
        "checkbox" to MatrixAxes.Strict,
        "radio-group" to MatrixAxes.Strict,
        "switch" to MatrixAxes.Strict,
        "slider" to MatrixAxes.Strict,
        "range-slider" to MatrixAxes.Strict,
        "segmented-control" to MatrixAxes.Strict,
        "tag" to MatrixAxes.Strict,
        "checkbox-group" to MatrixAxes.Strict,
        "radio-card-group" to MatrixAxes.Strict,
        "switch-settings-list" to MatrixAxes.Strict,
    )

    private val all: Boolean = System.getenv("MONARCH_MATRIX_STRICT") == "all"

    fun axesOf(id: String): MatrixAxes = if (all) MatrixAxes.Strict else optedIn[id] ?: MatrixAxes()
}

/** Runs the matrix axes on the content of [frameTag]. */
internal class MatrixChecker(private val rule: ComposeContentTestRule, private val frameTag: String) {
    /** The view that hosts the fixture, set by the matrix from inside the content; used to ask for a redraw. */
    var host: android.view.View? = null

    /** Enabled elements with a click, toggle or select action. */
    private val takesTaps = SemanticsMatcher("takes taps") { node ->
        node.config.contains(SemanticsActions.OnClick) && !node.config.contains(SemanticsProperties.Disabled)
    }

    /** Touch target axis: every enabled tap element has at least 48dp, both received and reserved. */
    fun touchTargets(where: String) {
        rule.onAllNodes(takesTaps).fetchSemanticsNodes().forEach { node ->
            if (!node.isClipped() && !node.isTextLink()) {
                val side = node.minimumTouchSide(rule.density)
                if (side < MonarchTouchTarget.Minimum - TouchTolerance) {
                    fail("$where: the touch target of ${describe(node)} is $side, expected at least 48dp")
                }
            }
        }
    }

    /** Font scale axis: no text is ellipsized, wrapped out of its box, or outside the window. */
    fun textNotCut(where: String) {
        val root = rule.onNodeWithTag(frameTag).fetchSemanticsNode()
        for (node in textNodes(root)) {
            val layout = layoutOf(node) ?: continue
            if (!layout.isFullyVisible()) fail("$where: the text \"${textOf(node)}\" is cut off or ellipsized")
            if (node.isClipped()) fail("$where: the text \"${textOf(node)}\" is clipped by its container or the window")
        }
    }

    /** Contrast axis: the pixels of every enabled text reach [ratio] against the background drawn behind it. */
    fun textContrast(where: String) {
        val root = rule.onNodeWithTag(frameTag).fetchSemanticsNode()
        val image = captureDrawnFrame(root)
        for (node in measurableTextNodes(root)) {
            val measured = measuredContrast(node, root, image)
                ?: if (isEditable(node)) {
                    continue
                } else {
                    fail("$where: the text \"${textOf(node)}\" drew nothing that can be measured")
                }
            val needed = neededContrast(node)
            if (measured < needed) {
                fail(
                    "$where: the text \"${textOf(node)}\" has contrast " +
                        "${"%.2f".format(measured)}, expected at least $needed",
                )
            }
        }
    }

    /**
     * Captures the frame once every text in it has drawn. Robolectric can hand back the first capture
     * before the first frame is drawn, so the capture is retried for a few frames before it is judged.
     */
    private fun captureDrawnFrame(root: SemanticsNode): ImageBitmap {
        var image = settleAndCapture()
        repeat(DrawRetries) {
            val drawn = measurableTextNodes(root).all { isEditable(it) || measuredContrast(it, root, image) != null }
            if (drawn) return image
            Thread.sleep(DrawRetryPauseMillis)
            // A first frame that never drew is asked for again: the host view is invalidated, not just waited for.
            host?.let { view -> rule.runOnUiThread { view.invalidate() } }
            // A frame that has not drawn yet may be waiting for animation time, not wall time.
            rule.mainClock.advanceTimeBy(RetryAdvanceMillis)
            image = settleAndCapture()
        }
        return image
    }

    /** Runs the frames and the main looper that Robolectric holds until told, then captures the frame twice. */
    private fun settleAndCapture(): ImageBitmap {
        rule.mainClock.autoAdvance = false
        Snapshot.sendApplyNotifications()
        repeat(SettleFrames) { rule.mainClock.advanceTimeByFrame() }
        ShadowLooper.idleMainLooper(VsyncMillis, TimeUnit.MILLISECONDS)
        ShadowLooper.idleMainLooper(VsyncMillis, TimeUnit.MILLISECONDS)
        rule.waitForIdle()
        // The first capture draws the pending frame; the second one is the frame itself.
        rule.onNodeWithTag(frameTag).captureToImage()
        return rule.onNodeWithTag(frameTag).captureToImage()
    }

    /** The enabled texts, editable fields included (see [isEditable] for how a field is measured). */
    private fun measurableTextNodes(root: SemanticsNode): List<SemanticsNode> = textNodes(root).filterNot {
        it.config.contains(SemanticsProperties.Disabled)
    }

    /**
     * An editable field's node spans its whole decoration (label, border, helper), so the pixels inside its bounds
     * are mostly the field's own fill and border, not glyphs. It is therefore measured the other way round: the
     * colour its text style is drawn in (the engine's `textStyle`) against the colour that fills most of the node,
     * which is the fill behind the text. A field whose style colour is unspecified, or whose node is a single
     * colour, is not measured here: nothing says what the text is drawn in, and the field's own test covers it.
     */
    private fun isEditable(node: SemanticsNode): Boolean = node.config.contains(SemanticsProperties.EditableText)

    /** The contrast a text node must reach: large text needs less than normal text. */
    private fun neededContrast(node: SemanticsNode): Double {
        val size = layoutOf(node)?.layoutInput?.style?.fontSize?.value ?: 0f
        return if (size >= LargeTextSp) LargeTextRatio else NormalTextRatio
    }

    /** The measured contrast of a text node, or null when its area holds a single colour (nothing was drawn). */
    private fun measuredContrast(node: SemanticsNode, root: SemanticsNode, image: ImageBitmap): Double? {
        val bounds = node.boundsInRoot.translate(-root.positionInRoot.x, -root.positionInRoot.y)
        // Thin 12sp strokes never reach their full colour in a pixel, so use the colour the text is drawn in.
        val drawn = layoutOf(node)?.layoutInput?.style?.color?.takeIf { it.isSpecified }
        return measureContrast(image, bounds, drawn)
    }

    private fun textNodes(root: SemanticsNode): List<SemanticsNode> {
        val found = mutableListOf<SemanticsNode>()
        fun walk(node: SemanticsNode) {
            if (node.config.contains(SemanticsActions.GetTextLayoutResult)) found += node
            node.children.forEach(::walk)
        }
        walk(root)
        return found
    }

    private fun layoutOf(node: SemanticsNode): TextLayoutResult? {
        val results = mutableListOf<TextLayoutResult>()
        node.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(results)
        return results.firstOrNull()
    }

    private fun textOf(node: SemanticsNode): String =
        node.config.getOrNull(SemanticsProperties.Text)?.joinToString { it.text } ?: ""

    private fun SemanticsNode.isClipped(): Boolean =
        boundsInRoot.width < size.width - 1 || boundsInRoot.height < size.height - 1

    private fun describe(node: SemanticsNode): String {
        val text = node.config.getOrNull(SemanticsProperties.Text)?.joinToString { it.text }
        val description = node.config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString()
        val role = node.config.getOrNull(SemanticsProperties.Role)
        return listOfNotNull(role?.toString(), text ?: description).joinToString(" ").ifEmpty { "an unnamed element" }
    }

    private companion object {
        const val NormalTextRatio = 4.5
        const val LargeTextRatio = 3.0
        const val LargeTextSp = 24f
        // The first fixture of a cold test JVM can take seconds to draw on a busy machine: wait up to 3 s, not 1 s.
        const val DrawRetries = 60
        const val DrawRetryPauseMillis = 50L
        const val RetryAdvanceMillis = 100L
        const val SettleFrames = 3
        const val VsyncMillis = 16L
    }
}

/**
 * The contrast ratio between the background and the text pixels inside [bounds] of [image]: the background
 * is the most common colour, the text is the colour farthest from it. `null` when the area has one colour
 * (nothing drawn, so nothing to measure). With [textColor], the text is that colour (the glyph core, which thin
 * small strokes never reach in a pixel) and only the background is measured.
 */
internal fun measureContrast(image: ImageBitmap, bounds: Rect, textColor: Color? = null): Double? {
    val width = image.width
    val height = image.height
    val left = max(0, bounds.left.toInt())
    val top = max(0, bounds.top.toInt())
    val right = min(width, bounds.right.toInt())
    val bottom = min(height, bounds.bottom.toInt())
    if (right <= left || bottom <= top) return null
    val pixels = IntArray(width * height)
    image.readPixels(pixels, 0, 0, width, height, 0, width)
    val counts = HashMap<Int, Int>()
    for (y in top until bottom) for (x in left until right) counts.merge(pixels[y * width + x], 1, Int::plus)
    val backgroundLuminance = luminance(counts.maxBy { it.value }.key)
    return when {
        counts.size < 2 -> null
        textColor != null -> contrastRatio(luminance(textColor.toArgb()), backgroundLuminance)
        else -> counts.keys.maxOf { contrastRatio(luminance(it), backgroundLuminance) }
    }
}

/** WCAG relative luminance of an ARGB colour. */
internal fun luminance(argb: Int): Double {
    fun channel(value: Int): Double {
        val c = value / 255.0
        return if (c <= 0.03928) c / 12.92 else Math.pow((c + 0.055) / 1.055, 2.4)
    }
    return 0.2126 * channel(argb shr 16 and 0xFF) +
        0.7152 * channel(argb shr 8 and 0xFF) +
        0.0722 * channel(argb and 0xFF)
}

/** WCAG contrast ratio between two relative luminances. */
internal fun contrastRatio(a: Double, b: Double): Double = (max(a, b) + 0.05) / (min(a, b) + 0.05)

/** The contrast ratio between two colours. */
internal fun contrastRatio(a: Color, b: Color): Double = contrastRatio(luminance(a.toArgb()), luminance(b.toArgb()))
