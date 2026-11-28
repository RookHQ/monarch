import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchImpersonationBanner``.
enum MonarchImpersonationBannerTokens {
    /// Padding before and after the content.
    static let horizontalPadding: CGFloat = MonarchTokens.Space.space4
    /// Padding above and below the content.
    static let verticalPadding: CGFloat = MonarchTokens.Space.space2
    /// The gap between the icon, the text and the button.
    static let spacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between the lines of text.
    static let textSpacing: CGFloat = MonarchTokens.Space.space0_5
    /// The height of the warning edge under the banner.
    static let edgeHeight: CGFloat = MonarchTokens.Space.space0_5 + MonarchTokens.Space.px
    /// The SF Symbol before the text.
    static let symbol = "eye.fill"
    /// The SF Symbol before an error.
    static let errorSymbol = "exclamationmark.circle.fill"

    /// The colour roles. Inverse colours make the banner unlike every other
    /// banner; the warning edge and icon mark it as a risk.
    struct Appearance: Equatable {
        let background: MonarchColorRole
        let text: MonarchColorRole
        let muted: MonarchColorRole
        let edge: MonarchColorRole
        let icon: MonarchColorRole
    }

    /// The colour roles of the banner.
    static var appearance: Appearance {
        Appearance(
            background: \.bgInverse,
            text: \.textInverse,
            muted: \.textInverseMuted,
            edge: \.warningSolid,
            icon: \.warningSolid
        )
    }

    /// The main line split around the name, so the name can be bold. `nil`
    /// when the app replaced the line, which is then shown as it is.
    static func titleParts(
        name: String,
        labels: MonarchImpersonationBannerLabels
    ) -> (prefix: String, suffix: String)? {
        guard labels.viewingAs == nil else { return nil }
        let format = MonarchImpersonationBannerStrings.viewingAsFormat
        guard let range = format.range(of: "%@") else { return nil }
        return (String(format[..<range.lowerBound]), String(format[range.upperBound...]))
    }

    /// The main line as plain text, which is also what is announced.
    static func titleText(name: String, labels: MonarchImpersonationBannerLabels) -> String {
        labels.viewingAs?(name) ?? MonarchImpersonationBannerStrings.viewingAs(name)
    }

    /// The spoken name of the stop button.
    static func stopLabel(name: String, labels: MonarchImpersonationBannerLabels) -> String {
        labels.stopLabel?(name) ?? MonarchImpersonationBannerStrings.stopLabel(name)
    }

    /// The reason line: "Reason: Ticket 4821", or `nil` without a reason.
    static func reasonText(_ reason: String?, labels: MonarchImpersonationBannerLabels) -> String? {
        guard let reason, !reason.isEmpty else { return nil }
        return "\(labels.reason ?? MonarchImpersonationBannerStrings.reason) \(reason)"
    }

    /// The text of a failed stop: the error's description, or the fallback.
    static func errorMessage(_ error: any Error, labels: MonarchImpersonationBannerLabels) -> String {
        MonarchFailureMessage.message(
            for: error,
            fallback: labels.stopError ?? MonarchImpersonationBannerStrings.stopError
        )
    }
}
