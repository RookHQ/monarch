package dev.monarchui.testing

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.monarchui.accessibility.MonarchTouchTarget
import kotlin.test.assertTrue

/** The text layout of a text node. Use with an unmerged node. */
internal fun SemanticsNodeInteraction.textLayout(): TextLayoutResult {
    val results = mutableListOf<TextLayoutResult>()
    val action = fetchSemanticsNode().config.getOrNull(SemanticsActions.GetTextLayoutResult)
    checkNotNull(action?.action) { "Node has no text layout" }.invoke(results)
    return results.first()
}

/** The state description of a node, or `null`. */
internal fun SemanticsNodeInteraction.stateDescription(): String? =
    fetchSemanticsNode().config.getOrNull(SemanticsProperties.StateDescription)

/**
 * Whether every line of the text is inside its layout and nothing is
 * ellipsized. `hasVisualOverflow` is not used because it is also true for
 * text that is narrower than the width it was offered.
 */
internal fun TextLayoutResult.isFullyVisible(): Boolean {
    val lines = 0 until lineCount
    val insideWidth = lines.all { getLineLeft(it) >= -1f && getLineRight(it) <= size.width + 1f }
    return !didOverflowHeight && insideWidth && lines.none { isLineEllipsized(it) }
}

/**
 * Font scale: asserts that the text of this node is not cut off or ellipsized.
 * Use with an unmerged text node, at `LargestFontScale`.
 */
internal fun SemanticsNodeInteraction.assertTextFullyVisible(): SemanticsNodeInteraction {
    assertTrue(textLayout().isFullyVisible(), "Text is cut off or ellipsized")
    return this
}

/**
 * The smaller side of the touch target of a node, in dp: the smaller of the
 * area that receives taps and the space the element reserves in the layout.
 *
 * Compose extends the tap area of any small element to 48dp, also over its
 * neighbours. Only reserved layout space guarantees that two targets do not
 * overlap, so both are measured.
 */
internal fun SemanticsNode.minimumTouchSide(density: Density): Dp = with(density) {
    val tapArea = minOf(touchBoundsInRoot.width, touchBoundsInRoot.height).toDp()
    val reserved = minOf(layoutInfo.width, layoutInfo.height).toDp()
    minOf(tapArea, reserved)
}

/** The smaller side of the touch target of this node, in dp. See [SemanticsNode.minimumTouchSide]. */
internal fun SemanticsNodeInteraction.minimumTouchSide(density: Density): Dp =
    fetchSemanticsNode().minimumTouchSide(density)

/**
 * Touch target: asserts that this element receives taps in, and reserves in
 * the layout, at least 48dp by 48dp. The visible bounds may be smaller.
 */
internal fun SemanticsNodeInteraction.assertTouchTargetAtLeast48dp(density: Density): SemanticsNodeInteraction {
    val side = minimumTouchSide(density)
    assertTrue(side >= MonarchTouchTarget.Minimum - TouchTolerance, "Touch target is $side, expected at least 48dp")
    return this
}

/**
 * Right to left: asserts that this node comes before [other] in reading
 * order, that is to its left in [LayoutDirection.Ltr] and to its right in
 * [LayoutDirection.Rtl]. Use unmerged nodes.
 */
internal fun SemanticsNodeInteraction.assertIsBefore(
    other: SemanticsNodeInteraction,
    layoutDirection: LayoutDirection,
): SemanticsNodeInteraction {
    val mine = getUnclippedBoundsInRoot()
    val theirs = other.getUnclippedBoundsInRoot()
    val before = if (layoutDirection == LayoutDirection.Ltr) mine.left < theirs.left else mine.right > theirs.right
    assertTrue(before, "Expected to be before the other node in $layoutDirection: $mine and $theirs")
    return this
}

/** Rounding of dp to pixels can lose a fraction of a dp. */
internal val TouchTolerance: Dp = 0.5.dp

/**
 * [this] with the narrow no-break space (U+202F) and the no-break space
 * (U+00A0) as plain spaces. ICU puts them before "AM"/"PM" and in some
 * dates, and the choice changes between ICU versions, so a test that
 * compares written times compares `expected.withPlainSpaces()` with
 * `actual.withPlainSpaces()`.
 */
internal fun String.withPlainSpaces(): String = replace('\u202F', ' ').replace('\u00A0', ' ')

/**
 * Whether this node is a link span inside a `Text` (a `LinkAnnotation`). Compose gives each such span its
 * own node marked with the semantics property `LinkTestMarker`, which is internal to Compose, so it is
 * matched by its name. [MonarchAssertionsTest] guards the marker: if Compose renames it, that test fails
 * instead of the exemption silently stopping.
 */
internal fun SemanticsNode.isTextLink(): Boolean = config.any { (key, _) -> key.name == TextLinkMarker }

/** The name of Compose's semantics property on a link span of a `Text`. */
internal const val TextLinkMarker = "LinkTestMarker"


/**
 * The node whose text, or whose content description, is [text]: a control is a text button without an icon and an
 * icon button, named by its description, with one (the default icons of a component are drawn by Monarch).
 */
internal fun SemanticsNodeInteractionsProvider.onNodeWithTextOrDescription(
    text: String,
    substring: Boolean = false,
    ignoreCase: Boolean = false,
    useUnmergedTree: Boolean = false,
): SemanticsNodeInteraction = onNode(
    hasText(text, substring, ignoreCase) or hasContentDescription(text, substring, ignoreCase),
    useUnmergedTree,
)
