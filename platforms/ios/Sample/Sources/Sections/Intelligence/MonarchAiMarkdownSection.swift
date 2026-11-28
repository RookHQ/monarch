import MonarchUI
import SwiftUI

struct MonarchAiMarkdownSection: CataloguePage {
    static let title = "AI markdown"

    private static let chunks = [
        "## Release plan\nShip the **beta** on *Monday*, then read the [notes](https://example.com/notes)[^1].\n\n",
        "1. Freeze the branch\n2. Run `make release`\n   - Check the build\n\n",
        "- [x] Write the changelog\n- [ ] Tell support\n\n> Small steps every day add up.\n\n",
        "| Plan | Seats | Price |\n| :--- | :---: | ----: |\n| Team | 10 | $90 |\n| Business | 50 | $400 |\n\n",
        "```swift\nlet total = items.reduce(0, +)\n",
        "```\n\n---\n\n[^1]: Updated every Friday.",
    ]

    @State private var shown = 1

    private var text: String { Self.chunks.prefix(shown).joined() }

    var body: some View {
        CatalogueCaption("A streamed answer")
        MonarchAiMarkdown(text, isStreaming: shown < Self.chunks.count)
        HStack {
            Button("Stream the next part") { shown = min(shown + 1, Self.chunks.count) }
                .buttonStyle(.monarch(.secondary, size: .sm))
                .disabled(shown == Self.chunks.count)
            Button("Start again") { shown = 1 }
                .buttonStyle(.monarch(.ghost, size: .sm))
        }

        CatalogueCaption("Untrusted links and raw HTML")
        MonarchAiMarkdown(
            "Sign in at [paypal.com](https://evil.example/login). [This](javascript:alert(1)) is not a link, "
                + "and <b>HTML</b> stays text. Mail <ada@example.com> or visit www.example.com."
        )

        CatalogueCaption("Code blocks drawn by the app")
        MonarchAiMarkdown("Einstein wrote:\n\n$$\nE = mc^2\n$$")
            .codeBlock { language, code, _ in
                Text(language == "math" ? code : language + ": " + code)
                    .monarchTextStyle(.code)
            }
    }
}
