import Foundation

/// One open role of a ``MonarchCareersJobList``.
public struct MonarchCareerJob: Sendable, Hashable, Identifiable {
    /// A stable id.
    public let id: String
    /// The role's title.
    public let title: String
    /// The team, shown above the title.
    public let department: String?
    /// Where the role is based, such as "Remote" or "Hybrid · Berlin".
    public let location: String?
    /// Such as "Full time".
    public let employmentType: String?
    /// Such as "3+ years".
    public let experience: String?
    /// The salary range as written, such as "USD 90,000 – 120,000". Never formatted.
    public let salary: String?
    /// One or two sentences about the role.
    public let summary: String?
    /// What the person will do.
    public let responsibilities: [String]
    /// What the team is looking for.
    public let qualifications: [String]
    /// Skills the role needs, shown as tags.
    public let mustHaveSkills: [String]
    /// Skills that help.
    public let niceToHaveSkills: [String]
    /// Benefits of the role.
    public let benefits: [String]
    /// Where to apply. Without it, and with a contact email, people apply by email.
    public let applyURL: URL?

    /// An open role.
    ///
    /// - Parameters:
    ///   - id: A stable id.
    ///   - title: The role's title, already localised.
    ///   - department: The team, shown above the title.
    ///   - location: Where the role is based.
    ///   - employmentType: Such as "Full time".
    ///   - experience: Such as "3+ years".
    ///   - salary: The salary range as written.
    ///   - summary: One or two sentences about the role.
    ///   - responsibilities: What the person will do.
    ///   - qualifications: What the team is looking for.
    ///   - mustHaveSkills: Skills the role needs.
    ///   - niceToHaveSkills: Skills that help.
    ///   - benefits: Benefits of the role.
    ///   - applyURL: Where to apply.
    public init(
        _ id: String,
        title: String,
        department: String? = nil,
        location: String? = nil,
        employmentType: String? = nil,
        experience: String? = nil,
        salary: String? = nil,
        summary: String? = nil,
        responsibilities: [String] = [],
        qualifications: [String] = [],
        mustHaveSkills: [String] = [],
        niceToHaveSkills: [String] = [],
        benefits: [String] = [],
        applyURL: URL? = nil
    ) {
        self.id = id
        self.title = title
        self.department = department
        self.location = location
        self.employmentType = employmentType
        self.experience = experience
        self.salary = salary
        self.summary = summary
        self.responsibilities = responsibilities
        self.qualifications = qualifications
        self.mustHaveSkills = mustHaveSkills
        self.niceToHaveSkills = niceToHaveSkills
        self.benefits = benefits
        self.applyURL = applyURL
    }
}
