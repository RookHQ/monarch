import MonarchUI
import SwiftUI

/// Recipe: remove a member after handing over their open work, in a sheet.
struct RemoveMemberFlowSection: CataloguePage {
    static let title = "Remove member flow (recipe)"

    private struct OpenWork: Identifiable {
        let id: String
        let label: String
        let count: Int
    }

    @Environment(\.monarchTheme) private var theme
    @State private var toasts = MonarchToastCenter()
    @State private var isOpen = false
    @State private var ownsWork = true
    @State private var allTo: String?
    @State private var isPerItem = false
    @State private var each: [String: String] = [:]
    @State private var isConfirming = false
    @State private var isRemoving = false
    @State private var failsNext = false
    @State private var failure: String?
    @State private var isMember = true

    private let member = "Jordan Lee"
    private let people = [
        MonarchAssigneePerson(id: "sam", name: "Sam Rivera", email: "sam@example.com"),
        MonarchAssigneePerson(id: "alex", name: "Alex Morgan", email: "alex@example.com"),
        MonarchAssigneePerson(id: "priya", name: "Priya Shah", email: "priya@example.com"),
    ]
    private let allWork = [
        OpenWork(id: "issues", label: "open issues", count: 12),
        OpenWork(id: "reviews", label: "pending reviews", count: 3),
        OpenWork(id: "docs", label: "draft documents", count: 5),
    ]

    private var work: [OpenWork] { ownsWork ? allWork : [] }

    var body: some View {
        VStack(alignment: .leading, spacing: theme.spacing.space3) {
            Toggle("Jordan owns open work", isOn: $ownsWork)
            Toggle("Fail the next removal", isOn: $failsNext)
            if isMember {
                MonarchButton("Remove \(member)", systemImage: "person.badge.minus", variant: .danger) {
                    open()
                }
            } else {
                CatalogueCaption("\(member) is no longer a member.")
            }
        }
        .frame(minHeight: 280, alignment: .top)
        .sheet(isPresented: $isOpen) {
            NavigationStack {
                content
                    .navigationTitle("Remove \(member)")
                    .toolbar {
                        ToolbarItem(placement: .cancellationAction) {
                            Button("Cancel") { isOpen = false }
                                .disabled(isRemoving)
                        }
                        ToolbarItem(placement: .confirmationAction) {
                            if isRemoving {
                                ProgressView().accessibilityLabel("Removing \(member)")
                            } else {
                                Button("Remove", role: .destructive) { isConfirming = true }
                            }
                        }
                    }
                    .confirmationDialog(
                        "Remove \(member)?",
                        isPresented: $isConfirming,
                        titleVisibility: .visible,
                        actions: {
                            Button("Remove member", role: .destructive) { remove() }
                            Button("Cancel", role: .cancel) {}
                        },
                        message: {
                            Text("They lose access to the workspace straight away. Their past activity stays.")
                        }
                    )
            }
            .interactiveDismissDisabled(isRemoving)
        }
        .monarchToastHost(toasts)
    }

    private var content: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: theme.spacing.space4) {
                MonarchMemberRow(member, image: nil, email: "jordan@example.com", role: "Member")
                if !work.isEmpty {
                    Text("Hand over their open work")
                        .monarchTextStyle(.heading)
                        .accessibilityAddTraits(.isHeader)
                    Text("Choose who takes over what \(member) still owns, or leave it unassigned.")
                        .monarchTextStyle(.body)
                    if isPerItem {
                        ForEach(work) { item in
                            MonarchAssigneePicker(
                                "\(item.count) \(item.label)",
                                selection: binding(for: item.id),
                                people: people,
                                showsAssignToMe: false,
                                labels: MonarchAssigneePickerLabels(unassigned: "Leave unassigned")
                            )
                        }
                    } else {
                        MonarchAssigneePicker(
                            "Reassign all open work to",
                            selection: $allTo,
                            people: people,
                            showsAssignToMe: false,
                            labels: MonarchAssigneePickerLabels(unassigned: "Leave unassigned")
                        )
                    }
                    Toggle("Choose a person for each kind of work", isOn: perItemBinding)
                }
                MonarchConsequenceList(consequences, heading: "What happens")
                if let failure {
                    MonarchInlineNotice(failure, tone: .danger)
                }
            }
            .padding(theme.spacing.space4)
        }
        .background(theme.colors.bgCanvas)
    }

    private var perItemBinding: Binding<Bool> {
        Binding(
            get: { isPerItem },
            set: { isOn in
                // Copy the all-at-once choice into each picker so nothing is lost.
                if isOn {
                    for item in work { each[item.id] = allTo }
                }
                isPerItem = isOn
            }
        )
    }

    private func binding(for workID: String) -> Binding<String?> {
        Binding(get: { each[workID] }, set: { each[workID] = $0 })
    }

    private func assignee(of workID: String) -> String? {
        isPerItem ? each[workID] : allTo
    }

    private var consequences: [MonarchConsequence] {
        var items = [MonarchConsequence(.removed, "\(member) loses access to the workspace")]
        for item in work {
            let what = "\(item.count) \(item.label)"
            if let name = people.first(where: { $0.id == assignee(of: item.id) })?.name {
                items.append(MonarchConsequence(.changed, "\(what) go to \(name)", id: item.id))
            } else {
                items.append(MonarchConsequence(.warning, "\(what) are left unassigned", id: item.id))
            }
        }
        items.append(MonarchConsequence(.kept, "Their past activity and comments stay"))
        return items
    }

    private func open() {
        allTo = nil
        isPerItem = false
        each = [:]
        failure = nil
        isOpen = true
    }

    private func remove() {
        isRemoving = true
        failure = nil
        Task {
            // The app sends the removal and each work item's assignee to its server here.
            try? await Task.sleep(for: .milliseconds(600))
            isRemoving = false
            if failsNext {
                failsNext = false
                failure = "Couldn’t remove \(member). Try again."
                return
            }
            isOpen = false
            isMember = false
            toasts.show("\(member) removed", actionTitle: "Undo", duration: 8) {
                // The app restores the membership and the work on its server here.
                isMember = true
                toasts.show("\(member) is back", tone: .success, duration: 4)
            }
        }
    }
}
