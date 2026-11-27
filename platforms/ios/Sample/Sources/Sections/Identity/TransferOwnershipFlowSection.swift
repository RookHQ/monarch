import MonarchUI
import SwiftUI

/// Recipe: hand a workspace to another member in three steps, in a sheet.
struct TransferOwnershipFlowSection: CataloguePage {
    static let title = "Transfer ownership flow (recipe)"

    private enum Step: Hashable {
        case review
        case confirm
    }

    @Environment(\.monarchTheme) private var theme
    @State private var isOpen = false
    @State private var path: [Step] = []
    @State private var owner: String?
    @State private var yourRole: String? = "admin"
    @State private var isTransferring = false
    @State private var transferredTo: String?

    private let steps = [
        MonarchStepperStep(id: "pick", label: "New owner"),
        MonarchStepperStep(id: "review", label: "What changes"),
        MonarchStepperStep(id: "confirm", label: "Confirm"),
    ]

    private let members = [
        MonarchEntityPickerEntity(id: "sam", name: "Sam Lee", description: "Admin"),
        MonarchEntityPickerEntity(id: "kim", name: "Kim Park", description: "Admin"),
        MonarchEntityPickerEntity(
            id: "lee",
            name: "Lee Moreau",
            description: "Member",
            hint: "Only admins can become the owner",
            isDisabled: true
        ),
    ]

    private var ownerName: String { members.first { $0.id == owner }?.name ?? "" }

    var body: some View {
        MonarchButton("Transfer ownership", systemImage: "arrow.left.arrow.right") {
            path = []
            owner = nil
            isOpen = true
        }
        .sheet(isPresented: $isOpen) {
            NavigationStack(path: $path) {
                page(0) { pick }
                    .navigationTitle("Transfer ownership")
                    .toolbar {
                        ToolbarItem(placement: .cancellationAction) { Button("Cancel") { isOpen = false } }
                    }
                    .navigationDestination(for: Step.self) { step in
                        switch step {
                        case .review: page(1) { review }
                        case .confirm: page(2) { confirm }
                        }
                    }
            }
            .interactiveDismissDisabled(isTransferring)
        }
        if let transferredTo {
            MonarchBanner("Waiting for \(transferredTo) to accept", message: "You stay the owner until they accept.")
        }
    }

    private func page(_ index: Int, @ViewBuilder content: () -> some View) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: theme.spacing.space4) {
                MonarchStepper(steps, current: index)
                content()
            }
            .padding(theme.spacing.space4)
        }
        .background(theme.colors.bgCanvas)
    }

    private var pick: some View {
        VStack(alignment: .leading, spacing: theme.spacing.space4) {
            Text("The new owner gets full control of the workspace, including billing.")
                .monarchTextStyle(.body)
            MonarchEntityPicker("New owner", selection: $owner, entities: members, prompt: "Choose a member")
            MonarchButton("Continue", variant: .primary, fullWidth: true) { path.append(.review) }
                .disabled(owner == nil)
        }
    }

    private var review: some View {
        VStack(alignment: .leading, spacing: theme.spacing.space4) {
            MonarchConsequenceList(
                [
                    MonarchConsequence(.changed, "You become an admin"),
                    MonarchConsequence(.removed, "You can no longer delete or transfer the workspace"),
                ],
                heading: "For you"
            )
            MonarchConsequenceList(
                [
                    MonarchConsequence(.changed, "\(ownerName) becomes the owner"),
                    MonarchConsequence(.warning, "\(ownerName) can change your role or remove you"),
                ],
                heading: "For \(ownerName)"
            )
            MonarchRolePicker(
                "Your role after the transfer",
                selection: $yourRole,
                roles: [
                    MonarchRoleOption("admin", title: "Admin"),
                    MonarchRoleOption("member", title: "Member"),
                ],
                layout: .compact
            )
            MonarchButton("Continue", variant: .primary, fullWidth: true) { path.append(.confirm) }
        }
    }

    private var confirm: some View {
        VStack(alignment: .leading, spacing: theme.spacing.space4) {
            Text("\(ownerName) gets an email and becomes the owner once they accept.")
                .monarchTextStyle(.body)
            MonarchButton("Transfer ownership", variant: .danger, isLoading: isTransferring, fullWidth: true) {
                transfer()
            }
        }
    }

    private func transfer() {
        isTransferring = true
        Task {
            // The app asks its server to start the transfer here.
            try? await Task.sleep(for: .milliseconds(600))
            isTransferring = false
            transferredTo = ownerName
            isOpen = false
            AccessibilityNotification.Announcement("Waiting for \(ownerName) to accept").post()
        }
    }
}
