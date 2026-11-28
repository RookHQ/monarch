import MonarchTokens
import SwiftUI

/// Component tokens and logic for ``MonarchPasswordStrengthMeter``.
enum MonarchPasswordStrengthMeterTokens {
    /// The number of segments in the bar.
    static let segmentCount = 4
    /// The height of the bar.
    static let barHeight: CGFloat = MonarchTokens.Space.space1
    /// The gap between segments.
    static let segmentSpacing: CGFloat = MonarchTokens.Space.space1
    /// The gap between the bar, the strength line and the rules.
    static let stackSpacing: CGFloat = MonarchTokens.Space.space1_5
    /// The gap between rules.
    static let ruleSpacing: CGFloat = MonarchTokens.Space.space1
    /// The gap between the icon and the text of a rule.
    static let ruleIconSpacing: CGFloat = MonarchTokens.Space.space1_5
    /// The length the default length rule asks for, in Unicode scalars, as on the web.
    static let minimumLength = 8
    /// The length that lifts a password that meets every rule from good to strong.
    static let strongLength = 14
    /// The share of rules met that makes a password fair.
    static let fairRatio = 0.75
    /// The share of rules met that makes a password weak rather than very weak.
    static let weakRatio = 0.5

    /// Whether a scalar is a letter or a decimal digit in any script.
    static func isLetterOrDigit(_ scalar: Unicode.Scalar) -> Bool {
        switch scalar.properties.generalCategory {
        case .uppercaseLetter, .lowercaseLetter, .titlecaseLetter, .modifierLetter, .otherLetter, .decimalNumber:
            true
        default:
            false
        }
    }

    /// The strength to show, or `nil` while the password is empty.
    static func strength(
        password: String,
        rules: [MonarchPasswordRule],
        score: ((String) -> MonarchPasswordStrength)?
    ) -> MonarchPasswordStrength? {
        guard !password.isEmpty else { return nil }
        return score?(password) ?? MonarchPasswordStrength.score(password, rules: rules)
    }

    /// Whether a segment, counted from 1, is filled. A password that is not
    /// empty always fills the first segment.
    static func isFilled(segment: Int, strength: MonarchPasswordStrength?) -> Bool {
        guard let strength else { return false }
        return segment <= max(1, strength.rawValue)
    }

    /// The role of a segment.
    static func segmentFill(segment: Int, strength: MonarchPasswordStrength?) -> MonarchColorRole {
        guard let strength, isFilled(segment: segment, strength: strength) else { return \.bgEmphasis }
        switch strength {
        case .veryWeak, .weak: return \.dangerSolid
        case .fair: return \.warningSolid
        case .good, .strong: return \.successSolid
        }
    }

    /// The strength line, such as "Password strength: Good".
    static func strengthText(_ strength: MonarchPasswordStrength?) -> String {
        MonarchPasswordStrengthMeterStrings.strength(
            strength.map(MonarchPasswordStrengthMeterStrings.level) ?? MonarchPasswordStrengthMeterStrings.empty
        )
    }

    /// Whether a rule is met. No rule is met by an empty password.
    static func isMet(_ rule: MonarchPasswordRule, password: String) -> Bool {
        !password.isEmpty && rule.test(password)
    }

    /// The role of a rule's icon and text.
    static func ruleForeground(isMet: Bool) -> MonarchColorRole {
        isMet ? \.successStrong : \.textMuted
    }

    /// The SF Symbol of a rule, so met and not met differ by shape.
    static func ruleSymbol(isMet: Bool) -> String {
        isMet ? "checkmark" : "circle.fill"
    }

    /// What VoiceOver reads for a rule, such as "Met: A number".
    static func ruleText(_ label: String, isMet: Bool) -> String {
        isMet ? MonarchPasswordStrengthMeterStrings.met(label) : MonarchPasswordStrengthMeterStrings.unmet(label)
    }
}
