/// Replacements for the built-in strings of ``MonarchSchemaDrivenForm``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchSchemaDrivenFormLabels: Sendable {
    /// The submit button, and the Save button in the navigation bar with `isDirty`. Built in: "Save".
    public var submit: String?
    /// Shown when `onSubmit` throws an error without a description.
    /// Built in: "The form could not be saved. Try again."
    public var submitFailed: String?
    /// What VoiceOver reads for an unanswered question in the read-only view, which shows a dash.
    /// Built in: "Not set".
    public var empty: String?
    /// What VoiceOver reads for the progress shown in place of Save while saving. Built in: "Saving".
    public var saving: String?
    /// The Discard button in the navigation bar. Built in: "Discard".
    public var discard: String?
    /// The title of the discard confirmation. Built in: "Discard unsaved changes?"
    public var discardTitle: String?
    /// The message of the discard confirmation. Built in: "Your changes will be lost. This cannot be undone."
    public var discardMessage: String?
    /// The destructive button of the discard confirmation. Built in: "Discard changes".
    public var discardConfirm: String?
    /// The cancel button of the discard confirmation. Built in: "Keep editing".
    public var keepEditing: String?
    /// The strings of the questions: the validation messages, Yes and No, and the pickers.
    public var fields: MonarchFieldRendererLabels

    /// Replacements for the built-in strings. Every parameter is described on its property.
    public init(
        submit: String? = nil,
        submitFailed: String? = nil,
        empty: String? = nil,
        saving: String? = nil,
        discard: String? = nil,
        discardTitle: String? = nil,
        discardMessage: String? = nil,
        discardConfirm: String? = nil,
        keepEditing: String? = nil,
        fields: MonarchFieldRendererLabels = MonarchFieldRendererLabels()
    ) {
        self.submit = submit
        self.submitFailed = submitFailed
        self.empty = empty
        self.saving = saving
        self.discard = discard
        self.discardTitle = discardTitle
        self.discardMessage = discardMessage
        self.discardConfirm = discardConfirm
        self.keepEditing = keepEditing
        self.fields = fields
    }
}
