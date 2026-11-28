/// Built-in strings of the careers job list, from the `MonarchCareersJobList` strings table.
enum MonarchCareersJobListStrings {
    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.careersJobList.\(key)", table: "MonarchCareersJobList")
    }

    /// The list title.
    static var title: String { string("title") }
    /// The count of one role.
    static var countOne: String { string("countOne") }
    /// The count of several roles, with a `%d` for the number.
    static var countOther: String { string("countOther") }
    /// The text shown when there are no roles.
    static var empty: String { string("empty") }
    /// The hint of a role card.
    static var viewDetails: String { string("viewDetails") }
    /// The responsibilities heading.
    static var responsibilities: String { string("responsibilities") }
    /// The qualifications heading.
    static var qualifications: String { string("qualifications") }
    /// The must-have skills heading.
    static var mustHave: String { string("mustHave") }
    /// The nice-to-have skills heading.
    static var niceToHave: String { string("niceToHave") }
    /// The benefits heading.
    static var benefits: String { string("benefits") }
    /// The apply button.
    static var apply: String { string("apply") }
    /// The apply button when the role has only an email.
    static var applyByEmail: String { string("applyByEmail") }
    /// The email link beside the apply button.
    static var emailInstead: String { string("emailInstead") }
    /// The close button of the details sheet.
    static var close: String { string("close") }
    /// The line above the apply actions, with a `%@` for the employer.
    static var applyingTo: String { string("applyingTo") }
    /// The subject of an application email, with a `%@` for the role.
    static var emailSubject: String { string("emailSubject") }
}
