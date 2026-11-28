import SwiftUI

/// A 0 to 100 score produced by a model, as a ring with the number in its
/// centre or as a bar, with its band ("High"), the rationale and the factors
/// that raised or lowered it.
///
/// ```swift
/// MonarchAiScore(
///     "Lead score",
///     value: 72,
///     rationale: "Replied twice this week and matches the target industry.",
///     factors: [
///         MonarchAiScoreFactor("Replied to the last two emails", impact: 18),
///         MonarchAiScoreFactor("No budget confirmed", impact: -9),
///     ]
/// )
/// ```
///
/// The ring is a ``MonarchScoreRing`` in the tone of the band. The band is
/// a ``MonarchBadge`` with the tone's icon and the band's name, so the score
/// never depends on colour. A ``MonarchAiGeneratedBadge`` marks it as the
/// model's. The band legend is a `DisclosureGroup` in the
/// ``MonarchDisclosureGroupStyle``.
///
/// Accessibility: the score is one element named by the label, whose value
/// is "72 out of 100" (the ring) or "72 out of 100, High" (the bar); the
/// band badge is read after it. Each factor is one element: its label, then
/// "Raised the score by 18" and its detail. The legend marks the current
/// band in words. Everything scales with Dynamic Type.
public struct MonarchAiScore: View {
    private let label: String?
    private let value: Double?
    private let bands: [MonarchAiScoreBand]
    private let rationale: String?
    private let factors: [MonarchAiScoreFactor]
    private let variant: MonarchAiScoreVariant
    private let size: MonarchAiScoreSize
    private let showsBands: Bool
    private let labels: MonarchAiScoreLabels

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale
    @ScaledMetric(relativeTo: .body) private var impactWidth = MonarchAiScoreTokens.impactWidth
    @ScaledMetric(relativeTo: .body) private var barHeight = MonarchAiScoreTokens.barHeight

    /// An AI score.
    ///
    /// - Parameters:
    ///   - label: What is scored, such as "Lead score". Already localised. "Score" when `nil`.
    ///   - value: The score from 0 to 100; values outside are limited. `nil` while there is no score.
    ///   - bands: Named ranges with a label and a tone. Low, Medium and High by default.
    ///   - rationale: Why the model gave this score, in plain words.
    ///   - factors: The inputs that moved the score, with signed impacts.
    ///   - variant: A ring with the number in its centre, or a bar.
    ///   - size: The size of the ring.
    ///   - showsBands: Whether the band legend is offered.
    ///   - labels: Replacements for the built-in strings.
    public init(
        _ label: String? = nil,
        value: Double?,
        bands: [MonarchAiScoreBand] = MonarchAiScoreBand.defaults,
        rationale: String? = nil,
        factors: [MonarchAiScoreFactor] = [],
        variant: MonarchAiScoreVariant = .ring,
        size: MonarchAiScoreSize = .md,
        showsBands: Bool = true,
        labels: MonarchAiScoreLabels = MonarchAiScoreLabels()
    ) {
        self.label = label
        self.value = value
        self.bands = bands
        self.rationale = rationale
        self.factors = factors
        self.variant = variant
        self.size = size
        self.showsBands = showsBands
        self.labels = labels
    }

    private var title: String { label ?? labels.label ?? MonarchAiScoreStrings.label }

