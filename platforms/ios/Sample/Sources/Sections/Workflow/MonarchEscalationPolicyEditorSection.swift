import MonarchUI
import SwiftUI

struct MonarchEscalationPolicyEditorSection: CataloguePage {
    static let title = "Escalation policy editor"

    @Environment(\.monarchTheme) private var theme
    @State private var levels = [
        MonarchEscalationLevel(id: "l1", afterMinutes: 5, targets: ["oncall"]),
        MonarchEscalationLevel(id: "l2", afterMinutes: 30, targets: ["oncall", "ops"]),
    ]
    @State private var timing = MonarchEscalationPolicyTiming.cumulative
    @State private var showsErrors = false

    private let targets = [
        MonarchEscalationTarget(id: "oncall", label: "On-call rota"),
        MonarchEscalationTarget(id: "ops", label: "#ops", description: "Slack channel"),
        MonarchEscalationTarget(id: "lead", label: "Ada Moss", description: "Team lead"),
    ]

    var body: some View {
        CatalogueCaption("Edit the ladder")
        Picker("Timing", selection: $timing) {
            Text("From the start").tag(MonarchEscalationPolicyTiming.cumulative)
            Text("From the last level").tag(MonarchEscalationPolicyTiming.interval)
        }
        .pickerStyle(.segmented)
        MonarchEscalationPolicyEditor(
            levels: $levels,
            targets: targets,
            timing: timing,
            description: "Each level fires only if nobody has acknowledged yet.",
            maxLevels: 5,
            showsErrors: showsErrors
        )
        MonarchButton("Save", variant: .primary) { showsErrors = true }
        let problems = MonarchEscalationPolicyEditor.validate(levels, timing: timing)
        Text(problems.isEmpty ? "Ready to save" : "\(problems.count) levels need attention")
            .monarchTextStyle(.caption)
            .foregroundStyle(theme.colors.textMuted)
    }
}
