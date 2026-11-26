package com.rookhq.monarch.components.billing.renewalspanel

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchRenewalsPanel`. Every
 * `null` uses the built-in string named after it in `strings_renewalspanel.xml`.
 *
 * @property title the panel title when the app gives none: "Renewals".
 * @property list the name TalkBack gives the list: "Renewals".
 * @property filters the name TalkBack gives the filter options: "Filter renewals".
 * @property totals the name TalkBack gives the totals: "Totals by currency".
 * @property daysUntil the day count under the date: "in 12 days", "today", "3 days ago".
 * @property noticeBy the notice deadline under the date, given the date: "Notice by Mar 1, 2027".
 * @property unknownDate in place of a missing date: "Unknown date".
 * @property unpriced in place of a missing amount: "No price".
 * @property autoRenews an item that renews by itself: "Auto-renews".
 * @property manualRenewal an item that must be renewed by hand: "Manual renewal".
 * @property noOwner in place of a missing owner: "No owner".
 * @property noNextAction in place of a missing next step: "No next action".
 * @property overdue the prefix of an overdue next step: "Overdue".
 * @property dueSoon the badge of an urgent open renewal: "Due soon".
 * @property probability the likelihood, given the percent: "60% likely".
 * @property totalIn the heading of a total, given the currency and the count: "USD · 4 renewals".
 * @property estimated the estimated part of a total, given the amount: "Includes $120.00 estimated".
 * @property unpricedCount the heading of the count of unpriced items: "Without a price".
 * @property unpricedHint the line under that count: "Not included in the totals.".
 * @property action the row action: "Update".
 * @property actionFor what TalkBack reads for the row action, given the action and the name: "Update Acme renewal".
 * @property emptyTitle the title of the empty state: "No renewals coming up".
 * @property emptyMessage the message of the empty state.
 * @property errorTitle the title of the error state: "Renewals could not be loaded.".
 * @property loading what TalkBack reads while loading: "Loading renewals".
 * @property columnItem the heading of the item column of the table: "Item".
 * @property columnRenewal the heading of the renewal column: "Renewal".
 * @property columnAmount the heading of the amount column: "Amount".
 * @property columnStatus the heading of the status column: "Status".
 */
@Immutable
public class MonarchRenewalsPanelLabels(
    public val title: String? = null,
    public val list: String? = null,
    public val filters: String? = null,
    public val totals: String? = null,
    public val daysUntil: ((Int) -> String)? = null,
    public val noticeBy: ((String) -> String)? = null,
    public val unknownDate: String? = null,
    public val unpriced: String? = null,
    public val autoRenews: String? = null,
    public val manualRenewal: String? = null,
    public val noOwner: String? = null,
    public val noNextAction: String? = null,
    public val overdue: String? = null,
    public val dueSoon: String? = null,
    public val probability: ((Int) -> String)? = null,
    public val totalIn: ((String, Int) -> String)? = null,
    public val estimated: ((String) -> String)? = null,
    public val unpricedCount: String? = null,
    public val unpricedHint: String? = null,
    public val action: String? = null,
    public val actionFor: ((String, String) -> String)? = null,
    public val emptyTitle: String? = null,
    public val emptyMessage: String? = null,
    public val errorTitle: String? = null,
    public val loading: String? = null,
    public val columnItem: String? = null,
    public val columnRenewal: String? = null,
    public val columnAmount: String? = null,
    public val columnStatus: String? = null,
)
