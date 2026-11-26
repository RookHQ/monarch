package com.rookhq.monarch.components.billing.subscriptionstable

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.rookhq.monarch.R
import com.rookhq.monarch.accessibility.MonarchTouchTarget
import com.rookhq.monarch.accessibility.monarchFocusRing
import com.rookhq.monarch.components.billing.money.MonarchMoney
import com.rookhq.monarch.components.datadisplay.statusbadge.LocalMonarchStatusRegistry
import com.rookhq.monarch.components.datadisplay.statusbadge.MonarchStatusBadge
import com.rookhq.monarch.components.datadisplay.statusbadge.MonarchStatusBadgeSize
import com.rookhq.monarch.components.datadisplay.statusbadge.MonarchStatusDefinition
import com.rookhq.monarch.components.datadisplay.statusbadge.MonarchStatusRegistry
import com.rookhq.monarch.components.feedback.emptystate.MonarchEmptyState
import com.rookhq.monarch.components.feedback.emptystate.MonarchEmptyStateSize
import com.rookhq.monarch.components.layout.divider.MonarchDivider
import com.rookhq.monarch.components.layout.divider.MonarchDividerVariant
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.internal.MonarchShapeDefaults
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.theme.MonarchTheme
import java.util.Locale

/**
 * A table of recurring agreements: name and plan, customer, status, price per
 * billing period, the monthly figure and the next renewal. Each amount stays
 * in its own currency; nothing is converted or added up.
 *
 * Built by Monarch from `MonarchText`, `MonarchStatusBadge` and `MonarchMoney`
 * in a `Column`, so it sits inside the screen's `verticalScroll`. On a phone
 * each subscription is a row with the name, plan and renewal at the start and
 * the status and price at the end; from 640dp wide the rows are a table with
 * column headings. Each row is one TalkBack element that reads the whole row
 * in words, including the price "per month"; with [onSelect] the row is a
 * button. With no subscriptions it shows a `MonarchEmptyState`.
 *
 * @param subscriptions the rows, in display order. Sorting and filtering stay in the app.
 * @param modifier the modifier of the table.
 * @param registry more statuses, or replacements for the built-in ones of
 * `MonarchSubscriptionsTableDefaults.statuses()`.
 * @param locale the locale of amounts and dates. `null` uses the first locale of the configuration.
 * @param showCustomer shows the customer. Turn it off on a single customer's screen.
 * @param showMonthly shows the monthly figure. `null` shows it when any subscription has one.
 * @param onSelect makes each row a button that is called with its subscription, for example to open it.
 * @param labels replacements for the built-in strings.
 * @param renewalUnderName in the table layout, writes the customer and the renewal under the plan, as the web
 * table does, instead of in columns of their own.
 */
@Composable
public fun MonarchSubscriptionsTable(
    subscriptions: List<MonarchSubscription>,
    modifier: Modifier = Modifier,
    registry: MonarchStatusRegistry? = null,
    locale: Locale? = null,
    showCustomer: Boolean = true,
    showMonthly: Boolean? = null,
    onSelect: ((MonarchSubscription) -> Unit)? = null,
    labels: MonarchSubscriptionsTableLabels = MonarchSubscriptionsTableLabels(),
    renewalUnderName: Boolean = false,
) {
    if (subscriptions.isEmpty()) {
        MonarchEmptyState(
            title = monarchString(labels.emptyTitle, R.string.monarch_subscriptionstable_empty_title),
            modifier = modifier,
            size = MonarchEmptyStateSize.Sm,
            message = monarchString(labels.emptyMessage, R.string.monarch_subscriptionstable_empty_message),
        )
        return
    }
    val tokens = MonarchSubscriptionsTableTokens
    val resolvedLocale = locale ?: LocalConfiguration.current.locales[0]
    val statuses = registry?.let { MonarchSubscriptionsTableDefaults.statuses().merging(it) }
        ?: MonarchSubscriptionsTableDefaults.statuses()
    val provided = LocalMonarchStatusRegistry.current
    val monthlyShown = tokens.monthlyShown(showMonthly, subscriptions)
    val caption = monarchString(labels.caption, R.string.monarch_subscriptionstable_caption)
    BoxWithConstraints(modifier) {
        val wide = maxWidth >= tokens.WideFrom
        Column(
            Modifier.semantics {
                contentDescription = caption
                isTraversalGroup = true
                collectionInfo = CollectionInfo(rowCount = subscriptions.size, columnCount = 1)
            },
        ) {
            if (wide) {
                SubscriptionsTableHeadings(showCustomer && !renewalUnderName, monthlyShown, labels, !renewalUnderName)
            }
            subscriptions.forEachIndexed { index, subscription ->
                if (index > 0 || wide) MonarchDivider(variant = MonarchDividerVariant.Subtle)
                val status = tokens.status(subscription.status, statuses, provided)
                val text = subscriptionsTableRowText(
                    subscription,
                    status,
                    showCustomer,
                    monthlyShown,
                    resolvedLocale,
                    labels,
                )
                SubscriptionsTableRow(index, text, onSelect?.let { select -> { select(subscription) } }) {
                    if (wide) {
                        SubscriptionsTableWideCells(
                            subscription,
                            status,
                            text,
                            showCustomer,
                            monthlyShown,
                            resolvedLocale,
                            renewalUnderName,
                        )
                    } else {
                        SubscriptionsTableCompactCells(subscription, status, text, resolvedLocale, labels)
                    }
                }
            }
        }
    }
}

