/// What a piece of AI context is. Sets the icon and the spoken prefix, such as "File:".
public enum MonarchAiContextKind: String, Sendable, Hashable, Codable, CaseIterable {
    /// An attached or referenced file.
    case file
    /// Text the person selected.
    case selection
    /// The current page or screen.
    case page
    /// A record, such as a customer or an invoice.
    case record
    /// A connected app.
    case app
    /// A person.
    case person
}
