import MonarchUI
import SwiftUI

/// Recipe: the full record of one log entry in a sheet.
struct LogDrawerSection: CataloguePage {
    static let title = "Log drawer (recipe)"

    private struct LogEntry: Identifiable {
        let id: String
        let method: String
        let path: String
        let status: Int
        let errorCode: String?
        let durationMs: Int
        let body: String
    }

    private static let entries = [
        LogEntry(
            id: "req_8f2kQ1",
            method: "POST",
            path: "/v1/orders",
            status: 429,
            errorCode: "rate_limited",
            durationMs: 182,
            body: "{\n  \"error\": \"rate_limited\",\n  \"retry_after\": 30\n}"
        ),
        LogEntry(
            id: "req_3m9xT7",
            method: "GET",
            path: "/v1/orders/{id}",
            status: 200,
            errorCode: nil,
            durationMs: 41,
            body: "{\n  \"id\": \"ord_42\",\n  \"status\": \"paid\"\n}"
        ),
    ]

    @Environment(\.monarchTheme) private var theme
    @State private var selected: LogEntry?
    @State private var isLoading = false
    @State private var isFailed = false

    var body: some View {
        ForEach(Self.entries) { entry in
            Button {
                selected = entry
            } label: {
                HStack(spacing: theme.spacing.space2) {
                    MonarchMethodBadge(entry.method)
                    Text(entry.path).monarchTextStyle(.code)
                    Spacer(minLength: 0)
                    MonarchStatusBadge(
                        MonarchStatusDefinition(String(entry.status), tone: Self.tone(entry.status)),
                        size: .sm
                    )
                }
                .monarchMinimumHitTarget()
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
        }
        Toggle("Open while loading", isOn: $isLoading).monarchTextStyle(.body)
        Toggle("Open with a load error", isOn: $isFailed).monarchTextStyle(.body)
            .sheet(item: $selected) { entry in
                NavigationStack {
                    ScrollView {
                        details(entry)
                            .padding(theme.spacing.space4)
                    }
                    .navigationTitle("Request details")
                    .toolbar {
                        ToolbarItem(placement: .confirmationAction) {
                            Button("Close") { selected = nil }
                        }
                    }
                }
                .presentationDetents([.medium, .large])
            }

        CatalogueCaption("The sheet content")
        details(Self.entries[0])
    }

    @ViewBuilder private func details(_ entry: LogEntry) -> some View {
        if isLoading {
            MonarchLoadingState("Loading details", variant: .detail)
        } else if isFailed {
            MonarchInlineErrorRetry("The details did not load.") { isFailed = false }
        } else {
            VStack(alignment: .leading, spacing: theme.spacing.space4) {
                LabeledContent("Operation") {
                    HStack(spacing: theme.spacing.space2) {
                        MonarchMethodBadge(entry.method)
                        Text(entry.path).monarchTextStyle(.code)
                    }
                }
                LabeledContent("Status") {
                    HStack(spacing: theme.spacing.space2) {
                        MonarchStatusBadge(
                            MonarchStatusDefinition(String(entry.status), tone: Self.tone(entry.status)),
                            size: .sm
                        )
                        if let errorCode = entry.errorCode {
                            Text(errorCode).monarchTextStyle(.code)
                        }
                    }
                }
                HStack(alignment: .center, spacing: theme.spacing.space2) {
                    LabeledContent("Request ID") {
                        Text(entry.id).monarchTextStyle(.code)
                    }
                    MonarchCopyButton(value: entry.id, target: "request ID", variant: .ghost, isIconOnly: true)
                }
                LabeledContent("Duration") {
                    Text("\(entry.durationMs) ms")
                }
                MonarchCodeBlock(entry.body, language: "json", title: "Response body")
            }
            .labeledContentStyle(.monarch)
        }
    }

    /// The tone of an HTTP status: 2xx success, 3xx info, 4xx warning, 5xx danger.
    private static func tone(_ status: Int) -> MonarchTone {
        switch status {
        case 500...: .danger
        case 400..<500: .warning
        case 300..<400: .info
        case 200..<300: .success
        default: .neutral
        }
    }
}
