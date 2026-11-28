import Foundation
import SwiftUI

/// The claims of an AI answer with the passages that back or contradict
/// them: a summary, a filter for the claims that need checking, and one
/// expandable row per claim with its support level, a note, and each
/// passage with its source, stance and actions.
///
/// ```swift
/// MonarchAiClaimSources(
///     answer.claims,
///     sources: answer.sources.map { MonarchAiClaimSource($0) },
///     onFlag: { claimID in report(claimID) }
/// )
/// ```
///
/// Each claim is a `DisclosureGroup` in the Monarch style. Its header shows
/// the support level as an icon and in words, and the number of passages.
/// Opened, a claim that is not fully supported starts with a note, then
/// lists its passages: the quotation, the source title, kind, location and
/// publication date, a Supports or Contradicts badge, Open source (the
/// app's handler, or the system for a safe web address) and Copy quote. A
/// passage whose source is missing reads "Unknown source". The filter and
/// the open claims are internal unless bindings are passed. The view does
/// not scroll.
///
/// Accessibility: the heading is a header and the summary follows it. Each
/// claim header is one button read as the claim, then the support level and
/// passage count, with "Expanded" or "Collapsed". Support levels have their
/// own icons and words, so they never depend on colour. Each passage is one
/// element read as its stance, quote, source and detail, with its buttons
/// separate and named after the source. Changing the filter announces how
/// many claims are shown.
@_spi(Experimental) public struct MonarchAiClaimSources: View {
    private let claims: [MonarchAiClaim]
    private let sources: [MonarchAiClaimSource]
    private let filter: Binding<MonarchAiClaimSourcesFilter>?
    private let expandedIDs: Binding<Set<String>>?
    private let locale: Locale?
    private let calendar: Calendar?
    private let labels: MonarchAiClaimSourcesLabels
    private let onOpenSource: (@MainActor (MonarchAiClaimSource, MonarchAiClaimEvidence) -> Void)?
    private let onFlag: (@MainActor (String) -> Void)?
    private let onVerify: (@MainActor (String) -> Void)?
    private let onCopyQuote: (@MainActor (MonarchAiClaimEvidence) -> Void)?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var environmentLocale
    @Environment(\.calendar) private var environmentCalendar
    @Environment(\.timeZone) private var timeZone
    @Environment(\.openURL) private var openURL
    @State private var filterState = MonarchAiClaimSourcesFilter.all
    @State private var expandedState: Set<String> = []

    private typealias Tokens = MonarchAiClaimSourcesTokens
    private typealias Strings = MonarchAiClaimSourcesStrings

    /// A claim sources view.
    ///
    /// - Parameters:
    ///   - claims: The claims of the answer, in the order the answer makes them.
    ///   - sources: The sources the passages point at.
    ///   - filter: Which claims are shown. Internal when `nil`.
    ///   - expandedIDs: The ids of the open claims. Internal when `nil`.
    ///   - locale: The locale of dates and counts. Default: the environment's.
    ///   - calendar: The calendar and time zone of dates. Default: the environment's.
    ///   - labels: Replacements for the built-in strings.
    ///   - onOpenSource: Opens a passage's source. Without it a safe web address opens in the system.
    ///   - onFlag: Reports a problem with a claim, given its id. Shows Report a problem on each open claim.
    ///   - onVerify: Checks an unchecked claim, given its id. Shows Check again on unchecked claims.
    ///   - onCopyQuote: Runs after a passage is copied.
    public init(
        _ claims: [MonarchAiClaim],
        sources: [MonarchAiClaimSource],
        filter: Binding<MonarchAiClaimSourcesFilter>? = nil,
        expandedIDs: Binding<Set<String>>? = nil,
        locale: Locale? = nil,
        calendar: Calendar? = nil,
        labels: MonarchAiClaimSourcesLabels = MonarchAiClaimSourcesLabels(),
        onOpenSource: (@MainActor (MonarchAiClaimSource, MonarchAiClaimEvidence) -> Void)? = nil,
        onFlag: (@MainActor (String) -> Void)? = nil,
        onVerify: (@MainActor (String) -> Void)? = nil,
        onCopyQuote: (@MainActor (MonarchAiClaimEvidence) -> Void)? = nil
    ) {
        self.claims = claims
        self.sources = sources
        self.filter = filter
        self.expandedIDs = expandedIDs
        self.locale = locale
        self.calendar = calendar
        self.labels = labels
        self.onOpenSource = onOpenSource
        self.onFlag = onFlag
        self.onVerify = onVerify
        self.onCopyQuote = onCopyQuote
    }

