import MonarchUI
import SwiftUI

struct MonarchSparklineSection: CataloguePage {
    static let title = "Sparkline"

    @Environment(\.monarchTheme) private var theme
    @State private var values: [Double?] = [3, 5, 4, 8, 7, 9, 6, 11]

    var body: some View {
        CatalogueCaption("Line, area and bars")
        HStack(spacing: theme.spacing.space4) {
            ForEach(MonarchSparklineVariant.allCases, id: \.self) { variant in
                MonarchSparkline(values, label: "Sign-ups, last \(values.count) days", variant: variant)
            }
        }
        MonarchButton("Add a day", size: .sm) {
            values.append(Double.random(in: 2...12).rounded())
        }

        CatalogueCaption("Tones, beside the number they explain")
        HStack(spacing: theme.spacing.space2) {
            Text("p95 latency 412 ms").monarchTextStyle(.body)
            MonarchSparkline(values, tone: .warning, variant: .area)
        }
        MonarchSparkline(values, tone: .success, variant: .area, fullWidth: true)

        CatalogueCaption("A gap, a rank (lower is better) and too little data")
        MonarchSparkline([3, 5, nil, 8, 7], label: "Sign-ups with a gap")
        MonarchSparkline([9, 7, 8, 4, 2], label: "Rank", tone: .info, isInverted: true)
        MonarchSparkline([4], label: "Sign-ups")
    }
}
