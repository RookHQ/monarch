package com.rookhq.monarch.components.collaboration.emojipicker

import android.view.ContextThemeWrapper
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.util.Consumer
import androidx.emoji2.emojipicker.EmojiPickerView
import androidx.emoji2.emojipicker.EmojiViewItem
import androidx.emoji2.emojipicker.RecentEmojiProvider
import com.rookhq.monarch.R
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.theme.MonarchTheme

/**
 * An emoji picker: the emoji in categories with tabs, the recently used ones
 * first, and the skin-tone and gender variants on a long press, drawn with
 * the emoji font the device supports.
 *
 * Android has this picker in AndroidX, so Monarch hosts the system
 * `androidx.emoji2.emojipicker.EmojiPickerView` in an `AndroidView` rather
 * than drawing one. The view keeps the recently used emoji itself, in the
 * app's shared preferences. It has no search field; the keyboard's emoji
 * search covers that. Put the picker in a `ModalBottomSheet` opened from a
 * reaction or emoji button, or pass it to `MonarchReactionBar`'s `picker`.
 *
 * The picker names its area for TalkBack with [label]; every emoji in the
 * grid is read by the view.
 *
 * @param onEmojiPicked called with the emoji the person picks, including its chosen variant.
 * @param modifier the modifier of the picker. It fills the width and is 320dp high unless the modifier sets a
 * size first.
 * @param columns the number of emoji per row. `null` keeps the view's 9.
 * @param rows the number of rows visible at once, a fraction shows that the grid scrolls. `null` keeps the
 * view's 7.5.
 * @param label the name of the picker for TalkBack: "Emoji" (`monarch_emojipicker_label`) by default.
 * @param recentlyUsed the emoji of the "Recently used" row, newest first. `null` keeps what the view has recorded
 * in the app's shared preferences; a list (even an empty one) replaces that record, for a demo or a test.
 */
@Composable
public fun MonarchEmojiPicker(
    onEmojiPicked: (String) -> Unit,
    modifier: Modifier = Modifier,
    columns: Int? = null,
    rows: Float? = null,
    label: String? = null,
    recentlyUsed: List<String>? = null,
) {
    val name = monarchString(label, R.string.monarch_emojipicker_label)
    val picked by rememberUpdatedState(onEmojiPicked)
    // The view draws its headings and tabs in the colours of its context's theme, which follows the system: give it
    // the one of the Monarch theme, so a dark theme on a light system is not black on dark.
    val themeRes = if (MonarchTheme.isDark) android.R.style.Theme_Material else android.R.style.Theme_Material_Light
    key(themeRes) {
        AndroidView(
            factory = { context ->
                EmojiPickerView(ContextThemeWrapper(context, themeRes)).apply {
                    val listener = MonarchEmojiPickedListener { emoji -> picked(emoji) }
                    tag = listener
                    setOnEmojiPickedListener(listener)
                    recentlyUsed?.let { setRecentEmojiProvider(MonarchFixedRecentEmoji(it)) }
                }
            },
            modifier = modifier
                .fillMaxWidth()
                .height(MonarchEmojiPickerTokens.Height)
                .semantics {
                    paneTitle = name
                    isTraversalGroup = true
                },
            update = { view ->
                columns?.let { if (view.emojiGridColumns != it) view.emojiGridColumns = it }
                rows?.let { if (view.emojiGridRows != it) view.emojiGridRows = it }
            },
        )
    }
}

/** Passes the picked emoji of the view on to the composable's latest callback. */
internal class MonarchEmojiPickedListener(private val onPicked: (String) -> Unit) : Consumer<EmojiViewItem> {
    override fun accept(value: EmojiViewItem) {
        onPicked(value.emoji)
    }
}

/** The recently used emoji as a fixed list: a pick is not recorded. */
internal class MonarchFixedRecentEmoji(private val emoji: List<String>) : RecentEmojiProvider {
    override suspend fun getRecentEmojiList(): List<String> = emoji

    override fun recordSelection(emoji: String) = Unit
}
