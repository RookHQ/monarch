import MonarchUI
import SwiftUI

struct MonarchMyStatusSection: CataloguePage {
    static let title = "My status"

    @Environment(\.monarchTheme) private var theme
    @State private var presence: MonarchPresenceStatus = .online
    @State private var text = "On holiday"
    @State private var emoji = "🌴"
    @State private var clearsInAMinute = false
    @State private var until: Date?
    @State private var isEditing = false

    var body: some View {
        CatalogueCaption("Tap the row to change the status")
        MonarchMyStatus(
            "Ana Silva",
            image: nil,
            presence: presence,
            customStatus: MonarchMyStatusCustomStatus(text, emoji: emoji, until: until)
        ) {
            isEditing = true
        }
        .sheet(isPresented: $isEditing) {
            editor
                .presentationDetents([.medium, .large])
                .presentationDragIndicator(.visible)
        }

        CatalogueCaption("No status: presence or the invitation")
        MonarchMyStatus("Ben Okafor", image: nil, presence: .doNotDisturb) {}
        MonarchMyStatus("Chen Wei", image: nil, presence: .online) {}
    }

    private var editor: some View {
        NavigationStack {
            Form {
                Picker("Presence", selection: $presence) {
                    Text("Online").tag(MonarchPresenceStatus.online)
                    Text("Away").tag(MonarchPresenceStatus.away)
                    Text("Busy").tag(MonarchPresenceStatus.busy)
                    Text("Do not disturb").tag(MonarchPresenceStatus.doNotDisturb)
                    Text("Offline").tag(MonarchPresenceStatus.offline)
                }
                TextField("Emoji", text: $emoji)
                TextField("Status", text: $text)
                Toggle("Clear after one minute", isOn: $clearsInAMinute)
                Button("Clear status", role: .destructive) {
                    text = ""
                    emoji = ""
                }
            }
            .navigationTitle("Your status")
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") {
                        until = clearsInAMinute ? Date.now.addingTimeInterval(60) : nil
                        isEditing = false
                    }
                }
            }
        }
    }
}