    /// The content of the view.
    public var body: some View {
        let score = MonarchAiScoreTokens.clamped(value)
        let band = score.flatMap { MonarchAiScoreTokens.band(for: $0, in: bands) }
        VStack(alignment: .leading, spacing: MonarchAiScoreTokens.sectionSpacing) {
            header
            display(score: score, band: band)
            if let rationale, !rationale.isEmpty {
                Text(rationale)
                    .monarchTextStyle(.body)
                    .foregroundStyle(theme.colors[role: MonarchAiScoreTokens.text])
            }
            if !factors.isEmpty {
                factorList
            }
            if showsBands, !bands.isEmpty {
                legend(current: band)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .contain)
    }

    private var header: some View {
        ViewThatFits(in: .horizontal) {
            HStack(alignment: .firstTextBaseline, spacing: MonarchAiScoreTokens.itemSpacing) {
                headerTitle
                Spacer(minLength: 0)
                aiMark
            }
            VStack(alignment: .leading, spacing: MonarchAiScoreTokens.lineSpacing) {
                headerTitle
                aiMark
            }
        }
    }

    private var headerTitle: some View {
        Text(title)
            .monarchTextStyle(.label)
            .foregroundStyle(theme.colors[role: MonarchAiScoreTokens.text])
            .accessibilityAddTraits(.isHeader)
    }

    private var aiMark: some View {
        MonarchAiGeneratedBadge(size: .sm, label: labels.aiGenerated ?? MonarchAiScoreStrings.aiGenerated)
    }

    @ViewBuilder private func display(score: Double?, band: MonarchAiScoreBand?) -> some View {
        let tone = MonarchAiScoreTokens.tone(for: score, in: bands)
        switch variant {
        case .ring:
            HStack(alignment: .center, spacing: MonarchAiScoreTokens.sectionSpacing) {
                MonarchScoreRing(
                    value: score,
                    color: .tone(tone),
                    size: MonarchAiScoreTokens.ringSize(size),
                    format: { [locale] in MonarchAiScoreTokens.number($0, locale: locale) },
                    labels: MonarchScoreRingLabels(
                        name: title,
                        valueText: labels.valueText,
                        noValue: labels.unavailable ?? MonarchAiScoreStrings.unavailable
                    )
                )
                bandBadge(band)
            }
        case .bar:
            VStack(alignment: .leading, spacing: MonarchAiScoreTokens.lineSpacing) {
                HStack(alignment: .firstTextBaseline, spacing: MonarchAiScoreTokens.itemSpacing) {
                    readout(score)
                    Spacer(minLength: 0)
                    bandBadge(band)
                        .accessibilityHidden(true)
                }
                bar(score: score, tone: tone)
            }
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(title)
            .accessibilityValue(MonarchAiScoreTokens.barValue(score, band: band, locale: locale, labels: labels))
        }
    }

    @ViewBuilder private func readout(_ score: Double?) -> some View {
        if let score {
            HStack(alignment: .firstTextBaseline, spacing: 0) {
                Text(MonarchAiScoreTokens.number(score, locale: locale))
                    .monarchTextStyle(.title)
                    .foregroundStyle(theme.colors[role: MonarchAiScoreTokens.text])
                Text("/" + MonarchAiScoreTokens.number(MonarchAiScoreTokens.scaleMax, locale: locale))
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors[role: MonarchAiScoreTokens.muted])
            }
            .monarchTabularNumber()
        } else {
            Text(labels.unavailable ?? MonarchAiScoreStrings.unavailable)
                .monarchTextStyle(.body)
                .foregroundStyle(theme.colors[role: MonarchAiScoreTokens.muted])
        }
    }

    @ViewBuilder private func bandBadge(_ band: MonarchAiScoreBand?) -> some View {
        if let band {
            MonarchBadge(band.label, tone: band.tone, systemImage: MonarchTonePalette.palette(for: band.tone).symbol)
        }
    }

    private func bar(score: Double?, tone: MonarchTone) -> some View {
        let fraction = MonarchAiScoreTokens.fraction(score)
        let ticks = MonarchAiScoreTokens.ticks(bands)
        return GeometryReader { proxy in
            let width = proxy.size.width
            ZStack(alignment: .leading) {
                Capsule().fill(theme.colors[role: MonarchAiScoreTokens.track])
                Capsule()
                    .fill(theme.colors[role: MonarchAiScoreTokens.fill(tone)])
                    .frame(width: width * fraction)
                    .monarchAnimation(.base, value: fraction)
                ForEach(ticks, id: \.self) { tick in
                    // Leading padding, not an offset, so the marks mirror in right-to-left layouts.
                    Rectangle()
                        .fill(theme.colors[role: MonarchAiScoreTokens.tick])
                        .frame(width: MonarchAiScoreTokens.tickWidth)
                        .padding(.leading, width * tick)
                }
            }
        }
        .frame(height: barHeight)
    }

    private var factorList: some View {
        VStack(alignment: .leading, spacing: MonarchAiScoreTokens.itemSpacing) {
            Text(labels.factors ?? MonarchAiScoreStrings.factors)
                .monarchTextStyle(.label)
                .foregroundStyle(theme.colors[role: MonarchAiScoreTokens.muted])
                .accessibilityAddTraits(.isHeader)
            // The column headings, as the web's table has them; read by the rows, so hidden from VoiceOver.
            HStack(spacing: MonarchAiScoreTokens.itemSpacing) {
                Text(labels.factorColumn ?? MonarchAiScoreStrings.factorColumn)
                    .frame(maxWidth: .infinity, alignment: .leading)
                Text(labels.impactColumn ?? MonarchAiScoreStrings.impactColumn)
                    .frame(width: impactWidth, alignment: .leading)
            }
            .monarchTextStyle(.caption)
            .foregroundStyle(theme.colors[role: MonarchAiScoreTokens.muted])
            .accessibilityHidden(true)
            ForEach(Array(factors.enumerated()), id: \.offset) { _, factor in
                factorRow(factor)
            }
        }
    }

