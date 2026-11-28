package dev.monarchui.components.geo.languageselect

import dev.monarchui.components.forms.combobox.MonarchComboboxOption
import dev.monarchui.components.geo.countryselect.CountrySelectData
import java.util.Locale

/** The language list and option rules of MonarchLanguageSelect. Pure, so it is tested without a view. */
internal object LanguageSelectData {
    /** About 80 widely used BCP 47 tags, the same default list as the web component. */
    val TAGS: List<String> = (
        "af am ar az be bg bn bs ca cs cy da de el en en-GB en-US es es-419 et eu fa fi fil fr fr-CA " +
            "ga gl gu ha he hi hr hu hy id ig is it ja jv ka kk km kn ko ky lo lt lv mk ml mn mr ms mt my " +
            "nb ne nl pa pl ps pt pt-BR pt-PT ro ru si sk sl so sq sr sv sw ta te th tr uk ur uz vi xh yo " +
            "zh-Hans zh-Hant zu"
        ).split(" ")

    private const val UNDETERMINED = "und"

    /** The canonical form of a BCP 47 tag ("EN-us" gives "en-US"), or `null` when it names no language. */
    fun canonical(tag: String): String? {
        val trimmed = tag.trim()
        if (trimmed.isEmpty()) return null
        val canonical = Locale.forLanguageTag(trimmed).toLanguageTag()
        return canonical.takeIf { it != UNDETERMINED && !it.startsWith("$UNDETERMINED-") }
    }

    /** The language's name in [inLocale] with its first letter capitalised for that locale, else the tag. */
    fun name(tag: String, inLocale: Locale): String {
        val name = Locale.forLanguageTag(tag).getDisplayName(inLocale)
        if (name.isBlank() || name == tag) return tag
        return name.substring(0, 1).uppercase(inLocale) + name.substring(1)
    }

    /** The language's name in the language itself ("Deutsch" for "de"). */
    fun endonym(tag: String): String = name(tag, Locale.forLanguageTag(tag))

    /**
     * The option for [tag]: the endonym or the localised name as the label, the other as a second line when it
     * differs, the tag as the hint when [showTag], and the tag and English name as keywords.
     */
    fun option(
        tag: String,
        locale: Locale,
        style: MonarchLanguageSelectLabelStyle,
        showTag: Boolean,
    ): MonarchComboboxOption {
        val endonym = endonym(tag)
        val localized = name(tag, locale)
        val label = if (style == MonarchLanguageSelectLabelStyle.Endonym) endonym else localized
        val other = if (style == MonarchLanguageSelectLabelStyle.Endonym) localized else endonym
        return MonarchComboboxOption(
            id = tag,
            label = label,
            description = other.takeIf { it != label },
            hint = tag.takeIf { showTag },
            keywords = "$tag ${name(tag, Locale.ENGLISH)}",
        )
    }

    /**
     * The options for [languages] (or [TAGS]): canonical, without invalid tags or repeats, sorted by the name in
     * [locale], with [selected] listed too when it is not among them.
     */
    fun options(
        languages: List<String>?,
        selected: String?,
        locale: Locale,
        style: MonarchLanguageSelectLabelStyle,
        showTag: Boolean,
    ): List<MonarchComboboxOption> {
        val tags = (languages ?: TAGS).mapNotNull(::canonical).distinct()
        val extra = selected?.takeIf { it !in tags }
        return CountrySelectData.sorted(listOfNotNull(extra) + tags, locale) { name(it, locale) }
            .map { option(it, locale, style, showTag) }
    }
}
