import MonarchUI
import SwiftUI

struct MonarchApprovalChainEditorSection: CataloguePage {
    static let title = "Approval chain editor"

    @Environment(\.monarchTheme) private var theme
    @State private var steps = [
        MonarchApprovalChainStep(id: "s1", approver: "manager"),
        MonarchApprovalChainStep(id: "s2", approver: "role", target: "finance", label: "Finance check"),
    ]
    @State private var showsErrors = false

    private let types = [
        MonarchApprovalApproverType(id: "manager", label: "Reporting manager"),
        MonarchApprovalApproverType(
            id: "role",
            label: "Anyone with a role",
            targets: [
                MonarchComboboxOption(id: "finance", label: "Finance team"),
                MonarchComboboxOption(id: "legal", label: "Legal"),
            ],
            targetLabel: "Role"
        ),
        MonarchApprovalApproverType(
            id: "person",
            label: "A specific person",
            targets: [
                MonarchComboboxOption(id: "ada", label: "Ada Moss"),
                MonarchComboboxOption(id: "sam", label: "Sam Ortiz"),
            ],
            targetLabel: "Person"
        ),
    ]

    var body: some View {
        CatalogueCaption("Edit, reorder, add and remove steps")
        MonarchApprovalChainEditor(
            steps: $steps,
            approverTypes: types,
            description: "Each step must approve before the next one is asked.",
            maxSteps: 5,
            showsErrors: showsErrors
        )
        MonarchButton("Save", variant: .primary) { showsErrors = true }
        Text(MonarchApprovalChainEditor.summarize(steps, approverTypes: types))
            .monarchTextStyle(.caption)
            .foregroundStyle(theme.colors.textMuted)
    }
}
