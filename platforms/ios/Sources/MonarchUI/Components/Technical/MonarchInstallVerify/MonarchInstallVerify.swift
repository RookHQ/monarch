import Foundation
import SwiftUI

/// Install a snippet on a website and wait for it to report: the code for each platform with a
/// copy button, a handoff to a developer through the share sheet, then "Waiting for your site…"
/// until the product sees the code and says "Connected on example.com".
///
/// ```swift
/// MonarchInstallVerify(
///     snippets: [
///         MonarchInstallSnippet("HTML", code: tag, language: "html", description: "Paste before </head>."),
///         MonarchInstallSnippet("npm", code: "npm install @acme/widget", language: "bash"),
///     ],
///     status: model.installStatus,
///     targets: [MonarchInstallTarget("www.example.com", lastSeen: model.lastSeen)],
///     developerInstructions: model.handoffText,
///     testURL: URL(string: "https://www.example.com/?acme-test=1")
/// )
/// ```
///
/// The app polls or listens for the first report and changes `status`; the view does no
/// networking. The snippets are ``MonarchCodeSnippetTabs``. The handoff is a `ShareLink`, so the
/// person sends the instructions by Mail, Messages or any other app, or copies them. The test page
/// opens with `Link` in the browser, and only `http` and `https` addresses are offered. A credential
/// the snippet needs (a key and its state) goes in the optional `credential` content above the
/// snippets.
///
/// Accessibility: "Install" and "Verify" are headers. The status is one element read as its title
/// and text; a change to connected is announced. Each site is one element read as its name with
/// "seen 5 minutes ago" or "Not seen" as its value, so the state is never colour alone. Touch
/// targets are at least 44 points.
@_spi(Experimental) public struct MonarchInstallVerify<Credential: View>: View {
    private let snippets: [MonarchInstallSnippet]
    private let status: MonarchInstallVerifyStatus
    private let externalSelection: Binding<String>?
    private let targets: [MonarchInstallTarget]
    private let connectedOn: [String]?
    private let developerInstructions: String?
    private let testURL: URL?
    private let isTestDisabled: Bool
    private let testHint: String?
    private let part: MonarchInstallVerifyPart
    private let now: Date?
    private let text: MonarchInstallVerifyText
    private let onConnected: (@MainActor () -> Void)?
    private let credential: Credential

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale
    @Environment(\.calendar) private var calendar
    @State private var ownSelection: String?
    @State private var hasReportedConnected = false

    /// An install and verify panel with a credential above the snippets.
    ///
    /// - Parameters:
    ///   - snippets: The ways to install, one tab each. One snippet shows without tabs.
    ///   - status: Whether the installed code has reported.
    ///   - selection: The selected snippet's id, when the app controls it.
    ///   - targets: The sites the install is expected on, with when each last reported.
    ///   - connectedOn: The sites named when connected. By default the targets that reported.
    ///   - developerInstructions: Plain text for a developer. Shows a share button.
    ///   - testURL: A page where the install can be tried. Only `http` and `https` are shown.
    ///   - isTestDisabled: Disables the test button; say why in `testHint`.
    ///   - testHint: One line under the test button.
    ///   - part: Both halves, or one for a step of a wizard.
    ///   - now: The time relative times are measured from. By default the time of drawing.
    ///   - labels: Replacements for the built-in strings.
    ///   - onConnected: Runs once, the first time this view shows the connected status.
    ///   - credential: The key or credential the snippet needs and its state, above the snippets.
    public init(
        snippets: [MonarchInstallSnippet],
        status: MonarchInstallVerifyStatus,
        selection: Binding<String>? = nil,
        targets: [MonarchInstallTarget] = [],
        connectedOn: [String]? = nil,
        developerInstructions: String? = nil,
        testURL: URL? = nil,
        isTestDisabled: Bool = false,
        testHint: String? = nil,
        part: MonarchInstallVerifyPart = .all,
        now: Date? = nil,
        labels: MonarchInstallVerifyLabels = MonarchInstallVerifyLabels(),
        onConnected: (@MainActor () -> Void)? = nil,
        @ViewBuilder credential: () -> Credential
    ) {
        self.snippets = snippets
        self.status = status
        self.externalSelection = selection
        self.targets = targets
        self.connectedOn = connectedOn
        self.developerInstructions = developerInstructions
        self.testURL = MonarchInstallVerifyTokens.safeTestURL(testURL)
        self.isTestDisabled = isTestDisabled
        self.testHint = testHint
        self.part = part
        self.now = now
        self.text = MonarchInstallVerifyText(labels: labels)
        self.onConnected = onConnected
        self.credential = credential()
    }

    private var selection: Binding<String> {
        externalSelection
            ?? Binding(
                get: { ownSelection ?? snippets.first?.id ?? "" },
                set: { ownSelection = $0 }
            )
    }

