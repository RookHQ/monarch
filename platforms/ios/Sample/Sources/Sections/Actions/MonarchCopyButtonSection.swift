import MonarchUI
import SwiftUI

struct MonarchCopyButtonSection: CataloguePage {
    static let title = "CopyButton"

    @Environment(\.monarchTheme) private var theme
    @State private var lastCopied = "nothing yet"

    var body: some View {
        CatalogueCaption("Default")
        MonarchCopyButton(value: "https://example.com/hooks/1042", target: "webhook URL") { lastCopied = $0 }

        CatalogueCaption("Variants and sizes")
        HStack(spacing: theme.spacing.space2) {
            MonarchCopyButton(value: "sm", variant: .primary, size: .sm) { lastCopied = $0 }
            MonarchCopyButton(value: "md", variant: .ghost) { lastCopied = $0 }
            MonarchCopyButton(value: "lg", variant: .secondary, size: .lg) { lastCopied = $0 }
        }

        CatalogueCaption("Icon only, custom titles, disabled")
        MonarchCopyButton(value: "sk_test_1042", target: "API key", variant: .ghost, isIconOnly: true) {
            lastCopied = $0
        }
        MonarchCopyButton(
            "Copy link",
            value: "https://example.com",
            systemImage: "link",
            copiedTitle: "Link copied"
        ) { lastCopied = $0 }
        MonarchCopyButton(value: "disabled").disabled(true)
        CatalogueCaption("Sensitive: stays on this device, cleared after two minutes")
        MonarchCopyButton(value: "rk_live_example", target: "secret key", isSensitive: true) { lastCopied = $0 }

        CatalogueCaption("Last copied: \(lastCopied)")
    }
}
