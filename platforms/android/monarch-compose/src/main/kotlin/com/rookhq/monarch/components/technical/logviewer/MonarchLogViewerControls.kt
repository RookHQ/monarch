package com.rookhq.monarch.components.technical.logviewer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

/**
 * The filters, search, follow and wrap states of a MonarchLogViewer: the
 * app's value when it controls one, otherwise the viewer's own, and the
 * announcements of following pausing and resuming.
 */
@Stable
internal class MonarchLogViewerControls(
    private val ownLevels: MutableState<Set<MonarchLogLevel>>,
    private val ownQuery: MutableState<String>,
    private val ownFollowing: MutableState<Boolean>,
    private val ownWrapped: MutableState<Boolean>,
) {
    private var levelsValue: Set<MonarchLogLevel>? = null
    private var queryValue: String? = null
    private var followingValue: Boolean? = null
    private var wrappedValue: Boolean? = null
    private var onLevelsChange: ((Set<MonarchLogLevel>) -> Unit)? = null
    private var onQueryChange: ((String) -> Unit)? = null
    private var onFollowingChange: ((Boolean) -> Unit)? = null
    private var onWrappedChange: ((Boolean) -> Unit)? = null
    private var pausedText: String = ""
    private var resumedText: String = ""

    /** The text TalkBack reads next: following paused or resumed. */
    var announcement: String by mutableStateOf("")
        private set

    /** Following was paused by a drag toward older lines, so reaching the end resumes it. */
    var pausedByDrag: Boolean by mutableStateOf(false)
        private set

    /** How many lines arrived since following was paused. */
    var newLines: Int = 0

    val levels: Set<MonarchLogLevel> get() = levelsValue ?: ownLevels.value
    val query: String get() = queryValue ?: ownQuery.value
    val following: Boolean get() = followingValue ?: ownFollowing.value
    val wrapped: Boolean get() = wrappedValue ?: ownWrapped.value

    /** Takes the app's values and callbacks of this composition. */
    fun update(values: MonarchLogViewerValues, texts: MonarchLogViewerTexts) {
        levelsValue = values.levels
        queryValue = values.query
        followingValue = values.following
        wrappedValue = values.wrapped
        onLevelsChange = values.onLevelsChange
        onQueryChange = values.onQueryChange
        onFollowingChange = values.onFollowingChange
        onWrappedChange = values.onWrappedChange
        pausedText = texts.followPaused
        resumedText = texts.followResumed
    }

    fun setLevels(levels: Set<MonarchLogLevel>) {
        ownLevels.value = levels
        onLevelsChange?.invoke(levels)
    }

    fun setQuery(query: String) {
        ownQuery.value = query
        onQueryChange?.invoke(query)
    }

    fun setWrapped(wrapped: Boolean) {
        ownWrapped.value = wrapped
        onWrappedChange?.invoke(wrapped)
    }

    /** Starts or stops following; [byDrag] marks a pause the end of the log undoes. Changes are announced. */
    fun setFollowing(following: Boolean, byDrag: Boolean) {
        pausedByDrag = byDrag && !following
        if (following == this.following) return
        ownFollowing.value = following
        onFollowingChange?.invoke(following)
        val text = if (following) resumedText else pausedText
        // The same text twice in a row is still announced.
        announcement = if (text == announcement) "$text " else text
    }

    /** Empties the search and turns every level back on. */
    fun clearFilters() {
        setQuery("")
        setLevels(MonarchLogViewerTokens.AllLevels)
    }
}

/** The app's values and callbacks for one composition of a MonarchLogViewer. */
internal class MonarchLogViewerValues(
    val levels: Set<MonarchLogLevel>?,
    val query: String?,
    val following: Boolean?,
    val wrapped: Boolean?,
    val onLevelsChange: ((Set<MonarchLogLevel>) -> Unit)?,
    val onQueryChange: ((String) -> Unit)?,
    val onFollowingChange: ((Boolean) -> Unit)?,
    val onWrappedChange: ((Boolean) -> Unit)?,
)

/** The controls of a MonarchLogViewer; its own values survive configuration changes. */
@Composable
internal fun rememberMonarchLogViewerControls(): MonarchLogViewerControls {
    val levels = rememberSaveable { mutableStateOf(MonarchLogViewerTokens.AllLevels) }
    val query = rememberSaveable { mutableStateOf("") }
    val following = rememberSaveable { mutableStateOf(true) }
    val wrapped = rememberSaveable { mutableStateOf(false) }
    return remember { MonarchLogViewerControls(levels, query, following, wrapped) }
}
