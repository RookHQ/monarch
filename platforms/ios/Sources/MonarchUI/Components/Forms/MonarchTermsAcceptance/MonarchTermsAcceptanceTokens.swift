import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchTermsAcceptance``.
enum MonarchTermsAcceptanceTokens {
    /// The gap between the checkbox, the links and the error.
    static let stackSpacing: CGFloat = MonarchTokens.Space.space1
    /// The gap between document links.
    static let linkSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between the error icon and its text.
    static let iconSpacing: CGFloat = MonarchTokens.Space.space1
    /// How far the links and the error are indented, so they line up with the label and not the box:
    /// the checkbox size plus its label gap.
    static let indent: CGFloat = MonarchTokens.Space.space5 + MonarchTokens.Space.space2
    /// The SF Symbol before the error.
    static let errorSymbol = "exclamationmark.circle"

    /// The document titles joined in the locale's list format, such as "A, B and C".
    static func joinedTitles(_ documents: [MonarchTermsDocument], locale: Locale) -> String {
        documents.map(\.title).formatted(.list(type: .and).locale(locale))
    }

    /// The checkbox label: the app's builder or the built-in sentence around the joined titles.
    static func label(
        _ documents: [MonarchTermsDocument],
        locale: Locale,
        builder: (@Sendable (String) -> String)?
    ) -> String {
        let titles = joinedTitles(documents, locale: locale)
        return builder?(titles) ?? MonarchTermsAcceptanceStrings.label(titles)
    }

    /// Whether the required error is shown: the app asked for it, the box is required and not ticked.
    static func showsError(isRequired: Bool, isAccepted: Bool, showsRequiredError: Bool) -> Bool {
        showsRequiredError && isRequired && !isAccepted
    }

    /// The colour of the document links.
    static func linkRole(isEnabled: Bool) -> MonarchColorRole {
        isEnabled ? \.textLink : \.textDisabled
    }

    /// The colour of the required error.
    static var errorRole: MonarchColorRole { \.dangerStrong }
}
