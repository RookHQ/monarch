import SwiftUI

/// One bar of a team calendar: a run of days of one entry, one VoiceOver
/// element and one touch target as wide as its days. A tap calls the app, or
/// without an app action shows the entry's details in a popover.
struct MonarchTeamCalendarBar: View {
    let segment: MonarchTeamCalendarSegment
    let cellSize: CGFloat
    /// What VoiceOver reads for the bar.
    let spokenLabel: String
    /// The second line of the details: person, days, half and pending.
    let detail: String
    let action: (@MainActor () -> Void)?

    @State private var isShowingDetail = false
    @Environment(\.monarchTheme) private var theme

    private typealias Tokens = MonarchTeamCalendarTokens

    var body: some View {
        Button {
            if let action {
                action()
            } else {
                isShowingDetail = true
            }
        } label: {
            placed
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(spokenLabel)
        .popover(isPresented: $isShowingDetail) {
            MonarchTeamCalendarDetail(title: segment.entry.label, detail: detail)
        }
    }

    /// The bar within its day, as on the web: a small square in the middle of the day, half as wide
    /// for a half day, at the leading or trailing side.
    @ViewBuilder private var placed: some View {
        // A day narrowed to fit the phone keeps a gap between neighbouring bars.
        let side = min(Tokens.barSide, max(Tokens.barHalfInset * 4, cellSize - Tokens.barHalfInset * 4))
        switch segment.portion {
        case .full:
            bar(width: side)
        case .firstHalf:
            bar(width: side / 2)
                .padding(.leading, Tokens.barHalfInset)
                .frame(maxWidth: .infinity, alignment: .leading)
        case .secondHalf:
            bar(width: side / 2)
                .padding(.trailing, Tokens.barHalfInset)
                .frame(maxWidth: .infinity, alignment: .trailing)
        }
    }

    private func bar(width: CGFloat) -> some View {
        let shape = RoundedRectangle(cornerRadius: Tokens.barRadius, style: .continuous)
        let color = segment.entry.color ?? theme.colors[role: Tokens.defaultBarColor]
        return
            shape
            .fill(color)
            .opacity(segment.entry.isPending ? Tokens.pendingOpacity : 1)
            .overlay {
                if segment.entry.isPending {
                    shape.strokeBorder(theme.colors[role: Tokens.pendingBorder], lineWidth: MonarchShapes.borderWidth)
                }
            }
            .frame(width: width, height: Tokens.barSide)
    }
}

/// The details of a bar or a holiday, shown in a popover when it is tapped:
/// the phone's stand-in for the web's hover title.
struct MonarchTeamCalendarDetail: View {
    let title: String
    let detail: String?

    @Environment(\.monarchTheme) private var theme

    private typealias Tokens = MonarchTeamCalendarTokens

    var body: some View {
        VStack(alignment: .leading, spacing: Tokens.detailSpacing) {
            Text(title)
                .monarchTextStyle(.bodyStrong)
                .foregroundStyle(theme.colors[role: Tokens.primaryText])
            if let detail {
                Text(detail)
                    .monarchTextStyle(Tokens.captionStyle)
                    .foregroundStyle(theme.colors[role: Tokens.secondaryText])
            }
        }
        .fixedSize(horizontal: false, vertical: true)
        .padding(Tokens.detailPadding)
        .accessibilityElement(children: .combine)
        .presentationCompactAdaptation(.popover)
        .presentationBackground(theme.colors[role: Tokens.surfaceColor])
    }
}
