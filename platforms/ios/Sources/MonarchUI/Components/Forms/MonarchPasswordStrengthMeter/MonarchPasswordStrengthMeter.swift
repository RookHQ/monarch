import SwiftUI

/// Shows how strong a password is while it is typed: a four-part bar, the
/// strength in words, and each requirement as met or not met.
///
/// ```swift
/// MonarchPasswordInput("New password", text: $password, purpose: .new)
/// MonarchPasswordStrengthMeter(password: password)
/// ```
///
/// The password is only read. Nothing is stored or sent.
///
/// Accessibility: the bar is decorative. The strength line is read as text
/// ("Password strength: Good") and is announced when the strength changes.
/// Each rule is one element read as "Met: …" or "Not met: …", and met and not
/// met differ by icon as well as colour. The list has the name "Password
/// requirements". Text scales with Dynamic Type.
public struct MonarchPasswordStrengthMeter: View {
    private let password: String
    private let rules: [MonarchPasswordRule]
    private let showsRules: Bool
    private let strengthText: ((MonarchPasswordStrength?) -> String)?
    private let rulesLabel: String?
    private let ruleText: ((String, Bool) -> String)?
    private let score: ((String) -> MonarchPasswordStrength)?

    @Environment(\.monarchTheme) private var theme

    /// A password strength meter.
    ///
    /// - Parameters:
    ///   - password: The password to rate.
    ///   - rules: The requirements listed under the bar. An empty list hides the list.
    ///   - showsRules: Whether to list the requirements.
    ///   - strengthText: Replaces the built-in strength line. It receives the strength, or `nil` for an
    ///     empty password.
    ///   - rulesLabel: Replaces the built-in "Password requirements" spoken name of the list.
    ///   - ruleText: Replaces the built-in spoken text of a rule. It receives the rule's label and whether
    ///     it is met.
    ///   - score: A custom strength estimator. By default ``MonarchPasswordStrength/score(_:rules:)``.
    public init(
        password: String,
        rules: [MonarchPasswordRule] = MonarchPasswordRule.defaults,
        showsRules: Bool = true,
        strengthText: ((MonarchPasswordStrength?) -> String)? = nil,
        rulesLabel: String? = nil,
        ruleText: ((String, Bool) -> String)? = nil,
        score: ((String) -> MonarchPasswordStrength)? = nil
    ) {
        self.password = password
        self.rules = rules
        self.showsRules = showsRules
        self.strengthText = strengthText
        self.rulesLabel = rulesLabel
        self.ruleText = ruleText
        self.score = score
    }

    /// The content of the view.
    public var body: some View {
        let strength = MonarchPasswordStrengthMeterTokens.strength(password: password, rules: rules, score: score)
        let line = strengthText?(strength) ?? MonarchPasswordStrengthMeterTokens.strengthText(strength)
        VStack(alignment: .leading, spacing: MonarchPasswordStrengthMeterTokens.stackSpacing) {
            bar(strength)
            Text(line)
                .monarchTextStyle(.caption)
                .foregroundStyle(theme.colors.textMuted)
            if showsRules, !rules.isEmpty {
                ruleList
            }
        }
        .onChange(of: line) { _, newLine in
            AccessibilityNotification.Announcement(newLine).post()
        }
    }

    private func bar(_ strength: MonarchPasswordStrength?) -> some View {
        HStack(spacing: MonarchPasswordStrengthMeterTokens.segmentSpacing) {
            ForEach(1...MonarchPasswordStrengthMeterTokens.segmentCount, id: \.self) { segment in
                let role = MonarchPasswordStrengthMeterTokens.segmentFill(segment: segment, strength: strength)
                Capsule()
                    .fill(theme.colors[role: role])
                    .frame(height: MonarchPasswordStrengthMeterTokens.barHeight)
            }
        }
        .monarchAnimation(.base, value: strength)
        .accessibilityHidden(true)
    }

    private var ruleList: some View {
        VStack(alignment: .leading, spacing: MonarchPasswordStrengthMeterTokens.ruleSpacing) {
            ForEach(rules) { rule in
                let isMet = MonarchPasswordStrengthMeterTokens.isMet(rule, password: password)
                HStack(alignment: .firstTextBaseline, spacing: MonarchPasswordStrengthMeterTokens.ruleIconSpacing) {
                    if isMet {
                        MonarchIcon(systemName: MonarchPasswordStrengthMeterTokens.ruleSymbol(isMet: true), size: .sm)
                    } else {
                        // The web draws a small dot for a rule not met yet, not a ring.
                        Circle().frame(width: 5, height: 5)
                            .frame(width: 14, height: 14)
                    }
                    Text(rule.label).monarchTextStyle(.caption)
                }
                .foregroundStyle(theme.colors[role: MonarchPasswordStrengthMeterTokens.ruleForeground(isMet: isMet)])
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(
                    ruleText?(rule.label, isMet)
                        ?? MonarchPasswordStrengthMeterTokens.ruleText(rule.label, isMet: isMet)
                )
            }
        }
        .accessibilityElement(children: .contain)
        .accessibilityLabel(rulesLabel ?? MonarchPasswordStrengthMeterStrings.rulesLabel)
    }
}

#if DEBUG
    #Preview("MonarchPasswordStrengthMeter") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchPasswordStrengthMeter(password: "")
                MonarchPasswordStrengthMeter(password: "abc")
                MonarchPasswordStrengthMeter(password: "Abcdefg1")
                MonarchPasswordStrengthMeter(password: "Abcdefg1!", showsRules: false)
                MonarchPasswordStrengthMeter(password: "Correct horse 9 battery")
            }
        }
    }
#endif
