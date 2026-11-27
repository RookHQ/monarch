/// Replacements for the built-in strings of ``MonarchCareersJobList``. Each `nil` keeps the built-in English text.
public struct MonarchCareersJobListLabels: Sendable, Hashable {
    /// The list title. Built-in: "Open roles".
    public var title: String?
    /// The text shown when there are no roles. Built-in: "No open roles right now. Check back soon."
    public var empty: String?
    /// The hint of a role card. Built-in: "View details".
    public var viewDetails: String?
    /// The responsibilities heading. Built-in: "What you’ll do".
    public var responsibilities: String?
    /// The qualifications heading. Built-in: "What we’re looking for".
    public var qualifications: String?
    /// The must-have skills heading. Built-in: "Must-have skills".
    public var mustHave: String?
    /// The nice-to-have skills heading. Built-in: "Nice to have".
    public var niceToHave: String?
    /// The benefits heading. Built-in: "Benefits".
    public var benefits: String?
    /// The apply button. Built-in: "Apply now".
    public var apply: String?
    /// The apply button when the role has only an email. Built-in: "Apply by email".
    public var applyByEmail: String?
    /// The email link beside the apply button. Built-in: "Email instead".
    public var emailInstead: String?
    /// The close button of the details sheet. Built-in: "Close".
    public var close: String?
    /// The line above the apply actions, with a `%@` for the employer. Built-in: "Applying to %@".
    public var applyingTo: String?

    /// Replacement strings.
    ///
    /// - Parameters:
    ///   - title: The list title.
    ///   - empty: The text shown when there are no roles.
    ///   - viewDetails: The hint of a role card.
    ///   - responsibilities: The responsibilities heading.
    ///   - qualifications: The qualifications heading.
    ///   - mustHave: The must-have skills heading.
    ///   - niceToHave: The nice-to-have skills heading.
    ///   - benefits: The benefits heading.
    ///   - apply: The apply button.
    ///   - applyByEmail: The apply button when the role has only an email.
    ///   - emailInstead: The email link beside the apply button.
    ///   - close: The close button of the details sheet.
    ///   - applyingTo: The line above the apply actions, with a `%@` for the employer.
    public init(
        title: String? = nil,
        empty: String? = nil,
        viewDetails: String? = nil,
        responsibilities: String? = nil,
        qualifications: String? = nil,
        mustHave: String? = nil,
        niceToHave: String? = nil,
        benefits: String? = nil,
        apply: String? = nil,
        applyByEmail: String? = nil,
        emailInstead: String? = nil,
        close: String? = nil,
        applyingTo: String? = nil
    ) {
        self.title = title
        self.empty = empty
        self.viewDetails = viewDetails
        self.responsibilities = responsibilities
        self.qualifications = qualifications
        self.mustHave = mustHave
        self.niceToHave = niceToHave
        self.benefits = benefits
        self.apply = apply
        self.applyByEmail = applyByEmail
        self.emailInstead = emailInstead
        self.close = close
        self.applyingTo = applyingTo
    }
}
