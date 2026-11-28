import MonarchUI
import SwiftUI

struct MonarchStatusDotSection: CataloguePage {
    static let title = "Status dot"

    @State private var isDeploying = true

    var body: some View {
        ForEach(MonarchStatusDotSize.allCases, id: \.self) { size in
            CatalogueCaption(String(describing: size))
            ForEach(MonarchTone.allCases, id: \.self) { tone in
                MonarchStatusDot(String(describing: tone), tone: tone, size: size)
            }
        }
        CatalogueCaption("Pulsing while something is in progress")
        MonarchStatusDot(
            isDeploying ? "Deploying" : "Deployed",
            tone: isDeploying ? .info : .success,
            size: .md,
            isPulsing: isDeploying
        )
        Toggle("In progress", isOn: $isDeploying)
        CatalogueCaption("Label hidden, read by VoiceOver only")
        MonarchStatusDot("Online", tone: .success, isLabelHidden: true)
    }
}
