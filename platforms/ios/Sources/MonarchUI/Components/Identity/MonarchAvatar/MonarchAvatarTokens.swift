import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchAvatar`` and ``MonarchAvatarGroup``.
enum MonarchAvatarTokens {
    /// The fill of an avatar inside a skeleton: the muted placeholder colour.
    static var placeholderFill: MonarchColorRole { \.bgMuted }

    /// The side of an avatar.
    static func side(_ size: MonarchAvatarSize) -> CGFloat {
        switch size {
        case .xs: MonarchTokens.Space.space5
        case .sm: MonarchTokens.Space.space6
        case .md: MonarchTokens.Space.space8
        case .lg: MonarchTokens.Space.space10
        case .xl: MonarchTokens.Space.space12 + MonarchTokens.Space.space2
        }
    }

    /// The corner radius of the square shape.
    static func squareRadius(_ size: MonarchAvatarSize) -> CGFloat {
        switch size {
        case .xs, .sm: MonarchTokens.Radius.sm
        case .md, .lg: MonarchTokens.Radius.md
        case .xl: MonarchTokens.Radius.lg
        }
    }

    /// The type role of the initials.
    static func initialsStyle(_ size: MonarchAvatarSize) -> MonarchTextStyle {
        switch size {
        case .xs, .sm: .caption
        case .md: .label
        case .lg: .bodyStrong
        case .xl: .subheading
        }
    }

    /// The size of the fallback symbol.
    static func iconSize(_ size: MonarchAvatarSize) -> MonarchIconSize {
        switch size {
        case .xs, .sm: .sm
        case .md: .md
        case .lg, .xl: .lg
        }
    }

    /// The size of the presence dot.
    static func dotSize(_ size: MonarchAvatarSize) -> MonarchPresenceDotSize {
        switch size {
        case .xs, .sm: .sm
        case .md: .md
        case .lg, .xl: .lg
        }
    }

    /// How far the initials may shrink to fit at large text sizes.
    static let initialsMinimumScale: CGFloat = 0.5
    /// The SF Symbol shown when there is no image and no initials.
    static let personSymbol = "person.fill"
    /// The ring in the surface colour around each avatar of a group.
    static let ringWidth: CGFloat = MonarchTokens.Space.space0_5
    /// How much of an avatar the next one in a group covers (a quarter, like the web).
    static let overlapRatio: CGFloat = 0.25

    /// How much of an avatar the next one covers at `size`. A 20 or 24 pt
    /// avatar is mostly initials, so the next one covers only an eighth and
    /// the letters stay whole; larger avatars keep the quarter.
    static func overlapRatio(_ size: MonarchAvatarSize) -> CGFloat {
        switch size {
        case .xs, .sm: 0.125
        default: overlapRatio
        }
    }

    /// How far the presence dot moves out over the corner, as a share of its
    /// own diameter. On a 20 or 24 pt avatar the dot would sit on the initials.
    static func dotOutset(_ size: MonarchAvatarSize) -> CGFloat {
        switch size {
        case .xs, .sm: 0.4
        default: 0
        }
    }

    /// The number of fallback colour pairs.
    static let paletteCount = 5

    /// The colour roles of a fallback.
    struct Fallback: Equatable {
        let background: MonarchColorRole
        let foreground: MonarchColorRole
    }

    /// The fallback colour pair at an index: a subtle fill with the strong
    /// text of the same family. Theme roles, so the colours follow dark and
    /// high-contrast themes. Danger is left out so no face reads as an error.
    static func fallback(index: Int) -> Fallback {
        switch index % paletteCount {
        case 0: Fallback(background: \.accentSubtle, foreground: \.accentStrong)
        case 1: Fallback(background: \.infoSubtle, foreground: \.infoStrong)
        case 2: Fallback(background: \.successSubtle, foreground: \.successStrong)
        case 3: Fallback(background: \.warningSubtle, foreground: \.warningStrong)
        default: Fallback(background: \.aiSubtle, foreground: \.aiStrong)
        }
    }

    /// The index of the colour pair for a seed. The same seed gives the same
    /// colour on every platform: the web's hash, modulo 360, then modulo 5.
    static func paletteIndex(seed: String) -> Int {
        var hash = 0
        for unit in seed.utf16 {
            hash = (hash * 31 + Int(unit)) % 360
        }
        return hash % paletteCount
    }

    /// The hue, 0 to 359, of a seed: the web's hash, modulo 360. The fallback colour is derived from it, so a person
    /// has the same colour on every platform.
    static func hue(seed: String) -> Int {
        var hash = 0
        for unit in seed.utf16 {
            hash = (hash * 31 + Int(unit)) % 360
        }
        return hash
    }

    /// The fallback fill and text of a seed, as the web draws them: a soft fill and a strong text of the same hue
    /// (light: 45% 92% and 55% 28%; dark: 30% 22% and 60% 85%, as HSL).
    static func hueFallback(seed: String, isDark: Bool) -> (background: Color, foreground: Color) {
        let hue = Double(hue(seed: seed))
        if isDark {
            return (hsl(hue, 0.30, 0.22), hsl(hue, 0.60, 0.85))
        }
        return (hsl(hue, 0.45, 0.92), hsl(hue, 0.55, 0.28))
    }

    /// A colour from hue in degrees, saturation and lightness in 0 to 1.
    static func hsl(_ hue: Double, _ saturation: Double, _ lightness: Double) -> Color {
        let brightness = lightness + saturation * min(lightness, 1 - lightness)
        let sat = brightness == 0 ? 0 : 2 * (1 - lightness / brightness)
        return Color(hue: hue / 360, saturation: sat, brightness: brightness)
    }

    /// The initials of a name: the first character of the first and last
    /// words, upper-cased. Whole characters are kept, so an emoji or an
    /// accented letter is never cut in half; a word in brackets such as "(you)" is left out. Empty when the name has no words.
    static func initials(_ name: String) -> String {
        let all = name.split(whereSeparator: \.isWhitespace)
        // A note in brackets, such as "(you)", is not part of the name.
        let named = all.filter { $0.first != "(" && $0.first != "[" }
        let words = named.isEmpty ? all : named
        guard let first = words.first?.first else { return "" }
        var result = String(first)
        if words.count > 1, let last = words.last?.first {
            result.append(last)
        }
        return result.uppercased()
    }

    /// Whether the initials can be shown: they contain a letter or a digit.
    static func hasReadableInitials(_ initials: String) -> Bool {
        initials.unicodeScalars.contains { CharacterSet.alphanumerics.contains($0) }
    }

    /// What VoiceOver reads: the name, and the presence when there is one.
    static func accessibilityLabel(name: String, status: MonarchPresenceStatus?, statusLabel: String?) -> String {
        guard let status else { return name }
        let spoken = statusLabel.flatMap { $0.isEmpty ? nil : $0 } ?? MonarchPresenceDot.label(for: status)
        return MonarchAvatarStrings.withStatus(name: name, status: spoken)
    }

    /// How many avatars a group shows. A "+1" tile would take the space of
    /// the avatar it hides, so that avatar is shown instead. The rule is
    /// shared with every overflowing row (`Internal/MonarchOverflow`).
    static func visibleCount(total: Int, maxVisible: Int?) -> Int {
        MonarchOverflow.visibleCount(total: total, maxVisible: maxVisible)
    }

    /// The spoken name of a group without a label: "3 people".
    static func groupLabel(_ label: String?, count: Int) -> String {
        if let label, !label.isEmpty { return label }
        return MonarchAvatarStrings.groupSummary(count)
    }
}
