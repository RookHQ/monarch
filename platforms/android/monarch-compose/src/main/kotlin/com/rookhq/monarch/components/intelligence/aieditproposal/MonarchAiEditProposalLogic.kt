package com.rookhq.monarch.components.intelligence.aieditproposal

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.rookhq.monarch.components.files.diffviewer.DiffViewerSegment
import com.rookhq.monarch.components.files.diffviewer.MonarchDiffViewerLineKind
import com.rookhq.monarch.components.files.diffviewer.diffViewerWords
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiEditProposalState
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.theme.MonarchColors

/** The pure logic of MonarchAiEditProposal: decisions, the diff and its text. */
internal object MonarchAiEditProposalLogic {
    /** The decision a change shows: the app's, else the remembered one, overruled by the proposal's state. */
    fun statusOf(
        hunk: MonarchAiEditHunk,
        decided: Map<String, MonarchAiEditHunkStatus>,
        state: MonarchAiEditProposalState,
    ): MonarchAiEditHunkStatus {
        val own = hunk.status ?: decided[hunk.id] ?: MonarchAiEditHunkStatus.Proposed
        return when (state) {
            MonarchAiEditProposalState.Applied ->
                if (own == MonarchAiEditHunkStatus.Rejected) own else MonarchAiEditHunkStatus.Accepted
            MonarchAiEditProposalState.Rejected -> MonarchAiEditHunkStatus.Rejected
            // Reverted edits are no longer in the document: no change reads "Accepted".
            MonarchAiEditProposalState.Reverted -> MonarchAiEditHunkStatus.Proposed
            MonarchAiEditProposalState.Proposed -> own
        }
    }

    /** The decisions after Accept all or Reject all: every undecided change gets [status]. */
    fun decideAll(
        hunks: List<MonarchAiEditHunk>,
        statuses: List<MonarchAiEditHunkStatus>,
        decided: Map<String, MonarchAiEditHunkStatus>,
        status: MonarchAiEditHunkStatus,
    ): Map<String, MonarchAiEditHunkStatus> = decided + hunks.filterIndexed { index, _ ->
        statuses[index] == MonarchAiEditHunkStatus.Proposed
    }.associate { it.id to status }

    /** The tone of the state badge. */
    fun stateTone(state: MonarchAiEditProposalState): MonarchTone = when (state) {
        MonarchAiEditProposalState.Proposed -> MonarchTone.Info
        MonarchAiEditProposalState.Applied -> MonarchTone.Success
        MonarchAiEditProposalState.Rejected -> MonarchTone.Neutral
        MonarchAiEditProposalState.Reverted -> MonarchTone.Warning
    }

    /** The word diff of a change. */
    fun diff(hunk: MonarchAiEditHunk): List<DiffViewerSegment> = diffViewerWords(hunk.before, hunk.after)

    /** Whether the diff changes anything. */
    fun changed(segments: List<DiffViewerSegment>): Boolean = segments.any { it.kind != MonarchDiffViewerLineKind.Same }

    /** The diff as styled text. */
    fun styled(
        segments: List<DiffViewerSegment>,
        status: MonarchAiEditHunkStatus,
        colors: MonarchColors,
    ): AnnotatedString = buildAnnotatedString {
        for (segment in segments) {
            if (segment.kind == MonarchDiffViewerLineKind.Same) {
                append(segment.text)
            } else {
                val span = MonarchAiEditProposalTokens.spanColors(segment.kind, status, colors)
                val style = SpanStyle(color = span.text, background = span.background, textDecoration = span.decoration)
                withStyle(style) {
                    append(segment.text)
                }
            }
        }
    }

    /** The diff as TalkBack reads it: removed and added text after "Deleted:" and "Inserted:". */
    fun spoken(segments: List<DiffViewerSegment>, deleted: String, inserted: String): String =
        segments.joinToString("") { segment ->
            when (segment.kind) {
                MonarchDiffViewerLineKind.Same -> segment.text
                MonarchDiffViewerLineKind.Removed -> " $deleted ${segment.text}"
                MonarchDiffViewerLineKind.Added -> " $inserted ${segment.text}"
            }
        }.replace(Spaces, " ").trim()

    private val Spaces = Regex("\\s+")

    /** The index of the next undecided change after [from], else the first before it, else `null`. */
    fun nextUndecided(statuses: List<MonarchAiEditHunkStatus>, from: Int): Int? {
        val undecided = statuses.indices.filter { statuses[it] == MonarchAiEditHunkStatus.Proposed && it != from }
        return undecided.firstOrNull { it > from } ?: undecided.lastOrNull { it < from }
    }
}
