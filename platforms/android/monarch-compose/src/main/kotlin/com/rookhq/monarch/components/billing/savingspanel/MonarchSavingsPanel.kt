package com.rookhq.monarch.components.billing.savingspanel

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.R
import com.rookhq.monarch.accessibility.MonarchTouchTarget
import com.rookhq.monarch.accessibility.monarchFocusRing
import com.rookhq.monarch.components.billing.money.MonarchMoney
import com.rookhq.monarch.components.datadisplay.statusbadge.LocalMonarchStatusRegistry
import com.rookhq.monarch.components.datadisplay.statusbadge.MonarchStatusBadge
import com.rookhq.monarch.components.datadisplay.statusbadge.MonarchStatusBadgeSize
import com.rookhq.monarch.components.datadisplay.statusbadge.MonarchStatusBadgeVariant
import com.rookhq.monarch.components.datadisplay.statusbadge.MonarchStatusDefinition
import com.rookhq.monarch.components.datadisplay.statusbadge.MonarchStatusRegistry
import com.rookhq.monarch.components.feedback.emptystate.MonarchEmptyState
import com.rookhq.monarch.components.feedback.emptystate.MonarchEmptyStateKind
import com.rookhq.monarch.components.feedback.emptystate.MonarchEmptyStateSize
import com.rookhq.monarch.components.feedback.errorstate.MonarchErrorState
import com.rookhq.monarch.components.feedback.loadingstate.MonarchLoadingState
import com.rookhq.monarch.components.feedback.loadingstate.MonarchLoadingStateVariant
import com.rookhq.monarch.components.layout.divider.MonarchDivider
import com.rookhq.monarch.components.layout.divider.MonarchDividerVariant
import com.rookhq.monarch.components.layout.sectioncard.MonarchSectionCard
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.theme.MonarchTheme
import java.util.Locale

/**
 * What unused items cost to keep: a headline with the number of unused items
 * and their renewal cost per currency, the reasons they count as unused, the
 * decisions made so far, what letting them lapse saves, and the top items.
 * Only real prices are shown; unpriced items are counted, never estimated.
 *
 * Built by Monarch in a `MonarchSectionCard` from `MonarchText`,
 * `MonarchStatusBadge` and `MonarchBadge`-style status badges with counts,
 * and a `Column` of rows, so it sits inside the screen's `verticalScroll`.
 * Each category and each row is one TalkBack element read in words, and a
 * button when the app passes [onCategoryClick] or [onItemClick]. Loading,
 * failed and empty states use `MonarchLoadingState`, `MonarchErrorState` and
 * `MonarchEmptyState`.
 *
 * @param unusedCount the number of unused items. 0 shows the empty state.
 * @param modifier the modifier of the card.
 * @param costs the renewal cost of the unused items, one entry per currency.
 * @param unpricedCount the unused items without a price, counted in the headline.
 * @param categories the unused items grouped by reason; groups with 0 are not shown.
 * @param decisions the number of items per decision, written as one line.
 * @param potentialSavings what letting the marked items lapse saves, per currency.
 * @param items the unused items to list, most urgent first.
 * @param maxItems the most items listed; the rest are counted as "and N more".
 * @param registry the statuses of category and decision keys. Keys found nowhere are neutral, labelled from the key.
 * @param title the card title. `null` uses "Savings".
 * @param description help text under the title.
 * @param locale the locale of amounts and dates. `null` uses the first locale of the configuration.
 * @param loading shows a loading state in place of the content.
 * @param error shows the error state with this message; an empty string shows only the title. Wins over [loading].
 * @param retrying the retry of the error state is running.
 * @param onRetry shows a retry button in the error state.
 * @param onCategoryClick makes each category a button, for example to open the filtered list.
 * @param onItemClick makes each listed item a button that opens it.
 * @param emptyIcon an icon for the empty state. Monarch bundles no icons.
 * @param actions header actions, such as "Review all".
 * @param labels replacements for the built-in strings.
 */
