package dev.monarchui.components.intelligence.aicomputerview

import androidx.compose.ui.geometry.Offset

/** What of the screenshot and the actions may be shown, and where the markers go. */
internal object AiComputerViewContent {
    private val Credentials = Regex("^([a-z][a-z0-9+.-]*://)[^/?#@]*@", RegexOption.IGNORE_CASE)

    /** The label of an action as it may be shown: a sensitive one is never named. */
    fun actionLabel(action: MonarchAiComputerAction, sensitiveLabel: String): String =
        if (action.sensitive) sensitiveLabel else action.label

    /** A sensitive action never shows its detail; typed text shows only with [showTypedText]. */
    fun detail(action: MonarchAiComputerAction, showTypedText: Boolean): AiComputerViewDetail = when {
        action.sensitive -> AiComputerViewDetail(null, hidden = true)
        action.detail.isNullOrEmpty() -> AiComputerViewDetail(null, hidden = false)
        action.kind == MonarchAiComputerActionKind.Type && !showTypedText -> AiComputerViewDetail(null, hidden = true)
        else -> AiComputerViewDetail(action.detail, hidden = false)
    }

    /**
     * [point] as fractions of the screenshot, from 0 to 1, clamped to its
     * edges; `null` when the point or the size is not usable.
     */
    fun position(point: MonarchAiComputerPoint?, width: Int, height: Int): Offset? {
        if (point == null || width <= 0 || height <= 0) return null
        val x = (point.x.toFloat() / width).coerceIn(0f, 1f)
        val y = (point.y.toFloat() / height).coerceIn(0f, 1f)
        return Offset(x, y)
    }

    /** The address without any `user:password@` part, so credentials never show. */
    fun displayUrl(url: String): String = Credentials.replace(url, "$1")

    /** The aspect ratio of the screen box: the screenshot's, or 16 by 10. */
    fun aspectRatio(frame: MonarchAiComputerFrame?): Float =
        if (frame != null && frame.width > 0 && frame.height > 0) {
            frame.width.toFloat() / frame.height
        } else {
            MonarchAiComputerViewTokens.DEFAULT_ASPECT_RATIO
        }

    /** A picture marked private, taken right after a private action, or a private live picture is never shown. */
    fun pictureHidden(
        frame: MonarchAiComputerFrame?,
        latest: MonarchAiComputerAction?,
        live: Boolean,
        liveSensitive: Boolean,
    ): Boolean = (frame != null || live) &&
        (frame?.sensitive == true || latest?.sensitive == true || (live && liveSensitive))

    /** The newest action with a point, the one the "last" marker shows. */
    fun lastPointAction(actions: List<MonarchAiComputerAction>): MonarchAiComputerAction? =
        actions.lastOrNull { it.point != null }

    /** How many older actions are behind "Show earlier". */
    fun hiddenBefore(count: Int, visible: Int, showAll: Boolean): Int =
        if (showAll) 0 else maxOf(0, count - maxOf(1, visible))
}
