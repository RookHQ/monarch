import SwiftUI

/// A short reminder that AI output can be wrong, worded and styled for how
/// much a mistake would matter.
///
/// ```swift
/// MonarchAiDisclaimer()                     // "AI can make mistakes. Check important info."
/// MonarchAiDisclaimer(risk: .high)          // a warning note for answers people act on
/// MonarchAiDisclaimer("Answers come from your team's documents only.", risk: .medium)
/// ```
///
/// `low` is a quiet centred caption with the AI glyph, for under a composer.
/// `medium` is a leading-aligned line with an information icon. `high` is a
/// warning ``MonarchInlineNotice``.
///
/// Accessibility: the disclaimer is static text read as one element. The
/// low and medium icons are decorative; the high risk is read with the
/// "Warning" tone name first, so the risk does not depend on colour. The text
/// wraps at every text size.
public struct MonarchAiDisclaimer: View {
    private let text: String?
    private let risk: MonarchAiDisclaimerRisk

    @Environment(\.monarchTheme) private var theme

    /// A disclaimer.
    ///
    /// - Parameters:
    ///   - text: Replaces the built-in wording of the risk. Already localised.
    ///   - risk: How much harm a wrong answer could do here.
    public init(_ text: String? = nil, risk: MonarchAiDisclaimerRisk = .low) {
        self.text = text
        self.risk = risk
    }

    /// The content of the view.
    public var body: some View {
        let shown = MonarchAiDisclaimerTokens.text(text, risk: risk)
        if risk == .high {
            MonarchInlineNotice(shown, systemImage: MonarchAiDisclaimerTokens.highSymbol, tone: .warning)
        } else {
            line(shown)
        }
    }

    private func line(_ shown: String) -> some View {
        let appearance = MonarchAiDisclaimerTokens.appearance(risk)
        let isCentred = MonarchAiDisclaimerTokens.isCentred(risk)
        return HStack(alignment: .firstTextBaseline, spacing: MonarchAiDisclaimerTokens.iconSpacing) {
            Group {
                if risk == .low {
                    MonarchAiBotGlyph(size: .sm)
                } else {
                    MonarchIcon(systemName: MonarchAiDisclaimerTokens.mediumSymbol, size: .sm)
                }
            }
            .foregroundStyle(theme.colors[role: appearance.icon])
            Text(shown)
                .monarchTextStyle(
                    MonarchAiDisclaimerTokens.textStyle(risk),
                    weight: MonarchAiDisclaimerTokens.mediumWeight
                )
                .foregroundStyle(theme.colors[role: appearance.text])
                .multilineTextAlignment(isCentred ? .center : .leading)
                .fixedSize(horizontal: false, vertical: true)
        }
        .frame(maxWidth: .infinity, alignment: isCentred ? .center : .leading)
        .accessibilityElement(children: .combine)
    }
}

#if DEBUG
    #Preview("MonarchAiDisclaimer") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                ForEach(MonarchAiDisclaimerRisk.allCases, id: \.self) { risk in
                    MonarchAiDisclaimer(risk: risk)
                }
                MonarchAiDisclaimer("Answers come from your team's documents only.", risk: .medium)
            }
        }
    }
#endif
