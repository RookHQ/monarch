package com.rookhq.monarch.components.scheduling.timezoneselect

import com.rookhq.monarch.components.forms.combobox.MonarchComboboxOption
import com.rookhq.monarch.internal.MonarchSearchText
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

/** The section headings of the list, resolved. */
internal class TimezoneSelectSections(val detected: String, val pinned: String, val recent: String, val all: String)

/** What the list of a time zone select is built from. */
internal class TimezoneSelectRequest(
    val all: List<String>,
    val selected: String?,
    val detected: String?,
    val pinned: List<String>,
    val recent: List<String>,
    val searching: Boolean,
)

/**
 * The IANA zones of MonarchTimezoneSelect: listing, current names, offsets,
 * labels, search words and the options of the list. Pure apart from the
 * system's zone data, so it is tested without a view. `java.time` needs API
 * 26, so this reads `java.util.TimeZone` and `android.icu`.
 */
internal object TimezoneSelectZones {
    const val UTC: String = "UTC"
    private const val SECONDS_PER_MINUTE = 60
    private const val MINUTES_PER_HOUR = 60
    private const val MILLIS_PER_SECOND = 1_000
    private val gmtName = Regex("^GMT[+\\-−]")
    private val words = Regex("\\s+")

    /**
     * Old ids that systems still list (ICU lists `Asia/Calcutta`, not
     * `Asia/Kolkata`), mapped to the current IANA names, as on the web.
     */
    private val legacy: Map<String, String> = mapOf(
        "Africa/Asmera" to "Africa/Asmara",
        "America/Buenos_Aires" to "America/Argentina/Buenos_Aires",
        "America/Catamarca" to "America/Argentina/Catamarca",
        "America/Cordoba" to "America/Argentina/Cordoba",
        "America/Jujuy" to "America/Argentina/Jujuy",
        "America/Mendoza" to "America/Argentina/Mendoza",
        "America/Coral_Harbour" to "America/Atikokan",
        "America/Godthab" to "America/Nuuk",
        "America/Indianapolis" to "America/Indiana/Indianapolis",
        "America/Louisville" to "America/Kentucky/Louisville",
        "Asia/Calcutta" to "Asia/Kolkata",
        "Asia/Katmandu" to "Asia/Kathmandu",
        "Asia/Rangoon" to "Asia/Yangon",
        "Asia/Saigon" to "Asia/Ho_Chi_Minh",
        "Asia/Dacca" to "Asia/Dhaka",
        "Asia/Thimbu" to "Asia/Thimphu",
        "Asia/Ulan_Bator" to "Asia/Ulaanbaatar",
        "Asia/Macao" to "Asia/Macau",
        "Atlantic/Faeroe" to "Atlantic/Faroe",
        "Europe/Kiev" to "Europe/Kyiv",
        "Pacific/Enderbury" to "Pacific/Kanton",
        "Pacific/Ponape" to "Pacific/Pohnpei",
        "Pacific/Truk" to "Pacific/Chuuk",
        "Etc/UTC" to UTC,
        "Etc/GMT" to UTC,
        "Etc/Universal" to UTC,
        "Etc/Zulu" to UTC,
        "GMT" to UTC,
        "UCT" to UTC,
        "Universal" to UTC,
        "Zulu" to UTC,
    )
    private val currentToLegacy: Map<String, String> = legacy.entries.associate { it.value to it.key }

