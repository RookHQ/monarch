import Foundation
import SwiftUI
import Testing

@_spi(Experimental) @testable import MonarchUI

private let created = Date(timeIntervalSinceReferenceDate: 800_000_000)

private let document = MonarchAiCanvasArtifact(
    id: "q3",
    kind: .document,
    title: "Q3 summary",
    version: 2,
    content: "# Q3 summary\n\nRevenue grew **12%**, led by the north region.\n\n- New customers: 140\n- Churn: 2.1%"
)

private let code = MonarchAiCanvasArtifact(
    id: "parser",
    kind: .code,
    title: "parse-invoice.swift",
    version: 1,
    content: "func total(_ text: String) -> Double {\n    Double(text) ?? 0\n}",
    language: "swift"
)

private let page = MonarchAiCanvasArtifact(
    id: "page",
    kind: .html,
    title: "Event landing page",
    version: 1,
    content:
        "<main><h1>Team offsite</h1><p>Two days of planning.</p><script>document.body.textContent = 'x'</script></main>"
)

private let versions = [
    MonarchAiArtifactVersion(1, createdAt: created, content: "# Q3 summary\n\nRevenue grew."),
    MonarchAiArtifactVersion(
        2,
        createdAt: created.addingTimeInterval(1_200),
        label: "Added numbers",
        content: document.content
    ),
]

enum MonarchAiArtifactCanvasFixtures: ComponentFixtures {
    static let id = "ai-artifact-canvas"
    static let fixtures = [
        Fixture("document, versions, every action") {
            MonarchAiArtifactCanvas(
                document,
                versions: versions,
                saveTitle: "Save to library",
                onVersionChange: { _ in },
                onCopy: { _ in },
                onSave: { _ in },
                onClose: {}
            )
        },
        Fixture("document, code tab") {
            MonarchAiArtifactCanvas(document, tab: .constant(.code), isShareable: false)
        },
        Fixture("code only") {
            MonarchAiArtifactCanvas(code, onClose: {})
        },
        Fixture("html, sandboxed") {
            MonarchAiArtifactCanvas(page)
        },
        Fixture("unsafe image") {
            MonarchAiArtifactCanvas(
                MonarchAiCanvasArtifact(
                    id: "img",
                    kind: .image,
                    title: "Chart",
                    version: 1,
                    content: "javascript:alert(1)"
                )
            )
        },
        Fixture("chart with an app preview") {
            MonarchAiArtifactCanvas(
                MonarchAiCanvasArtifact(id: "c", kind: .chart, title: "Revenue", version: 3, content: "{\"north\": 12}")
            )
            .preview(for: [.chart]) { artifact in
                Text("Chart of \(artifact.title)")
            }
        },
        Fixture("long title, custom labels") {
            MonarchAiArtifactCanvas(
                MonarchAiCanvasArtifact(
                    id: "t",
                    kind: .table,
                    title: "Quarterly revenue by region, product line and sales channel for the planning meeting",
                    version: 12,
                    content: "| Region | Revenue |\n| --- | --- |\n| North | 12 |"
                ),
                labels: MonarchAiArtifactCanvasLabels(copy: "Copy table", share: "Send"),
                onClose: {}
            )
        },
    ]
}

@Suite("MonarchAiArtifactCanvas")
struct MonarchAiArtifactCanvasTests {
    private typealias Tokens = MonarchAiArtifactCanvasTokens
    private let labels = MonarchAiArtifactCanvasLabels()

    @Test("Documents, tables, web pages and images have a preview; code and charts only with the app's")
    func previews() {
        #expect(MonarchAiArtifactKind.allCases.filter(Tokens.hasBuiltInPreview) == [.document, .table, .image, .html])
        #expect(!Tokens.hasPreview(.chart, customKinds: []))
        #expect(Tokens.hasPreview(.chart, customKinds: [.chart]))
        #expect(Tokens.hasPreview(.document, customKinds: []))
    }

    @Test("Without a preview the Code view shows")
    func tab() {
        #expect(Tokens.tab(.preview, hasPreview: true) == .preview)
        #expect(Tokens.tab(.code, hasPreview: true) == .code)
        #expect(Tokens.tab(.preview, hasPreview: false) == .code)
    }

