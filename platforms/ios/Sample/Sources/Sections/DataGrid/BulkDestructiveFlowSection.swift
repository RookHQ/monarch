import MonarchUI
import SwiftUI

/// Recipe: confirm and run a bulk deletion, with progress and a partial-failure report.
struct BulkDestructiveFlowSection: CataloguePage {
    static let title = "Bulk destructive flow (recipe)"

    @Environment(\.monarchTheme) private var theme
    @State private var isOpen = false
    @State private var typed = ""
    @State private var done = 0
    @State private var isRunning = false
    @State private var isFinished = false
    @State private var failed: [String] = []
    @State private var leads = (1...37).map { "Lead \($0)" }

    var body: some View {
        MonarchButton("Delete \(leads.count) leads", systemImage: "trash", variant: .danger) {
            typed = ""
            done = 0
            failed = []
            isFinished = false
            isOpen = true
        }
        .disabled(leads.isEmpty)
        .sheet(isPresented: $isOpen) {
            ScrollView {
                VStack(alignment: .leading, spacing: theme.spacing.space4) {
                    if isFinished {
                        result
                    } else if isRunning {
                        ProgressView(
                            "\(done) of \(leads.count) deleted",
                            value: Double(done),
                            total: Double(leads.count)
                        )
                        .progressViewStyle(.monarchLinear)
                    } else {
                        confirmation
                    }
                }
                .padding(theme.spacing.space4)
            }
            .background(theme.colors.bgCanvas)
            .interactiveDismissDisabled(isRunning)
            .presentationDetents([.large])
        }
    }

    private var confirmation: some View {
        VStack(alignment: .leading, spacing: theme.spacing.space4) {
            Text("Delete \(leads.count) leads")
                .monarchTextStyle(.heading)
                .accessibilityAddTraits(.isHeader)
            Text("This cannot be undone.")
                .monarchTextStyle(.body)
                .foregroundStyle(theme.colors.textMuted)
            Text("Selected: \(leads.prefix(3).joined(separator: ", ")), +\(leads.count - 3) more")
                .monarchTextStyle(.body)
            MonarchConsequenceList(
                [
                    MonarchConsequence(.removed, "leads are deleted", count: leads.count),
                    MonarchConsequence(.changed, "Their open tasks are unassigned"),
                    MonarchConsequence(.kept, "Won deals stay in the pipeline"),
                ],
                heading: "What happens"
            )
            MonarchTypeToConfirmInput(phrase: "\(leads.count)", text: $typed, label: "Type \(leads.count) to confirm")
            MonarchButton("Delete \(leads.count) leads", variant: .danger, fullWidth: true) { run() }
                .disabled(!MonarchTypeToConfirmInput.matches(typed, phrase: "\(leads.count)"))
            MonarchButton("Cancel", variant: .ghost, fullWidth: true) { isOpen = false }
        }
    }

    private var result: some View {
        let deleted = leads.count - failed.count
        return VStack(alignment: .leading, spacing: theme.spacing.space4) {
            if failed.isEmpty {
                MonarchBanner("\(deleted) leads deleted", message: "They are no longer in any list.", tone: .success)
            } else {
                MonarchBanner(
                    "\(deleted) of \(leads.count) leads deleted",
                    message: "\(failed.count) leads could not be deleted.",
                    tone: .warning
                )
                Text("Not deleted")
                    .monarchTextStyle(.subheading)
                    .accessibilityAddTraits(.isHeader)
                ForEach(failed, id: \.self) { name in
                    Text("\(name): it has an open invoice").monarchTextStyle(.body)
                }
            }
            MonarchButton("Done", variant: .primary, fullWidth: true) {
                leads.removeAll { !failed.contains($0) }
                isOpen = false
            }
        }
    }

    private func run() {
        isRunning = true
        Task {
            for lead in leads {
                try? await Task.sleep(for: .milliseconds(40))
                if lead == "Lead 7" || lead == "Lead 21" { failed.append(lead) }
                done += 1
            }
            isRunning = false
            isFinished = true
            AccessibilityNotification.Announcement("\(leads.count - failed.count) of \(leads.count) leads deleted")
                .post()
        }
    }
}
