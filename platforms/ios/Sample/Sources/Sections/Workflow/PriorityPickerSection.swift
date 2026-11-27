import MonarchUI
import SwiftUI

/// Native adoption: `Picker` in the menu style with a `Label` per level.
struct PriorityPickerSection: CataloguePage {
    static let title = "Priority picker (standard Picker)"

    /// The five levels, each with its own symbol and name.
    private enum Priority: String, CaseIterable, Identifiable {
        case urgent
        case high
        case medium
        case low
        case unset

        var id: Self { self }

        var name: String {
            switch self {
            case .urgent: "Urgent"
            case .high: "High"
            case .medium: "Medium"
            case .low: "Low"
            case .unset: "No priority"
            }
        }

        var systemImage: String {
            switch self {
            case .urgent: "exclamationmark.square.fill"
            case .high: "chevron.up"
            case .medium: "equal"
            case .low: "chevron.down"
            case .unset: "minus"
            }
        }

        var role: MonarchColorRole {
            switch self {
            case .urgent: \.dangerStrong
            case .high: \.warningStrong
            case .medium, .low: \.textDefault
            case .unset: \.textMuted
            }
        }
    }

    @State private var priority = Priority.medium
    @State private var compact = Priority.urgent

    @Environment(\.monarchTheme) private var theme

    var body: some View {
        CatalogueCaption("Picker in the menu style")
        Picker("Priority", selection: $priority) {
            ForEach(Priority.allCases) { level in
                Label(level.name, systemImage: level.systemImage).tag(level)
            }
        }
        .pickerStyle(.menu)
        .monarchTextStyle(.body)

        CatalogueCaption("Menu with a Monarch trigger that shows the symbol")
        Menu {
            Picker("Priority", selection: $priority) {
                ForEach(Priority.allCases) { level in
                    Label(level.name, systemImage: level.systemImage).tag(level)
                }
            }
        } label: {
            Label {
                Text(priority.name)
            } icon: {
                Image(systemName: priority.systemImage)
                    .foregroundStyle(theme.colors[role: priority.role])
            }
        }
        .buttonStyle(.monarch(.secondary, size: .sm))
        .accessibilityLabel("Priority: \(priority.name)")

        CatalogueCaption("Symbol only, in a row of an issue list")
        Menu {
            Picker("Priority", selection: $compact) {
                ForEach(Priority.allCases) { level in
                    Label(level.name, systemImage: level.systemImage).tag(level)
                }
            }
        } label: {
            Image(systemName: compact.systemImage)
                .foregroundStyle(theme.colors[role: compact.role])
                .monarchMinimumHitTarget()
        }
        .accessibilityLabel("Priority: \(compact.name)")

        CatalogueCaption("Read-only, every level")
        ForEach(Priority.allCases) { level in
            Label {
                Text(level.name)
            } icon: {
                Image(systemName: level.systemImage)
                    .foregroundStyle(theme.colors[role: level.role])
            }
            .monarchTextStyle(.body)
        }
    }
}
