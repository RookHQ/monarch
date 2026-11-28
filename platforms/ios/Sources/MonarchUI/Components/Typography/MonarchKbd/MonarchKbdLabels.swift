/// Replacements for the built-in strings of ``MonarchKbd``: the names
/// VoiceOver reads for keys that are written as glyphs. A property that is
/// `nil` keeps the built-in English string.
public struct MonarchKbdLabels: Sendable, Hashable {
    /// The name read for ⌘. Built in: "Command".
    public var command: String?
    /// The name read for ⇧. Built in: "Shift".
    public var shift: String?
    /// The name read for ⌥. Built in: "Option".
    public var option: String?
    /// The name read for ⌃. Built in: "Control".
    public var control: String?
    /// The name read for ↩ and ⏎. Built in: "Return".
    public var `return`: String?
    /// The name read for ⎋. Built in: "Escape".
    public var escape: String?
    /// The name read for ⌫. Built in: "Delete".
    public var delete: String?
    /// The name read for ⇥. Built in: "Tab".
    public var tab: String?
    /// The name read for ␣. Built in: "Space".
    public var space: String?
    /// The name read for ⇪. Built in: "Caps Lock".
    public var capsLock: String?
    /// The name read for ↑. Built in: "Up Arrow".
    public var upArrow: String?
    /// The name read for ↓. Built in: "Down Arrow".
    public var downArrow: String?
    /// The name read for ←. Built in: "Left Arrow".
    public var leftArrow: String?
    /// The name read for →. Built in: "Right Arrow".
    public var rightArrow: String?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        command: String? = nil,
        shift: String? = nil,
        option: String? = nil,
        control: String? = nil,
        return: String? = nil,
        escape: String? = nil,
        delete: String? = nil,
        tab: String? = nil,
        space: String? = nil,
        capsLock: String? = nil,
        upArrow: String? = nil,
        downArrow: String? = nil,
        leftArrow: String? = nil,
        rightArrow: String? = nil
    ) {
        self.command = command
        self.shift = shift
        self.option = option
        self.control = control
        self.return = `return`
        self.escape = escape
        self.delete = delete
        self.tab = tab
        self.space = space
        self.capsLock = capsLock
        self.upArrow = upArrow
        self.downArrow = downArrow
        self.leftArrow = leftArrow
        self.rightArrow = rightArrow
    }
}
