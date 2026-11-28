import MonarchUI
import SwiftUI

/// Native adoption: announcements are posted, not rendered.
struct LiveRegionSection: CataloguePage {
    static let title = "Live region (announcements)"

    @State private var saved = 0
    @State private var progress = 0.2

    var body: some View {
        CatalogueCaption("Turn on VoiceOver to hear these")
        MonarchButton("Save, announced politely") {
            saved += 1
            AccessibilityNotification.Announcement("Saved \(saved) changes").post()
        }
        MonarchButton("Fail, announced at once", variant: .danger) {
            var message = AttributedString("Upload failed")
            message.accessibilitySpeechAnnouncementPriority = .high
            AccessibilityNotification.Announcement(message).post()
        }

        CatalogueCaption("A value that changes often carries the updatesFrequently trait")
        ProgressView("Uploading", value: progress)
            .progressViewStyle(.monarchLinear)
        MonarchButton("Advance") {
            progress = progress >= 1 ? 0 : min(1, progress + 0.2)
        }
    }
}
