import MonarchTokens
import SwiftUI

/// A small pill in the AI accent, as the web draws the agent kind, a capability or a status of an agent: the AI
/// tint as fill, the AI border as outline and the strong AI colour as text.
struct MonarchAiTag: View {
    let text: String
    var systemImage: String?
    var style: MonarchTextStyle = .caption
    var isSmall = false

    @Environment(\.monarchTheme) private var theme

    init(_ text: String, systemImage: String? = nil, style: MonarchTextStyle = .caption, isSmall: Bool = false) {
        self.text = text
        self.systemImage = systemImage
        self.style = style
        self.isSmall = isSmall
    }

    var body: some View {
        HStack(spacing: MonarchTokens.Space.space1) {
            if let systemImage {
                MonarchIcon(systemName: systemImage, size: .sm).imageScale(.small)
            }
            Text(text).monarchTextStyle(style, weight: .medium)
        }
        .foregroundStyle(theme.colors.aiStrong)
        .padding(.horizontal, isSmall ? MonarchTokens.Space.space1_5 : MonarchTokens.Space.space2)
        .padding(.vertical, isSmall ? 0 : MonarchTokens.Space.space0_5)
        .background(Capsule().fill(theme.colors.aiSubtle))
        .overlay(Capsule().strokeBorder(theme.colors.aiBorder, lineWidth: 1))
        .accessibilityElement(children: .combine)
    }
}
