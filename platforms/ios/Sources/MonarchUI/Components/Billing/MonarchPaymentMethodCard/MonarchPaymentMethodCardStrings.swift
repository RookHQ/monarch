/// Built-in strings of ``MonarchPaymentMethodCard``, from the
/// `MonarchPaymentMethodCard` strings table.
enum MonarchPaymentMethodCardStrings {
    private static let table = "MonarchPaymentMethodCard"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.paymentMethodCard." + key, table: table)
    }

    /// The card networks with a built-in display name, by lower-case id.
    static let brandIDs: Set<String> = [
        "visa", "mastercard", "amex", "rupay", "discover", "diners", "jcb", "unionpay", "maestro",
    ]

    /// The display name of a known card network.
    static func brand(_ id: String) -> String { string("brand." + id) }
    /// "Visa ending in 4242".
    static func name(brand: String, last4: String) -> String {
        MonarchStrings.string("monarch.paymentMethodCard.name", table: table, brand, last4)
    }
    /// "Expires 08/27".
    static func expires(_ date: String) -> String {
        MonarchStrings.string("monarch.paymentMethodCard.expires", table: table, date)
    }
    /// "Expired 08/24".
    static func expiredOn(_ date: String) -> String {
        MonarchStrings.string("monarch.paymentMethodCard.expiredOn", table: table, date)
    }
    /// "Remove, Visa ending in 4242".
    static func actionLabel(action: String, name: String) -> String {
        MonarchStrings.string("monarch.paymentMethodCard.actionLabel", table: table, action, name)
    }
    /// "Remove Visa ending in 4242?".
    static func removeTitle(_ name: String) -> String {
        MonarchStrings.string("monarch.paymentMethodCard.removeTitle", table: table, name)
    }
    /// The badge of the default method.
    static var defaultBadge: String { string("defaultBadge") }
    /// The badge of a card that expires soon.
    static var expiringBadge: String { string("expiringBadge") }
    /// The badge of an expired card.
    static var expiredBadge: String { string("expiredBadge") }
    /// The title of the make-default button.
    static var makeDefault: String { string("makeDefault") }
    /// The title of the remove button.
    static var remove: String { string("remove") }
    /// The message of the removal confirmation.
    static var removeMessage: String { string("removeMessage") }
    /// The confirming button of the removal confirmation.
    static var removeConfirm: String { string("removeConfirm") }
    /// The cancelling button of the removal confirmation.
    static var cancel: String { string("cancel") }
    /// The failure text when an error has no description.
    static var failed: String { string("failed") }
}