    @Test("The code language comes from the artifact, else from the kind")
    func language() {
        #expect(Tokens.language(code) == "swift")
        #expect(Tokens.language(document) == "markdown")
        #expect(Tokens.language(page) == "html")
        var chart = code
        chart.kind = .chart
        chart.language = ""
        #expect(Tokens.language(chart) == "json")
        chart.kind = .code
        chart.language = nil
        #expect(Tokens.language(chart) == nil)
    }

    @Test("Only http and https image addresses load")
    func images() {
        #expect(Tokens.imageURL(" https://example.com/a.png ")?.host == "example.com")
        #expect(Tokens.imageURL("HTTP://example.com/a.png") != nil)
        #expect(Tokens.imageURL("javascript:alert(1)") == nil)
        #expect(Tokens.imageURL("file:///etc/hosts") == nil)
        #expect(Tokens.imageURL("data:image/png;base64,AAAA") == nil)
        #expect(Tokens.imageURL("not a url") == nil)
    }

    @Test("The web preview blocks the network and every navigation away")
    func sandbox() {
        let html = Tokens.document("<p>Hi</p>")
        #expect(
            html.hasPrefix(
                "<!doctype html><html><head><meta charset=\"utf-8\"><meta http-equiv=\"Content-Security-Policy\""
            )
        )
        #expect(html.contains("default-src 'none'"))
        #expect(html.contains("img-src data:"))
        #expect(html.contains("script-src 'none'"))
        #expect(html.hasSuffix("<body><p>Hi</p></body></html>"))
        #expect(Tokens.allowsNavigation(to: URL(string: "about:blank")))
        #expect(Tokens.allowsNavigation(to: nil))
        #expect(!Tokens.allowsNavigation(to: URL(string: "https://example.com")))
        #expect(!Tokens.allowsNavigation(to: URL(string: "file:///etc/hosts")))
        #expect(!Tokens.allowsNavigation(to: URL(string: "javascript:alert(1)")))
    }

    @Test("The stepper needs two versions and a handler")
    func versionsShown() {
        #expect(Tokens.showsVersions(count: 2, hasHandler: true))
        #expect(!Tokens.showsVersions(count: 1, hasHandler: true))
        #expect(!Tokens.showsVersions(count: 3, hasHandler: false))
    }

    @Test("The header reads the title, kind and version in words")
    func header() {
        #expect(Tokens.meta(document, labels: labels) == "Document · v2")
        #expect(Tokens.headerLabel(document, labels: labels) == "Q3 summary, Document, version 2")
        #expect(Tokens.label(page, labels: labels) == "Artifact: Event landing page")
        let custom = MonarchAiArtifactCanvasLabels(
            label: { "Open: \($0)" },
            kind: { _ in "Page" },
            version: { "#\($0)" }
        )
        #expect(Tokens.meta(page, labels: custom) == "Page · #1")
        #expect(Tokens.headerLabel(page, labels: custom) == "Event landing page, Page, #1")
        #expect(Tokens.label(page, labels: custom) == "Open: Event landing page")
    }

    @Test("Colour roles")
    func roles() {
        #expect(Tokens.backgroundRole == \.bgSurface)
        #expect(Tokens.iconRole == \.aiStrong)
        #expect(Tokens.iconTileRole == \.aiSubtle)
        #expect(Tokens.secondaryRole == \.textMuted)
    }

    @Test("Built-in strings resolve")
    func builtIn() {
        let all = [
            MonarchAiArtifactCanvasStrings.label("x"), MonarchAiArtifactCanvasStrings.preview,
            MonarchAiArtifactCanvasStrings.code, MonarchAiArtifactCanvasStrings.views,
            MonarchAiArtifactCanvasStrings.close, MonarchAiArtifactCanvasStrings.copy,
            MonarchAiArtifactCanvasStrings.copied, MonarchAiArtifactCanvasStrings.share,
            MonarchAiArtifactCanvasStrings.save, MonarchAiArtifactCanvasStrings.frameTitle("x"),
            MonarchAiArtifactCanvasStrings.sandboxNote, MonarchAiArtifactCanvasStrings.unsafeImage,
            MonarchAiArtifactCanvasStrings.noPreview,
        ]
        for string in all {
            #expect(!string.hasPrefix("monarch."))
        }
        #expect(MonarchAiArtifactCanvasStrings.frameTitle("Page") == "Preview of Page")
    }
}
