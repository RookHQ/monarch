import MonarchTokens
import SwiftUI

/// A short, step-by-step introduction to AI in a product: what it can see,
/// what it can do and how to turn it off, with "Turn off AI" on every step.
///
/// ```swift
/// .sheet(isPresented: $showsTour) {
///     MonarchAiOnboardingTour(
///         steps,
///         onSkip: { showsTour = false },
///         onDisable: { settings.isAIEnabled = false; showsTour = false },
///         onFinish: { showsTour = false }
///     )
///     .padding()
///     .presentationDetents([.medium, .large])
/// }
/// ```
///
/// The steps are pages of a `TabView` with the page style, so people can
/// swipe between them as well as use Back and Next; a ``MonarchStepper``
/// shows the position. Present it in a `sheet` for a first-run
/// introduction, or place it inside a ``MonarchCard`` at the top of a
/// screen. The tour takes the height of its tallest step, so it grows with
/// the text size. To point at one control instead, use TipKit's
/// `popoverTip(_:arrowEdge:)`.
///
/// Accessibility: each step's heading is a header and VoiceOver moves to it
/// after Back, Next or a swipe. The stepper reads "Step 2 of 3" and the
/// step's name. The close button is named "Skip introduction". Every button
/// has a 44-point touch target, and the page swipe follows Reduce Motion
/// through the system.
public struct MonarchAiOnboardingTour: View {
    private let steps: [MonarchAiOnboardingTourStep]
    private let externalStep: Binding<Int>?
    private let labels: MonarchAiOnboardingTourLabels
    private let onSkip: (@MainActor () -> Void)?
    private let onDisable: @MainActor () -> Void
    private let onFinish: @MainActor () -> Void

    @Environment(\.monarchTheme) private var theme
    @State private var internalStep = 0
    @AccessibilityFocusState private var focusedStep: Int?

    /// A tour that keeps its own position.
    ///
    /// - Parameters:
    ///   - steps: The steps. Cover what the AI sees, what it can do and how to turn it off.
    ///   - labels: Replaces the built-in strings.
    ///   - onSkip: Adds a close button that skips the tour.
    ///   - onDisable: Runs for "Turn off AI". Turn AI off, or open its settings, and hide the tour.
    ///   - onFinish: Runs for the last step's button.
    public init(
        _ steps: [MonarchAiOnboardingTourStep],
        labels: MonarchAiOnboardingTourLabels = MonarchAiOnboardingTourLabels(),
        onSkip: (@MainActor () -> Void)? = nil,
        onDisable: @escaping @MainActor () -> Void,
        onFinish: @escaping @MainActor () -> Void
    ) {
        self.steps = steps
        self.externalStep = nil
        self.labels = labels
        self.onSkip = onSkip
        self.onDisable = onDisable
        self.onFinish = onFinish
    }

    /// A tour whose position the app keeps.
    ///
    /// - Parameters:
    ///   - steps: The steps.
    ///   - step: The position of the current step, from 0. It is kept inside the steps.
    ///   - labels: Replaces the built-in strings.
    ///   - onSkip: Adds a close button that skips the tour.
    ///   - onDisable: Runs for "Turn off AI".
    ///   - onFinish: Runs for the last step's button.
    public init(
        _ steps: [MonarchAiOnboardingTourStep],
        step: Binding<Int>,
        labels: MonarchAiOnboardingTourLabels = MonarchAiOnboardingTourLabels(),
        onSkip: (@MainActor () -> Void)? = nil,
        onDisable: @escaping @MainActor () -> Void,
        onFinish: @escaping @MainActor () -> Void
    ) {
        self.steps = steps
        self.externalStep = step
        self.labels = labels
        self.onSkip = onSkip
        self.onDisable = onDisable
        self.onFinish = onFinish
    }

    private typealias Tokens = MonarchAiOnboardingTourTokens

    private var selection: Binding<Int> {
        Binding(
            get: { Tokens.clamped(externalStep?.wrappedValue ?? internalStep, count: steps.count) },
            set: { newValue in
                let clamped = Tokens.clamped(newValue, count: steps.count)
                if let externalStep {
                    externalStep.wrappedValue = clamped
                } else {
                    internalStep = clamped
                }
            }
        )
    }

