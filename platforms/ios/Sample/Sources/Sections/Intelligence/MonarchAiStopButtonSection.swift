import MonarchUI
import SwiftUI

struct MonarchAiStopButtonSection: CataloguePage {
    static let title = "AI stop button"

    @Environment(\.monarchTheme) private var theme
    @State private var isStopped = false

    var body: some View {
        CatalogueCaption("Stop, then Continue")
        Text(
            isStopped
                ? "The summary covers revenue and hiring"
                : "The summary covers revenue, hiring and the three open risks…"
        )
        .monarchTextStyle(.body)
        MonarchAiStopButton(
            isStopped: isStopped,
            onContinue: { isStopped = false },
            action: { isStopped = true }
        )

        CatalogueCaption("Sizes")
        ForEach(MonarchButtonSize.allCases, id: \.self) { size in
            MonarchAiStopButton(size: size) {}
        }

        CatalogueCaption("Symbol only, disabled, stopped without Continue")
        HStack(spacing: theme.spacing.space2) {
            MonarchAiStopButton(showsLabel: false) {}
            MonarchAiStopButton {}.disabled(true)
        }
        MonarchAiStopButton(isStopped: true) {}
    }
}
