/// Replacements for the built-in strings of ``MonarchApiKeyRow``.
///
/// Every property is `nil` by default, which keeps the built-in string.
/// Strings that name something take `%@` where it goes.
public struct MonarchApiKeyRowLabels: Sendable, Hashable {
    /// The spoken name of the prefix, with `%@` for it. Built in: "Key starting with %@".
    public var prefix: String?
    /// With `%@` for a relative time. Built in: "Created: %@".
    public var created: String?
    /// With `%@` for a relative time. Built in: "Last used: %@".
    public var lastUsed: String?
    /// Built in: "Never used".
    public var neverUsed: String?
    /// With `%@` for a relative time. Built in: "Expires: %@".
    public var expires: String?
    /// The spoken name of the scopes. Built in: "Scopes".
    public var scopes: String?
    /// Built in: "No scopes".
    public var noScopes: String?
    /// Replaces the status name in the badge.
    public var status: String?
    /// Built in: "Rotate".
    public var rotate: String?
    /// Built in: "Revoke".
    public var revoke: String?
    /// The spoken name of the actions menu, with `%@` for the key's name. Built in: "Actions for %@".
    public var actions: String?
    /// With `%@` for the key's name. Built in: "Rotate %@?".
    public var rotateTitle: String?
    /// Built in: "A new key is issued and shown once. Update every place
    /// that uses this key before the old one stops working."
    public var rotateMessage: String?
    /// Built in: "Rotate key".
    public var rotateConfirm: String?
    /// With `%@` for the key's name. Built in: "Revoke %@?".
    public var revokeTitle: String?
    /// Built in: "Requests that use this key will be rejected. This cannot be
    /// undone. Apps using this key stop working right away."
    public var revokeMessage: String?
    /// Built in: "Revoke key".
    public var revokeConfirm: String?
    /// The prompt of the field that asks for the confirmation phrase, with
    /// `%@` for the phrase. Built in: "Type %@ to confirm".
    public var revokePhrasePrompt: String?
    /// Built in: "Cancel".
    public var cancel: String?
    /// Built in: "Rotating…".
    public var rotating: String?
    /// Built in: "Revoking…".
    public var revoking: String?
    /// Shown when a rotation fails without a message. Built in: "Couldn't rotate the key."
    public var rotateFailed: String?
    /// Shown when a revocation fails without a message. Built in: "Couldn't revoke the key."
    public var revokeFailed: String?

    /// Replacements for built-in strings. Pass only the ones to replace.
    public init(
        prefix: String? = nil,
        created: String? = nil,
        lastUsed: String? = nil,
        neverUsed: String? = nil,
        expires: String? = nil,
        scopes: String? = nil,
        noScopes: String? = nil,
        status: String? = nil,
        rotate: String? = nil,
        revoke: String? = nil,
        actions: String? = nil,
        rotateTitle: String? = nil,
        rotateMessage: String? = nil,
        rotateConfirm: String? = nil,
        revokeTitle: String? = nil,
        revokeMessage: String? = nil,
        revokeConfirm: String? = nil,
        revokePhrasePrompt: String? = nil,
        cancel: String? = nil,
        rotating: String? = nil,
        revoking: String? = nil,
        rotateFailed: String? = nil,
        revokeFailed: String? = nil
    ) {
        self.prefix = prefix
        self.created = created
        self.lastUsed = lastUsed
        self.neverUsed = neverUsed
        self.expires = expires
        self.scopes = scopes
        self.noScopes = noScopes
        self.status = status
        self.rotate = rotate
        self.revoke = revoke
        self.actions = actions
        self.rotateTitle = rotateTitle
        self.rotateMessage = rotateMessage
        self.rotateConfirm = rotateConfirm
        self.revokeTitle = revokeTitle
        self.revokeMessage = revokeMessage
        self.revokeConfirm = revokeConfirm
        self.revokePhrasePrompt = revokePhrasePrompt
        self.cancel = cancel
        self.rotating = rotating
        self.revoking = revoking
        self.rotateFailed = rotateFailed
        self.revokeFailed = revokeFailed
    }
}
