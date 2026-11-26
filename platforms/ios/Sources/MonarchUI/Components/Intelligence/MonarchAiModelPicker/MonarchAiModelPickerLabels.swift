/// Replacements for the built-in strings of ``MonarchAiModelPicker``.
///
/// Every property is `nil` by default, which keeps the built-in string.
public struct MonarchAiModelPickerLabels: Sendable {
    /// Names the button and the list. Built in: "Model".
    public var label: String?
    /// The button's text when no model is selected. Built in: "Choose a model".
    public var placeholder: String?
    /// The name of a capability. Built in: "Vision", "Tools", "Reasoning", "Web".
    public var capability: (@Sendable (MonarchAiModelCapability) -> String)?
    /// The context window from the compact number. Built in: "200K context".
    public var context: (@Sendable (String) -> String)?
    /// The name of a speed. Built in: "Fast", "Balanced", "Slower".
    public var speed: (@Sendable (MonarchAiModelSpeed) -> String)?
    /// Read for a cost tier from 1 to 3. Built in: "Cost: low", "Cost: medium", "Cost: high".
    public var cost: (@Sendable (Int) -> String)?
    /// The badge of a model on a configured workspace key. Built in: "Your key".
    public var ownKey: String?
    /// The badge of a model whose key is missing. Built in: "Key needed".
    public var keyNeeded: String?
    /// The hint of a model that leads to adding a key. Built in: "Select to add an API key."
    public var addKeyHint: String?
    /// Read on a model that cannot be picked. Built in: "Unavailable".
    public var unavailable: String?
    /// The button that closes the list. Built in: "Done".
    public var done: String?

    /// Replacements for built-in strings. Pass only the ones to replace.
    ///
    /// - Parameters:
    ///   - label: Names the button and the list.
    ///   - placeholder: The button's text without a selection.
    ///   - capability: The name of a capability.
    ///   - context: The context window from the compact number.
    ///   - speed: The name of a speed.
    ///   - cost: Read for a cost tier.
    ///   - ownKey: The badge of a model on a configured key.
    ///   - keyNeeded: The badge of a model whose key is missing.
    ///   - addKeyHint: The hint of a model that leads to adding a key.
    ///   - unavailable: Read on a model that cannot be picked.
    ///   - done: The button that closes the list.
    public init(
        label: String? = nil,
        placeholder: String? = nil,
        capability: (@Sendable (MonarchAiModelCapability) -> String)? = nil,
        context: (@Sendable (String) -> String)? = nil,
        speed: (@Sendable (MonarchAiModelSpeed) -> String)? = nil,
        cost: (@Sendable (Int) -> String)? = nil,
        ownKey: String? = nil,
        keyNeeded: String? = nil,
        addKeyHint: String? = nil,
        unavailable: String? = nil,
        done: String? = nil
    ) {
        self.label = label
        self.placeholder = placeholder
        self.capability = capability
        self.context = context
        self.speed = speed
        self.cost = cost
        self.ownKey = ownKey
        self.keyNeeded = keyNeeded
        self.addKeyHint = addKeyHint
        self.unavailable = unavailable
        self.done = done
    }
}
