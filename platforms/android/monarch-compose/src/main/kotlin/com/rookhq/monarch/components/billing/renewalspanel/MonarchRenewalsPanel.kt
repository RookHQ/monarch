package com.rookhq.monarch.components.billing.renewalspanel

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.R
import com.rookhq.monarch.components.billing.money.MonarchMoney
import com.rookhq.monarch.components.billing.money.MonarchMoneyFormat
import com.rookhq.monarch.components.datadisplay.statusbadge.LocalMonarchStatusRegistry
import com.rookhq.monarch.components.datadisplay.statusbadge.MonarchStatusRegistry
import com.rookhq.monarch.components.feedback.emptystate.MonarchEmptyState
import com.rookhq.monarch.components.feedback.emptystate.MonarchEmptyStateSize
import com.rookhq.monarch.components.feedback.errorstate.MonarchErrorState
import com.rookhq.monarch.components.feedback.loadingstate.MonarchLoadingState
import com.rookhq.monarch.components.feedback.loadingstate.MonarchLoadingStateVariant
import com.rookhq.monarch.components.forms.segmentedcontrol.MonarchSegmentedControl
import com.rookhq.monarch.components.layout.sectioncard.MonarchSectionCard
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.theme.MonarchTheme
import com.rookhq.monarch.tokens.MonarchTokens
import java.util.Locale

/**
 * A renewals workspace: what renews or expires next, when, for how much and
 * who owns it, with filter options, per-currency totals and a row action. The
 * app fetches, filters and sorts; the panel shows what it is given.
 *
 * Built by Monarch in a `MonarchSectionCard`: a
 * `MonarchSegmentedControl` for the filters, tiles of `MonarchMoney`
 * for the totals and one row per item with `MonarchStatusBadge`,
 * `MonarchMoney`, a warning "Due soon" `MonarchBadge` for open renewals that
 * are close or past, and a `MonarchButton` for the action. The rows are a
 * `Column`, so the panel sits inside the screen's `verticalScroll`. Each row's
 * text is one TalkBack element (a button with [onItemClick]); the action is a
 * separate button named after the item. Loading, failed and empty states use
 * `MonarchLoadingState`, `MonarchErrorState` and `MonarchEmptyState`.
 *
 * @param items the renewals in view, already filtered and sorted by the app.
 * @param modifier the modifier of the card.
 * @param title the card title. `null` uses "Renewals".
 * @param description help text under the title, such as when items appear.
 * @param filters filter options, such as time windows. Shown as a segmented row.
 * @param selectedFilter the selected filter value. `null` selects the first option.
 * @param totals per-currency totals of the items in view, shown above the list.
 * @param unpricedCount the number of items without a price, shown next to the totals.
 * @param locale the locale of amounts and dates. `null` uses the first locale of the configuration.
 * @param now the reference time for day counts, in epoch milliseconds. `null` reads the clock once.
 * @param urgentWithinDays open renewals this many days away or fewer, or past, are marked "Due soon".
 * @param registry more statuses, or replacements for `MonarchRenewalsPanelDefaults.statuses()`.
 * @param loading shows a loading state in place of the totals and the list.
 * @param error shows the error state with this message; an empty string shows only the title. Wins over [loading].
 * @param retrying the retry of the error state is running.
 * @param onFilterChange called with the value of the chosen filter.
 * @param onRetry shows a retry button in the error state.
 * @param onItemClick makes each row's text a button that opens the item.
 * @param onItemAction shows a row action, such as Update, called with the item.
 * @param canAct hides the row action for some items, such as closed ones. `null` shows it for every item.
 * @param actions header actions, such as a "Check now" button.
 * @param labels replacements for the built-in strings.
 */
