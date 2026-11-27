import Foundation
@_spi(Experimental) import MonarchUI
import SwiftUI

struct MonarchAiArtifactCanvasSection: CataloguePage {
    static let title = "AI artifact canvas"

    private static let created = Date(timeIntervalSinceReferenceDate: 800_000_000)
    private static let contents = [
        1: "# Q3 summary\n\nRevenue grew in every region.\n\n- New customers: 140\n- Churn: 2.1%",
        2:
            "# Q3 summary\n\nRevenue grew **12%**, led by the north region.\n\n- New customers: 140\n- Churn: 2.1%\n\n## Next steps\n\nReview pricing before the planning meeting.",
    ]

    private static let page = MonarchAiCanvasArtifact(
        id: "page",
        kind: .html,
        title: "Event landing page",
        version: 1,
        content:
            "<main style=\"font-family: system-ui; padding: 1rem\"><h1>Team offsite</h1><p>Two days of planning and workshops.</p><button>Register</button><script>document.body.textContent = 'Scripts never run here.'</script></main>"
    )

    private static let code = MonarchAiCanvasArtifact(
        id: "code",
        kind: .code,
        title: "parse-invoice.swift",
        version: 1,
        content:
            "func total(in text: String) -> Double {\n    Double(text.split(separator: \":\").last ?? \"\") ?? 0\n}",
        language: "swift"
    )

    @State private var shown: MonarchAiCanvasArtifact?
    @State private var version = 2

    private var document: MonarchAiCanvasArtifact {
        MonarchAiCanvasArtifact(
            id: "q3",
            kind: .document,
            title: "Q3 summary",
            version: version,
            content: Self.contents[version] ?? ""
        )
    }

    private var versions: [MonarchAiArtifactVersion] {
        [
            MonarchAiArtifactVersion(1, createdAt: Self.created, content: Self.contents[1]),
            MonarchAiArtifactVersion(
                2,
                createdAt: Self.created.addingTimeInterval(1_200),
                label: "Added next steps",
                content: Self.contents[2]
            ),
        ]
    }

    var body: some View {
        CatalogueCaption("Open an artifact in a sheet")
        MonarchAiArtifactCard("Q3 summary", kind: .document, version: version, isActive: shown?.id == "q3") {
            shown = document
        }
        MonarchAiArtifactCard("Event landing page", kind: .html, version: 1, isActive: shown?.id == "page") {
            shown = Self.page
        }
        MonarchAiArtifactCard("parse-invoice.swift", kind: .code, version: 1, isActive: shown?.id == "code") {
            shown = Self.code
        }
        .sheet(item: $shown) { artifact in
            MonarchAiArtifactCanvas(
                artifact.id == "q3" ? document : artifact,
                versions: artifact.id == "q3" ? versions : [],
                saveTitle: "Save to library",
                onVersionChange: { version = $0 },
                onSave: { _ in try? await Task.sleep(for: .milliseconds(800)) },
                onClose: { shown = nil }
            )
            .presentationDetents([.large])
        }

        CatalogueCaption("Inline")
        MonarchAiArtifactCanvas(Self.code)
            .frame(minHeight: 360)
    }
}