    private var resolvedLocale: Locale { locale ?? environmentLocale }

    private var resolvedCalendar: Calendar {
        if let calendar { return calendar }
        var calendar = environmentCalendar
        calendar.timeZone = timeZone
        return calendar
    }

    private var filterBinding: Binding<MonarchAiClaimSourcesFilter> { filter ?? $filterState }
    private var expandedBinding: Binding<Set<String>> { expandedIDs ?? $expandedState }

    /// The content of the view.
    public var body: some View {
        let counts = MonarchAiClaimCounts.of(claims)
        let shown = Tokens.shown(claims, filter: filterBinding.wrappedValue)
        VStack(alignment: .leading, spacing: Tokens.sectionSpacing) {
            VStack(alignment: .leading, spacing: Tokens.lineSpacing) {
                Text(labels.heading ?? Strings.heading)
                    .monarchTextStyle(.subheading)
                    .foregroundStyle(theme.colors.textDefault)
                    .accessibilityAddTraits(.isHeader)
                Text(labels.summary?(counts) ?? Strings.summary(counts))
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors.textMuted)
                    .fixedSize(horizontal: false, vertical: true)
            }
            if Tokens.showsFilter(counts) {
                // The counts are shown in the segments' count pills, as in the web.
                MonarchSegmentedControl(
                    labels.filterLabel ?? Strings.filterLabel,
                    selection: filterBinding,
                    items: [
                        MonarchSegmentedItem(
                            labels.all ?? Strings.all,
                            value: MonarchAiClaimSourcesFilter.all,
                            count: counts.total.formatted(.number.locale(resolvedLocale))
                        ),
                        MonarchSegmentedItem(
                            labels.needsChecking ?? Strings.needsChecking,
                            value: MonarchAiClaimSourcesFilter.needsChecking,
                            count: counts.needsChecking.formatted(.number.locale(resolvedLocale))
                        ),
                    ]
                )
                .onChange(of: filterBinding.wrappedValue) { _, newValue in
                    let count = Tokens.shown(claims, filter: newValue).count
                    AccessibilityNotification.Announcement(
                        labels.showing?(count, counts.total) ?? Strings.showing(count, counts.total)
                    ).post()
                }
            }
            if claims.isEmpty {
                emptyText(labels.empty ?? Strings.empty)
            } else if shown.isEmpty {
                emptyText(labels.noneNeedChecking ?? Strings.noneNeedChecking)
            } else {
                VStack(alignment: .leading, spacing: Tokens.claimSpacing) {
                    ForEach(shown) { claim in
                        claimRow(claim)
                    }
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func emptyText(_ text: String) -> some View {
        Text(text)
            .monarchTextStyle(.body)
            .foregroundStyle(theme.colors.textMuted)
            .fixedSize(horizontal: false, vertical: true)
    }

    private func isExpanded(_ id: String) -> Binding<Bool> {
        Binding(
            get: { expandedBinding.wrappedValue.contains(id) },
            set: { isOpen in
                if isOpen {
                    expandedBinding.wrappedValue.insert(id)
                } else {
                    expandedBinding.wrappedValue.remove(id)
                }
            }
        )
    }

    private func supportText(_ support: MonarchAiClaimSupport) -> String {
        labels.support?(support) ?? Strings.support(support)
    }

    /// One claim as a card with a status stripe, as on the web: the chevron and the claim, then the status
    /// badge and the passage count, then the passages when open.
    private func claimRow(_ claim: MonarchAiClaim) -> some View {
        let support = supportText(claim.support)
        let passages = labels.passages?(claim.evidence.count) ?? Strings.passages(claim.evidence.count)
        let open = isExpanded(claim.id)
        let tone = Tokens.tone(claim.support)
        let stripe =
            tone == .neutral
            ? theme.colors.borderDefault : theme.colors[role: MonarchTonePalette.palette(for: tone).border]
        let shape = RoundedRectangle(cornerRadius: Tokens.passageRadius, style: .continuous)
        return VStack(alignment: .leading, spacing: 0) {
            Button {
                open.wrappedValue.toggle()
            } label: {
                HStack(alignment: .top, spacing: Tokens.inlineSpacing) {
                    MonarchIcon(systemName: open.wrappedValue ? "chevron.down" : "chevron.right", size: .sm)
                        .foregroundStyle(theme.colors.textMuted)
                        .frame(height: 24)
                    VStack(alignment: .leading, spacing: Tokens.inlineSpacing) {
                        Text(claim.text)
                            .monarchTextStyle(.bodyStrong, weight: .medium)
                            .foregroundStyle(theme.colors.textDefault)
                            .multilineTextAlignment(.leading)
                        HStack(spacing: Tokens.inlineSpacing) {
                            MonarchBadge(
                                support,
                                tone: tone,
                                systemImage: Tokens.badgeSymbol(claim.support),
                                size: .sm
                            )
                            Text(passages)
                                .monarchTextStyle(.caption)
                                .foregroundStyle(theme.colors.textMuted)
                        }
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                }
                .padding(Tokens.passagePadding)
                .frame(minHeight: MonarchAccessibility.minimumHitTarget)
                .contentShape(Rectangle())
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(claim.text)
                .accessibilityValue(Tokens.claimValue(support: support, passages: passages))
            }
            .buttonStyle(.plain)
            .accessibilityAddTraits(.isButton)
            if open.wrappedValue {
                MonarchDivider()
                claimBody(claim)
                    .padding(Tokens.passagePadding)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(shape.fill(theme.colors.bgSurface))
        .overlay(shape.strokeBorder(theme.colors.borderSubtle, lineWidth: MonarchShapes.borderWidth))
        .overlay(alignment: .leading) {
            Rectangle().fill(stripe).frame(width: 3)
        }
        .clipShape(shape)
    }

    @ViewBuilder private func claimBody(_ claim: MonarchAiClaim) -> some View {
        VStack(alignment: .leading, spacing: Tokens.bodySpacing) {
            if claim.support != .supported {
                Text(labels.note?(claim.support) ?? Strings.note(claim.support))
                    .monarchTextStyle(.bodySmall)
                    .foregroundStyle(theme.colors.textDefault)
                    .fixedSize(horizontal: false, vertical: true)
            }
            if claim.evidence.isEmpty, claim.support == .supported {
                emptyText(labels.noEvidence ?? Strings.noEvidence)
            }
            if !claim.evidence.isEmpty {
                VStack(alignment: .leading, spacing: Tokens.bodySpacing) {
                    ForEach(claim.evidence) { evidence in
                        passage(evidence)
                    }
                }
                .accessibilityElement(children: .contain)
                .accessibilityLabel(labels.evidenceLabel?(claim.text) ?? Strings.evidenceLabel(claim.text))
            }
            if onFlag != nil || (onVerify != nil && claim.support == .unchecked) {
                HStack(spacing: Tokens.inlineSpacing) {
                    if let onVerify, claim.support == .unchecked {
                        MonarchButton(
                            labels.verify ?? Strings.verify,
                            systemImage: Tokens.verifySymbol,
                            variant: .secondary,
                            size: .sm
                        ) { onVerify(claim.id) }
                    }
                    if let onFlag {
                        MonarchButton(
                            labels.flag ?? Strings.flag,
                            systemImage: Tokens.flagSymbol,
                            variant: .ghost,
                            size: .sm
                        ) {
                            onFlag(claim.id)
                        }
                    }
                }
            }
        }
    }

    private func passage(_ evidence: MonarchAiClaimEvidence) -> some View {
        let source = Tokens.source(evidence, in: sources)
        let title = source?.title ?? labels.unknownSource ?? Strings.unknownSource
        let detail = Tokens.sourceDetail(
            source,
            evidence: evidence,
            kind: { labels.kind?($0) ?? MonarchAiSourceListStrings.kind($0) },
            published: { labels.published?($0) ?? Strings.published($0) },
            locale: resolvedLocale,
            calendar: resolvedCalendar
        )
        let stance = evidence.stance.map { labels.stance?($0) ?? Strings.stance($0) }
        let from = labels.fromSource?(title) ?? Strings.fromSource(title)
        let barColor: Color =
            evidence.stance.map { theme.colors[role: MonarchTonePalette.palette(for: Tokens.tone($0)).border] }
            ?? theme.colors.borderDefault
        return VStack(alignment: .leading, spacing: Tokens.inlineSpacing) {
            VStack(alignment: .leading, spacing: Tokens.lineSpacing) {
                HStack(alignment: .top, spacing: Tokens.inlineSpacing) {
                    Rectangle()
                        .fill(barColor)
                        .frame(width: Tokens.quoteBarWidth)
                    Text(evidence.quote)
                        .monarchTextStyle(.bodySmall)
                        .foregroundStyle(theme.colors.textDefault)
                        .fixedSize(horizontal: false, vertical: true)
                }
                .fixedSize(horizontal: false, vertical: true)
                MonarchFlowLayout(spacing: Tokens.inlineSpacing) {
                    HStack(spacing: Tokens.lineSpacing) {
                        if let kind = source?.kind {
                            MonarchIcon(systemName: MonarchAiSourceFormat.symbol(kind), size: .sm)
                                .foregroundStyle(theme.colors.textMuted)
                        }
                        Text(title)
                            .monarchTextStyle(.caption, weight: .medium)
                            .foregroundStyle(theme.colors.textDefault)
                    }
                    if !detail.isEmpty {
                        Text(detail.replacingOccurrences(of: " · ", with: "  "))
                            .monarchTextStyle(.caption)
                            .foregroundStyle(theme.colors.textMuted)
                    }
                    if let stance, let value = evidence.stance {
                        let strong = theme.colors[role: MonarchTonePalette.palette(for: Tokens.tone(value)).strong]
                        HStack(spacing: Tokens.lineSpacing) {
                            MonarchIcon(
                                systemName: value == .supports ? "checkmark.circle" : "xmark.circle",
                                size: .sm
                            )
                            Text(stance).monarchTextStyle(.caption, weight: .medium)
                        }
                        .foregroundStyle(strong)
                    }
                }
            }
            .padding(.leading, Tokens.inlineSpacing)
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(
                Tokens.passageLabel(stance: stance, quote: evidence.quote, source: title, detail: detail)
            )
            MonarchFlowLayout(spacing: Tokens.inlineSpacing) {
                if let source, Tokens.canOpen(source, hasHandler: onOpenSource != nil) {
                    MonarchButton(
                        labels.openSource ?? Strings.openSource,
                        variant: .ghost,
                        size: .sm
                    ) {
                        open(source, evidence: evidence)
                    }
                    .accessibilityLabel("\(labels.openSource ?? Strings.openSource) \(from)")
                }
                MonarchCopyButton(
                    labels.copyQuote ?? Strings.copyQuote,
                    value: evidence.quote,
                    target: from,
                    variant: .ghost,
                    size: .sm,
                    copiedTitle: labels.copied ?? Strings.copied,
                    copiedAnnouncement: labels.copiedAnnouncement ?? Strings.copiedAnnouncement,
                    onCopy: { _ in onCopyQuote?(evidence) }
                )
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func open(_ source: MonarchAiClaimSource, evidence: MonarchAiClaimEvidence) {
        if let onOpenSource {
            onOpenSource(source, evidence)
        } else if let url = MonarchAiSourceFormat.safeURL(source.url) {
            openURL(url)
        }
    }
}

#if DEBUG
    #Preview("MonarchAiClaimSources") {
        MonarchPreviewMatrix {
            MonarchAiClaimSources(
                [
                    MonarchAiClaim(
                        id: "c1",
                        text: "Revenue grew 12% in the third quarter.",
                        support: .supported,
                        evidence: [
                            MonarchAiClaimEvidence(
                                id: "e1",
                                sourceID: "s1",
                                quote: "Third-quarter revenue rose 12% year on year.",
                                location: "Page 4",
                                stance: .supports
                            )
                        ]
                    ),
                    MonarchAiClaim(id: "c2", text: "Churn halved.", support: .unsupported),
                ],
                sources: [MonarchAiClaimSource(id: "s1", title: "Q3 report", kind: .doc)],
                expandedIDs: .constant(["c1", "c2"]),
                onFlag: { _ in }
            )
        }
    }
#endif