@ExperimentalMonarchApi
@Composable
public fun MonarchSavingsPanel(
    unusedCount: Int,
    modifier: Modifier = Modifier,
    costs: List<MonarchSavingsAmount> = emptyList(),
    unpricedCount: Int = 0,
    categories: List<MonarchSavingsCategory> = emptyList(),
    decisions: List<MonarchSavingsDecisionCount> = emptyList(),
    potentialSavings: List<MonarchSavingsAmount> = emptyList(),
    items: List<MonarchSavingsItem> = emptyList(),
    maxItems: Int = MonarchSavingsPanelDefaults.MAX_ITEMS,
    registry: MonarchStatusRegistry? = null,
    title: String? = null,
    description: String? = null,
    locale: Locale? = null,
    loading: Boolean = false,
    error: String? = null,
    retrying: Boolean = false,
    onRetry: (() -> Unit)? = null,
    onCategoryClick: ((MonarchSavingsCategory) -> Unit)? = null,
    onItemClick: ((MonarchSavingsItem) -> Unit)? = null,
    emptyIcon: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    labels: MonarchSavingsPanelLabels = MonarchSavingsPanelLabels(),
) {
    val tokens = MonarchSavingsPanelTokens
    val resolvedLocale = locale ?: LocalConfiguration.current.locales[0]
    val provided = LocalMonarchStatusRegistry.current
    val resolve: (String) -> MonarchStatusDefinition = { tokens.status(it, registry, provided) }
    MonarchSectionCard(
        title = title ?: monarchString(labels.title, R.string.monarch_savingspanel_title),
        modifier = modifier,
        description = description,
        actions = actions,
    ) {
        when {
            error != null -> MonarchErrorState(
                title = monarchString(labels.errorTitle, R.string.monarch_savingspanel_error_title),
                size = MonarchEmptyStateSize.Sm,
                retrying = retrying,
                message = error.takeIf { it.isNotBlank() },
                onRetry = onRetry,
            )
            loading -> MonarchLoadingState(
                label = monarchString(labels.loading, R.string.monarch_savingspanel_loading),
                variant = MonarchLoadingStateVariant.Rows,
                rows = 2,
            )
            unusedCount <= 0 -> MonarchEmptyState(
                title = monarchString(labels.emptyTitle, R.string.monarch_savingspanel_empty_title),
                kind = MonarchEmptyStateKind.Success,
                size = MonarchEmptyStateSize.Sm,
                message = monarchString(labels.emptyMessage, R.string.monarch_savingspanel_empty_message),
                icon = emptyIcon,
            )
            else -> Column(verticalArrangement = Arrangement.spacedBy(tokens.SectionGap)) {
                MonarchText(
                    text = savingsPanelHeadline(unusedCount, tokens.join(costs, resolvedLocale), unpricedCount, labels),
                    style = MonarchTextStyle.Label,
                )
                SavingsPanelCategories(tokens.shownCategories(categories), resolve, onCategoryClick, labels)
                SavingsPanelSummary(decisions, tokens.join(potentialSavings, resolvedLocale), labels)
                val (shown, hidden) = tokens.shownItems(items, maxItems)
                if (shown.isNotEmpty()) SavingsPanelItems(shown, resolve, resolvedLocale, onItemClick, labels)
                if (hidden > 0) {
                    MonarchText(
                        text = labels.more?.invoke(hidden)
                            ?: pluralStringResource(R.plurals.monarch_savingspanel_more, hidden, hidden),
                        style = MonarchTextStyle.Caption,
                        color = tokens.mutedColor(MonarchTheme.colors),
                    )
                }
            }
        }
    }
}

/** The headline: "3 unused items cost $120.00 to renew (1 without a price).". */
@Composable
private fun savingsPanelHeadline(
    count: Int,
    cost: String?,
    unpriced: Int,
    labels: MonarchSavingsPanelLabels,
): String {
    labels.headline?.let { return it(count, cost, unpriced) }
    var text = pluralStringResource(R.plurals.monarch_savingspanel_headline_count, count, count)
    if (cost != null) text = stringResource(R.string.monarch_savingspanel_headline_cost, text, cost)
    if (unpriced > 0) {
        text = pluralStringResource(R.plurals.monarch_savingspanel_headline_unpriced, unpriced, text, unpriced)
    }
    return stringResource(R.string.monarch_savingspanel_headline_end, text)
}

/** A pressable area: at least 48dp tall with a focus ring, a button for TalkBack. */
private fun Modifier.savingsPanelPressable(onClick: (() -> Unit)?): Modifier =
    if (onClick == null) {
        this
    } else {
        heightIn(min = MonarchTouchTarget.Minimum)
            .monarchFocusRing(MonarchSavingsPanelTokens.PressShape)
            .clickable(role = Role.Button, onClick = onClick)
    }

@Composable
private fun SavingsPanelCategories(
    categories: List<MonarchSavingsCategory>,
    resolve: (String) -> MonarchStatusDefinition,
    onClick: ((MonarchSavingsCategory) -> Unit)?,
    labels: MonarchSavingsPanelLabels,
) {
    if (categories.isEmpty()) return
    val tokens = MonarchSavingsPanelTokens
    val name = monarchString(labels.categories, R.string.monarch_savingspanel_categories)
    FlowRow(
        modifier = Modifier.semantics {
            contentDescription = name
            isTraversalGroup = true
        },
        horizontalArrangement = Arrangement.spacedBy(tokens.ItemGap),
        verticalArrangement = Arrangement.spacedBy(tokens.ItemGap),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        categories.forEach { category ->
            val status = resolve(category.id).let { found ->
                category.label?.let { MonarchStatusDefinition(it, found.tone, found.icon) } ?: found
            }
            Row(
                modifier = Modifier
                    .savingsPanelPressable(onClick?.let { click -> { click(category) } })
                    .semantics(mergeDescendants = true) { contentDescription = "${status.label}, ${category.count}" },
                horizontalArrangement = Arrangement.spacedBy(tokens.LineGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.clearAndSetSemantics { }) {
                    MonarchStatusBadge(definition = status, size = MonarchStatusBadgeSize.Sm)
                }
                MonarchText(text = category.count.toString(), style = MonarchTextStyle.Label)
            }
        }
    }
}

