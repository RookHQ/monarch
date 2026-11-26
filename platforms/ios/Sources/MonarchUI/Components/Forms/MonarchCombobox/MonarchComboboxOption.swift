import Foundation

/// One choice of a ``MonarchCombobox``.
public struct MonarchComboboxOption: Identifiable, Hashable, Sendable {
    /// The value put in the selection. Unique within the list.
    public let id: String
    /// The visible label, already localised. It names the option and is searched.
    public let label: String
    /// Secondary text under the label.
    public let description: String?
    /// Short trailing text, such as a count, a code or an email. Searched too.
    public let hint: String?
    /// A heading the option is listed under. Groups keep the order in which
    /// they first appear; options without a group come first.
    public let group: String?
    /// Extra words that are searched but never shown, such as old names.
    public let keywords: String?
    /// A decorative SF Symbol before the label.
    public let systemImage: String?
    /// Decorative text before the label, such as a flag emoji. Not read by
    /// VoiceOver: the label names the option.
    public let leadingText: String?
    /// The language the label is written in, when it is not the app's: a
    /// language's own name ("Deutsch") or a country's name in its own
    /// language. VoiceOver reads the label in that language.
    public let labelLanguage: Locale.Language?
    /// Whether the option cannot be chosen.
    public let isDisabled: Bool

    /// An option.
    ///
    /// - Parameters:
    ///   - id: The value put in the selection.
    ///   - label: The visible label, already localised.
    ///   - description: Secondary text under the label.
    ///   - hint: Short trailing text, such as a code.
    ///   - group: A heading the option is listed under.
    ///   - keywords: Extra words that are searched but never shown.
    ///   - systemImage: A decorative SF Symbol before the label.
    ///   - leadingText: Decorative text before the label, such as a flag emoji.
    ///   - labelLanguage: The language the label is written in, when it is not the app's.
    ///   - isDisabled: Whether the option cannot be chosen.
    public init(
        id: String,
        label: String,
        description: String? = nil,
        hint: String? = nil,
        group: String? = nil,
        keywords: String? = nil,
        systemImage: String? = nil,
        leadingText: String? = nil,
        labelLanguage: Locale.Language? = nil,
        isDisabled: Bool = false
    ) {
        self.id = id
        self.label = label
        self.description = description
        self.hint = hint
        self.group = group
        self.keywords = keywords
        self.systemImage = systemImage
        self.leadingText = leadingText
        self.labelLanguage = labelLanguage
        self.isDisabled = isDisabled
    }
}