    /// The content of the view.
    public var body: some View {
        let current = selection.wrappedValue
        VStack(alignment: .leading, spacing: Tokens.stackSpacing) {
            header
            if !steps.isEmpty {
                pages
            }
            if steps.count > 1 {
                dots(current)
            }
            buttons(current)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .onChange(of: current) { _, newValue in
            focusedStep = newValue
        }
    }

    private var header: some View {
        HStack(spacing: Tokens.spacing) {
            MonarchAiBotGlyph(size: .sm)
            Text(labels.eyebrow)
                .monarchTextStyle(.caption, weight: .semibold)
            if steps.count > 1 {
                Text(labels.stepOf(selection.wrappedValue + 1, steps.count))
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors.textMuted)
                    .accessibilityHidden(true)
            }
            Spacer(minLength: 0)
            if let onSkip {
                Button(action: onSkip) {
                    MonarchIcon(systemName: Tokens.closeSymbol, label: labels.skip)
                        .foregroundStyle(theme.colors.textMuted)
                        .monarchMinimumHitTarget()
                        .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
            }
        }
        .foregroundStyle(theme.colors[role: Tokens.eyebrowRole])
    }

    /// The progress dots under the page, as the web draws them: done dots in the AI border colour, the current one a
    /// longer AI solid pill, the rest in the default border colour.
    private func dots(_ current: Int) -> some View {
        HStack(spacing: MonarchTokens.Space.space1) {
            ForEach(steps.indices, id: \.self) { index in
                Capsule()
                    .fill(
                        index == current
                            ? theme.colors.aiSolid
                            : index < current ? theme.colors.aiBorder : theme.colors.borderDefault
                    )
                    .frame(
                        width: index == current ? MonarchTokens.Space.space4 : MonarchTokens.Space.space1_5,
                        height: MonarchTokens.Space.space1_5
                    )
            }
        }
        .monarchAnimation(.fast, value: current)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(labels.stepOf(current + 1, steps.count))
    }

    /// Every page is laid out hidden so the tour takes the tallest page's
    /// height; the pager sits on top.
    private var pages: some View {
        ZStack(alignment: .topLeading) {
            ForEach(steps.indices, id: \.self) { index in
                page(index).hidden()
            }
        }
        // The page style adds its own vertical room, so the last line is not cut by the buttons.
        .padding(.bottom, Tokens.pageSpacing * 2)
        .overlay(alignment: .topLeading) {
            #if os(iOS)
                TabView(selection: selection) {
                    ForEach(steps.indices, id: \.self) { index in
                        page(index)
                            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
                            .tag(index)
                    }
                }
                .tabViewStyle(.page(indexDisplayMode: .never))
            #else
                // The paging style does not exist on macOS, which is only a
                // build convenience: the host build shows the current step.
                page(selection.wrappedValue)
            #endif
        }
    }

    private func page(_ index: Int) -> some View {
        let step = steps[index]
        return VStack(alignment: .leading, spacing: Tokens.pageSpacing) {
            if let systemImage = step.systemImage {
                MonarchIcon(systemName: systemImage, size: .lg)
                    .foregroundStyle(theme.colors[role: Tokens.eyebrowRole])
            }
            Text(step.title)
                .monarchTextStyle(.heading)
                .foregroundStyle(theme.colors.textDefault)
                .accessibilityAddTraits(.isHeader)
                .accessibilityFocused($focusedStep, equals: index)
            Text(step.body)
                .monarchTextStyle(.body)
                .foregroundStyle(theme.colors.textMuted)
        }
        .fixedSize(horizontal: false, vertical: true)
    }

    private func buttons(_ current: Int) -> some View {
        ViewThatFits(in: .horizontal) {
            HStack(spacing: Tokens.spacing) {
                disableButton
                Spacer(minLength: 0)
                navigation(current)
            }
            VStack(alignment: .leading, spacing: Tokens.spacing) {
                HStack(spacing: Tokens.spacing) { navigation(current) }
                disableButton
            }
        }
    }

    private var disableButton: some View {
        MonarchButton(labels.disable, variant: .ghost, size: .sm, action: onDisable)
    }

    @ViewBuilder private func navigation(_ current: Int) -> some View {
        if Tokens.showsBack(current, count: steps.count) {
            MonarchButton(labels.back, size: .sm) {
                selection.wrappedValue = current - 1
            }
        }
        MonarchButton(Tokens.primaryTitle(current, count: steps.count, labels: labels), variant: .primary, size: .sm) {
            if Tokens.isLast(current, count: steps.count) {
                onFinish()
            } else {
                selection.wrappedValue = current + 1
            }
        }
    }
}

#if DEBUG
    #Preview("MonarchAiOnboardingTour") {
        MonarchPreviewMatrix {
            MonarchAiOnboardingTour(
                [
                    MonarchAiOnboardingTourStep(
                        "What the assistant can see",
                        body: "The page you have open, the records you select and any files you attach.",
                        systemImage: "eye"
                    ),
                    MonarchAiOnboardingTourStep(
                        "What it can do",
                        body: "Summarise pages, draft text and answer questions. It asks before it changes anything."
                    ),
                    MonarchAiOnboardingTourStep(
                        "How to turn it off",
                        body: "Use Turn off AI below at any time, or switch it off in settings."
                    ),
                ],
                onSkip: {},
                onDisable: {},
                onFinish: {}
            )
        }
    }
#endif
