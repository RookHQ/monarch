import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchCareersJobList``.
enum MonarchCareersJobListTokens {
    /// The gap between the header and the cards.
    static let spacing: CGFloat = MonarchTokens.Space.space5
    /// The gap between cards.
    static let cardSpacing: CGFloat = MonarchTokens.Space.space3
    /// The padding inside a card.
    static let cardPadding: CGFloat = MonarchTokens.Space.space5
    /// The gap between the lines of a card.
    static let lineSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between meta items and between tags.
    static let metaSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between sections of the details sheet.
    static let detailSpacing: CGFloat = MonarchTokens.Space.space6
    /// The gap between the meta items and the salary.
    static let metaLineSpacing: CGFloat = MonarchTokens.Space.space1
    /// The size of a list bullet relative to a small icon.
    static let bulletScale: CGFloat = 0.4
    /// The number of summary lines on a card.
    static let summaryLines = 2
    /// The symbol at the trailing edge of a card.
    static let chevronSymbol = "chevron.right"
    /// The weight of the department line.
    static let departmentWeight = MonarchTextStyleSpec.fontWeight(MonarchTokens.FontWeight.medium)
    /// The colour of supporting text.
    static var supportingColor: MonarchColorRole { \.textMuted }
    /// The colour of the department and the chevron.
    static var subtleColor: MonarchColorRole { \.textSubtle }

    /// The count beside the title: "1 position", "3 positions".
    static func count(_ count: Int) -> String {
        count == 1
            ? MonarchCareersJobListStrings.countOne : String(format: MonarchCareersJobListStrings.countOther, count)
    }

    /// The meta items of a card, in order: location, employment type, experience.
    static func meta(_ job: MonarchCareerJob) -> [String] {
        [job.location, job.employmentType, job.experience].compactMap { $0 }.filter { !$0.isEmpty }
    }

    /// What VoiceOver reads for a card: the title, the department, the meta items, the salary and the summary.
    static func accessibilityLabel(_ job: MonarchCareerJob) -> String {
        ([job.title, job.department] + meta(job).map(Optional.some) + [job.salary, job.summary])
            .compactMap { $0 }
            .filter { !$0.isEmpty }
            .joined(separator: ", ")
    }

    /// The number of columns: the requested number at regular width below the accessibility sizes, else one.
    static func columns(requested: Int, isRegularWidth: Bool, isAccessibilitySize: Bool) -> Int {
        isRegularWidth && !isAccessibilitySize ? min(max(requested, 1), 3) : 1
    }

    /// A mail link for an application to `email`, with the role in the subject.
    static func mailURL(email: String, job: MonarchCareerJob) -> URL? {
        var components = URLComponents()
        components.scheme = "mailto"
        components.path = email
        components.queryItems = [
            URLQueryItem(name: "subject", value: String(format: MonarchCareersJobListStrings.emailSubject, job.title))
        ]
        return components.url
    }
}