    /** Country names, old names and abbreviations, so people find zones the way they think of them. */
    private val aliases: Map<String, String> = mapOf(
        UTC to "gmt zulu universal coordinated",
        "Asia/Kolkata" to "india ist calcutta mumbai delhi bengaluru bangalore chennai hyderabad",
        "Asia/Dubai" to "uae emirates gulf abu dhabi",
        "Asia/Riyadh" to "saudi arabia",
        "Asia/Karachi" to "pakistan",
        "Asia/Dhaka" to "bangladesh",
        "Asia/Kathmandu" to "nepal katmandu",
        "Asia/Colombo" to "sri lanka",
        "Asia/Singapore" to "sgt",
        "Asia/Tokyo" to "japan jst",
        "Asia/Seoul" to "korea kst",
        "Asia/Shanghai" to "china beijing cst",
        "Asia/Hong_Kong" to "hkt",
        "Asia/Jakarta" to "indonesia wib",
        "Asia/Manila" to "philippines",
        "Asia/Ho_Chi_Minh" to "vietnam saigon",
        "Asia/Bangkok" to "thailand",
        "Asia/Jerusalem" to "israel tel aviv",
        "Europe/London" to "uk united kingdom britain england gmt bst",
        "Europe/Dublin" to "ireland",
        "Europe/Paris" to "france cet",
        "Europe/Berlin" to "germany cet",
        "Europe/Madrid" to "spain",
        "Europe/Rome" to "italy",
        "Europe/Amsterdam" to "netherlands holland",
        "Europe/Kyiv" to "ukraine kiev",
        "Europe/Istanbul" to "turkey türkiye",
        "Europe/Moscow" to "russia msk",
        "Africa/Lagos" to "nigeria wat",
        "Africa/Nairobi" to "kenya eat",
        "Africa/Johannesburg" to "south africa sast",
        "Africa/Cairo" to "egypt",
        "America/New_York" to "us usa united states eastern est edt",
        "America/Chicago" to "us usa united states central cst cdt",
        "America/Denver" to "us usa united states mountain mst mdt",
        "America/Phoenix" to "us usa arizona mst",
        "America/Los_Angeles" to "us usa united states pacific pst pdt california san francisco",
        "America/Anchorage" to "alaska",
        "Pacific/Honolulu" to "hawaii hst",
        "America/Toronto" to "canada eastern",
        "America/Vancouver" to "canada pacific",
        "America/Mexico_City" to "mexico",
        "America/Sao_Paulo" to "brazil brasil",
        "America/Argentina/Buenos_Aires" to "argentina",
        "America/Bogota" to "colombia",
        "Australia/Sydney" to "australia aest aedt",
        "Australia/Melbourne" to "australia aest aedt",
        "Australia/Perth" to "australia awst",
        "Pacific/Auckland" to "new zealand nzst nzdt",
    )

    /** The current IANA name of [zone]: `Asia/Calcutta` gives `Asia/Kolkata`, `Etc/UTC` gives `UTC`. */
    fun canonical(zone: String): String = legacy[zone] ?: zone

    /** The first parts of the ids of zones named for a place; the rest are aliases and fixed offsets. */
    private val regions = setOf(
        "Africa", "America", "Antarctica", "Arctic", "Asia", "Atlantic", "Australia", "Europe", "Indian", "Pacific",
    )

    /**
     * Every zone the system knows for a place, by current name, with `UTC`
     * first. Aliases collapse to one zone through ICU's canonical id (API 24),
     * then to the current IANA name.
     */
    fun available(): List<String> {
        val ids = TimeZone.getAvailableIDs().filter { it.substringBefore('/') in regions && '/' in it }
        val current = ids.map { canonical(android.icu.util.TimeZone.getCanonicalID(it) ?: it) }
        return (listOf(UTC) + current.filter { it.substringBefore('/') in regions }).distinct()
    }

    /** The system's zone for [zone], trying its old name when the system only knows that; `null` if unknown. */
    private fun systemZone(zone: String): TimeZone? {
        if (zone == UTC) return TimeZone.getTimeZone(UTC)
        val known = TimeZone.getTimeZone(zone).takeIf { it.id != "GMT" }
        return known ?: currentToLegacy[zone]?.let { old -> TimeZone.getTimeZone(old).takeIf { it.id != "GMT" } }
    }

    /** "UTC+05:30", "UTC-08:00", "UTC+00:00". */
    fun offsetLabel(seconds: Int): String {
        val minutes = abs(seconds) / SECONDS_PER_MINUTE
        val sign = if (seconds < 0) "-" else "+"
        val hours = minutes / MINUTES_PER_HOUR
        return String.format(Locale.ROOT, "UTC%s%02d:%02d", sign, hours, minutes % MINUTES_PER_HOUR)
    }

