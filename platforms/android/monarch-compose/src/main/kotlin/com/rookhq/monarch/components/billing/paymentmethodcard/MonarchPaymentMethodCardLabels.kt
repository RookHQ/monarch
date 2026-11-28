package com.rookhq.monarch.components.billing.paymentmethodcard

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchPaymentMethodCard`.
 * Every `null` keeps the built-in string `monarch_paymentmethodcard_<key>`.
 *
 * @property name the card's name, given the network's display name and the last four digits: "Visa ending in 4242".
 * @property expires the expiry line of a valid card, given "MM/YY".
 * @property expiredOn the expiry line of an expired card, given "MM/YY".
 * @property defaultBadge the badge of the default method.
 * @property expiringBadge the badge of a card that expires soon.
 * @property expiredBadge the badge of an expired card.
 * @property makeDefault the button that makes the card the default.
 * @property remove the button that removes the card.
 * @property actionLabel what TalkBack reads for a button, given its label and the card's name: "Remove, Visa ending
 * in 4242".
 * @property removeTitle the title of the removal confirmation, given the card's name.
 * @property removeMessage the message of the removal confirmation.
 * @property removeConfirm the confirming button of the removal confirmation.
 * @property cancel the cancelling button of the removal confirmation.
 * @property failed shown and announced when an action fails.
 */
@Immutable
public class MonarchPaymentMethodCardLabels(
    public val name: ((brand: String, last4: String) -> String)? = null,
    public val expires: ((date: String) -> String)? = null,
    public val expiredOn: ((date: String) -> String)? = null,
    public val defaultBadge: String? = null,
    public val expiringBadge: String? = null,
    public val expiredBadge: String? = null,
    public val makeDefault: String? = null,
    public val remove: String? = null,
    public val actionLabel: ((action: String, name: String) -> String)? = null,
    public val removeTitle: ((name: String) -> String)? = null,
    public val removeMessage: String? = null,
    public val removeConfirm: String? = null,
    public val cancel: String? = null,
    public val failed: String? = null,
)
