@_spi(Experimental) import MonarchUI
import SwiftUI

struct MonarchInstallVerifySection: CataloguePage {
    static let title = "Install verify"

    private static let snippets = [
        MonarchInstallSnippet(
            "HTML",
            code: "<script src=\"https://cdn.example.com/widget.js\" data-site=\"site_42\" async></script>",
            language: "html",
            description: "Paste before the closing head tag on every page."
        ),
        MonarchInstallSnippet(
            "React",
            code: "import { Widget } from \"@example/widget\";\n\n<Widget site=\"site_42\" />",
            language: "jsx",
            description: "Render once in your root layout."
        ),
        MonarchInstallSnippet(
            "WordPress",
            code: "Plugins > Add New > \"Example Widget\"",
            description: "Install the plugin, then paste the site ID."
        ),
    ]

    @State private var status = MonarchInstallVerifyStatus.waiting
    @State private var lastSeen: Date?
    @State private var connections = 0

    var body: some View {
        Picker("Status", selection: $status) {
            ForEach(MonarchInstallVerifyStatus.allCases, id: \.self) { status in
                Text(status.rawValue.capitalized).tag(status)
            }
        }
        .pickerStyle(.segmented)
        .onChange(of: status) { _, new in lastSeen = new == .connected ? Date() : nil }
        MonarchInstallVerify(
            snippets: Self.snippets,
            status: status,
            targets: [MonarchInstallTarget("www.example.com", lastSeen: lastSeen)],
            developerInstructions: "Please add this tag to every page of www.example.com:\n\n\(Self.snippets[0].code)",
            testURL: URL(string: "https://www.example.com/?widget-test=1"),
            isTestDisabled: status != .connected,
            testHint: status == .connected ? nil : "Available once the site is connected.",
            onConnected: { connections += 1 },
            credential: { MonarchCopyField("Site ID", value: "site_42") }
        )
        Text("onConnected ran \(connections) times").monarchTextStyle(.caption)
    }
}
