import MonarchUI
import SwiftUI

/// Native adoption: `Picker` in the menu style with a `Section` per category.
struct StatusPickerSection: CataloguePage {
    static let title = "Status picker (standard Picker)"

    /// The five fixed categories, in workflow order, each with its own symbol.
    private enum Category: CaseIterable {
        case backlog
        case unstarted
        case started
        case completed
        case cancelled

        var name: String {
            switch self {
            case .backlog: "Backlog"
            case .unstarted: "Not started"
            case .started: "In progress"
            case .completed: "Completed"
            case .cancelled: "Cancelled"
            }
        }

        var systemImage: String {
            switch self {
            case .backlog: "circle.dashed"
            case .unstarted: "circle"
            case .started: "circle.righthalf.filled"
            case .completed: "checkmark.circle.fill"
            case .cancelled: "xmark.circle.fill"
            }
        }

        var role: MonarchColorRole {
            switch self {
            case .backlog, .cancelled: \.textSubtle
            case .unstarted: \.textMuted
            case .started: \.warningStrong
            case .completed: \.successStrong
            }
        }
    }

    /// A team's own status in one category.
    private struct Status: Identifiable {
        let id: String
        let name: String
        let category: Category
    }

    private let statuses = [
        Status(id: "triage", name: "Triage", category: .backlog),
        Status(id: "backlog", name: "Backlog", category: .backlog),
        Status(id: "todo", name: "Todo", category: .unstarted),
        Status(id: "doing", name: "In progress", category: .started),
        Status(id: "review", name: "In review", category: .started),
        Status(id: "done", name: "Done", category: .completed),
        Status(id: "dropped", name: "Won’t do", category: .cancelled),
    ]

    @State private var statusID = "doing"
    @State private var compactID = "review"

    @Environment(\.monarchTheme) private var theme

    var body: some View {
        CatalogueCaption("Picker in the menu style, grouped by category")
        Picker("Status", selection: $statusID) { options }
            .pickerStyle(.menu)
            .monarchTextStyle(.body)

        CatalogueCaption("Menu with a Monarch trigger that shows the category symbol")
        Menu {
            Picker("Status", selection: $statusID) { options }
        } label: {
            glyphLabel(statusID)
        }
        .buttonStyle(.monarch(.secondary, size: .sm))
        .accessibilityLabel("Status: \(status(statusID)?.name ?? "No status")")

        CatalogueCaption("Symbol only, in a row of an issue list")
        Menu {
            Picker("Status", selection: $compactID) { options }
        } label: {
            if let status = status(compactID) {
                Image(systemName: status.category.systemImage)
                    .foregroundStyle(theme.colors[role: status.category.role])
                    .monarchMinimumHitTarget()
            }
        }
        .accessibilityLabel("Status: \(status(compactID)?.name ?? "No status")")

        CatalogueCaption("Disabled")
        Picker("Status", selection: .constant("done")) { options }
            .pickerStyle(.menu)
            .disabled(true)

        CatalogueCaption("Read-only, every category")
        ForEach(Category.allCases, id: \.self) { category in
            Label {
                Text(category.name)
            } icon: {
                Image(systemName: category.systemImage)
                    .foregroundStyle(theme.colors[role: category.role])
            }
            .monarchTextStyle(.body)
        }
    }

    @ViewBuilder private var options: some View {
        ForEach(Category.allCases, id: \.self) { category in
            let members = statuses.filter { $0.category == category }
            if !members.isEmpty {
                Section(category.name) {
                    ForEach(members) { status in
                        Label(status.name, systemImage: category.systemImage).tag(status.id)
                    }
                }
            }
        }
    }

    private func status(_ id: String) -> Status? {
        statuses.first { $0.id == id }
    }

    private func glyphLabel(_ id: String) -> some View {
        let current = status(id)
        return Label {
            Text(current?.name ?? "No status")
        } icon: {
            Image(systemName: current?.category.systemImage ?? Category.backlog.systemImage)
                .foregroundStyle(theme.colors[role: current?.category.role ?? Category.backlog.role])
        }
    }
}