    private func factorRow(_ factor: MonarchAiScoreFactor) -> some View {
        let direction = MonarchAiScoreTokens.direction(factor.impact)
        let role = MonarchAiScoreTokens.impactRole(direction)
        return HStack(alignment: .firstTextBaseline, spacing: MonarchAiScoreTokens.itemSpacing) {
            VStack(alignment: .leading, spacing: MonarchAiScoreTokens.lineSpacing) {
                Text(factor.label)
                    .monarchTextStyle(.body)
                    .foregroundStyle(theme.colors[role: MonarchAiScoreTokens.text])
                if let detail = factor.detail, !detail.isEmpty {
                    Text(detail)
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors[role: MonarchAiScoreTokens.muted])
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            VStack(alignment: .trailing, spacing: MonarchAiScoreTokens.lineSpacing) {
                HStack(spacing: MonarchAiScoreTokens.lineSpacing) {
                    switch direction {
                    case .up: MonarchIcon(systemName: MonarchAiScoreTokens.upSymbol, size: .sm)
                    case .down: MonarchIcon(systemName: MonarchAiScoreTokens.downSymbol, size: .sm)
                    case .none: EmptyView()
                    }
                    Text(MonarchAiScoreTokens.signed(factor.impact, locale: locale))
                        .monarchTextStyle(.label)
                        .monarchTabularNumber()
                }
                .foregroundStyle(theme.colors[role: role])
                ZStack(alignment: .leading) {
                    Capsule().fill(theme.colors[role: MonarchAiScoreTokens.track])
                    Capsule()
                        .fill(theme.colors[role: role])
                        .frame(
                            width: impactWidth * MonarchAiScoreTokens.impactFraction(factor.impact, factors: factors)
                        )
                }
                .frame(width: impactWidth, height: MonarchAiScoreTokens.impactHeight)
            }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(factor.label)
        .accessibilityValue(
            [MonarchAiScoreTokens.impactText(factor.impact, locale: locale, labels: labels), factor.detail]
                .compactMap { $0 }
                .filter { !$0.isEmpty }
                .joined(separator: ", ")
        )
    }

    private func legend(current: MonarchAiScoreBand?) -> some View {
        DisclosureGroup(labels.bands ?? MonarchAiScoreStrings.bands) {
            VStack(alignment: .leading, spacing: MonarchAiScoreTokens.lineSpacing) {
                ForEach(Array(MonarchAiScoreTokens.ranges(bands).enumerated()), id: \.offset) { _, range in
                    let isCurrent = current == range.band
                    HStack(alignment: .firstTextBaseline, spacing: MonarchAiScoreTokens.itemSpacing) {
                        Circle()
                            .fill(theme.colors[role: MonarchAiScoreTokens.fill(range.band.tone)])
                            .frame(width: MonarchAiScoreTokens.swatchSize, height: MonarchAiScoreTokens.swatchSize)
                            .accessibilityHidden(true)
                        Text(
                            isCurrent
                                ? "\(range.band.label) (\(labels.currentBand ?? MonarchAiScoreStrings.currentBand))"
                                : range.band.label
                        )
                        .monarchTextStyle(isCurrent ? .bodyStrong : .body)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        Text(MonarchAiScoreTokens.rangeText(range, locale: locale))
                            .monarchTextStyle(.body)
                            .monarchTabularNumber()
                    }
                    .foregroundStyle(theme.colors[role: MonarchAiScoreTokens.text])
                    .accessibilityElement(children: .combine)
                }
            }
        }
        .disclosureGroupStyle(.monarch)
    }
}

#if DEBUG
    #Preview("MonarchAiScore") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchAiScore(
                    "Lead score",
                    value: 72,
                    rationale: "Replied twice this week and matches the target industry.",
                    factors: [
                        MonarchAiScoreFactor("Replied to the last two emails", impact: 18),
                        MonarchAiScoreFactor("No budget confirmed", impact: -9, detail: "Asked on the last call"),
                    ]
                )
                MonarchAiScore("Churn risk", value: 31, variant: .bar, showsBands: false)
                MonarchAiScore(value: nil, size: .sm)
            }
        }
    }
#endif
