import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchVideoThumbnail``.
enum MonarchVideoThumbnailTokens {
    /// The width divided by the height when none is given: 16 to 9.
    static let defaultAspectRatio: CGFloat = 16 / 9
    /// The diameter of the round play mark, before Dynamic Type.
    static let playSize: CGFloat = MonarchTokens.Space.space12
    /// The distance of the duration badge from the corner.
    static let badgeInset: CGFloat = MonarchTokens.Space.space2
    /// The SF Symbol of the play mark and of the placeholder.
    static let playSymbol = "play.fill"
    /// Where a frame is captured in a long video, in seconds.
    static let defaultCaptureTime: TimeInterval = 1
    /// The part of a short video's length at which a frame is captured.
    static let shortVideoCaptureRatio: Double = 0.1
    /// How far before the end the last frame that can be captured is, in seconds.
    static let captureEndMargin: TimeInterval = 0.05

    /// The fill behind the poster, which shows while there is no image and
    /// beside a letterboxed one.
    static var background: MonarchColorRole { \.bgInverse }
    /// The glyph of the placeholder.
    static var placeholderGlyph: MonarchColorRole { \.textInverseMuted }
    /// The disc of the play mark.
    static var playBackground: MonarchColorRole { \.bgSurface }
    /// The triangle of the play mark.
    static var playGlyph: MonarchColorRole { \.textDefault }

    /// The aspect ratio to draw: the given one when it is a positive number,
    /// otherwise 16 to 9.
    static func aspectRatio(_ ratio: CGFloat?) -> CGFloat {
        guard let ratio, ratio.isFinite, ratio > 0 else { return defaultAspectRatio }
        return ratio
    }

    /// The name of the video: the title without surrounding spaces, or the
    /// built-in "Video" when it is empty.
    static func name(title: String) -> String {
        let trimmed = title.trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmed.isEmpty ? MonarchVideoThumbnailStrings.fallbackTitle : trimmed
    }

    /// What VoiceOver reads as the label: "Play <name>" for a button (or the
    /// override), the name alone for an image.
    static func accessibilityLabel(title: String, isButton: Bool, playLabel: String?) -> String {
        let name = name(title: title)
        guard isButton else { return name }
        if let playLabel, !playLabel.isEmpty { return playLabel }
        return MonarchVideoThumbnailStrings.play(name)
    }

    /// Whole seconds of a length, never negative; `nil` when there is none.
    static func wholeSeconds(_ duration: TimeInterval?) -> Int64? {
        guard let duration, duration.isFinite else { return nil }
        return Int64(max(0, duration.rounded()))
    }

    /// The length as shown on the badge: `1:05`, or `1:01:05` from an hour.
    static func durationText(_ duration: TimeInterval?, locale: Locale) -> String? {
        guard let seconds = wholeSeconds(duration) else { return nil }
        let hour: Int64 = 3600
        let pattern: Duration.TimeFormatStyle.Pattern = seconds >= hour ? .hourMinuteSecond : .minuteSecond
        return Duration.seconds(seconds).formatted(.time(pattern: pattern).locale(locale))
    }

    /// The length as VoiceOver reads it: "1 minute, 5 seconds".
    static func spokenDuration(_ duration: TimeInterval?, locale: Locale) -> String? {
        guard let seconds = wholeSeconds(duration) else { return nil }
        let style = Duration.UnitsFormatStyle(
            allowedUnits: [.hours, .minutes, .seconds],
            width: .wide,
            zeroValueUnits: .hide
        )
        return Duration.seconds(seconds).formatted(style.locale(locale))
    }

    /// The second at which a frame is captured: the requested one, or one
    /// second in, or a tenth of the length for a video shorter than ten
    /// seconds. Always inside the video.
    static func captureTime(requested: TimeInterval?, duration: TimeInterval) -> TimeInterval {
        let length = duration.isFinite ? max(0, duration) : 0
        let wanted = requested ?? min(defaultCaptureTime, length * shortVideoCaptureRatio)
        guard wanted.isFinite else { return 0 }
        return max(0, min(wanted, max(0, length - captureEndMargin)))
    }
}
