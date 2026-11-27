package com.rookhq.monarch.components.billing.paymentmethodcard

import java.util.Calendar
import java.util.TimeZone

/** The expiry status of a saved card in a `MonarchPaymentMethodCard`. The same names exist on every platform. */
public enum class MonarchPaymentMethodCardExpiry {
    /** The card can be charged; no badge. */
    Valid,

    /** The card expires this month or within the next [status] `soonMonths`; a warning badge. */
    Expiring,

    /** The card's expiry month has passed; a danger badge, and it cannot be made the default. */
    Expired,
    ;

    /** Works out the status from an expiry date. */
    public companion object {
        private const val MONTHS = 12
        private const val CENTURY = 2000
        private const val TWO_DIGITS = 100

        /**
         * The status of a card that expires at the end of [month] of [year], in UTC.
         *
         * @param month the expiry month, 1 to 12. Values outside are clamped.
         * @param year the expiry year, two digits (read as 20YY) or four.
         * @param now the current time in epoch milliseconds.
         * @param soonMonths how many months after the current one still count as expiring soon.
         * @return the status.
         */
        public fun status(
            month: Int,
            year: Int,
            now: Long = System.currentTimeMillis(),
            soonMonths: Int = 1,
        ): MonarchPaymentMethodCardExpiry {
            val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = now }
            val current = calendar.get(Calendar.YEAR) * MONTHS + calendar.get(Calendar.MONTH)
            val expires = fullYear(year) * MONTHS + (month.coerceIn(1, MONTHS) - 1)
            return when {
                current > expires -> Expired
                expires - current <= soonMonths -> Expiring
                else -> Valid
            }
        }

        /** A two-digit year read as 20YY. */
        internal fun fullYear(year: Int): Int = if (year in 0 until TWO_DIGITS) CENTURY + year else year
    }
}