/** One row: a single TalkBack element that reads [text], and a button when [onClick] is given. */
@Composable
private fun SubscriptionsTableRow(
    index: Int,
    text: SubscriptionsTableRowText,
    onClick: (() -> Unit)?,
    content: @Composable RowScope.() -> Unit,
) {
    val tokens = MonarchSubscriptionsTableTokens
    val interaction = if (onClick != null) {
        Modifier
            .heightIn(min = MonarchTouchTarget.Minimum)
            .monarchFocusRing(MonarchShapeDefaults.Row)
            .clickable(role = Role.Button, onClick = onClick)
    } else {
        Modifier
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(interaction)
            .semantics(mergeDescendants = true) {
                contentDescription = text.spoken
                collectionItemInfo = CollectionItemInfo(rowIndex = index, rowSpan = 1, columnIndex = 0, columnSpan = 1)
            },
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = tokens.RowPadding),
            horizontalArrangement = Arrangement.spacedBy(tokens.CellGap),
            verticalAlignment = Alignment.Top,
            content = content,
        )
    }
}

/** The status badge; its words are part of the row's description, so it is not a TalkBack stop of its own. */
@Composable
private fun SubscriptionsTableStatus(status: MonarchStatusDefinition) {
    Box(Modifier.clearAndSetSemantics { }) { MonarchStatusBadge(definition = status, size = MonarchStatusBadgeSize.Sm) }
}

@Composable
private fun SubscriptionsTableHeadings(
    showCustomer: Boolean,
    showMonthly: Boolean,
    labels: MonarchSubscriptionsTableLabels,
    showRenewal: Boolean = true,
) {
    val tokens = MonarchSubscriptionsTableTokens
    val color = tokens.headingColor(MonarchTheme.colors)
    val subscription = monarchString(labels.subscription, R.string.monarch_subscriptionstable_subscription)
    val customer = monarchString(labels.customer, R.string.monarch_subscriptionstable_customer)
    val status = monarchString(labels.status, R.string.monarch_subscriptionstable_status)
    val price = monarchString(labels.price, R.string.monarch_subscriptionstable_price)
    val monthly = monarchString(labels.monthly, R.string.monarch_subscriptionstable_monthly)
    val renewal = monarchString(labels.renewal, R.string.monarch_subscriptionstable_renewal)
    // Hidden from TalkBack: every row reads its own values in words.
    Row(
        modifier = Modifier.padding(bottom = tokens.RowPadding).clearAndSetSemantics { },
        horizontalArrangement = Arrangement.spacedBy(tokens.CellGap),
    ) {
        MonarchText(subscription, Modifier.weight(tokens.NAME_WEIGHT), MonarchTextStyle.Label, color)
        if (showCustomer) MonarchText(customer, Modifier.weight(1f), MonarchTextStyle.Label, color)
        MonarchText(status, Modifier.weight(1f), MonarchTextStyle.Label, color)
        MonarchText(price, Modifier.weight(1f), MonarchTextStyle.Label, color, TextAlign.End)
        if (showMonthly) MonarchText(monthly, Modifier.weight(1f), MonarchTextStyle.Label, color, TextAlign.End)
        if (showRenewal) MonarchText(renewal, Modifier.weight(1f), MonarchTextStyle.Label, color)
    }
}

@Composable
private fun SubscriptionsTableName(
    subscription: MonarchSubscription,
    text: SubscriptionsTableRowText,
    modifier: Modifier,
    under: String? = null,
) {
    val tokens = MonarchSubscriptionsTableTokens
    Column(modifier, verticalArrangement = Arrangement.spacedBy(tokens.LineGap)) {
        MonarchText(text = subscription.name, style = MonarchTextStyle.BodyStrong)
        MonarchText(text = text.meta, style = MonarchTextStyle.Caption, color = tokens.mutedColor(MonarchTheme.colors))
        under?.let {
            MonarchText(text = it, style = MonarchTextStyle.Caption, color = tokens.mutedColor(MonarchTheme.colors))
        }
    }
}

