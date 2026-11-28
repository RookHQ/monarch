package dev.monarchui.components.geo.currencyselect

import dev.monarchui.components.forms.combobox.MonarchComboboxOption
import dev.monarchui.components.geo.countryselect.CountrySelectData
import java.util.Currency
import java.util.Locale

/** The currency list and option rules of MonarchCurrencySelect. Pure, so it is tested without a view. */
internal object CurrencySelectData {
    /**
     * The active ISO 4217 codes in circulation, the same list as the web
     * component: no funds, metals, test codes or retired currencies, which
     * `Currency.getAvailableCurrencies` would include.
     */
    val CODES: List<String> = (
        "AED AFN ALL AMD AOA ARS AUD AWG AZN BAM BBD BDT BHD BIF BMD BND BOB BRL BSD BTN BWP BYN BZD " +
            "CAD CDF CHF CLP CNY COP CRC CUP CVE CZK DJF DKK DOP DZD EGP ERN ETB EUR FJD FKP GBP GEL GHS " +
            "GIP GMD GNF GTQ GYD HKD HNL HTG HUF IDR ILS INR IQD IRR ISK JMD JOD JPY KES KGS KHR KMF KPW " +
            "KRW KWD KYD KZT LAK LBP LKR LRD LSL LYD MAD MDL MGA MKD MMK MNT MOP MRU MUR MVR MWK MXN MYR " +
            "MZN NAD NGN NIO NOK NPR NZD OMR PAB PEN PGK PHP PKR PLN PYG QAR RON RSD RUB RWF SAR SBD SCR " +
            "SDG SEK SGD SHP SLE SOS SRD SSP STN SVC SYP SZL THB TJS TMT TND TOP TRY TTD TWD TZS UAH UGX " +
            "USD UYU UZS VED VES VND VUV WST XAF XCD XCG XOF XPF YER ZAR ZMW ZWG"
        ).split(" ")

    private fun currency(code: String): Currency? = runCatching { Currency.getInstance(code) }.getOrNull()

    /** The currency's name in [locale], or the code when the runtime does not know the currency. */
    fun name(code: String, locale: Locale): String =
        currency(code)?.getDisplayName(locale)?.takeIf { it.isNotBlank() } ?: code

    /** The currency's symbol in [locale] ("$" for USD in the US, "US$" elsewhere), or the code. */
    fun symbol(code: String, locale: Locale): String =
        currency(code)?.getSymbol(locale)?.takeIf { it.isNotBlank() } ?: code

    /** The label of [code] for [display]. */
    fun label(code: String, name: String, display: MonarchCurrencySelectDisplay): String = when (display) {
        MonarchCurrencySelectDisplay.Name -> name
        MonarchCurrencySelectDisplay.Code -> code
        MonarchCurrencySelectDisplay.CodeAndName -> "$code · $name"
    }

    /**
     * The option for [code]: the label for [display], the name as a second line unless the label is the name, the
     * code and symbol as the hint, and the symbol and English name as keywords.
     */
    fun option(
        code: String,
        locale: Locale,
        display: MonarchCurrencySelectDisplay,
        showSymbol: Boolean,
    ): MonarchComboboxOption {
        val name = name(code, locale)
        val symbol = symbol(code, locale)
        val english = if (CountrySelectData.isEnglish(locale)) null else name(code, Locale.ENGLISH)
        return MonarchComboboxOption(
            id = code,
            label = label(code, name, display),
            description = name.takeIf { display != MonarchCurrencySelectDisplay.Name },
            hint = if (showSymbol && symbol != code) "$code $symbol" else code,
            keywords = listOfNotNull(symbol, english).joinToString(" "),
        )
    }

    /** The options for [currencies] (or [CODES]) sorted by name, with [selected] listed when it is not among them. */
    fun options(
        currencies: List<String>?,
        selected: String?,
        locale: Locale,
        display: MonarchCurrencySelectDisplay,
        showSymbol: Boolean,
    ): List<MonarchComboboxOption> {
        val codes = CountrySelectData.codes(currencies ?: CODES, emptyList())
        val extra = selected?.takeIf { it.isNotEmpty() && it !in codes }
        return CountrySelectData.sorted(listOfNotNull(extra) + codes, locale) { name(it, locale) }
            .map { option(it, locale, display, showSymbol) }
    }
}
