package dev.monarchui.components.geo.phonecountryselect

import dev.monarchui.components.forms.combobox.MonarchComboboxOption
import dev.monarchui.components.geo.countryselect.CountrySelectData
import java.util.Locale

/** One country or territory and its international calling code (ITU-T E.164). */
internal class PhoneCountrySelectCountry(
    /** ISO 3166-1 alpha-2 code, upper case, such as "GB". */
    val code: String,
    /** The calling code with a leading plus, such as "+44". Several countries can share one. */
    val dialCode: String,
    /** Digits after a shared calling code that belong to this country, such as "242" for the Bahamas under "+1". */
    val leadingDigits: List<String>,
)

/**
 * The calling codes of MonarchPhoneCountrySelect. Android and the JDK expose
 * no calling codes, so they are bundled: the same table as the web component,
 * from the ITU-T E.164 assignments. Pure, so it is tested without a view.
 */
internal object PhoneCountrySelectData {
    // Code, calling code (without "+") and, after "-", dot-separated leading digits.
    private const val DATA: String =
        "AD376 AE971 AF93 AG1-268 AI1-264 AL355 AM374 AO244 AR54 AS1-684 AT43 AU61 AW297 AX358-18 AZ994 " +
            "BA387 BB1-246 BD880 BE32 BF226 BG359 BH973 BI257 BJ229 BL590 BM1-441 BN673 BO591 BQ599-3.4.7 " +
            "BR55 BS1-242 BT975 BW267 BY375 BZ501 CA1 CC61 CD243 CF236 CG242 CH41 CI225 CK682 CL56 CM237 " +
            "CN86 CO57 CR506 CU53 CV238 CW599 CX61 CY357 CZ420 DE49 DJ253 DK45 DM1-767 DO1-809.829.849 " +
            "DZ213 EC593 EE372 EG20 EH212 ER291 ES34 ET251 FI358 FJ679 FK500 FM691 FO298 FR33 GA241 GB44 " +
            "GD1-473 GE995 GF594 GG44-1481 GH233 GI350 GL299 GM220 GN224 GP590 GQ240 GR30 GT502 GU1-671 " +
            "GW245 GY592 HK852 HN504 HR385 HT509 HU36 ID62 IE353 IL972 IM44-1624 IN91 IO246 IQ964 IR98 " +
            "IS354 IT39 JE44-1534 JM1-876.658 JO962 JP81 KE254 KG996 KH855 KI686 KM269 KN1-869 KP850 KR82 " +
            "KW965 KY1-345 KZ7-6.7 LA856 LB961 LC1-758 LI423 LK94 LR231 LS266 LT370 LU352 LV371 LY218 " +
            "MA212 MC377 MD373 ME382 MF590 MG261 MH692 MK389 ML223 MM95 MN976 MO853 MP1-670 MQ596 MR222 " +
            "MS1-664 MT356 MU230 MV960 MW265 MX52 MY60 MZ258 NA264 NC687 NE227 NF672 NG234 NI505 NL31 " +
            "NO47 NP977 NR674 NU683 NZ64 OM968 PA507 PE51 PF689 PG675 PH63 PK92 PL48 PM508 PR1-787.939 " +
            "PS970 PT351 PW680 PY595 QA974 RE262 RO40 RS381 RU7 RW250 SA966 SB677 SC248 SD249 SE46 SG65 " +
            "SH290 SI386 SJ47-79 SK421 SL232 SM378 SN221 SO252 SR597 SS211 ST239 SV503 SX1-721 SY963 SZ268 " +
            "TC1-649 TD235 TG228 TH66 TJ992 TK690 TL670 TM993 TN216 TO676 TR90 TT1-868 TV688 TW886 TZ255 " +
            "UA380 UG256 US1 UY598 UZ998 VA39-06698 VC1-784 VE58 VG1-284 VI1-340 VN84 VU678 WF681 WS685 " +
            "XK383 YE967 YT262-269.639 ZA27 ZM260 ZW263"

    private const val CODE_LENGTH = 2

    /** Every country and territory with its calling code, ordered by ISO code. */
    val COUNTRIES: List<PhoneCountrySelectCountry> = DATA.split(" ").map { entry ->
        val head = entry.substringBefore("-")
        val digits = entry.substringAfter("-", "")
        PhoneCountrySelectCountry(
            code = head.take(CODE_LENGTH),
            dialCode = "+" + head.drop(CODE_LENGTH),
            leadingDigits = if (digits.isEmpty()) emptyList() else digits.split("."),
        )
    }

    private val byCode = COUNTRIES.associateBy { it.code }

    /** The entry for an ISO code in any case, or `null`. */
    fun country(code: String): PhoneCountrySelectCountry? = byCode[CountrySelectData.canonical(code)]

    /** The words a country is found by besides its name: the ISO code, the calling code with and without "+". */
    fun keywords(country: PhoneCountrySelectCountry): String {
        val shared = country.leadingDigits.map { country.dialCode + it }
        return (listOf(country.code, country.dialCode, country.dialCode.drop(1)) + shared).joinToString(" ")
    }

    /**
     * The options for [countries] (or every country), unknown codes dropped, each labelled by [optionText] from the
     * name in [locale] and the calling code, sorted by that label, with the flag as the hint when [showFlag].
     */
    fun options(
        countries: List<String>?,
        locale: Locale,
        showFlag: Boolean,
        optionText: (name: String, dialCode: String) -> String,
    ): List<MonarchComboboxOption> {
        val list = countries?.mapNotNull(::country)?.distinctBy { it.code } ?: COUNTRIES
        val options = list.map { country ->
            MonarchComboboxOption(
                id = country.code,
                label = optionText(CountrySelectData.name(country.code, locale), country.dialCode),
                hint = CountrySelectData.flag(country.code).takeIf { showFlag },
                keywords = keywords(country),
            )
        }
        return CountrySelectData.sorted(options, locale) { it.label }
    }
}
