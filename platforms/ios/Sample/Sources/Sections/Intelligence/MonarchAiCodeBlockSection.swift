import MonarchUI
import SwiftUI

struct MonarchAiCodeBlockSection: CataloguePage {
    static let title = "AI code block"

    private static let full =
        "func greet(_ name: String) -> String {\n    \"Hello, \\(name)\"\n}\n\nprint(greet(\"Ana\"))"

    @State private var streamed = ""
    @State private var isStreaming = false
    @State private var result = "Nothing applied yet"

    var body: some View {
        CatalogueCaption("A suggested edit")
        MonarchAiCodeBlock(
            "@@ -1,3 +1,3 @@\n import express from 'express';\n-const port = 3000;\n+const port = Number(process.env.PORT ?? 8080);\n app.listen(port);",
            language: "diff",
            title: "server.ts",
            onInsert: { _ in result = "Inserted server.ts" },
            onApply: { _ in result = "Applied to server.ts" }
        )
        Text(result).monarchTextStyle(.caption)

        CatalogueCaption("Streaming: Apply waits for the whole code")
        MonarchAiCodeBlock(
            streamed,
            language: "swift",
            showsLineNumbers: true,
            isIncomplete: isStreaming,
            onApply: { _ in result = "Applied the Swift code" }
        )
        MonarchButton("Stream again", variant: .secondary, size: .sm, isLoading: isStreaming) { stream() }
            .onAppear { if streamed.isEmpty { streamed = Self.full } }
    }

    private func stream() {
        streamed = ""
        isStreaming = true
        Task {
            for character in Self.full {
                streamed.append(character)
                try? await Task.sleep(for: .milliseconds(20))
            }
            isStreaming = false
        }
    }
}
