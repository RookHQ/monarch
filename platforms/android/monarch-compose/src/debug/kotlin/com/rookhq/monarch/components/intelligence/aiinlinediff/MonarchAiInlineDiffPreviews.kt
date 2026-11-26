@file:OptIn(ExperimentalMonarchApi::class)

package com.rookhq.monarch.components.intelligence.aiinlinediff

import androidx.compose.runtime.Composable
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

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
