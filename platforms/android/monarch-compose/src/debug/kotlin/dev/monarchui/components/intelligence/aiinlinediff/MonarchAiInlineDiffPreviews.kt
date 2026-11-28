@file:OptIn(ExperimentalMonarchApi::class)

package dev.monarchui.components.intelligence.aiinlinediff

import androidx.compose.runtime.Composable
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

private val PreviewSegments = MonarchAiDiffSegment.diffWords(
    "Our team meets every week to review the plan.",
    "Our team meets every Monday to review the roadmap.",
)

@MonarchPreviews
@Composable
internal fun MonarchAiInlineDiffPreview() {
    PreviewFrame {
        MonarchAiInlineDiff(segments = PreviewSegments, onAccept = {}, onReject = {})
        MonarchAiInlineDiff(segments = PreviewSegments, showNavigation = false)
        MonarchAiInlineDiff(segments = MonarchAiDiffSegment.diffWords("No changes here.", "No changes here."))
    }
}
