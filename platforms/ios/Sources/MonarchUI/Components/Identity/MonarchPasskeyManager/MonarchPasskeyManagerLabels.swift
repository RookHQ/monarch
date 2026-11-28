/// Replacements for the built-in strings of ``MonarchPasskeyManager``.
///
/// Every property is `nil` by default, which keeps the built-in string.
public struct MonarchPasskeyManagerLabels: Sendable {
    /// The card title. Built in: "Passkeys".
    public var title: String?
    /// The line under the title. Built in: "Sign in with Face ID, Touch ID or your device passcode instead of a password. Passkeys can’t be phished or reused."
    public var description: String?
    /// The spoken name of the list. Built in: "Your passkeys".
    public var listLabel: String?
    /// The add button. Built in: "Add a passkey".
    public var add: String?
    /// The row button that renames. Built in: "Rename".
    public var rename: String?
    /// The spoken name of a row's rename button. Built in: "Rename <name>".
    public var renameLabel: (@Sendable (String) -> String)?
    /// The title of the rename prompt. Built in: "Rename passkey".
    public var renameTitle: String?
    /// The name field of the rename prompt. Built in: "Name".
    public var nameField: String?
    /// The save button of the rename prompt. Built in: "Save".
    public var save: String?
    /// The row button that removes. Built in: "Remove".
    public var remove: String?
    /// The spoken name of a row's remove button. Built in: "Remove <name>".
    public var removeLabel: (@Sendable (String) -> String)?
    /// Before the time it was added. Built in: "Added".
    public var created: String?
    /// Before the last-used time. Built in: "Last used".
    public var lastUsed: String?
    /// When it was never used. Built in: "Never used".
    public var neverUsed: String?
    /// The badge of a synced passkey. Built in: "Synced".
    public var synced: String?
    /// The removal confirmation title. Built in: "Remove <name>?".
    public var confirmTitle: (@Sendable (String) -> String)?
    /// The removal confirmation message. Built in: "You won’t be able to sign in with this passkey any more."
    public var confirmMessage: String?
    /// Added to the confirmation when removing the last passkey of a person who has no other way to sign in.
    public var lastMethodWarning: String?
    /// The confirm button. Built in: "Remove passkey".
    public var confirmRemove: String?
    /// The cancel buttons. Built in: "Cancel".
    public var cancel: String?
    /// Shown when this device cannot create passkeys.
    public var unsupported: String?
    /// Shown when the person cancels the system passkey sheet. Built in: "Passkey setup was cancelled. Nothing was changed."
    public var cancelled: String?
    /// Shown when adding fails without a message. Built in: "Couldn’t add the passkey. Try again."
    public var addFailed: String?
    /// Announced after adding. Built in: "Passkey added".
    public var added: String?
    /// Announced after removing. Built in: "Removed <name>".
    public var removed: (@Sendable (String) -> String)?
    /// Shown when renaming fails. Built in: "Couldn’t rename <name>. Try again."
    public var renameFailed: (@Sendable (String) -> String)?
    /// Shown when removing fails. Built in: "Couldn’t remove <name>. Try again."
    public var removeFailed: (@Sendable (String) -> String)?
    /// Read while loading. Built in: "Loading passkeys".
    public var loading: String?
    /// The empty state title. Built in: "No passkeys yet".
    public var emptyTitle: String?
    /// The empty state message. Built in: "Add one to sign in faster and more safely."
    public var emptyMessage: String?
    /// The error state title. Built in: "Couldn’t load your passkeys".
    public var errorTitle: String?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        title: String? = nil,
        description: String? = nil,
        listLabel: String? = nil,
        add: String? = nil,
        rename: String? = nil,
        renameLabel: (@Sendable (String) -> String)? = nil,
        renameTitle: String? = nil,
        nameField: String? = nil,
        save: String? = nil,
        remove: String? = nil,
        removeLabel: (@Sendable (String) -> String)? = nil,
        created: String? = nil,
        lastUsed: String? = nil,
        neverUsed: String? = nil,
        synced: String? = nil,
        confirmTitle: (@Sendable (String) -> String)? = nil,
        confirmMessage: String? = nil,
        lastMethodWarning: String? = nil,
        confirmRemove: String? = nil,
        cancel: String? = nil,
        unsupported: String? = nil,
        cancelled: String? = nil,
        addFailed: String? = nil,
        added: String? = nil,
        removed: (@Sendable (String) -> String)? = nil,
        renameFailed: (@Sendable (String) -> String)? = nil,
        removeFailed: (@Sendable (String) -> String)? = nil,
        loading: String? = nil,
        emptyTitle: String? = nil,
        emptyMessage: String? = nil,
        errorTitle: String? = nil
    ) {
        self.title = title
        self.description = description
        self.listLabel = listLabel
        self.add = add
        self.rename = rename
        self.renameLabel = renameLabel
        self.renameTitle = renameTitle
        self.nameField = nameField
        self.save = save
        self.remove = remove
        self.removeLabel = removeLabel
        self.created = created
        self.lastUsed = lastUsed
        self.neverUsed = neverUsed
        self.synced = synced
        self.confirmTitle = confirmTitle
        self.confirmMessage = confirmMessage
        self.lastMethodWarning = lastMethodWarning
        self.confirmRemove = confirmRemove
        self.cancel = cancel
        self.unsupported = unsupported
        self.cancelled = cancelled
        self.addFailed = addFailed
        self.added = added
        self.removed = removed
        self.renameFailed = renameFailed
        self.removeFailed = removeFailed
        self.loading = loading
        self.emptyTitle = emptyTitle
        self.emptyMessage = emptyMessage
        self.errorTitle = errorTitle
    }
}