    /// The content of the view.
    public var body: some View {
        VStack(alignment: .leading, spacing: MonarchInstallVerifyTokens.sectionSpacing) {
            if part != .verify { installSection }
            if part != .install { verifySection }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .onAppear { reportConnectedIfNeeded() }
        .onChange(of: status) { old, new in
            if new == .connected && old != .connected {
                let hosts = MonarchInstallVerifyTokens.hosts(connectedOn: connectedOn, targets: targets)
                AccessibilityNotification.Announcement(text.title(.connected, hosts: hosts)).post()
            }
            reportConnectedIfNeeded()
        }
    }

    // MARK: Install

    private var installSection: some View {
        VStack(alignment: .leading, spacing: MonarchInstallVerifyTokens.spacing) {
            if part == .all { heading(text.installHeading) }
            credential
            if !snippets.isEmpty {
                let selectedID = selection.wrappedValue
                if let description = (snippets.first { $0.id == selectedID } ?? snippets.first)?.description {
                    muted(description)
                }
                MonarchCodeSnippetTabs(
                    text.platforms,
                    snippets: snippets.map {
                        MonarchCodeSnippet($0.label, code: $0.code, language: $0.language, id: $0.id)
                    },
                    selection: selection,
                    variant: .pill
                )
            }
            if let developerInstructions {
                // As on the web: a copy button and an "Email them" link with the same handoff.
                MonarchFlowLayout(spacing: MonarchInstallVerifyTokens.rowSpacing) {
                    MonarchCopyButton(
                        text.shareInstructions,
                        value: developerInstructions,
                        target: nil,
                        variant: .secondary,
                        size: .sm
                    )
                    if let mail = MonarchInstallVerifyTokens.mailURL(
                        subject: text.shareSubject,
                        body: developerInstructions
                    ) {
                        Link(text.emailDeveloper, destination: mail)
                            .buttonStyle(.monarch(.ghost, size: .sm))
                    }
                }
            }
        }
    }

    // MARK: Verify

    private var verifySection: some View {
        VStack(alignment: .leading, spacing: MonarchInstallVerifyTokens.spacing) {
            if part == .all { heading(text.verifyHeading) }
            statusBox
            if !targets.isEmpty { targetList }
            if let testURL {
                VStack(alignment: .leading, spacing: MonarchInstallVerifyTokens.rowSpacing) {
                    Link(destination: testURL) {
                        Label(text.openTest, systemImage: MonarchInstallVerifyTokens.testSymbol)
                    }
                    .buttonStyle(.monarch(.secondary))
                    .disabled(isTestDisabled)
                    .accessibilityHint(text.openTestHint)
                    if let testHint { muted(testHint) }
                }
            }
        }
    }

    private var statusBox: some View {
        let hosts = MonarchInstallVerifyTokens.hosts(connectedOn: connectedOn, targets: targets)
        let title = text.title(status, hosts: hosts)
        let body = text.body(status, targets: targets.map(\.label))
        let surface = MonarchInstallVerifyTokens.surface(status)
        return HStack(alignment: .top, spacing: MonarchInstallVerifyTokens.rowSpacing) {
            Group {
                if let symbol = MonarchInstallVerifyTokens.symbol(status) {
                    MonarchIcon(systemName: symbol)
                } else {
                    // The web waits with a solid accent dot, not a spinner.
                    Circle()
                        .fill(theme.colors.accentSolid)
                        .frame(width: 10, height: 10)
                        .padding(.vertical, 3)
                }
            }
            .foregroundStyle(theme.colors[role: surface.foreground])
            .accessibilityHidden(true)
            VStack(alignment: .leading, spacing: MonarchInstallVerifyTokens.textSpacing) {
                Text(title)
                    .monarchTextStyle(.bodyStrong)
                    .foregroundStyle(theme.colors.textDefault)
                Text(body)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors[role: MonarchInstallVerifyTokens.muted])
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(MonarchInstallVerifyTokens.statusPadding)
        .monarchSurface(surface, in: MonarchShapes.container)
        .accessibilityElement(children: .combine)
        .accessibilityAddTraits(status == .waiting ? .updatesFrequently : [])
    }

    private var targetList: some View {
        let reference = now ?? Date()
        return VStack(alignment: .leading, spacing: MonarchInstallVerifyTokens.rowSpacing) {
            ForEach(targets) { target in
                HStack(alignment: .firstTextBaseline, spacing: MonarchInstallVerifyTokens.rowSpacing) {
                    if target.lastSeen == nil {
                        // A site that has not reported in is a hollow ring, as on the web.
                        Circle()
                            .strokeBorder(theme.colors.borderStrong, lineWidth: 1)
                            .frame(width: 8, height: 8)
                            .accessibilityHidden(true)
                    } else {
                        MonarchStatusDot(
                            text.targetValue(target.lastSeen, now: reference, locale: locale, calendar: calendar),
                            tone: MonarchInstallVerifyTokens.targetTone(target.lastSeen),
                            isLabelHidden: true
                        )
                        .accessibilityHidden(true)
                    }
                    Text(target.label)
                        .monarchTextStyle(.code)
                        .foregroundStyle(theme.colors.textDefault)
                    Text(text.seen(target.lastSeen, now: reference, locale: locale, calendar: calendar))
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors[role: MonarchInstallVerifyTokens.muted])
                        .accessibilityHidden(true)
                }
                .accessibilityElement(children: .combine)
                .accessibilityValue(
                    text.targetValue(target.lastSeen, now: reference, locale: locale, calendar: calendar)
                )
            }
        }
        .accessibilityElement(children: .contain)
        .accessibilityLabel(text.targets)
    }

    private func heading(_ string: String) -> some View {
        Text(string)
            .monarchTextStyle(.bodyStrong)
            .foregroundStyle(theme.colors.textDefault)
            .accessibilityAddTraits(.isHeader)
    }

    private func muted(_ string: String) -> some View {
        Text(string)
            .monarchTextStyle(.caption)
            .foregroundStyle(theme.colors[role: MonarchInstallVerifyTokens.muted])
    }

    private func reportConnectedIfNeeded() {
        guard MonarchInstallVerifyTokens.shouldReportConnected(status, alreadyReported: hasReportedConnected) else {
            return
        }
        hasReportedConnected = true
        onConnected?()
    }
}

extension MonarchInstallVerify where Credential == EmptyView {
    /// An install and verify panel.
    ///
    /// - Parameters:
    ///   - snippets: The ways to install, one tab each. One snippet shows without tabs.
    ///   - status: Whether the installed code has reported.
    ///   - selection: The selected snippet's id, when the app controls it.
    ///   - targets: The sites the install is expected on, with when each last reported.
    ///   - connectedOn: The sites named when connected. By default the targets that reported.
    ///   - developerInstructions: Plain text for a developer. Shows a share button.
    ///   - testURL: A page where the install can be tried. Only `http` and `https` are shown.
    ///   - isTestDisabled: Disables the test button; say why in `testHint`.
    ///   - testHint: One line under the test button.
    ///   - part: Both halves, or one for a step of a wizard.
    ///   - now: The time relative times are measured from. By default the time of drawing.
    ///   - labels: Replacements for the built-in strings.
    ///   - onConnected: Runs once, the first time this view shows the connected status.
    public init(
        snippets: [MonarchInstallSnippet],
        status: MonarchInstallVerifyStatus,
        selection: Binding<String>? = nil,
        targets: [MonarchInstallTarget] = [],
        connectedOn: [String]? = nil,
        developerInstructions: String? = nil,
        testURL: URL? = nil,
        isTestDisabled: Bool = false,
        testHint: String? = nil,
        part: MonarchInstallVerifyPart = .all,
        now: Date? = nil,
        labels: MonarchInstallVerifyLabels = MonarchInstallVerifyLabels(),
        onConnected: (@MainActor () -> Void)? = nil
    ) {
        self.init(
            snippets: snippets,
            status: status,
            selection: selection,
            targets: targets,
            connectedOn: connectedOn,
            developerInstructions: developerInstructions,
            testURL: testURL,
            isTestDisabled: isTestDisabled,
            testHint: testHint,
            part: part,
            now: now,
            labels: labels,
            onConnected: onConnected,
            credential: { EmptyView() }
        )
    }
}

#if DEBUG
    #Preview("MonarchInstallVerify") {
        let now = Date(timeIntervalSinceReferenceDate: 800_000_000)
        let snippets = [
            MonarchInstallSnippet(
                "HTML",
                code: "<script src=\"https://cdn.example.com/w.js\" async></script>",
                language: "html",
                description: "Paste before the closing head tag."
            ),
            MonarchInstallSnippet("npm", code: "npm install @example/widget", language: "bash"),
        ]
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchInstallVerify(
                    snippets: snippets,
                    status: .waiting,
                    targets: [MonarchInstallTarget("www.example.com")],
                    developerInstructions: "Add the widget tag to every page.",
                    testURL: URL(string: "https://www.example.com"),
                    now: now
                )
                MonarchInstallVerify(
                    snippets: snippets,
                    status: .connected,
                    targets: [MonarchInstallTarget("www.example.com", lastSeen: now.addingTimeInterval(-300))],
                    part: .verify,
                    now: now
                )
                MonarchInstallVerify(snippets: snippets, status: .unavailable, part: .verify)
            }
        }
    }
#endif
