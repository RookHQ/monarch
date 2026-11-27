/// Which name of a language a ``MonarchLanguageSelect`` row shows first.
public enum MonarchLanguageSelectLabelStyle: Sendable, Hashable, CaseIterable {
    /// The language's own name ("Deutsch"), with the name in the interface's
    /// language ("German") under it. The default, so people find their own
    /// language whatever language the interface is in.
    case endonym
    /// The name in the interface's language ("German"), with the language's
    /// own name ("Deutsch") under it.
    case localized
}
