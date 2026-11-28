import SwiftUI

/// One line of the log: a single monospaced text, so long logs stay cheap
/// to lay out, tinted for warnings and errors.
struct MonarchLogViewerRow: View {
    let entry: MonarchLogViewerTokens.Entry
    let text: AttributedString
    let accessibilityText: String
    let isWrapped: Bool
    let minWidth: CGFloat

    @Environment(\.monarchTheme) private var theme

    var body: some View {
        Text(text)
            .monarchTextStyle(MonarchLogViewerTokens.lineTextStyle)
            .foregroundStyle(theme.colors.textDefault)
            .lineLimit(isWrapped ? nil : 1)
            .fixedSize(horizontal: !isWrapped, vertical: true)
            .multilineTextAlignment(.leading)
            .padding(.horizontal, MonarchLogViewerTokens.lineHorizontalPadding)
            .padding(.vertical, MonarchLogViewerTokens.lineVerticalPadding)
            .frame(minWidth: minWidth, maxWidth: isWrapped ? .infinity : nil, alignment: .leading)
            .background {
                if let role = MonarchLogViewerTokens.lineBackground(entry.line.level) {
                    theme.colors[role: role]
                }
            }
            .textSelection(.enabled)
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(accessibilityText)
    }
}
