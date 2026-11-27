package com.rookhq.monarch.components.navigation.tabs

import androidx.compose.runtime.Composable
import com.rookhq.monarch.foundation.MonarchTone

/** Declares the tabs of a `MonarchTabs`, in order. */
public interface MonarchTabsScope {
    /**
     * A tab.
     *
     * @param text the label. A tab never wraps its label: when the labels do not fit the row scrolls.
     * @param enabled whether the user can choose this tab. A disabled tab is announced as disabled.
     * @param icon an icon before the label, for example a `MonarchIcon` with a `null` content description.
     * @param count a short count after the label, for example "6", drawn as a small pill.
     * @param countTone the tone of the count pill. Neutral by default; use a status tone only when the count
     * means that status, and keep the meaning in the label too.
     * @param contentDescription what TalkBack reads instead of the label and the count, for example
     * "Open, 6 requests". `null` reads the label and the count.
     * @param secondary a second line under the label in the muted colour, for example the date under a weekday or
     * "Today". It makes the tab a rich face; keep it to a few characters, the row scrolls when tabs do not fit.
     * @param accent marks the tab as the current one in time ("today") with the accent colour on its text. The
     * selection stays the underline; say what the accent means in the text as well, here with [secondary].
     */
    public fun tab(
        text: String,
        enabled: Boolean = true,
        icon: (@Composable () -> Unit)? = null,
        count: String? = null,
        countTone: MonarchTone = MonarchTone.Neutral,
        contentDescription: String? = null,
        secondary: String? = null,
        accent: Boolean = false,
    )
}

/** One declared tab. */
internal class MonarchTabEntry(
    val text: String,
    val enabled: Boolean,
    val icon: (@Composable () -> Unit)?,
    val count: String?,
    val countTone: MonarchTone,
    val contentDescription: String?,
    val secondary: String?,
    val accent: Boolean,
)

/** Collects the tabs declared in the content of a `MonarchTabs`. */
internal class MonarchTabsScopeImpl : MonarchTabsScope {
    val entries: MutableList<MonarchTabEntry> = mutableListOf()

    override fun tab(
        text: String,
        enabled: Boolean,
        icon: (@Composable () -> Unit)?,
        count: String?,
        countTone: MonarchTone,
        contentDescription: String?,
        secondary: String?,
        accent: Boolean,
    ) {
        entries += MonarchTabEntry(text, enabled, icon, count, countTone, contentDescription, secondary, accent)
    }
}
