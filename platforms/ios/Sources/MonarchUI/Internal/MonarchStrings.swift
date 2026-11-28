import Foundation

/// Looks up Monarch's built-in strings in the package's localised resources.
///
/// Each component owns one strings table, `Resources/en.lproj/<TypeName>.strings`,
/// so adding a component never edits a shared file. Keys start with `monarch.`
/// followed by the component name in lower camel case. A component wraps its
/// keys in a small enum beside the component, for example `MonarchButtonStrings`.
enum MonarchStrings {
    /// The string for a key in a component's table.
    ///
    /// - Parameters:
    ///   - key: The key, for example `monarch.button.loading`.
    ///   - table: The table name, which is the component's type name.
    /// - Returns: The localised string, or the key if it is missing.
    static func string(_ key: String, table: String) -> String {
        Bundle.module.localizedString(forKey: key, value: nil, table: table)
    }

    /// A string with `%@` placeholders filled in.
    static func string(_ key: String, table: String, _ arguments: any CVarArg...) -> String {
        String(format: string(key, table: table), arguments: arguments)
    }

    /// The table of plural strings, `Resources/en.lproj/Localizable.stringsdict`.
    static let pluralTable = "Localizable"

    /// A plural string from the plurals table, with its counts filled in by
    /// the locale's plural rules ("1 item", "2 items").
    ///
    /// - Parameters:
    ///   - key: The key in `Localizable.stringsdict`, such as `monarch.loadMore.count`.
    ///   - counts: The numbers of the format, in order.
    ///   - locale: The locale of the plural rules and digits.
    /// - Returns: The string with the counts filled in.
    static func plural(_ key: String, _ counts: Int..., locale: Locale = .autoupdatingCurrent) -> String {
        let format = Bundle.module.localizedString(forKey: key, value: nil, table: pluralTable)
        return String(format: format, locale: locale, arguments: counts.map { Int64($0) as any CVarArg })
    }
}
