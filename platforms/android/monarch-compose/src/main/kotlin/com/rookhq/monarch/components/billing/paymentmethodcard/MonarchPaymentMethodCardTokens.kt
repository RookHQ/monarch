package com.rookhq.monarch.components.billing.paymentmethodcard

import androidx.annotation.StringRes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.R
import com.rookhq.monarch.internal.MonarchSurfaceColors
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens
import java.util.Locale

/** The action of a payment method card that is running or failed. */
internal enum class MonarchPaymentMethodCardAction { MakeDefault, Remove }

/** Component tokens and logic of MonarchPaymentMethodCard, built from primitives and roles only. */
internal object MonarchPaymentMethodCardTokens {
    val ImageWidth: Dp = MonarchTokens.Space.space10.dp
    val ImageHeight: Dp = MonarchTokens.Space.space6.dp
    val HeaderGap: Dp = MonarchTokens.Space.space3.dp
    val LineGap: Dp = MonarchTokens.Space.space1.dp
    val SectionGap: Dp = MonarchTokens.Space.space3.dp
    val ActionGap: Dp = MonarchTokens.Space.space2.dp
    private const val YEAR_DIGITS = 100

    /** The display name of a known network id, or `null` to show [brand] as given. */
    @StringRes
    fun brandName(brand: String): Int? = when (brand.lowercase(Locale.ROOT)) {
        "visa" -> R.string.monarch_paymentmethodcard_brand_visa
        "mastercard" -> R.string.monarch_paymentmethodcard_brand_mastercard
        "amex" -> R.string.monarch_paymentmethodcard_brand_amex
        "rupay" -> R.string.monarch_paymentmethodcard_brand_rupay
        "discover" -> R.string.monarch_paymentmethodcard_brand_discover
        "diners" -> R.string.monarch_paymentmethodcard_brand_diners
        "jcb" -> R.string.monarch_paymentmethodcard_brand_jcb
        "unionpay" -> R.string.monarch_paymentmethodcard_brand_unionpay
        "maestro" -> R.string.monarch_paymentmethodcard_brand_maestro
        else -> null
    }

    /** The expiry as "MM/YY", or `null` without a month and a year. */
    fun expiryDate(month: Int?, year: Int?): String? {
        if (month == null || year == null) return null
        val fullYear = MonarchPaymentMethodCardExpiry.fullYear(year)
        return String.format(Locale.ROOT, "%02d/%02d", month.coerceIn(1, MONTHS), fullYear % YEAR_DIGITS)
    }

    /** The status: the given one, else worked out from the date, else valid. */
    fun status(
        expiry: MonarchPaymentMethodCardExpiry?,
        month: Int?,
        year: Int?,
        now: Long,
    ): MonarchPaymentMethodCardExpiry = expiry
        ?: if (month != null && year != null) {
            MonarchPaymentMethodCardExpiry.status(month, year, now)
        } else {
            MonarchPaymentMethodCardExpiry.Valid
        }

    /** Whether an action may start: nothing else runs. */
    fun canStart(running: MonarchPaymentMethodCardAction?): Boolean = running == null

    /** Whether Make default is shown: there is an action and the card is not the default already. */
    fun showsMakeDefault(hasAction: Boolean, default: Boolean): Boolean = hasAction && !default

    /** Whether Make default can be used: an expired card cannot become the default. */
    fun makeDefaultEnabled(status: MonarchPaymentMethodCardExpiry, running: MonarchPaymentMethodCardAction?): Boolean =
        status != MonarchPaymentMethodCardExpiry.Expired &&
            (running == null || running == MonarchPaymentMethodCardAction.MakeDefault)

    /** Whether Remove can be used. */
    fun removeEnabled(removeEnabled: Boolean, running: MonarchPaymentMethodCardAction?): Boolean =
        removeEnabled && (running == null || running == MonarchPaymentMethodCardAction.Remove)

    /** The colours of the Default badge: the accent roles, because `MonarchTone` has no accent tone. */
    fun defaultBadgeColors(colors: MonarchColors): MonarchSurfaceColors =
        MonarchSurfaceColors(colors.accentSubtle, colors.accentStrong, null)

    /** The placeholder behind a missing network image. */
    fun imagePlaceholder(colors: MonarchColors): MonarchSurfaceColors =
        MonarchSurfaceColors(colors.bgMuted, colors.textMuted, colors.borderSubtle)

    private const val MONTHS = 12
}
