import MonarchUI
import SwiftUI

/// Native adoption: the platform's relative date formatters.
struct RelativeTimeSection: CataloguePage {
    static let title = "Relative time (standard formatters)"

    @Environment(\.monarchTheme) private var theme

    private let dates: [Date] = [-45, -3 * 3600, -26 * 3600, 5 * 86400].map { Date.now.addingTimeInterval($0) }

    var body: some View {
        CatalogueCaption("Date.RelativeFormatStyle, named, redrawn every minute")
        TimelineView(.everyMinute) { _ in
            VStack(alignment: .leading, spacing: theme.spacing.space1) {
                ForEach(dates, id: \.self) { date in
                    Text(date.formatted(.relative(presentation: .named)))
                        .accessibilityLabel(date.formatted(date: .abbreviated, time: .shortened))
                }
            }
        }
        .monarchTextStyle(.body)

        CatalogueCaption("Abbreviated units")
        ForEach(dates, id: \.self) { date in
            Text(date.formatted(.relative(presentation: .numeric, unitsStyle: .abbreviated)))
                .monarchTextStyle(.caption)
                .accessibilityLabel(date.formatted(date: .abbreviated, time: .shortened))
        }

        CatalogueCaption("Text(date, style: .relative), which counts by itself")
        Text(dates[0], style: .relative)
            .monarchTextStyle(.body)
            .monospacedDigit()
    }
}
