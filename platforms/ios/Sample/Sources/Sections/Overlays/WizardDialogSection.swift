import MonarchUI
import SwiftUI

/// Recipe: steps in a sheet with MonarchStepper, Back and Continue.
struct WizardDialogSection: CataloguePage {
    static let title = "Wizard dialog (sheet recipe)"

    @Environment(\.monarchTheme) private var theme
    @State private var isOpen = false
    @State private var step = 0
    @State private var destination = ""
    @State private var result = "Not moved yet"

    private let steps = [
        MonarchStepperStep(id: "where", label: "Destination"),
        MonarchStepperStep(id: "check", label: "Review"),
        MonarchStepperStep(id: "move", label: "Move"),
    ]

    private var isLast: Bool { step == steps.count - 1 }

    var body: some View {
        Text(result).monarchTextStyle(.bodyStrong)
        MonarchButton("Move project", variant: .primary) {
            step = 0
            destination = ""
            isOpen = true
        }
        .sheet(isPresented: $isOpen) {
            NavigationStack {
                VStack(alignment: .leading, spacing: theme.spacing.space4) {
                    MonarchStepper(steps, current: step)
                    Text(steps[step].label)
                        .monarchTextStyle(.heading)
                        .accessibilityAddTraits(.isHeader)
                    stepContent
                    Spacer(minLength: 0)
                    if step == 0, destination.isEmpty {
                        Text("Enter a workspace to continue.")
                            .monarchTextStyle(.caption)
                            .foregroundStyle(theme.colors.textMuted)
                    }
                    HStack {
                        if step > 0 {
                            MonarchButton("Back") { move(to: step - 1) }
                        }
                        Spacer(minLength: 0)
                        MonarchButton(isLast ? "Move project" : "Continue", variant: isLast ? .danger : .primary) {
                            if isLast {
                                result = "Moved to \(destination)"
                                isOpen = false
                            } else {
                                move(to: step + 1)
                            }
                        }
                        .disabled(step == 0 && destination.isEmpty)
                    }
                }
                .foregroundStyle(theme.colors.textDefault)
                .padding(theme.spacing.space4)
                .navigationTitle("Move project")
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .cancellationAction) {
                        Button("Cancel") { isOpen = false }
                    }
                }
            }
            .interactiveDismissDisabled(step > 0)
            .presentationBackground(theme.colors.bgSurface)
        }
    }

    @ViewBuilder private var stepContent: some View {
        switch step {
        case 0: MonarchTextField("Workspace", text: $destination, prompt: "Design team")
        case 1: Text("Moving to \(destination). Links to the project keep working.").monarchTextStyle(.body)
        default: Text("Members of \(destination) get access at once. This cannot be undone.").monarchTextStyle(.body)
        }
    }

    private func move(to index: Int) {
        step = index
        AccessibilityNotification.Announcement("Step \(index + 1) of \(steps.count), \(steps[index].label)").post()
    }
}
