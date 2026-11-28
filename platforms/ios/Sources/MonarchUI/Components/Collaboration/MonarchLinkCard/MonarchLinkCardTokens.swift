import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and pure logic for ``MonarchLinkCard``.
enum MonarchLinkCardTokens {
    /// The narrowest picture ratio, width to height: near square.
    static let minRatio: CGFloat = 1.2
    /// The widest picture ratio: a wide banner, also used when the size is unknown.
    static let maxRatio: CGFloat = 2.2
    /// The side of the thumbnail of a small card, before Dynamic Type.
    static let thumbnailSide: CGFloat = MonarchTokens.Space.space16
    /// The gap between the picture and the text.
    static let mediaSpacing: CGFloat = MonarchTokens.Space.space3
    /// The inset of the text under a picture that runs to the card's edges.
    static let bleedTextInset: CGFloat = MonarchTokens.Space.space3
    /// The gap between the lines of text.
    static let textSpacing: CGFloat = MonarchTokens.Space.space1
    /// The gap between the favicon and the site name.
    static let siteSpacing: CGFloat = MonarchTokens.Space.space1_5
    /// The number of lines of the title.
    static let titleLines = 2
    /// The number of lines of the description.
    static let descriptionLines = 2
    /// The side of the play mark on a video picture, before Dynamic Type.
    static let playSide: CGFloat = MonarchTokens.Space.space10
    /// The SF Symbol of the play mark.
    static let playSymbol = "play.fill"
    /// The SF Symbol of the remove button.
    static let dismissSymbol = "xmark"
    /// The URL schemes that are opened. Anything else is shown as plain text.
    static let safeSchemes: Set<String> = ["http", "https", "mailto"]

    /// The ratio a picture is shown at: its own, kept between near square
    /// and a wide banner, so one odd picture never dominates.
    static func imageRatio(_ size: CGSize?) -> CGFloat {
        guard let size, size.width > 0, size.height > 0 else { return maxRatio }
        return min(maxRatio, max(minRatio, size.width / size.height))
    }

    /// Whether the link is opened: only http, https and mailto.
    static func isSafeLink(_ url: URL) -> Bool {
        guard let scheme = url.scheme?.lowercased() else { return false }
        return safeSchemes.contains(scheme)
    }

    /// The site line: the given site name, or the host name of the link.
    static func siteName(_ siteName: String?, url: URL) -> String {
        if let siteName, !siteName.isEmpty { return siteName }
        return MonarchFaviconTokens.hostname(url.absoluteString)
    }

    /// What VoiceOver reads for the card: the site, the title, the
    /// description, then "Video" for a video.
    static func accessibilityLabel(
        site: String,
        title: String,
        description: String?,
        isVideo: Bool,
        videoLabel: String
    ) -> String {
        var parts = [site, title]
        if let description, !description.isEmpty { parts.append(description) }
        if isVideo { parts.append(videoLabel) }
        return parts.filter { !$0.isEmpty }.joined(separator: ", ")
    }

    /// The room kept after the text for the remove button in the corner.
    /// With a picture the button sits on the picture instead.
    static func textTrailingInset(hasDismiss: Bool, hasImage: Bool) -> CGFloat {
        guard hasDismiss, !hasImage else { return 0 }
        return MonarchAccessibility.minimumHitTarget - MonarchCardTokens.padding
    }
}