@ExperimentalMonarchApi
@Composable
public fun MonarchRenewalsPanel(
    items: List<MonarchRenewalItem>,
    modifier: Modifier = Modifier,
    title: String? = null,
    description: String? = null,
    filters: List<MonarchRenewalFilter> = emptyList(),
    selectedFilter: String? = null,
    totals: List<MonarchRenewalTotal> = emptyList(),
    unpricedCount: Int = 0,
    locale: Locale? = null,
    now: Long? = null,
    urgentWithinDays: Int = MonarchRenewalsPanelDefaults.URGENT_WITHIN_DAYS,
    registry: MonarchStatusRegistry? = null,
    loading: Boolean = false,
    error: String? = null,
    retrying: Boolean = false,
    onFilterChange: ((String) -> Unit)? = null,
    onRetry: (() -> Unit)? = null,
    onItemClick: ((MonarchRenewalItem) -> Unit)? = null,
    onItemAction: ((MonarchRenewalItem) -> Unit)? = null,
    canAct: ((MonarchRenewalItem) -> Boolean)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    labels: MonarchRenewalsPanelLabels = MonarchRenewalsPanelLabels(),
) {
    val tokens = MonarchRenewalsPanelTokens
    val resolvedLocale = locale ?: LocalConfiguration.current.locales[0]
    val clock = remember { System.currentTimeMillis() }
    MonarchSectionCard(
        title = title ?: monarchString(labels.title, R.string.monarch_renewalspanel_title),
        modifier = modifier,
        description = description,
        actions = actions,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(tokens.SectionGap)) {
            if (filters.isNotEmpty()) RenewalsPanelFilters(filters, selectedFilter, onFilterChange, labels)
            when {
                error != null -> MonarchErrorState(
                    title = monarchString(labels.errorTitle, R.string.monarch_renewalspanel_error_title),
                    size = MonarchEmptyStateSize.Sm,
                    retrying = retrying,
                    message = error.takeIf { it.isNotBlank() },
                    onRetry = onRetry,
                )
                loading -> MonarchLoadingState(
                    label = monarchString(labels.loading, R.string.monarch_renewalspanel_loading),
                    variant = MonarchLoadingStateVariant.Rows,
                    rows = 2,
                )
                else -> {
                    if (totals.isNotEmpty() || unpricedCount > 0) {
                        RenewalsPanelTotals(totals, unpricedCount, resolvedLocale, labels)
                    }
                    if (items.isEmpty()) {
                        MonarchEmptyState(
                            title = monarchString(labels.emptyTitle, R.string.monarch_renewalspanel_empty_title),
                            size = MonarchEmptyStateSize.Sm,
                            message = monarchString(labels.emptyMessage, R.string.monarch_renewalspanel_empty_message),
                        )
                    } else {
                        val context = RenewalsPanelRowContext(
                            now = now ?: clock,
                            urgentWithinDays = urgentWithinDays,
                            ownerShown = tokens.ownerShown(items),
                            locale = resolvedLocale,
                            labels = labels,
                        )
                        RenewalsPanelList(items, registry, context, onItemClick, onItemAction, canAct)
                    }
                }
            }
        }
    }
}

@Composable
@Suppress("LongParameterList")
private fun RenewalsPanelList(
    items: List<MonarchRenewalItem>,
    registry: MonarchStatusRegistry?,
    context: RenewalsPanelRowContext,
    onItemClick: ((MonarchRenewalItem) -> Unit)?,
    onItemAction: ((MonarchRenewalItem) -> Unit)?,
    canAct: ((MonarchRenewalItem) -> Boolean)?,
) {
    val tokens = MonarchRenewalsPanelTokens
    val statuses = registry?.let { MonarchRenewalsPanelDefaults.statuses().merging(it) }
        ?: MonarchRenewalsPanelDefaults.statuses()
    val provided = LocalMonarchStatusRegistry.current
    val listName = monarchString(context.labels.list, R.string.monarch_renewalspanel_list)
    Column(
        modifier = Modifier.semantics {
            contentDescription = listName
            isTraversalGroup = true
            collectionInfo = CollectionInfo(rowCount = items.size, columnCount = 1)
        },
    ) {
        RenewalsPanelTable(
            rows = items.map { item ->
                RenewalsPanelTableRow(
                    item = item,
                    status = tokens.status(item.status, statuses, provided),
                    onClick = onItemClick?.let { click -> { click(item) } },
                    onAction = onItemAction?.takeIf { canAct?.invoke(item) ?: true }?.let { act -> { act(item) } },
                )
            },
            context = context,
        )
    }
}

