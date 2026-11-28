package dev.monarchui.components.geo.languageselect

/** Which name of a language a `MonarchLanguageSelect` row shows first. */
public enum class MonarchLanguageSelectLabelStyle {
    /** The language's own name first ("Deutsch"), its name in the interface language second ("German"). */
    Endonym,

    /** The name in the interface language first ("German"), the language's own name second ("Deutsch"). */
    Localized,
}
