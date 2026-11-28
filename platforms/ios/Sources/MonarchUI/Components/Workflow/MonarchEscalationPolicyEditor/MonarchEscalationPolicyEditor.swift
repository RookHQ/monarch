import Foundation
import SwiftUI

/// Edits an escalation ladder: at each level, after how long and whom to
/// notify, so an alert or a request nobody acts on reaches someone else.
///
/// ```swift
/// @State private var levels = [MonarchEscalationLevel(id: "l1", afterMinutes: 5, targets: ["oncall"])]
///
/// MonarchEscalationPolicyEditor(levels: $levels, targets: [
///     MonarchEscalationTarget(id: "oncall", label: "On-call rota"),
///     MonarchEscalationTarget(id: "ops", label: "#ops", description: "Slack channel"),
/// ])
/// ```
///
/// Each level is a card with its title, a summary ("After 15m, notify
/// On-call rota"), a remove button, the delay as ``MonarchDurationInput``
/// (at least a minute, in steps of five, up to days) and the targets as a
/// ``MonarchCheckboxGroup``. A new level waits `defaultStepMinutes` longer
/// than the last one. Check the policy with ``validate(_:timing:)`` before
/// saving and pass `showsErrors` to show what is wrong on each level. The
/// app supplies the targets and runs the escalation.
///
/// Accessibility: the list is a container named "Escalation levels"; each
/// level is a container read as "Level 1, After 15 minutes, notify On-call
/// rota". The remove button is named "Remove level 1" and is disabled at
/// `minLevels`. Problems and the no-targets warning are text with an icon.
/// Touch targets are at least 44 points.
public struct MonarchEscalationPolicyEditor: View {
    @Binding private var levels: [MonarchEscalationLevel]
    private let targets: [MonarchEscalationTarget]
    private let timing: MonarchEscalationPolicyTiming
    private let description: String?
    private let defaultStepMinutes: Int
    private let minLevels: Int
    private let maxLevels: Int
    private let showsErrors: Bool
    private let labels: MonarchEscalationPolicyEditorLabels
    private let makeID: @MainActor () -> String

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale

    /// An escalation policy editor.
    ///
    /// - Parameters:
    ///   - levels: The levels, in order.
    ///   - targets: What a level can notify.
    ///   - timing: Whether times count from the start or from the previous level.
    ///   - description: Help under the list, such as "Each level fires only if nobody has acknowledged yet."
    ///   - defaultStepMinutes: Minutes added to the last level for a new one.
    ///   - minLevels: The fewest levels; remove is disabled at this count.
    ///   - maxLevels: The most levels; the add button is replaced by a note at this count.
    ///   - showsErrors: Shows the problems of each level, for example after a save attempt.
    ///   - labels: Replacements for the built-in strings.
    ///   - makeID: Makes the id of a new level.
    public init(
        levels: Binding<[MonarchEscalationLevel]>,
        targets: [MonarchEscalationTarget],
        timing: MonarchEscalationPolicyTiming = .cumulative,
        description: String? = nil,
        defaultStepMinutes: Int = 15,
        minLevels: Int = 1,
        maxLevels: Int = 10,
        showsErrors: Bool = false,
        labels: MonarchEscalationPolicyEditorLabels = MonarchEscalationPolicyEditorLabels(),
        makeID: @escaping @MainActor () -> String = { UUID().uuidString }
    ) {
        _levels = levels
        self.targets = targets
        self.timing = timing
        self.description = description
        self.defaultStepMinutes = max(defaultStepMinutes, MonarchEscalationPolicyEditorTokens.minimumMinutes)
        self.minLevels = max(minLevels, 0)
        self.maxLevels = max(maxLevels, 1)
        self.showsErrors = showsErrors
        self.labels = labels
        self.makeID = makeID
    }

    /// What is wrong with each level: no targets, or, with cumulative timing,
    /// not later than the level before. Keyed by level id; empty when the
    /// policy can be saved.
    ///
    /// - Parameters:
    ///   - levels: The levels, in order.
    ///   - timing: How the times are measured.
    /// - Returns: The problem of each level that has one.
    nonisolated public static func validate(
        _ levels: [MonarchEscalationLevel],
        timing: MonarchEscalationPolicyTiming = .cumulative
    ) -> [String: MonarchEscalationLevelProblem] {
        MonarchEscalationPolicyEditorTokens.problems(levels, timing: timing)
    }

    private typealias Tokens = MonarchEscalationPolicyEditorTokens

