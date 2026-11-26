import Charts
import MonarchUI
import SwiftUI

/// Recipe: one report on its own screen.
struct ReportViewerSection: CataloguePage {
    static let title = "Report viewer (recipe)"

    private struct TeamRow: Identifiable, Sendable {
        let id: Int
        let team: String
        let headcount: Int
        let leavers: Int
    }

    private enum Phase: String, CaseIterable {
        case ready = "Ready"
        case loading = "Loading"
        case empty = "Empty"
        case forbidden = "Forbidden"
    }

    private static let rows = [
        TeamRow(id: 1, team: "Sales", headcount: 42, leavers: 3),
        TeamRow(id: 2, team: "Support", headcount: 31, leavers: 4),
        TeamRow(id: 3, team: "Engineering", headcount: 64, leavers: 2),
    ]
    private static let generated = Date(timeIntervalSince1970: 1_793_534_400)

    @Environment(\.monarchTheme) private var theme
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize
    @State private var phase = Phase.ready
    @State private var period = "q3"

    private var rows: [TeamRow] { phase == .empty ? [] : Self.rows }

    var body: some View {
        CatalogueCaption("State")
        Picker("State", selection: $phase) {
            ForEach(Phase.allCases, id: \.self) { Text($0.rawValue).tag($0) }
        }
        .pickerStyle(.segmented)

        CatalogueCaption("The report screen")
        NavigationStack {
            content
                .navigationTitle("Team headcount")
                .toolbar {
                    ToolbarItem(placement: .primaryAction) {
                        MonarchDownloadReportButton(formats: [.csv, MonarchDownloadReportFormat("PDF", id: "pdf")]) {
                            _ in
                            try await Task.sleep(for: .milliseconds(300))
                            return .downloaded
                        }
                        .disabled(phase != .ready)
                    }
                }
        }
        .frame(minHeight: theme.spacing.space24 * 7)
    }

    @ViewBuilder private var content: some View {
        switch phase {
        case .loading:
            MonarchLoadingState("Loading report", variant: .detail)
                .padding(theme.spacing.space4)
                .frame(maxHeight: .infinity, alignment: .top)
        case .forbidden:
            MonarchEmptyState(
                "You cannot open this report",
                message: "Ask a workspace admin for access to pay figures.",
                systemImage: "lock"
            )
            .padding(theme.spacing.space4)
        case .ready, .empty:
            ScrollView {
                VStack(alignment: .leading, spacing: theme.spacing.space4) {
                    Text("Headcount and leavers by team.")
                        .monarchTextStyle(.body)
                        .foregroundStyle(theme.colors.textMuted)
                    Text("All teams · Generated \(Self.generated.formatted(.dateTime.day().month().year()))")
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors.textMuted)
                    Picker("Period", selection: $period) {
                        Text("Q2").tag("q2")
                        Text("Q3").tag("q3")
                    }
                    .pickerStyle(.segmented)
                    if rows.isEmpty {
                        MonarchEmptyState(
                            "No results for Q3",
                            message: "Try another period.",
                            systemImage: "chart.bar",
                            kind: .filtered
                        )
                    } else {
                        figures
                        MonarchChartFrame(
                            "Headcount by team",
                            table: MonarchChartTable(
                                categoryTitle: "Team",
                                categories: rows.map(\.team),
                                series: [
                                    MonarchChartTableSeries("Headcount", values: rows.map { Double($0.headcount) })
                                ]
                            ),
                            height: theme.spacing.space24 * 2
                        ) {
                            Chart(rows) { row in
                                BarMark(x: .value("Team", row.team), y: .value("Headcount", row.headcount))
                                    .foregroundStyle(theme.colors.data1)
                            }
                        }
                        NavigationLink {
                            table.navigationTitle("Teams")
                        } label: {
                            Label("All \(rows.count) teams", systemImage: "tablecells")
                                .monarchTextStyle(.body)
                                .frame(
                                    maxWidth: .infinity,
                                    minHeight: MonarchAccessibility.minimumHitTarget,
                                    alignment: .leading
                                )
                        }
                        Text("Leavers counts people whose last day fell in the period.")
                            .monarchTextStyle(.caption)
                            .foregroundStyle(theme.colors.textMuted)
                    }
                }
                .padding(theme.spacing.space4)
            }
            .refreshable {
                try? await Task.sleep(for: .milliseconds(300))
            }
        }
    }

    private var figures: some View {
        LazyVGrid(
            columns: Array(
                repeating: GridItem(.flexible(), spacing: theme.spacing.space3),
                count: dynamicTypeSize.isAccessibilitySize ? 1 : 2
            ),
            spacing: theme.spacing.space3
        ) {
            MonarchStatCard("Headcount", value: Double(rows.reduce(0) { $0 + $1.headcount }))
            MonarchStatCard("Leavers", value: Double(rows.reduce(0) { $0 + $1.leavers }))
        }
        .accessibilityElement(children: .contain)
        .accessibilityLabel("Summary")
    }

    private var table: some View {
        MonarchDataTable(
            "Teams",
            rows: rows,
            columns: [
                MonarchDataTableColumn("Team", isHideable: false, value: { $0.team }),
                MonarchDataTableColumn("Headcount", value: { "\($0.headcount)" }),
                MonarchDataTableColumn("Leavers", value: { "\($0.leavers)" }),
            ],
            layout: .list
        )
    }
}
