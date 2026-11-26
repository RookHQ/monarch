import MonarchUI
import SwiftUI

/// Recipe: a row of key figures as MonarchStatCards in a two-column
/// `LazyVGrid`, or in a sideways `ScrollView` that snaps card by card, with
/// one loading and error state for the row.
struct KpiRowSection: CataloguePage {
    static let title = "KPI row (stat card recipe)"

    @Environment(\.monarchTheme) private var theme
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize
    @State private var isLoading = false
    @State private var hasError = false
    @State private var isRetrying = false

    private var columns: [GridItem] {
        Array(
            repeating: GridItem(.flexible(), spacing: theme.spacing.space3),
            count: dynamicTypeSize.isAccessibilitySize ? 1 : 2
        )
    }

    var body: some View {
        Toggle("Loading", isOn: $isLoading).monarchTextStyle(.body)
        Toggle("Failed to load", isOn: $hasError).monarchTextStyle(.body)

        CatalogueCaption("Two columns")
        if hasError {
            MonarchInlineErrorRetry("The figures didn't load.", variant: .banner, isRetrying: isRetrying) {
                isRetrying = true
                Task {
                    try? await Task.sleep(for: .seconds(1))
                    isRetrying = false
                    hasError = false
                }
            }
        } else {
            LazyVGrid(columns: columns, spacing: theme.spacing.space3) {
                MonarchStatCard(
                    "Revenue",
                    value: 48_200,
                    change: MonarchMetricChange(0.12, comparison: "vs last month"),
                    format: .currency(code: "USD"),
                    isLoading: isLoading
                )
                MonarchStatCard("New customers", value: 214, change: MonarchMetricChange(0.04), isLoading: isLoading)
                MonarchStatCard(
                    "Churn",
                    value: 0.021,
                    change: MonarchMetricChange(-0.003, goodDirection: .down),
                    format: .percent,
                    isLoading: isLoading
                )
                MonarchStatCard("Open tickets", value: 38, sparkline: [52, 47, 44, 40, 38], isLoading: isLoading)
            }
            .accessibilityElement(children: .contain)
            .accessibilityLabel("This month")
        }

        CatalogueCaption("One row that scrolls sideways and snaps")
        ScrollView(.horizontal) {
            LazyHStack(spacing: theme.spacing.space3) {
                ForEach(["Revenue", "Orders", "Refunds", "Visitors"], id: \.self) { label in
                    MonarchStatCard(label, value: 1_280, change: MonarchMetricChange(0.05), size: .sm)
                        .containerRelativeFrame(.horizontal, count: 5, span: 4, spacing: theme.spacing.space3)
                }
            }
            .scrollTargetLayout()
        }
        .scrollTargetBehavior(.viewAligned)
        .scrollIndicators(.hidden)
        .accessibilityElement(children: .contain)
        .accessibilityLabel("Today")
    }
}
