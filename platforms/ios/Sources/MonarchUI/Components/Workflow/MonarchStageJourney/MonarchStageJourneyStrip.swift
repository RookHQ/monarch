import SwiftUI

/// The strip of stages, grouped by phase, that wraps onto further lines
/// when the phases do not fit in one.
struct MonarchStageJourneyStrip: View {
    let stages: [MonarchStageJourneyStage]
    let currentIndex: Int?
    let terminal: MonarchStageJourneyTerminal?
    let labels: MonarchStageJourneyLabels

    @Environment(\.monarchTheme) private var theme
    /// The web's 0.75rem stage icon, scaled with Dynamic Type.
    @ScaledMetric(relativeTo: .caption) private var symbolSize: CGFloat = 12

    var body: some View {
        // The phases wrap onto further lines when they do not fit, so no stage is scrolled out of sight.
        MonarchFlowLayout(
            spacing: MonarchStageJourneyTokens.groupSpacing,
            rowSpacing: MonarchStageJourneyTokens.groupSpacing
        ) {
            ForEach(Array(MonarchStageJourneyTokens.groups(stages).enumerated()), id: \.offset) { _, group in
                groupView(group)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .contain)
        .accessibilityLabel(MonarchStageJourneyStrings.stages(labels))
    }

    private func groupView(_ group: MonarchStageJourneyGroup) -> some View {
        VStack(alignment: .leading, spacing: MonarchStageJourneyTokens.groupLabelSpacing) {
            if let name = group.name {
                Text(name)
                    .monarchTextStyle(.caption, weight: .semibold)
                    .textCase(.uppercase)
                    .foregroundStyle(theme.colors.textMuted)
            }
            HStack(spacing: MonarchStageJourneyTokens.stageSpacing) {
                ForEach(group.indices, id: \.self) { index in
                    stageView(
                        stages[index],
                        status: MonarchStageJourneyTokens.status(at: index, currentIndex: currentIndex)
                    )
                    .id(stages[index].id)
                }
            }
        }
    }

    private func stageView(_ stage: MonarchStageJourneyStage, status: MonarchStageJourneyStatus) -> some View {
        let stageTerminal = status == .current ? terminal : nil
        return HStack(spacing: MonarchStageJourneyTokens.stageSpacing) {
            if let symbol = MonarchStageJourneyTokens.stageSymbol(status: status, terminal: stageTerminal) {
                Image(systemName: symbol)
                    .font(.system(size: symbolSize))
                    .accessibilityHidden(true)
                    .foregroundStyle(
                        status == .done
                            ? AnyShapeStyle(theme.colors[role: MonarchStageJourneyTokens.doneSymbol])
                            : AnyShapeStyle(.foreground)
                    )
            }
            Text(stage.label)
                .monarchTextStyle(.caption, weight: MonarchStageJourneyTokens.stageWeight(status: status))
                .fixedSize()
        }
        .padding(.horizontal, MonarchStageJourneyTokens.stagePadding)
        .frame(minHeight: MonarchStageJourneyTokens.stageMinHeight)
        .monarchSurface(
            MonarchStageJourneyTokens.stageAppearance(status: status, terminal: stageTerminal),
            in: Capsule()
        )
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(stage.label)
        .accessibilityValue(
            MonarchStageJourneyTokens.stageAccessibilityValue(status: status, terminal: stageTerminal, labels: labels)
        )
        .accessibilityAddTraits(status == .current ? .isSelected : [])
    }
}
