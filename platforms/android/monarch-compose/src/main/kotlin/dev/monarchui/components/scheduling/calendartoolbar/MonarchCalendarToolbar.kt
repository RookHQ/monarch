package dev.monarchui.components.scheduling.calendartoolbar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import dev.monarchui.R
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.iconbutton.MonarchIconButton
import dev.monarchui.components.datadisplay.progress.MonarchProgress
import dev.monarchui.components.datadisplay.progress.MonarchProgressVariant
import androidx.compose.ui.platform.LocalDensity
import dev.monarchui.components.forms.segmentedcontrol.MonarchSegmentedControl
import dev.monarchui.components.forms.segmentedcontrol.MonarchSegmentedControlSize
import dev.monarchui.components.overlays.menu.MonarchMenu
import dev.monarchui.components.overlays.menu.MonarchMenuItem
import dev.monarchui.internal.monarchString
import dev.monarchui.theme.MonarchTheme

/**
 * The header of a calendar: Today, previous and next, the title of the
 * range shown with an optional busy indicator, a switch between
 * the day, week, month and agenda views, and the app's own actions.
 *
 * Built by Monarch from `MonarchButton`, `MonarchIconButton`s for the
 * arrows and a `MonarchSegmentedControl` for up to four views (a `MonarchMenu` for more, or for large text), in a
 * `FlowRow`: when one line does not fit (a narrow screen, a long title, a large font size)
 * the parts wrap onto further lines instead of being cut. Navigation is the
 * app's: the toolbar only calls back.
 *
 * @param title the range shown, already formatted: "November 2026", "2 – 8 Nov 2026". A heading, announced
 * politely when it changes.
 * @param view the current view. It names the arrows ("Previous week") and is marked in the switcher.
 * @param onToday moves to the range that contains today.
 * @param onPrevious moves back one unit of the view.
 * @param onNext moves forward one unit of the view.
 * @param previousIcon the backward chevron, for example a `MonarchIcon` with `mirrorInRtl = true` and a `null`
 * description. Monarch bundles no icons.
 * @param nextIcon the forward chevron, mirrored right to left like [previousIcon].
 * @param modifier the modifier of the toolbar.
 * @param views the views offered in the switcher, in order. With one view or none the switcher is not shown.
 * @param onViewChange called with the view the person chooses. Without it the switcher is not shown.
 * @param busy shows a small circular progress indicator, named "Loading", after the title.
 * @param todayEnabled whether Today can be pressed; turn it off when the range already contains today.
 * @param previousEnabled whether the previous arrow can be pressed, for example at the start of a booking window.
 * @param nextEnabled whether the next arrow can be pressed.
 * @param viewMenuIcon a trailing icon of the switcher button, such as a down chevron. Decorative.
 * @param labels replacements for the built-in strings.
 * @param actions controls at the end: a New event button, a filter menu, a date picker button.
 * @param todayBetween puts Today between the arrows (previous, Today, next), as the timeline headers do.
 */
