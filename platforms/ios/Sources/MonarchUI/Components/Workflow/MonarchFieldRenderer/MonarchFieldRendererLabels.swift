/// Replacements for the built-in strings of ``MonarchFieldRenderer``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchFieldRendererLabels: Sendable {
    /// A required question without an answer. Built in: "This field is required."
    public var required: String?
    /// A required consent that is not ticked. Built in: "Tick this to continue."
    public var mustAgree: String?
    /// Text that is not an email address. Built in: "Enter an email address, like name@example.com."
    public var email: String?
    /// Text that is not a web address. Built in: "Enter a web address, like https://example.com."
    public var url: String?
    /// An answer that is not a number. Built in: "Enter a number."
    public var number: String?
    /// A number below the minimum, from the minimum formatted for the locale. Built in: "Enter 1 or more."
    public var min: (@Sendable (String) -> String)?
    /// A number above the maximum, from the maximum formatted for the locale. Built in: "Enter 10 or less."
    public var max: (@Sendable (String) -> String)?
    /// Text shorter than the minimum. Built in: "Use at least 3 characters."
    public var minLength: (@Sendable (Int) -> String)?
    /// Text longer than the maximum. Built in: "Use 80 characters or fewer."
    public var maxLength: (@Sendable (Int) -> String)?
    /// Fewer options than the minimum. Built in: "Choose at least 2."
    public var minSelections: (@Sendable (Int) -> String)?
    /// More options than the maximum. Built in: "Choose 3 or fewer."
    public var maxSelections: (@Sendable (Int) -> String)?
    /// The yes choice of a yes-or-no question. Built in: "Yes".
    public var yes: String?
    /// The no choice of a yes-or-no question. Built in: "No".
    public var no: String?
    /// The empty choice of a menu and the prompt of a multi-select. Built in: "Choose…".
    public var choose: String?
    /// The button of a date question without an answer. Built in: "Choose a date".
    public var chooseDate: String?
    /// The button of a time question without an answer. Built in: "Choose a time".
    public var chooseTime: String?
    /// The button that clears an optional date or time. Built in: "Clear".
    public var clear: String?
    /// The note on an answer filled in automatically with enough confidence.
    /// Built in: "Filled in automatically. Check it is right."
    public var aiFilled: String?
    /// The note on an answer filled in automatically with low confidence.
    /// Built in: "Filled in automatically, but unsure. Please check."
    public var aiCheck: String?
    /// The visible title of a file's remove button. Built in: "Remove".
    public var remove: String?
    /// The spoken name of a file's remove button, from the file name. Built in: "Remove cv.pdf".
    public var removeFile: (@Sendable (String) -> String)?

    /// Replacements for built-in strings. Every parameter is optional; each is described on its property.
    public init(
        required: String? = nil,
        mustAgree: String? = nil,
        email: String? = nil,
        url: String? = nil,
        number: String? = nil,
        min: (@Sendable (String) -> String)? = nil,
        max: (@Sendable (String) -> String)? = nil,
        minLength: (@Sendable (Int) -> String)? = nil,
        maxLength: (@Sendable (Int) -> String)? = nil,
        minSelections: (@Sendable (Int) -> String)? = nil,
        maxSelections: (@Sendable (Int) -> String)? = nil,
        yes: String? = nil,
        no: String? = nil,
        choose: String? = nil,
        chooseDate: String? = nil,
        chooseTime: String? = nil,
        clear: String? = nil,
        aiFilled: String? = nil,
        aiCheck: String? = nil,
        remove: String? = nil,
        removeFile: (@Sendable (String) -> String)? = nil
    ) {
        self.required = required
        self.mustAgree = mustAgree
        self.email = email
        self.url = url
        self.number = number
        self.min = min
        self.max = max
        self.minLength = minLength
        self.maxLength = maxLength
        self.minSelections = minSelections
        self.maxSelections = maxSelections
        self.yes = yes
        self.no = no
        self.choose = choose
        self.chooseDate = chooseDate
        self.chooseTime = chooseTime
        self.clear = clear
        self.aiFilled = aiFilled
        self.aiCheck = aiCheck
        self.remove = remove
        self.removeFile = removeFile
    }
}