@Composable
private fun SavingsPanelSummary(
    decisions: List<MonarchSavingsDecisionCount>,
    savings: String?,
    labels: MonarchSavingsPanelLabels,
) {
    val tokens = MonarchSavingsPanelTokens
    if (decisions.isNotEmpty()) {
        val line = tokens.decisionLine(decisions)
        val prefix = monarchString(labels.decisions, R.string.monarch_savingspanel_decisions)
        MonarchText(
            text = line,
            modifier = Modifier.semantics { contentDescription = "$prefix: $line" },
            style = MonarchTextStyle.Label,
            color = tokens.mutedColor(MonarchTheme.colors),
        )
    }
    if (savings != null) {
        MonarchText(
            text = labels.potentialSavings?.invoke(savings)
                ?: stringResource(R.string.monarch_savingspanel_potential_savings, savings),
            style = MonarchTextStyle.Label,
            color = tokens.savingsColor(MonarchTheme.colors),
        )
    }
}

@Composable
private fun SavingsPanelItems(
    items: List<MonarchSavingsItem>,
    resolve: (String) -> MonarchStatusDefinition,
    locale: Locale,
    onClick: ((MonarchSavingsItem) -> Unit)?,
    labels: MonarchSavingsPanelLabels,
) {
    val tokens = MonarchSavingsPanelTokens
    val name = monarchString(labels.items, R.string.monarch_savingspanel_items)
    val unpriced = monarchString(labels.unpriced, R.string.monarch_savingspanel_unpriced)
    // One entry per item. The web puts the name and the facts on one line; a phone is narrower than that line, so the
    // facts sit under the name and wrap, and the status chip at the end is never cut off.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = name
                isTraversalGroup = true
            },
        verticalArrangement = Arrangement.spacedBy(tokens.ItemGap),
    ) {
        items.forEachIndexed { index, item ->
            if (index > 0) MonarchDivider(variant = MonarchDividerVariant.Subtle)
            val category = item.category?.let(resolve)
            val decision = item.decision?.let(resolve)
            val date = item.dateMillis?.let { tokens.dateText(it, locale) }
            val price = item.amount?.let { tokens.join(listOf(it), locale) } ?: unpriced.takeIf { item.unpriced }
            val spoken = listOfNotNull(item.name, category?.label, date, price, decision?.label).joinToString(", ")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = MonarchTouchTarget.Minimum)
                    .savingsPanelPressable(onClick?.let { click -> { click(item) } })
                    .semantics(mergeDescendants = true) { contentDescription = spoken },
                verticalArrangement = Arrangement.spacedBy(tokens.ItemGap, Alignment.CenterVertically),
            ) {
                MonarchText(text = item.name, style = MonarchTextStyle.Label)
                SavingsPanelFacts(category, date, item.amount, price.takeIf { item.amount == null }, decision, locale)
            }
        }
    }
}

@Composable
@Suppress("LongParameterList")
private fun SavingsPanelFacts(
    category: MonarchStatusDefinition?,
    date: String?,
    amount: MonarchSavingsAmount?,
    unpriced: String?,
    decision: MonarchStatusDefinition?,
    locale: Locale,
) {
    val tokens = MonarchSavingsPanelTokens
    val muted = tokens.mutedColor(MonarchTheme.colors)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(tokens.ItemGap),
        verticalArrangement = Arrangement.spacedBy(tokens.ItemGap),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        // The badges are part of the row's words, not separate TalkBack stops.
        category?.let {
            Box(Modifier.clearAndSetSemantics { }) {
                MonarchStatusBadge(it, variant = MonarchStatusBadgeVariant.Outline, size = MonarchStatusBadgeSize.Sm)
            }
        }
        date?.let { MonarchText(text = it, style = MonarchTextStyle.Caption, color = muted) }
        amount?.let { MonarchMoney(it.amount, it.currencyCode, locale = locale, style = MonarchTextStyle.Caption) }
        unpriced?.let { MonarchText(text = it, style = MonarchTextStyle.Caption, color = muted) }
        decision?.let {
            Box(Modifier.clearAndSetSemantics { }) { MonarchStatusBadge(it, size = MonarchStatusBadgeSize.Sm) }
        }
    }
}
