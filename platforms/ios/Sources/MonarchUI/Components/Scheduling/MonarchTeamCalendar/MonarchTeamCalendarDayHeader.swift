import SwiftUI

/// The header of one day: its number (ringed for today) over its weekday
/// letter. A holiday's header is a button that shows the holiday's name.
struct MonarchTeamCalendarDayHeader: View {
    let number: String
    let weekday: String
    let isToday: Bool
    let isShaded: Bool
    /// The holiday's phrase, "Holiday: Thanksgiving", or `nil`.
    let holiday: String?
    /// What VoiceOver reads for the day.
    let spokenLabel: String
    let cellSize: CGFloat
    let markSize: CGFloat
    /// Receives the natural height of the content, so the header row can fit the tallest part.
    var onContentHeight: (@MainActor (CGFloat) -> Void)?

    @State private var isShowingHoliday = false
    @Environment(\.monarchTheme) private var theme

    private typealias Tokens = MonarchTeamCalendarTokens

    var body: some View {
        if let holiday {
            Button {
                isShowingHoliday = true
            } label: {
                column.contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel(spokenLabel)
            .popover(isPresented: $isShowingHoliday) {
                MonarchTeamCalendarDetail(title: holiday, detail: nil)
            }
        } else {
            column
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(spokenLabel)
        }
    }

    private var column: some View {
        let look = Tokens.headerAppearance(isToday: isToday)
        return VStack(spacing: 0) {
            Text(number)
                .monarchTextStyle(Tokens.labelStyle, weight: isToday ? Tokens.todayWeight : Tokens.headerWeight)
                .foregroundStyle(theme.colors[role: look.text])
                .lineLimit(1)
                .minimumScaleFactor(0.6)
                .frame(minWidth: markSize, minHeight: markSize)
                .overlay {
                    if let ring = look.ring {
                        Circle().strokeBorder(theme.colors[role: ring], lineWidth: MonarchShapes.borderWidth)
                    }
                }
            Text(weekday)
                .monarchTextStyle(Tokens.captionStyle)
                .foregroundStyle(theme.colors[role: Tokens.secondaryText])
        }
        .padding(.vertical, Tokens.headerVerticalPadding)
        .onGeometryChange(for: CGFloat.self, of: { $0.size.height }, action: { onContentHeight?($0) })
        .frame(width: cellSize)
        .frame(maxHeight: .infinity)
        .background(theme.colors[role: Tokens.columnColor(isShaded: isShaded) ?? Tokens.surfaceColor])
    }
}