    /// The content of the view.
    public var body: some View {
        let problems = showsErrors ? Tokens.problems(levels, timing: timing) : [:]
        VStack(alignment: .leading, spacing: Tokens.listSpacing) {
            if targets.isEmpty {
                iconLine(labels.noTargets ?? MonarchEscalationPolicyEditorStrings.noTargets, role: Tokens.warningText)
            }
            VStack(alignment: .leading, spacing: Tokens.listSpacing) {
                ForEach(Array(levels.enumerated()), id: \.element.id) { index, level in
                    // The numbered rail of the web: a circle per level and a line joining them.
                    HStack(alignment: .top, spacing: Tokens.listSpacing) {
                        VStack(spacing: 0) {
                            Text("\(index + 1)")
                                .monarchTextStyle(.caption, weight: .medium)
                                .foregroundStyle(theme.colors.textMuted)
                                .frame(width: 24, height: 24)
                                .background(Circle().fill(theme.colors.bgMuted))
                            if index < levels.count - 1 {
                                Rectangle()
                                    .fill(theme.colors.borderDefault)
                                    .frame(width: 2)
                                    .frame(maxHeight: .infinity)
                                    .padding(.bottom, -Tokens.listSpacing)
                            }
                        }
                        .accessibilityHidden(true)
                        levelCard(level, position: index + 1, problem: problems[level.id])
                    }
                }
            }
            .accessibilityElement(children: .contain)
            .accessibilityLabel(labels.label ?? MonarchEscalationPolicyEditorStrings.label)
            if let description {
                Text(description)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors[role: Tokens.mutedText])
                    .fixedSize(horizontal: false, vertical: true)
            }
            if Tokens.canAdd(count: levels.count, maxLevels: maxLevels) {
                Button(labels.add ?? MonarchEscalationPolicyEditorStrings.add, systemImage: Tokens.addSymbol) {
                    levels.append(
                        MonarchEscalationLevel(
                            id: makeID(),
                            afterMinutes: Tokens.newLevelMinutes(
                                after: levels,
                                timing: timing,
                                step: defaultStepMinutes
                            )
                        )
                    )
                }
                .buttonStyle(.monarch(.secondary, size: .sm))
            } else {
                Text(labels.maxReached?(maxLevels) ?? MonarchEscalationPolicyEditorStrings.maxReached(maxLevels))
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors[role: Tokens.mutedText])
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func levelCard(_ level: MonarchEscalationLevel, position: Int, problem: MonarchEscalationLevelProblem?)
        -> some View
    {
        let title = Tokens.levelTitle(position, labels: labels)
        let names = Tokens.names(level, targets: targets, labels: labels)
        let visible = Tokens.summary(duration: Tokens.shortDuration(level.afterMinutes), names: names, labels: labels)
        let spoken = Tokens.summary(
            duration: Tokens.spokenDuration(level.afterMinutes, locale: locale),
            names: names,
            labels: labels
        )
        return MonarchCard {
            VStack(alignment: .leading, spacing: Tokens.levelSpacing) {
                HStack(alignment: .firstTextBaseline) {
                    VStack(alignment: .leading, spacing: 0) {
                        Text(title)
                            .monarchTextStyle(.label, weight: .semibold)
                            .foregroundStyle(theme.colors.textDefault)
                            .accessibilityAddTraits(.isHeader)
                        Text(visible)
                            .monarchTextStyle(.caption)
                            .fixedSize(horizontal: false, vertical: true)
                            .foregroundStyle(theme.colors[role: Tokens.mutedText])
                            .accessibilityHidden(true)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    Button {
                        levels.removeAll { $0.id == level.id }
                    } label: {
                        Label(
                            labels.remove?(position) ?? MonarchEscalationPolicyEditorStrings.remove(position),
                            systemImage: Tokens.removeSymbol
                        )
                        .labelStyle(.iconOnly)
                    }
                    .buttonStyle(.monarch(.ghost, size: .sm))
                    .disabled(!Tokens.canRemove(count: levels.count, minLevels: minLevels))
                }
                MonarchDurationInput(
                    labels.after ?? MonarchEscalationPolicyEditorStrings.after,
                    minutes: Binding(
                        get: { level.afterMinutes },
                        set: { minutes in update(level.id) { $0.afterMinutes = minutes ?? Tokens.minimumMinutes } }
                    ),
                    minimum: Tokens.minimumMinutes,
                    step: Tokens.stepMinutes,
                    largestUnit: .days,
                    error: problem == .order ? Tokens.problemText(.order, labels: labels) : nil
                )
                if !targets.isEmpty {
                    MonarchCheckboxGroup(
                        labels.notify ?? MonarchEscalationPolicyEditorStrings.notify,
                        selection: Binding(
                            get: { Set(level.targets) },
                            set: { chosen in
                                update(level.id) { level in
                                    level.targets = Tokens.ordered(chosen, targets: targets)
                                }
                            }
                        ),
                        options: targets.map {
                            MonarchCheckboxGroupOption(id: $0.id, label: $0.label, description: $0.description)
                        },
                        error: problem == .targets ? Tokens.problemText(.targets, labels: labels) : nil
                    )
                }
            }
        }
        .overlay {
            if problem != nil {
                MonarchShapes.control
                    .strokeBorder(theme.colors.dangerBorder, lineWidth: MonarchShapes.borderWidth)
            }
        }
        .accessibilityElement(children: .contain)
        .accessibilityLabel("\(title), \(spoken)")
    }

    private func update(_ id: String, _ change: (inout MonarchEscalationLevel) -> Void) {
        guard let index = levels.firstIndex(where: { $0.id == id }) else { return }
        change(&levels[index])
    }

    private func iconLine(_ text: String, role: MonarchColorRole) -> some View {
        HStack(alignment: .firstTextBaseline, spacing: Tokens.iconSpacing) {
            MonarchIcon(systemName: Tokens.warningSymbol, size: .sm)
            Text(text).monarchTextStyle(.body)
        }
        .foregroundStyle(theme.colors[role: role])
        .accessibilityElement(children: .combine)
    }
}

#if DEBUG
    #Preview("MonarchEscalationPolicyEditor") {
        MonarchPreviewMatrix {
            MonarchEscalationPolicyEditor(
                levels: .constant([
                    MonarchEscalationLevel(id: "1", afterMinutes: 5, targets: ["oncall"]),
                    MonarchEscalationLevel(id: "2", afterMinutes: 5),
                ]),
                targets: [
                    MonarchEscalationTarget(id: "oncall", label: "On-call rota"),
                    MonarchEscalationTarget(id: "ops", label: "#ops", description: "Slack channel"),
                ],
                showsErrors: true
            )
        }
    }
#endif
