import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and logic for ``MonarchAiArtifactCanvas``.
enum MonarchAiArtifactCanvasTokens {
    /// The padding around the header, the toolbar and the panel.
    static let padding: CGFloat = MonarchTokens.Space.space4
    /// The gap between the parts of the canvas.
    static let spacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between the title and the kind line.
    static let titleSpacing: CGFloat = MonarchTokens.Space.space0_5
    /// The gap between the toolbar buttons.
    static let buttonSpacing: CGFloat = MonarchTokens.Space.space2
    /// The side of the kind icon tile before Dynamic Type.
    static let iconTileSize: CGFloat = MonarchTokens.Control.md
    /// The corner radius of the kind icon tile.
    static let iconTileRadius: CGFloat = MonarchTokens.Radius.md
    /// The shortest height of the preview and code panel, so a web preview always has room.
    static let panelMinHeight: CGFloat = MonarchTokens.Space.space24 * 3
    /// The SF Symbol of the close button.
    static let closeSymbol = "xmark"
    /// The SF Symbol of the share button.
    static let shareSymbol = "square.and.arrow.up"

    /// The background of the canvas.
    static var backgroundRole: MonarchColorRole { \.bgSurface }
    /// The icon tile behind the kind symbol.
    static var iconTileRole: MonarchColorRole { \.aiSubtle }
    /// The kind symbol.
    static var iconRole: MonarchColorRole { \.aiStrong }
    /// The kind and version line and the notes.
    static var secondaryRole: MonarchColorRole { \.textMuted }

    /// The Content Security Policy of the web preview: no network request of
    /// any kind, no scripts, forms, frames or plug-ins; only inline styles and
    /// `data:` images and fonts. The same policy as Android's.
    static let contentSecurityPolicy =
        "default-src 'none'; img-src data:; style-src 'unsafe-inline'; font-src data:; "
        + "form-action 'none'; base-uri 'none'; frame-src 'none'; object-src 'none'; script-src 'none'"

    /// The patterns `inertHtml` removes, in order: the same list as Android's
    /// `MonarchAiArtifactCanvasTokens.inertHtml`. Elements that run code or
    /// load other documents (with their content), the same elements' lone
    /// tags, `<meta http-equiv>` (refresh and policy overrides), and `on…`
    /// event handler attributes.
    static let inertPatterns: [String] = [
        #"<(script|iframe|frame|object|embed|template)\b[\s\S]*?</\1\s*>"#,
        #"<(script|iframe|frame|object|embed|base|link|frameset)\b[^>]*>"#,
        #"<meta\b[^>]*http-equiv[^>]*>"#,
        #"\son[a-z]+\s*=\s*("[^"]*"|'[^']*'|[^\s>]+)"#,
    ]

    /// `html` without the elements that run code, load other documents or
    /// redirect, and without event handler attributes. Applied until nothing
    /// more is removed, so a tag split around another one cannot reassemble.
    /// Defence in depth: the web view also has JavaScript off, the Content
    /// Security Policy and a rule list that blocks every network load.
    static func inertHtml(_ html: String) -> String {
        let expressions = inertPatterns.compactMap {
            try? NSRegularExpression(pattern: $0, options: [.caseInsensitive])
        }
        var current = html
        for _ in 0..<8 {
            var next = current
            for expression in expressions {
                let range = NSRange(next.startIndex..., in: next)
                next = expression.stringByReplacingMatches(in: next, options: [], range: range, withTemplate: "")
            }
            if next == current { break }
            current = next
        }
        return current
    }

    /// Whether the kind has a built-in preview.
    static func hasBuiltInPreview(_ kind: MonarchAiArtifactKind) -> Bool {
        switch kind {
        case .document, .table, .html, .image: true
        case .code, .chart: false
        }
    }

    /// Whether the Preview tab exists: a built-in preview or one the app supplies for the kind.
    static func hasPreview(_ kind: MonarchAiArtifactKind, customKinds: Set<MonarchAiArtifactKind>) -> Bool {
        customKinds.contains(kind) || hasBuiltInPreview(kind)
    }

    /// The tab on show: the requested one, or Code when there is no preview.
    static func tab(_ requested: MonarchAiArtifactCanvasTab, hasPreview: Bool) -> MonarchAiArtifactCanvasTab {
        hasPreview ? requested : .code
    }

    /// The language of the Code tab: the artifact's, else by kind.
    static func language(_ artifact: MonarchAiCanvasArtifact) -> String? {
        if let language = artifact.language, !language.isEmpty { return language }
        switch artifact.kind {
        case .document, .table: return "markdown"
        case .chart: return "json"
        case .html: return "html"
        case .code, .image: return nil
        }
    }

    /// The address of an image artifact, or nil when it is not http or https.
    static func imageURL(_ content: String) -> URL? {
        let trimmed = content.trimmingCharacters(in: .whitespacesAndNewlines)
        guard let url = URL(string: trimmed), let scheme = url.scheme?.lowercased(),
            scheme == "http" || scheme == "https", url.host != nil
        else { return nil }
        return url
    }

    /// The version stepper shows when there are several versions and the app can switch them.
    static func showsVersions(count: Int, hasHandler: Bool) -> Bool {
        count > 1 && hasHandler
    }

    /// The document the web preview loads: the Content Security Policy
    /// first, then the untrusted content.
    static func document(_ html: String) -> String {
        "<!doctype html><html><head><meta charset=\"utf-8\">"
            + "<meta http-equiv=\"Content-Security-Policy\" content=\"\(contentSecurityPolicy)\">"
            + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
            + "</head><body>" + inertHtml(html) + "</body></html>"
    }

    /// Whether the web preview may load a navigation: only its own document
    /// (`about:blank`); links, redirects and refreshes are cancelled.
    static func allowsNavigation(to url: URL?) -> Bool {
        guard let url else { return true }
        return url.scheme?.lowercased() == "about"
    }

    /// The visible kind and version line: "Document · v3".
    static func meta(_ artifact: MonarchAiCanvasArtifact, labels: MonarchAiArtifactCanvasLabels) -> String {
        MonarchAiArtifactCardTokens.meta(
            kind: artifact.kind,
            version: artifact.version,
            kindLabel: labels.kind?(artifact.kind),
            versionLabel: labels.version?(artifact.version)
        )
    }

    /// What VoiceOver reads for the header: "Q3 summary, Document, version 3".
    static func headerLabel(_ artifact: MonarchAiCanvasArtifact, labels: MonarchAiArtifactCanvasLabels) -> String {
        let kind = labels.kind?(artifact.kind) ?? MonarchAiArtifactCardStrings.kind(artifact.kind)
        let version = labels.version?(artifact.version) ?? MonarchAiArtifactCardStrings.versionLong(artifact.version)
        return [artifact.title, kind, version].filter { !$0.isEmpty }.joined(separator: ", ")
    }

    /// The spoken name of the canvas.
    static func label(_ artifact: MonarchAiCanvasArtifact, labels: MonarchAiArtifactCanvasLabels) -> String {
        labels.label?(artifact.title) ?? MonarchAiArtifactCanvasStrings.label(artifact.title)
    }
}