@Composable
private fun RenewalsPanelFilters(
    filters: List<MonarchRenewalFilter>,
    selectedFilter: String?,
    onFilterChange: ((String) -> Unit)?,
    labels: MonarchRenewalsPanelLabels,
) {
    val selected = selectedFilter ?: filters.firstOrNull()?.value
    val name = monarchString(labels.filters, R.string.monarch_renewalspanel_filters)
    MonarchSegmentedControl(modifier = Modifier.semantics { contentDescription = name }) {
        filters.forEach { filter ->
            Segment(
                selected = filter.value == selected,
                onClick = { onFilterChange?.invoke(filter.value) },
                label = filter.count?.let { "${filter.label} ($it)" } ?: filter.label,
            )
        }
    }
}

@Composable
private fun RenewalsPanelTotals(
    totals: List<MonarchRenewalTotal>,
    unpricedCount: Int,
    locale: Locale,
    labels: MonarchRenewalsPanelLabels,
) {
    val name = monarchString(labels.totals, R.string.monarch_renewalspanel_totals)
    Row(
        modifier = Modifier.height(IntrinsicSize.Max).semantics {
            contentDescription = name
            isTraversalGroup = true
        },
        horizontalArrangement = Arrangement.spacedBy(MonarchRenewalsPanelTokens.ItemGap),
    ) {
        totals.forEach { total ->
            val heading = labels.totalIn?.invoke(total.currencyCode, total.count)
                ?: pluralStringResource(
                    R.plurals.monarch_renewalspanel_total_in,
                    total.count,
                    total.currencyCode,
                    total.count,
                )
            val estimated = total.estimated?.let { MonarchMoneyFormat.format(it, total.currencyCode, locale) }?.let {
                labels.estimated?.invoke(it) ?: stringResource(R.string.monarch_renewalspanel_estimated, it)
            }
            RenewalsPanelTile(heading, estimated) {
                MonarchMoney(total.amount, total.currencyCode, locale = locale, style = MonarchTextStyle.BodyStrong)
            }
        }
        if (unpricedCount > 0) {
            RenewalsPanelTile(
                heading = monarchString(labels.unpricedCount, R.string.monarch_renewalspanel_unpriced_count),
                note = monarchString(labels.unpricedHint, R.string.monarch_renewalspanel_unpriced_hint),
            ) {
                MonarchText(text = unpricedCount.toString(), style = MonarchTextStyle.BodyStrong)
            }
        }
    }
}

/** One figure above the list: a heading, the value and a note, read together. */
@Composable
private fun RowScope.RenewalsPanelTile(heading: String, note: String?, value: @Composable () -> Unit) {
    val modifier = Modifier.weight(1f)
    val tokens = MonarchRenewalsPanelTokens
    val colors = MonarchTheme.colors
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(tokens.tileColor(colors), tokens.TileShape)
            .border(MonarchTokens.Space.px.dp, colors.borderSubtle, tokens.TileShape)
            .padding(tokens.TilePadding)
            .semantics(mergeDescendants = true) { },
        verticalArrangement = Arrangement.spacedBy(tokens.LineGap),
    ) {
        MonarchText(text = heading, style = MonarchTextStyle.Caption, color = tokens.mutedColor(colors))
        value()
        note?.let { MonarchText(text = it, style = MonarchTextStyle.Caption, color = tokens.mutedColor(colors)) }
    }
}
