/// Replacements for the built-in strings of ``MonarchPaymentMethodCard``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchPaymentMethodCardLabels: Sendable {
    /// The card's name. Receives the network name and the last four digits.
    /// Built in: "Visa ending in 4242".
    public var name: (@Sendable (String, String) -> String)?
    /// The expiry line. Receives the date as MM/YY. Built in: "Expires 08/27".
    public var expires: (@Sendable (String) -> String)?
    /// The expiry line of an expired card. Receives the date as MM/YY.
    /// Built in: "Expired 08/24".
    public var expiredOn: (@Sendable (String) -> String)?
    /// The badge of the default method. Built in: "Default".
    public var defaultBadge: String?
    /// The badge of a card that expires soon. Built in: "Expires soon".
    public var expiringBadge: String?
    /// The badge of an expired card. Built in: "Expired".
    public var expiredBadge: String?
    /// The make-default button. Built in: "Make default".
    public var makeDefault: String?
    /// The remove button. Built in: "Remove".
    public var remove: String?
    /// The spoken name of an action button. Receives the button title and
    /// the card name. Built in: "Remove, Visa ending in 4242".
    public var actionLabel: (@Sendable (String, String) -> String)?
    /// The title of the removal confirmation. Receives the card name.
    /// Built in: "Remove Visa ending in 4242?".
    public var removeTitle: (@Sendable (String) -> String)?
    /// The message of the removal confirmation. Built in: "It will no longer
    /// be charged. To use it again, add it as a new payment method."
    public var removeMessage: String?
    /// The confirming button. Built in: "Remove card".
    public var removeConfirm: String?
    /// The cancelling button. Built in: "Cancel".
    public var cancel: String?
    /// Shown when an action throws an error without a description. Built in:
    /// "That did not work. Try again."
    public var failed: String?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        name: (@Sendable (String, String) -> String)? = nil,
        expires: (@Sendable (String) -> String)? = nil,
        expiredOn: (@Sendable (String) -> String)? = nil,
        defaultBadge: String? = nil,
        expiringBadge: String? = nil,
        expiredBadge: String? = nil,
        makeDefault: String? = nil,
        remove: String? = nil,
        actionLabel: (@Sendable (String, String) -> String)? = nil,
        removeTitle: (@Sendable (String) -> String)? = nil,
        removeMessage: String? = nil,
        removeConfirm: String? = nil,
        cancel: String? = nil,
        failed: String? = nil
    ) {
        self.name = name
        self.expires = expires
        self.expiredOn = expiredOn
        self.defaultBadge = defaultBadge
        self.expiringBadge = expiringBadge
        self.expiredBadge = expiredBadge
        self.makeDefault = makeDefault
        self.remove = remove
        self.actionLabel = actionLabel
        self.removeTitle = removeTitle
        self.removeMessage = removeMessage
        self.removeConfirm = removeConfirm
        self.cancel = cancel
        self.failed = failed
    }
}
