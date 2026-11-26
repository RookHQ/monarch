import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchCallTile``.
enum MonarchCallTileTokens {
    /// The width-to-height ratio of a tile: 16 to 9.
    static let aspectRatio: CGFloat = 16.0 / 9.0
    /// How long a reaction floats, in seconds.
    static let reactionDuration: TimeInterval = 3
    /// The corner radius of the tile.
    static let cornerRadius: CGFloat = MonarchTokens.Radius.lg
    /// The width of the speaking border.
    static let speakingBorderWidth: CGFloat = MonarchTokens.Space.space0_5
    /// The width of the ring around the avatar of a speaking person who has no video.
    static let speakingRingWidth: CGFloat = MonarchTokens.Space.space1
    /// The gap between items in the name bar.
    static let barSpacing: CGFloat = MonarchTokens.Space.space1
    /// The height of the tallest quality bar.
    static let qualityHeight: CGFloat = MonarchTokens.Space.space3
    /// The width of one quality bar.
    static let qualityBarWidth: CGFloat = MonarchTokens.Space.space0_5
    /// How far a reaction floats up.
    static let reactionRise: CGFloat = MonarchTokens.Space.space8
    /// The SF Symbols of the tile.
    static let mutedSymbol = "mic.slash.fill"
    static let screenSymbol = "rectangle.on.rectangle"
    static let handSymbol = "hand.raised.fill"
    static let pinSymbol = "pin"
    static let unpinSymbol = "pin.slash"
    static let fullScreenSymbol = "arrow.up.left.and.arrow.down.right"

    /// The padding inside the tile.
    static func padding(_ size: MonarchCallTileSize) -> CGFloat {
        switch size {
        case .sm: MonarchTokens.Space.space1_5
        case .md: MonarchTokens.Space.space2
        case .lg: MonarchTokens.Space.space3
        }
    }

    /// The size of the avatar without video.
    static func avatarSize(_ size: MonarchCallTileSize) -> MonarchAvatarSize {
        size == .sm ? .md : .lg
    }

    /// The type role of the name bar.
    static func barStyle(_ size: MonarchCallTileSize) -> MonarchTextStyle {
        size == .sm ? .caption : .label
    }

    /// The colours of the tile.
    static var background: MonarchColorRole { \.bgEmphasis }
    static var barBackground: MonarchColorRole { \.bgInverse }
    static var barForeground: MonarchColorRole { \.textInverse }
    static var speakingBorder: MonarchColorRole { \.successSolid }
    static var speakingRing: MonarchColorRole { \.successBorder }
    static var qualityOn: MonarchColorRole { \.textInverse }
    static var qualityOff: MonarchColorRole { \.textInverseMuted }

    /// The colour of the status text in the name bar, on the inverse background.
    static func statusColor(_ tone: MonarchTone) -> MonarchColorRole {
        switch tone {
        case .warning: \.warningStrongInverse
        case .danger: \.dangerStrongInverse
        case .success: \.successStrongInverse
        case .info: \.infoStrongInverse
        case .neutral: \.textInverseMuted
        case .accent: \.accentStrongInverse
        case .ai: \.aiStrongInverse
        }
    }

    /// The number of lit bars.
    static func bars(_ quality: MonarchCallQuality) -> Int {
        switch quality {
        case .good: 3
        case .fair: 2
        case .poor: 1
        }
    }

    /// The name shown and read: "Ana (you)" for your own tile.
    static func displayName(_ name: String, isSelf: Bool, labels: MonarchCallTileLabels) -> String {
        guard isSelf else { return name }
        return labels.you?(name) ?? MonarchCallTileStrings.you(name)
    }

    /// Whether the pin and full-screen buttons are shown.
    static func showsActions(_ size: MonarchCallTileSize, hasPin: Bool, hasFullScreen: Bool) -> Bool {
        size != .sm && (hasPin || hasFullScreen)
    }

    /// Whether the video is mirrored: your own camera, never a shared screen.
    static func mirrors(isSelf: Bool, isScreen: Bool) -> Bool {
        isSelf && !isScreen
    }

    /// Whether a reaction is floating at `date`: from when it was sent for
    /// three seconds. A reaction from the future is not shown.
    static func isReactionVisible(_ reaction: MonarchCallReaction?, at date: Date) -> Bool {
        guard let reaction else { return false }
        let age = date.timeIntervalSince(reaction.sentAt)
        return age >= 0 && age < reactionDuration
    }

    /// The moments at which a reaction appears and disappears, for a
    /// `TimelineView` schedule.
    static func reactionSchedule(_ reaction: MonarchCallReaction?) -> [Date] {
        guard let reaction else { return [] }
        return [reaction.sentAt, reaction.sentAt.addingTimeInterval(reactionDuration)]
    }

    /// What VoiceOver reads after the name: speaking, muted, hand raised,
    /// sharing screen, the status and the connection quality.
    static func accessibilityValue(
        isSpeaking: Bool,
        isMicOff: Bool,
        isHandRaised: Bool,
        isScreen: Bool,
        status: String?,
        quality: MonarchCallQuality?,
        labels: MonarchCallTileLabels
    ) -> String {
        var parts: [String] = []
        if isSpeaking { parts.append(labels.speaking ?? MonarchCallTileStrings.speaking) }
        if isMicOff { parts.append(labels.muted ?? MonarchCallTileStrings.muted) }
        if isHandRaised { parts.append(labels.handRaised ?? MonarchCallTileStrings.handRaised) }
        if isScreen { parts.append(labels.sharingScreen ?? MonarchCallTileStrings.sharingScreen) }
        if let status, !status.isEmpty { parts.append(status) }
        if let quality { parts.append(qualityLabel(quality, labels: labels)) }
        return parts.joined(separator: ", ")
    }

    /// The spoken connection quality.
    static func qualityLabel(_ quality: MonarchCallQuality, labels: MonarchCallTileLabels) -> String {
        labels.quality?(quality) ?? MonarchCallTileStrings.quality(quality)
    }

    /// The announcement of a reaction.
    static func reactedAnnouncement(_ name: String, _ emoji: String, labels: MonarchCallTileLabels) -> String {
        labels.reacted?(name, emoji) ?? MonarchCallTileStrings.reacted(name, emoji)
    }

    /// The height of a tile: 16 by 9 at its width, or taller when its
    /// content needs more room, as at the largest text sizes.
    static func height(width: CGFloat, contentHeight: CGFloat) -> CGFloat {
        max(width / aspectRatio, contentHeight)
    }
}
