import SwiftUI

/// The priority mark of an issue, as on the web: signal bars for high, medium and low (three, two or one lit), a red
/// square with an exclamation mark for urgent and a dash for none.
struct MonarchPriorityGlyph: View {
    let priority: MonarchIssueCardPriority
    let color: Color

    /// How many of the three bars are lit, or `nil` for urgent and none, which have their own symbols.
    static func litBars(_ priority: MonarchIssueCardPriority) -> Int? {
        switch priority {
        case .high: 3
        case .medium: 2
        case .low: 1
        case .urgent, .none: nil
        }
    }

    var body: some View {
        if let lit = Self.litBars(priority) {
            HStack(alignment: .bottom, spacing: 2) {
                ForEach(0..<3, id: \.self) { index in
                    RoundedRectangle(cornerRadius: 1)
                        .fill(color.opacity(index < lit ? 1 : 0.3))
                        .frame(width: 3, height: CGFloat(5 + index * 3))
                }
            }
            .frame(width: 16, height: 16)
        } else {
            MonarchIcon(systemName: MonarchIssueCardTokens.prioritySymbol(priority), size: .sm)
                .foregroundStyle(color)
        }
    }
}