@Composable
private fun SubscriptionsTablePrice(
    subscription: MonarchSubscription,
    text: SubscriptionsTableRowText,
    locale: Locale,
    modifier: Modifier = Modifier,
    stacked: Boolean = false,
) {
    if (stacked) {
        Column(modifier, horizontalAlignment = Alignment.End) {
            MonarchMoney(
                amount = subscription.amount,
                currencyCode = subscription.currencyCode,
                locale = locale,
                style = MonarchTextStyle.BodyStrong,
            )
            MonarchText(
                text = text.priceSuffix,
                style = MonarchTextStyle.Caption,
                color = MonarchSubscriptionsTableTokens.mutedColor(MonarchTheme.colors),
            )
        }
        return
    }
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(MonarchSubscriptionsTableTokens.LineGap, Alignment.End),
        itemVerticalAlignment = Alignment.Bottom,
    ) {
        MonarchMoney(
            amount = subscription.amount,
            currencyCode = subscription.currencyCode,
            locale = locale,
            style = MonarchTextStyle.BodyStrong,
        )
        MonarchText(
            text = text.priceSuffix,
            style = MonarchTextStyle.Caption,
            color = MonarchSubscriptionsTableTokens.mutedColor(MonarchTheme.colors),
        )
    }
}

@Composable
private fun RowScope.SubscriptionsTableCompactCells(
    subscription: MonarchSubscription,
    status: MonarchStatusDefinition,
    text: SubscriptionsTableRowText,
    locale: Locale,
    labels: MonarchSubscriptionsTableLabels,
) {
    val tokens = MonarchSubscriptionsTableTokens
    val muted = tokens.mutedColor(MonarchTheme.colors)
    val customer = monarchString(labels.customer, R.string.monarch_subscriptionstable_customer)
    val statusName = monarchString(labels.status, R.string.monarch_subscriptionstable_status)
    val price = monarchString(labels.price, R.string.monarch_subscriptionstable_price)
    val renewal = monarchString(labels.renewal, R.string.monarch_subscriptionstable_renewal)
    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(tokens.LineGap)) {
        SubscriptionsTableName(subscription, text, Modifier)
        text.customer?.let { SubscriptionsTableLabelled(customer, it, muted) }
        SubscriptionsTableLabelled(renewal, text.renewal, muted)
    }
    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(tokens.LineGap)) {
        SubscriptionsTableCaption(statusName, muted)
        SubscriptionsTableStatus(status)
        SubscriptionsTableCaption(price, muted)
        SubscriptionsTablePrice(subscription, text, locale)
        text.monthly?.let {
            SubscriptionsTableCaption(monarchString(labels.monthly, R.string.monarch_subscriptionstable_monthly), muted)
            MonarchText(text = it, style = MonarchTextStyle.Caption, color = muted)
        }
    }
}

/** A column name as the caption of its value on a compact row. */
@Composable
private fun SubscriptionsTableCaption(name: String, color: Color) {
    MonarchText(text = name, style = MonarchTextStyle.Caption, color = color)
}

@Composable
private fun SubscriptionsTableLabelled(name: String, value: String, color: Color) {
    Column {
        SubscriptionsTableCaption(name, color)
        MonarchText(text = value, style = MonarchTextStyle.Caption)
    }
}

@Composable
@Suppress("LongParameterList")
private fun RowScope.SubscriptionsTableWideCells(
    subscription: MonarchSubscription,
    status: MonarchStatusDefinition,
    text: SubscriptionsTableRowText,
    showCustomer: Boolean,
    showMonthly: Boolean,
    locale: Locale,
    renewalUnderName: Boolean,
) {
    val tokens = MonarchSubscriptionsTableTokens
    val muted = tokens.mutedColor(MonarchTheme.colors)
    val under = if (renewalUnderName) tokens.line(listOf(text.customer, text.renewal)) else null
    SubscriptionsTableName(subscription, text, Modifier.weight(tokens.NAME_WEIGHT), under)
    if (showCustomer && !renewalUnderName) MonarchText(text.customer ?: "—", Modifier.weight(1f), color = muted)
    Column(Modifier.weight(1f)) { SubscriptionsTableStatus(status) }
    SubscriptionsTablePrice(subscription, text, locale, Modifier.weight(1f), stacked = renewalUnderName)
    if (showMonthly) {
        MonarchText(
            text = text.monthly ?: "—",
            modifier = Modifier.weight(1f),
            color = if (text.monthlyMuted || text.monthly == null) muted else MonarchTheme.colors.textDefault,
            textAlign = TextAlign.End,
        )
    }
    if (!renewalUnderName) MonarchText(text.renewal, Modifier.weight(1f), MonarchTextStyle.Body, muted)
}
