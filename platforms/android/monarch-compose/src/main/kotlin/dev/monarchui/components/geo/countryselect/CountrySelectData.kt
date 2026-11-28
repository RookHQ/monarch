package dev.monarchui.components.geo.countryselect

import dev.monarchui.components.forms.combobox.MonarchComboboxOption
import dev.monarchui.components.geo.worldmap.WorldMapText
import dev.monarchui.internal.MonarchSearchText
import java.text.Collator
import java.util.Locale

/**
 * The country list of MonarchCountrySelect and the option rules shared by the
 * geo pickers (country, currency, language, phone country). Pure, so it is
 * tested without a view.
 */
internal object CountrySelectData {
    /**
     * The 249 officially assigned ISO 3166-1 alpha-2 codes, the same list as
     * the web component. Bundled rather than read from `Locale.getISOCountries`
     * so every Android version and runtime lists the same countries.
     */
    val CODES: List<String> = (
        "AD AE AF AG AI AL AM AO AQ AR AS AT AU AW AX AZ BA BB BD BE BF BG BH BI BJ BL BM BN BO BQ BR " +
            "BS BT BV BW BY BZ CA CC CD CF CG CH CI CK CL CM CN CO CR CU CV CW CX CY CZ DE DJ DK DM DO DZ " +
            "EC EE EG EH ER ES ET FI FJ FK FM FO FR GA GB GD GE GF GG GH GI GL GM GN GP GQ GR GS GT GU GW " +
            "GY HK HM HN HR HT HU ID IE IL IM IN IO IQ IR IS IT JE JM JO JP KE KG KH KI KM KN KP KR KW KY " +
            "KZ LA LB LC LI LK LR LS LT LU LV LY MA MC MD ME MF MG MH MK ML MM MN MO MP MQ MR MS MT MU MV " +
            "MW MX MY MZ NA NC NE NF NG NI NL NO NP NR NU NZ OM PA PE PF PG PH PK PL PM PN PR PS PT PW PY " +
            "QA RE RO RS RU RW SA SB SC SD SE SG SH SI SJ SK SL SM SN SO SR SS ST SV SX SY SZ TC TD TF TG " +
            "TH TJ TK TL TM TN TO TR TT TV TW TZ UA UG UM US UY UZ VA VC VE VG VI VN VU WF WS YE YT ZA ZM ZW"
        ).split(" ")

    private const val REGIONAL_INDICATOR_A = 0x1F1E6
    private val twoLetters = Regex("^[A-Za-z]{2}$")
    private val spaces = Regex("\\s+")

    /** [code] in upper case without surrounding spaces. */
    fun canonical(code: String): String = code.trim().uppercase(Locale.ROOT)

    /** The country's name in [locale] (the same rule as MonarchWorldMap), else the code. */
    fun name(code: String, locale: Locale): String = WorldMapText.countryName(code, locale, null)

    /**
     * The flag emoji of a two-letter code, made of regional indicator symbols
     * ("PT" gives 🇵🇹), or "" for anything else. Some fonts draw the two letters.
     */
    fun flag(code: String): String {
        if (!twoLetters.matches(code)) return ""
        val builder = StringBuilder()
        canonical(code).forEach { builder.appendCodePoint(REGIONAL_INDICATOR_A + (it - 'A')) }
        return builder.toString()
    }

    /** Whether [locale] is an English locale, where names need no English keywords. */
    fun isEnglish(locale: Locale): Boolean = locale.language == Locale.ENGLISH.language

    /** Distinct canonical codes from [codes], dropping [exclude]. */
    fun codes(codes: List<String>?, exclude: List<String>): List<String> {
        val skip = exclude.map(::canonical).toSet()
        return (codes ?: CODES).map(::canonical).filter { it.isNotEmpty() && it !in skip }.distinct()
    }

    /** [items] sorted by [label] with the collation of [locale]. */
    fun <T> sorted(items: List<T>, locale: Locale, label: (T) -> String): List<T> {
        val collator = Collator.getInstance(locale)
        return items.sortedWith { a, b -> collator.compare(label(a), label(b)) }
    }

    /**
     * The option for [code]: the name (or [countryName]'s), with the flag and the code as the hint, and the English
     * name as a keyword outside English.
     */
    fun option(code: String, locale: Locale, style: CountrySelectStyle): MonarchComboboxOption {
        val name = style.countryName?.invoke(code) ?: name(code, locale)
        val hint = listOfNotNull(flag(code).takeIf { style.showFlag }, code.takeIf { style.showCode })
            .filter { it.isNotEmpty() }
            .joinToString(" ")
        return MonarchComboboxOption(
            id = code,
            label = name,
            hint = hint.ifEmpty { null },
            keywords = if (isEnglish(locale)) null else name(code, Locale.ENGLISH),
        )
    }

    /** The options for [codes] sorted by name, with [selected] listed too when it is not among them. */
    fun options(
        codes: List<String>,
        selected: String?,
        locale: Locale,
        style: CountrySelectStyle,
    ): List<MonarchComboboxOption> {
        val extra = selected?.let(::canonical)?.takeIf { it.isNotEmpty() && it !in codes }
        return sorted(listOfNotNull(extra) + codes, locale) { style.countryName?.invoke(it) ?: name(it, locale) }
            .map { option(it, locale, style) }
    }

    /** The default filter of the geo pickers: every word typed is in the label, id, description, hint or keywords. */
    fun matches(option: MonarchComboboxOption, query: String): Boolean {
        val haystack = MonarchSearchText.normalize(
            listOfNotNull(option.label, option.id, option.description, option.hint, option.keywords).joinToString(" "),
        )
        return MonarchSearchText.normalize(query).split(spaces).filter { it.isNotEmpty() }.all { haystack.contains(it) }
    }

    /**
     * [options] arranged for the list: while nothing is searched and some ids of [priority] are present, those come
     * first under [suggested] and the rest follow under [all], each option listed once; otherwise [options] as they
     * are, without headings.
     */
    fun arrange(
        options: List<MonarchComboboxOption>,
        priority: List<String>,
        searching: Boolean,
        suggested: String,
        all: String,
    ): List<MonarchComboboxOption> {
        if (searching) return options
        val byId = options.associateBy { it.id }
        val top = priority.distinct().mapNotNull { byId[it] }
        if (top.isEmpty()) return options
        val topIds = top.map { it.id }.toSet()
        return top.map { grouped(it, suggested) } + options.filter { it.id !in topIds }.map { grouped(it, all) }
    }

    /** [option] under the heading [group]. */
    private fun grouped(option: MonarchComboboxOption, group: String): MonarchComboboxOption = MonarchComboboxOption(
        id = option.id,
        label = option.label,
        description = option.description,
        hint = option.hint,
        group = group,
        keywords = option.keywords,
        enabled = option.enabled,
    )
}

/** What a country option shows: the flag, the code, and the app's own names. */
internal class CountrySelectStyle(
    val showFlag: Boolean,
    val showCode: Boolean,
    val countryName: ((String) -> String?)?,
)
