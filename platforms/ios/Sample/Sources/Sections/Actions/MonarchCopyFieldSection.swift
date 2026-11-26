import MonarchUI
import SwiftUI

struct MonarchCopyFieldSection: CataloguePage {
    static let title = "CopyField"

    @State private var lastCopied = ""

    var body: some View {
        CatalogueCaption("Single line")
        MonarchCopyField("Webhook URL", value: "https://example.com/hooks/workspace/42/events") { lastCopied = $0 }
        CatalogueCaption("Small, icon only")
        MonarchCopyField("SPF record", value: "v=spf1 include:_spf.example.com ~all", size: .sm, isIconOnly: true)
        CatalogueCaption("Multi-line")
        MonarchCopyField(
            "Embed code",
            value: "<script src=\"https://example.com/widget.js\"\n  data-workspace=\"ws_01HZX4\" async></script>",
            isMultiline: true
        )
        CatalogueCaption("Disabled")
        MonarchCopyField("Workspace ID", value: "ws_01HZX4").disabled(true)
        if !lastCopied.isEmpty {
            Text("Copied: \(lastCopied)").monarchTextStyle(.caption)
        }
    }
}
