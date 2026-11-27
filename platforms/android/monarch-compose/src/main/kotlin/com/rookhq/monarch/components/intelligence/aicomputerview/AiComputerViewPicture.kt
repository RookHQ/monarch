package com.rookhq.monarch.components.intelligence.aicomputerview

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.painter.Painter

/** What the screen box shows: the live picture, the screenshot, or a placeholder, and what TalkBack reads. */
@Stable
internal class AiComputerViewPicture(
    val showLive: Boolean,
    val image: Painter?,
    val alt: String,
    val placeholder: String?,
    val spoken: String,
)

/** Decides what the screen box shows from the privacy rules, and its text alternative. */
internal fun aiComputerViewPicture(
    frame: MonarchAiComputerFrame?,
    latest: MonarchAiComputerAction?,
    hasLive: Boolean,
    liveSensitive: Boolean,
    texts: AiComputerViewTexts,
): AiComputerViewPicture {
    val hidden = AiComputerViewContent.pictureHidden(frame, latest, hasLive, liveSensitive)
    val showLive = hasLive && !hidden
    val image = frame?.image?.takeIf { !showLive && !hidden }
    val alt = aiComputerViewAlt(frame, hidden, showLive, texts)
    val latestLabel = latest?.let { AiComputerViewContent.actionLabel(it, texts.sensitiveLabel) }
    val description = texts.frameDescription(alt, latestLabel)
    // Without a picture the box says why, then what it would show; a hidden picture says it once.
    val placeholder = when {
        showLive || image != null || hidden -> null
        frame != null -> texts.frameUnavailable
        else -> texts.noFrame
    }
    return AiComputerViewPicture(
        showLive = showLive,
        image = image,
        alt = alt,
        placeholder = if (hidden) texts.frameHidden else placeholder,
        spoken = placeholder?.let { "$it. $description" } ?: description,
    )
}

/** The text alternative of the picture: hidden, its own, or the default for a live picture or a screenshot. */
private fun aiComputerViewAlt(
    frame: MonarchAiComputerFrame?,
    hidden: Boolean,
    showLive: Boolean,
    texts: AiComputerViewTexts,
): String {
    val own = frame?.alt?.takeIf { it.isNotBlank() }
    return when {
        hidden -> texts.frameHidden
        own != null -> own
        showLive -> texts.liveAlt
        else -> texts.frameAlt
    }
}
