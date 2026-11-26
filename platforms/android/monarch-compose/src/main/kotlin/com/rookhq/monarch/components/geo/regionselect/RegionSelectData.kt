package com.rookhq.monarch.components.geo.regionselect

import androidx.annotation.StringRes
import com.rookhq.monarch.R
import com.rookhq.monarch.components.forms.combobox.MonarchComboboxOption
import java.util.Locale

/**
 * The built-in subdivision lists of MonarchRegionSelect and its option rules.
 * Pure, so it is tested without a view.
 *
 * The lists are the same as the web component's: the United States (states
 * and DC), Canada, Australia, the United Kingdom (nations) and India, with
 * English names and ISO 3166-2 codes. Every other country needs `regions` or
 * `loadRegions`.
 */
internal object RegionSelectData {
    private val lists: Map<String, String> = mapOf(
        "US" to "AL:Alabama;AK:Alaska;AZ:Arizona;AR:Arkansas;CA:California;CO:Colorado;CT:Connecticut;" +
            "DE:Delaware;DC:District of Columbia;FL:Florida;GA:Georgia;HI:Hawaii;ID:Idaho;IL:Illinois;" +
            "IN:Indiana;IA:Iowa;KS:Kansas;KY:Kentucky;LA:Louisiana;ME:Maine;MD:Maryland;" +
            "MA:Massachusetts;MI:Michigan;MN:Minnesota;MS:Mississippi;MO:Missouri;MT:Montana;" +
            "NE:Nebraska;NV:Nevada;NH:New Hampshire;NJ:New Jersey;NM:New Mexico;NY:New York;" +
            "NC:North Carolina;ND:North Dakota;OH:Ohio;OK:Oklahoma;OR:Oregon;PA:Pennsylvania;" +
            "RI:Rhode Island;SC:South Carolina;SD:South Dakota;TN:Tennessee;TX:Texas;UT:Utah;" +
            "VT:Vermont;VA:Virginia;WA:Washington;WV:West Virginia;WI:Wisconsin;WY:Wyoming",
        "CA" to "AB:Alberta;BC:British Columbia;MB:Manitoba;NB:New Brunswick;NL:Newfoundland and Labrador;" +
            "NS:Nova Scotia;NT:Northwest Territories;NU:Nunavut;ON:Ontario;PE:Prince Edward Island;" +
            "QC:Quebec;SK:Saskatchewan;YT:Yukon",
        "AU" to "ACT:Australian Capital Territory;NSW:New South Wales;NT:Northern Territory;QLD:Queensland;" +
            "SA:South Australia;TAS:Tasmania;VIC:Victoria;WA:Western Australia",
        "GB" to "ENG:England;NIR:Northern Ireland;SCT:Scotland;WLS:Wales",
        "IN" to "AN:Andaman and Nicobar Islands;AP:Andhra Pradesh;AR:Arunachal Pradesh;AS:Assam;BR:Bihar;" +
            "CH:Chandigarh;CG:Chhattisgarh;DH:Dadra and Nagar Haveli and Daman and Diu;DL:Delhi;GA:Goa;" +
            "GJ:Gujarat;HR:Haryana;HP:Himachal Pradesh;JK:Jammu and Kashmir;JH:Jharkhand;KA:Karnataka;" +
            "KL:Kerala;LA:Ladakh;LD:Lakshadweep;MP:Madhya Pradesh;MH:Maharashtra;MN:Manipur;" +
            "ML:Meghalaya;MZ:Mizoram;NL:Nagaland;OD:Odisha;PY:Puducherry;PB:Punjab;RJ:Rajasthan;" +
            "SK:Sikkim;TN:Tamil Nadu;TS:Telangana;TR:Tripura;UP:Uttar Pradesh;UK:Uttarakhand;" +
            "WB:West Bengal",
    )

    private val countryPrefix = Regex("^[A-Z]{2}-")

    /** The built-in regions of a country code (any case), or `null` when Monarch ships none. */
    fun builtIn(country: String): List<MonarchRegionSelectRegion>? {
        val upper = country.trim().uppercase(Locale.ROOT)
        return lists[upper]?.split(";")?.map { entry ->
            MonarchRegionSelectRegion(code = "$upper-${entry.substringBefore(":")}", name = entry.substringAfter(":"))
        }
    }

    /** The built-in string of the label for [country]: its own word for its regions, else "Region". */
    @StringRes
    fun labelRes(country: String?): Int = when (country?.trim()?.uppercase(Locale.ROOT)) {
        "US" -> R.string.monarch_regionselect_label_us
        "CA" -> R.string.monarch_regionselect_label_ca
        "AU" -> R.string.monarch_regionselect_label_au
        "GB" -> R.string.monarch_regionselect_label_gb
        "IN" -> R.string.monarch_regionselect_label_in
        else -> R.string.monarch_regionselect_label
    }

    /** The option of [region]: its name, its code as a keyword, and its code without the country as the hint. */
    fun option(region: MonarchRegionSelectRegion, showCode: Boolean): MonarchComboboxOption = MonarchComboboxOption(
        id = region.code,
        label = region.name,
        hint = region.code.replace(countryPrefix, "").takeIf { showCode },
        keywords = region.code,
    )
}

/** Where the regions of a MonarchRegionSelect come from, for the current country. */
internal sealed interface RegionSelectSource {
    /** No country is chosen. */
    data object NoCountry : RegionSelectSource

    /** The regions are known. An empty list means the country has none to list. */
    class Ready(val regions: List<MonarchRegionSelectRegion>) : RegionSelectSource

    /** `loadRegions` is running. */
    data object Loading : RegionSelectSource

    /** `loadRegions` failed; [retry] asks again. */
    class Failed(val retry: () -> Unit) : RegionSelectSource
}
