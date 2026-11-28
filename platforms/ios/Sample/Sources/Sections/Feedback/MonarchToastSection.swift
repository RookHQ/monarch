import MonarchUI
import SwiftUI

struct MonarchToastSection: CataloguePage {
    static let title = "Toast"

    @State private var toasts = MonarchToastCenter()
    @State private var undone = 0
    @State private var committed = 0
    @State private var note = ""

    var body: some View {
        Group {
            CatalogueCaption("Tones")
            ForEach(MonarchTone.allCases, id: \.self) { tone in
                MonarchButton("Show \(String(describing: tone))", size: .sm) {
                    toasts.show("Ticket updated", message: "Tone: \(String(describing: tone)).", tone: tone)
                }
            }
            CatalogueCaption("With an action. Undone \(undone) times.")
            MonarchButton("Archive", variant: .primary) {
                toasts.show("Ticket archived", tone: .success, actionTitle: "Undo") {
                    undone += 1
                }
            }
            CatalogueCaption("Undo that commits when the toast closes. Committed \(committed) times.")
            MonarchButton("Delete") {
                var wasUndone = false
                toasts.show(
                    "Ticket deleted",
                    actionTitle: "Undo",
                    action: { wasUndone = true },
                    onDismiss: { if !wasUndone { committed += 1 } }
                )
            }
            CatalogueCaption("Stays until dismissed")
            MonarchButton("Show a toast that stays") {
                toasts.show("Export ready", message: "Dismiss it when you have read it.", duration: .infinity)
            }
            MonarchButton("Dismiss all", variant: .ghost) { toasts.dismissAll() }
            CatalogueCaption("Toasts move above the keyboard")
            MonarchTextField("Note", text: $note)
        }
        .monarchToastHost(toasts)
    }
}
