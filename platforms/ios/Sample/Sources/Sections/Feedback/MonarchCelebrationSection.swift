import MonarchUI
import SwiftUI

struct MonarchCelebrationSection: CataloguePage {
    static let title = "Celebration"

    @State private var isShown = false
    @State private var replay = 0

    var body: some View {
        CatalogueCaption("Inline, at the end of a flow")
        MonarchCelebration("Workspace ready", message: "Invite your team to start working together.") {
            MonarchButton("Invite people", variant: .primary) {}
            MonarchButton("Replay", variant: .ghost) { replay += 1 }
        }
        .id(replay)
        CatalogueCaption("In a sheet")
        MonarchButton("Verify the domain", variant: .primary) { isShown = true }
            .sheet(isPresented: $isShown) {
                MonarchCelebration("Domain verified", message: "Email from acme.com is now signed.", tone: .info) {
                    MonarchButton("Continue", variant: .primary, fullWidth: true) { isShown = false }
                }
                .presentationDetents([.medium])
            }
    }
}