@Suppress("LongParameterList")
@OptIn(ExperimentalLayoutApi::class)
@Composable
public fun MonarchCalendarToolbar(
    title: String,
    view: MonarchCalendarToolbarView,
    onToday: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    previousIcon: @Composable () -> Unit,
    nextIcon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    views: List<MonarchCalendarToolbarView> = MonarchCalendarToolbarView.entries,
    onViewChange: ((MonarchCalendarToolbarView) -> Unit)? = null,
    busy: Boolean = false,
    todayEnabled: Boolean = true,
    previousEnabled: Boolean = true,
    nextEnabled: Boolean = true,
    viewMenuIcon: (@Composable () -> Unit)? = null,
    labels: MonarchCalendarToolbarLabels = MonarchCalendarToolbarLabels(),
    actions: (@Composable RowScope.() -> Unit)? = null,
    todayBetween: Boolean = false,
) {
    val tokens = MonarchCalendarToolbarTokens
    val unit = labels.unit?.invoke(view) ?: stringResource(tokens.unitRes(view))
    val previousName = labels.previous?.invoke(unit) ?: stringResource(R.string.monarch_calendartoolbar_previous, unit)
    val nextName = labels.next?.invoke(unit) ?: stringResource(R.string.monarch_calendartoolbar_next, unit)
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(tokens.Gap),
        verticalArrangement = Arrangement.spacedBy(tokens.Gap),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val todayButton = @Composable {
                MonarchButton(
                    text = monarchString(labels.today, R.string.monarch_calendartoolbar_today),
                    onClick = onToday,
                    size = MonarchButtonSize.Sm,
                    enabled = todayEnabled,
                )
            }
            if (!todayBetween) todayButton()
            Arrow(previousName, previousEnabled, onPrevious, previousIcon)
            if (todayBetween) todayButton()
            Arrow(nextName, nextEnabled, onNext, nextIcon)
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(tokens.Gap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .semantics {
                        heading()
                        liveRegion = LiveRegionMode.Polite
                    },
                style = MonarchTheme.typography.subheading,
                color = tokens.titleColor(MonarchTheme.colors),
            )
            if (busy) {
                MonarchProgress(
                    modifier = Modifier.size(tokens.BusySize),
                    variant = MonarchProgressVariant.Circular,
                    label = monarchString(labels.loading, R.string.monarch_calendartoolbar_loading),
                )
            }
        }
        val change = onViewChange
        if (tokens.showsSwitcher(views, change != null) || actions != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(tokens.Gap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (change != null && tokens.showsSwitcher(views, true)) {
                    ViewSwitcher(view, views, change, viewMenuIcon, labels)
                }
                actions?.invoke(this)
            }
        }
    }
}

@Composable
private fun Arrow(name: String, enabled: Boolean, onClick: () -> Unit, icon: @Composable () -> Unit) {
    MonarchIconButton(label = name, onClick = onClick, enabled = enabled, icon = icon)
}

/** Up to this many views are a segmented control, as on the web; more, or large text, open a menu. */
private const val SwitchMaxViews = 4
private const val SwitchMaxFontScale = 1.3f

@Composable
private fun ViewSwitcher(
    view: MonarchCalendarToolbarView,
    views: List<MonarchCalendarToolbarView>,
    onViewChange: (MonarchCalendarToolbarView) -> Unit,
    icon: (@Composable () -> Unit)?,
    labels: MonarchCalendarToolbarLabels,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val nameOf: @Composable (MonarchCalendarToolbarView) -> String = {
        labels.viewName?.invoke(it) ?: stringResource(MonarchCalendarToolbarTokens.viewNameRes(it))
    }
    val current = nameOf(view)
    val switcherName = monarchString(labels.view, R.string.monarch_calendartoolbar_view)
    if (views.size <= SwitchMaxViews && LocalDensity.current.fontScale <= SwitchMaxFontScale) {
        MonarchSegmentedControl(
            modifier = Modifier.semantics {
                contentDescription = switcherName
                stateDescription = current
            },
            size = MonarchSegmentedControlSize.Sm,
        ) {
            views.forEach { option ->
                Segment(
                    selected = option == view,
                    onClick = { if (option != view) onViewChange(option) },
                    label = nameOf(option),
                )
            }
        }
        return
    }
    Box {
        MonarchButton(
            onClick = { expanded = true },
            modifier = Modifier.semantics {
                contentDescription = switcherName
                stateDescription = current
            },
            size = MonarchButtonSize.Sm,
        ) {
            Text(current)
            icon?.invoke()
        }
        MonarchMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            views.forEach { option ->
                val chosen = option == view
                MonarchMenuItem(
                    text = nameOf(option),
                    onClick = {
                        expanded = false
                        if (!chosen) onViewChange(option)
                    },
                    selected = chosen,
                )
            }
        }
    }
}
