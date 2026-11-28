import Foundation
import UniformTypeIdentifiers

/// One question of a form, as data, rendered by ``MonarchFieldRenderer``.
///
/// ```swift
/// let question = MonarchFormFieldDefinition(id: "email", type: .email, label: "Email", isRequired: true)
/// let problem = question.problem(for: answers["email"])
/// ```
public struct MonarchFormFieldDefinition: Identifiable, Hashable, Sendable {
    /// A stable, unique id: the key of the answer.
    public var id: String
    /// The kind of question.
    public var type: MonarchFormFieldType
    /// The visible label, or the heading of a ``MonarchFormFieldType/section``.
    public var label: String
    /// Help under the label.
    public var description: String?
    /// The placeholder of a text field, or of a menu while nothing is chosen.
    public var placeholder: String?
    /// Whether an answer is needed: a mark after the label, and ``problem(for:)`` reports a missing one.
    public var isRequired: Bool
    /// The choices of select, multi-select, radio and checkboxes questions.
    public var options: [MonarchFormFieldOption]
    /// The fewest characters of a text answer.
    public var minLength: Int?
    /// The most characters of a text answer; typing stops there.
    public var maxLength: Int?
    /// The smallest number, or the first value of a rating (1 by default).
    public var min: Double?
    /// The largest number, or the last value of a rating (5 by default).
    public var max: Double?
    /// The earliest day that can be picked.
    public var minDate: Date?
    /// The latest day that can be picked.
    public var maxDate: Date?
    /// The fewest options of a checkboxes or multi-select question.
    public var minSelections: Int?
    /// The most options of a checkboxes or multi-select question.
    public var maxSelections: Int?
    /// The visible lines of a long-text question. Longer text scrolls inside the field.
    public var rows: Int
    /// The accepted file types of a file question, matched by conformance.
    public var allowedContentTypes: [UTType]
    /// The largest file in bytes.
    public var maxFileSize: Int64?
    /// The most files.
    public var maxFiles: Int
    /// The statement a consent question asks people to agree to; the label by default.
    public var consentText: String?
    /// The question is shown only when these hold, tested against other answers by question id.
    public var conditions: [MonarchCondition]
    /// Whether all ``conditions`` or at least one must hold.
    public var conditionMatch: MonarchConditionMatch
    /// Shown but not editable, such as a verified email address. Text and number questions stay readable
    /// and selectable; other questions are disabled.
    public var isReadOnly: Bool
    /// Whether a yes-or-no question is drawn as a switch (the question and its help at the leading edge) instead of
    /// a Yes and a No radio. Set it with ``asSwitch(_:)``.
    private(set) var showsAsSwitch = false

    /// The question drawn as a switch when it is a yes-or-no question, as a form of settings does; off is a
    /// saved answer of no.
    ///
    /// - Parameter isSwitch: Whether to draw a switch.
    /// - Returns: A copy of the question.
    public func asSwitch(_ isSwitch: Bool = true) -> MonarchFormFieldDefinition {
        var copy = self
        copy.showsAsSwitch = isSwitch
        return copy
    }

    /// A question. Every parameter is described on its property.
    public init(
        id: String,
        type: MonarchFormFieldType,
        label: String,
        description: String? = nil,
        placeholder: String? = nil,
        isRequired: Bool = false,
        options: [MonarchFormFieldOption] = [],
        minLength: Int? = nil,
        maxLength: Int? = nil,
        min: Double? = nil,
        max: Double? = nil,
        minDate: Date? = nil,
        maxDate: Date? = nil,
        minSelections: Int? = nil,
        maxSelections: Int? = nil,
        rows: Int = 4,
        allowedContentTypes: [UTType] = [.item],
        maxFileSize: Int64? = nil,
        maxFiles: Int = 1,
        consentText: String? = nil,
        conditions: [MonarchCondition] = [],
        conditionMatch: MonarchConditionMatch = .all,
        isReadOnly: Bool = false
    ) {
        self.id = id
        self.type = type
        self.label = label
        self.description = description
        self.placeholder = placeholder
        self.isRequired = isRequired
        self.options = options
        self.minLength = minLength
        self.maxLength = maxLength
        self.min = min
        self.max = max
        self.minDate = minDate
        self.maxDate = maxDate
        self.minSelections = minSelections
        self.maxSelections = maxSelections
        self.rows = rows
        self.allowedContentTypes = allowedContentTypes
        self.maxFileSize = maxFileSize
        self.maxFiles = maxFiles
        self.consentText = consentText
        self.conditions = conditions
        self.conditionMatch = conditionMatch
        self.isReadOnly = isReadOnly
    }

    /// Checks an answer against this question: required, the email, web address and number formats, the
    /// limits and the selections, in the web's order.
    ///
    /// - Parameter value: The answer, or `nil` for none.
    /// - Returns: The first problem, or `nil` when the answer is valid.
    public func problem(for value: MonarchFormFieldValue?) -> MonarchFormFieldProblem? {
        MonarchFieldRendererTokens.problem(self, value)
    }

    /// Whether this question is shown, given every answer keyed by question id: its ``conditions`` must hold,
    /// as ``MonarchCondition/matches(_:calendar:)`` tests them. A list answer (options or files) equals or
    /// contains a value when one of its items is that value. A question without conditions is always shown.
    ///
    /// - Parameters:
    ///   - values: Every answer, keyed by question id. A missing answer is no answer.
    ///   - calendar: The calendar that decides what a day is.
    /// - Returns: Whether the question is shown.
    public func isVisible(in values: [String: MonarchFormFieldValue], calendar: Calendar = .autoupdatingCurrent) -> Bool
    {
        MonarchFieldRendererTokens.isVisible(self, values: values, calendar: calendar)
    }
}
