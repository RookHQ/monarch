package dev.monarchui.components.intelligence.aireasoning

import androidx.compose.runtime.compositionLocalOf

/**
 * Whether a reasoning disclosure is set small and muted, the chevron included: inside a message, where the
 * reasoning is a quiet line above the answer, as on the web. Everywhere else it keeps the assistant colour.
 */
internal val LocalMonarchAiReasoningQuiet = compositionLocalOf { false }
