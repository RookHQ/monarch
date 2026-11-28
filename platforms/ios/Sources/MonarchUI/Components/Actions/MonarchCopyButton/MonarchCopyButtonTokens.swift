/// Component tokens for ``MonarchCopyButton``.
enum MonarchCopyButtonTokens {
    /// The SF Symbol shown before a copy.
    static let copySymbol = "doc.on.doc"
    /// The SF Symbol shown after a copy.
    static let copiedSymbol = "checkmark"

    /// The symbol for a state. A custom symbol replaces only the idle one.
    static func symbol(isCopied: Bool, custom: String?) -> String {
        isCopied ? copiedSymbol : (custom ?? copySymbol)
    }

    /// The spoken name: the visible title, then what is copied.
    static func accessibilityLabel(title: String, target: String?) -> String {
        guard let target, !target.isEmpty else { return title }
        return "\(title) \(target)"
    }

    /// Whether the copied state ends by itself: only after a copy, and only
    /// with a duration above zero.
    static func resets(copyCount: Int, after duration: Duration) -> Bool {
        copyCount > 0 && duration > .zero
    }
}
