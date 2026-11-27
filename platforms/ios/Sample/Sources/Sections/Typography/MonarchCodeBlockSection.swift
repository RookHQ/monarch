import MonarchUI
import SwiftUI

struct MonarchCodeBlockSection: CataloguePage {
    static let title = "Code block"

    @State private var isWrapped = false
    @State private var lastCopy = "Nothing copied yet."

    private let request = """
        curl -X POST https://api.example.com/v1/items \\
          -H "Authorization: Bearer $TOKEN" \\
          -d '{"name": "Quarterly report", "shared": true}'
        """

    private let typescript = """
        import { createClient } from "@example/sdk";

        // Create one client and reuse it.
        export const client = createClient({ retries: 3, timeout: 2500 });
        """

    private let diff = """
        @@ -1,3 +1,3 @@
         import express from "express";
        -const port = 3000;
        +const port = Number(process.env.PORT ?? 8080);
         app.listen(port);
        """

    var body: some View {
        MonarchCodeBlock(
            request,
            language: "bash",
            title: "Create an item",
            isWrapped: $isWrapped,
            onCopy: { _ in lastCopy = "Copied the request." }
        )
        Text(lastCopy)
            .monarchTextStyle(.caption)

        CatalogueCaption("Line numbers")
        MonarchCodeBlock(typescript, language: "ts", title: "client.ts", showsLineNumbers: true)

        CatalogueCaption("Diff")
        MonarchCodeBlock(diff, language: "diff", title: "server.ts")

        CatalogueCaption("Markup and JSON")
        MonarchCodeBlock("<a href=\"/docs\" class=\"link\">Docs</a>", language: "html")
        MonarchCodeBlock("{\"id\": 42, \"active\": true, \"tags\": [\"beta\"]}", language: "json")

        CatalogueCaption("Plain text, no header")
        MonarchCodeBlock("Output goes here.", showsWrapToggle: false, isCopyable: false)
    }
}
