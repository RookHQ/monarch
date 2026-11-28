import MonarchUI
import SwiftUI

struct MonarchInlineNoticeSection: CataloguePage {
    static let title = "InlineNotice"

    @State private var email = "ada@example.com"
    @State private var showsConflict = true
    @State private var reloads = 0

    var body: some View {
        CatalogueCaption("Subtle")
        ForEach(MonarchTone.allCases, id: \.self) { tone in
            MonarchInlineNotice("A one-line message.", title: "\(String(describing: tone)).", tone: tone)
        }
        CatalogueCaption("Plain, under a field")
        MonarchTextField("Email", text: $email)
        MonarchInlineNotice("This email is already invited.", tone: .warning, variant: .plain)
        CatalogueCaption("Action and dismiss")
        if showsConflict {
            MonarchInlineNotice(
                "Someone else edited this record. Reloaded \(reloads) times.",
                title: "Conflict.",
                tone: .danger,
                actionTitle: "Reload",
                action: { reloads += 1 },
                onDismiss: { showsConflict = false }
            )
        } else {
            MonarchButton("Show the notice", variant: .ghost, size: .sm) { showsConflict = true }
        }
    }
}
