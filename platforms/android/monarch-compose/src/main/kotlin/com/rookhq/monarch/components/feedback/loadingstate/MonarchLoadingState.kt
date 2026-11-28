package com.rookhq.monarch.components.feedback.loadingstate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.components.feedback.skeleton.MonarchSkeleton
import com.rookhq.monarch.components.feedback.skeleton.MonarchSkeletonShape
import com.rookhq.monarch.internal.MonarchLiveStatus
import com.rookhq.monarch.theme.MonarchTheme

/**
 * A placeholder for a region that is loading: skeleton rows, cards or a
 * detail layout, or a progress ring with a label.
 *
 * Built from `MonarchSkeleton` placeholders, or a Material 3
 * `CircularProgressIndicator` in the spinner variant. The skeletons are
 * hidden from TalkBack; [label] is announced once through a polite live
 * region and can be reached by swiping. In the spinner variant the label is
 * also shown under the ring. The skeleton shimmer stands still when the user
 * has turned animations off.
 *
 * @param label what is loading, already localised: "Loading tickets".
 * @param modifier the modifier of the region.
 * @param variant the placeholder layout.
 * @param rows the rows or cards drawn by `List`, `Rows` and `Cards`. Negative counts draw none.
 */
@Composable
public fun MonarchLoadingState(
    label: String,
    modifier: Modifier = Modifier,
    variant: MonarchLoadingStateVariant = MonarchLoadingStateVariant.List,
    rows: Int = MonarchLoadingStateTokens.DEFAULT_ROWS,
) {
    // Filled after the first frame: a live region whose text is there from the start is not read reliably.
    var ready by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { ready = true }
    val spoken = if (ready) label else ""
    if (variant == MonarchLoadingStateVariant.Spinner) {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(MonarchLoadingStateTokens.SpinnerGap),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.clearAndSetSemantics { },
                color = MonarchTheme.colors.accentSolid,
                trackColor = MonarchTheme.colors.bgEmphasis,
            )
            Text(
                text = label,
                modifier = Modifier.semantics {
                    liveRegion = LiveRegionMode.Polite
                    if (spoken.isNotEmpty()) contentDescription = spoken
                },
                style = MonarchTheme.typography.label,
                color = MonarchTheme.colors.textMuted,
            )
        }
        return
    }
    Box(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(MonarchLoadingStateTokens.RowGap)) {
            when (variant) {
                MonarchLoadingStateVariant.Detail -> DetailPlaceholder()
                else -> repeat(MonarchLoadingStateTokens.rowCount(rows)) { RowPlaceholder(variant, it) }
            }
        }
        MonarchLiveStatus(text = spoken.takeIf { it.isNotEmpty() }, visible = false)
    }
}

@Composable
private fun RowPlaceholder(variant: MonarchLoadingStateVariant, index: Int) {
    val (first, second) = MonarchLoadingStateTokens.lineWidths(index)
    when (variant) {
        MonarchLoadingStateVariant.Cards -> MonarchSkeleton(Modifier.height(MonarchLoadingStateTokens.CardHeight))
        MonarchLoadingStateVariant.Rows -> Row(
            horizontalArrangement = Arrangement.spacedBy(MonarchLoadingStateTokens.ItemGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.weight(1f)) { MonarchSkeleton(Modifier.fillMaxWidth(first), MonarchSkeletonShape.Text) }
            MonarchSkeleton(Modifier.fillMaxWidth(MonarchLoadingStateTokens.META_WIDTH), MonarchSkeletonShape.Text)
        }
        else -> Row(
            horizontalArrangement = Arrangement.spacedBy(MonarchLoadingStateTokens.ItemGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MonarchSkeleton(Modifier.size(MonarchLoadingStateTokens.AvatarSize), MonarchSkeletonShape.Circle)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(MonarchLoadingStateTokens.LineGap),
            ) {
                MonarchSkeleton(Modifier.fillMaxWidth(first), MonarchSkeletonShape.Text)
                MonarchSkeleton(Modifier.fillMaxWidth(second), MonarchSkeletonShape.Text)
            }
        }
    }
}

@Composable
private fun DetailPlaceholder() {
    MonarchLoadingStateTokens.DetailWidths.forEach { width ->
        MonarchSkeleton(Modifier.fillMaxWidth(width), MonarchSkeletonShape.Text)
    }
}