    /**
     * The offset, readable city and long name of [zone] at [at], in
     * [locale]. A zone the system does not know has offset 0 and no long name.
     */
    fun info(zone: String, at: Long, locale: Locale): MonarchTimezoneInfo {
        val segments = zone.split('/')
        val city = if (zone == UTC) UTC else segments.last().replace('_', ' ')
        val region = if (segments.size > 1) segments.first() else ""
        val system = systemZone(zone)
        val offset = (system?.getOffset(at) ?: 0) / MILLIS_PER_SECOND
        val longName = system
            ?.getDisplayName(system.inDaylightTime(Date(at)), TimeZone.LONG, locale)
            ?.takeUnless { gmtName.containsMatchIn(it) }
            .orEmpty()
        return MonarchTimezoneInfo(zone, city, region, offset, offsetLabel(offset), longName)
    }

    /** The offset of [zone] at [at] in seconds, for sorting. */
    private fun offsetOf(zone: String, at: Long): Int = (systemZone(zone)?.getOffset(at) ?: 0) / MILLIS_PER_SECOND

    /** [zones] by offset at [at], then by id. */
    fun sorted(zones: List<String>, at: Long): List<String> =
        zones.map { it to offsetOf(it, at) }.sortedWith(compareBy({ it.second }, { it.first })).map { it.first }

    /** The built-in option label: "(UTC+05:30) Kolkata". */
    fun defaultLabel(info: MonarchTimezoneInfo): String = "(${info.offsetLabel}) ${info.city}"

    /** The words an option is found by: its id, offsets in several spellings, and aliases. */
    fun keywords(info: MonarchTimezoneInfo): String {
        val short = info.offsetLabel.removePrefix("UTC").replace(Regex("^([+-])0"), "$1")
        return listOf(
            info.id.replace('_', ' '),
            info.offsetLabel,
            "gmt$short",
            "utc$short",
            short,
            short.replace(":", ""),
            aliases[info.id].orEmpty(),
        ).joinToString(" ")
    }

    /** An option for [info], under [group]. */
    fun option(info: MonarchTimezoneInfo, label: String, group: String?): MonarchComboboxOption =
        MonarchComboboxOption(
            id = info.id,
            label = label,
            description = if (info.longName.isNotEmpty()) "${info.id} · ${info.longName}" else info.id,
            group = group,
            keywords = keywords(info),
        )

    /** Every word typed must appear in the option's label, description or search words, ignoring case and accents. */
    fun matches(option: MonarchComboboxOption, query: String): Boolean {
        val text = listOfNotNull(option.label, option.description, option.keywords).joinToString(" ")
        val haystack = MonarchSearchText.normalize(text)
        return MonarchSearchText.normalize(query).split(words).filter { it.isNotEmpty() }.all { haystack.contains(it) }
    }

    /**
     * The options of the list. While searching: every zone once, the selected
     * zone first if [TimezoneSelectRequest.all] lacks it. Otherwise the
     * Detected, Pinned and Recent sections (each zone once among them), then
     * every zone under All time zones, so a zone also appears in its offset
     * order. Without sections the full list has no heading.
     */
    fun options(
        request: TimezoneSelectRequest,
        sections: TimezoneSelectSections,
        option: (zone: String, group: String?) -> MonarchComboboxOption,
    ): List<MonarchComboboxOption> {
        val extra = listOfNotNull(request.selected?.takeIf { it !in request.all })
        val full = extra + request.all
        if (request.searching) return full.map { option(it, null) }
        val out = mutableListOf<MonarchComboboxOption>()
        val seen = mutableSetOf<String>()
        fun section(list: List<String?>, group: String) {
            list.filterNotNull().map(::canonical).forEach { zone ->
                if (seen.add(zone)) out += option(zone, group)
            }
        }
        section(listOf(request.detected), sections.detected)
        section(request.pinned, sections.pinned)
        section(request.recent, sections.recent)
        val grouped = out.isNotEmpty()
        full.forEach { out += option(it, if (grouped) sections.all else null) }
        return out
    }
}
