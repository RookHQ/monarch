import MonarchUI
import SwiftUI

struct MonarchStepperSection: CataloguePage {
    static let title = "Stepper"

    @State private var current = 1
    @State private var furthest = 1

    @Environment(\.monarchTheme) private var theme

    private let steps = [
        MonarchStepperStep(id: "details", label: "Details"),
        MonarchStepperStep(id: "address", label: "Address", description: "Where we send invoices"),
        MonarchStepperStep(id: "team", label: "Team", isOptional: true),
        MonarchStepperStep(id: "review", label: "Review"),
    ]

    var body: some View {
        CatalogueCaption("Vertical; reached steps are buttons")
        MonarchStepper(steps, current: current, furthest: furthest, orientation: .vertical) { _, index in
            current = index
        }
        HStack(spacing: theme.spacing.space2) {
            Button("Back") { current = max(0, current - 1) }
                .buttonStyle(.monarch(.secondary, size: .sm))
                .disabled(current == 0)
            Button("Next") {
                current = min(steps.count - 1, current + 1)
                furthest = max(furthest, current)
            }
            .buttonStyle(.monarch(.primary, size: .sm))
            .disabled(current == steps.count - 1)
        }

        CatalogueCaption("Horizontal: the row where it fits, the compact form where it does not")
        MonarchStepper(steps, current: current)

        CatalogueCaption("Horizontal, never compact")
        MonarchStepper(
            [
                MonarchStepperStep(id: "a", label: "Plan"),
                MonarchStepperStep(id: "b", label: "Pay"),
                MonarchStepperStep(id: "c", label: "Done"),
            ],
            current: 1,
            compact: .never
        )

        CatalogueCaption("Always compact")
        MonarchStepper(steps, current: current, compact: .always)

        CatalogueCaption("Error, finished out of order and disabled")
        MonarchStepper(
            [
                MonarchStepperStep(id: "a", label: "Account", hasError: true),
                MonarchStepperStep(id: "b", label: "Locked", isDisabled: true),
                MonarchStepperStep(id: "c", label: "Billing"),
                MonarchStepperStep(id: "d", label: "Imported earlier", isComplete: true),
            ],
            current: 2,
            orientation: .vertical,
            onSelect: { _, _ in }
        )
    }
}
