import SwiftUI

/// Privacy choices for a sheet: one switch per category of optional storage
/// or processing, with Reject all, Accept all and Save choices.
///
/// ```swift
/// .sheet(isPresented: $showsChoices) {
///     ScrollView {
///         MonarchConsentPreferences(
///             categories: categories,
///             choices: consent.choices,
///             record: consent.record,
///             policyURL: policyURL
///         ) { method, choices in
///             consent.save(method, choices)
///             showsChoices = false
///         }
///         .padding()
///     }
///     .presentationDetents([.large])
/// }
/// ```
///
/// Strictly necessary categories are "Always on" and cannot be switched off;
/// sale-or-sharing categories stay off while a Global Privacy Control signal
/// is on. Nothing optional is on until the person chooses: start `choices`
/// empty. Reject all and Accept all look the same. Every decision reports the
/// method and every category's state; the app stores the record and closes
/// the sheet.
///
/// Accessibility: the title is a header. Each category is a standard
/// `Toggle` (a switch) whose label is the name, the description and any note,
/// so VoiceOver reads why a locked switch cannot change. In choice mode each
/// category is one element whose value is its status word. The buttons are
/// MonarchButtons with 44-point touch targets.
public struct MonarchConsentPreferences: View {
    private let categories: [MonarchConsentPreferencesCategory]
    private let choices: [String: Bool]
    private let mode: MonarchConsentPreferencesMode
    private let isGlobalPrivacyControl: Bool
    private let record: MonarchConsentRecord?
    private let policyURL: URL?
    private let allowsWithdraw: Bool
    private let now: Date?
    private let labels: MonarchConsentPreferencesLabels
    private let onDecide: @MainActor (MonarchConsentMethod, [String: Bool]) -> Void

    @State private var draft: [String: Bool]
    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale

    /// Privacy choices.
    ///
    /// - Parameters:
    ///   - categories: The categories, in order. Mark strictly necessary ones `isRequired`.
    ///   - choices: The saved choice by category id. Empty until the person has chosen.
    ///   - mode: Switches and Save, or only Reject all and Accept all.
    ///   - isGlobalPrivacyControl: Whether a Global Privacy Control signal is on. Keeps sale-or-sharing
    ///     categories off.
    ///   - record: The saved decision, for the policy version and time line and for Withdraw consent.
    ///   - policyURL: Where the privacy policy is. Adds a link under the description.
    ///   - allowsWithdraw: Whether Withdraw consent is offered when a decision is saved.
    ///   - now: The reference time for the saved time. The current time by default.
    ///   - labels: Replacements for the built-in strings.
    ///   - onDecide: Runs with the method and every category's state when a button is pressed.
    public init(
        categories: [MonarchConsentPreferencesCategory],
        choices: [String: Bool] = [:],
        mode: MonarchConsentPreferencesMode = .categories,
        isGlobalPrivacyControl: Bool = false,
        record: MonarchConsentRecord? = nil,
        policyURL: URL? = nil,
        allowsWithdraw: Bool = true,
        now: Date? = nil,
        labels: MonarchConsentPreferencesLabels = MonarchConsentPreferencesLabels(),
        onDecide: @escaping @MainActor (MonarchConsentMethod, [String: Bool]) -> Void
    ) {
        self.categories = categories
        self.choices = choices
        self.mode = mode
        self.isGlobalPrivacyControl = isGlobalPrivacyControl
        self.record = record
        self.policyURL = policyURL
        self.allowsWithdraw = allowsWithdraw
        self.now = now
        self.labels = labels
        self.onDecide = onDecide
        _draft = State(initialValue: choices)
    }

    /// The content of the view.
    public var body: some View {
        VStack(alignment: .leading, spacing: MonarchConsentPreferencesTokens.sectionSpacing) {
            header
            VStack(alignment: .leading, spacing: 0) {
                ForEach(Array(categories.enumerated()), id: \.element.id) { index, category in
                    if index > 0 { Divider().overlay(theme.colors.borderSubtle) }
                    Group {
                        switch mode {
                        case .categories: switchRow(category)
                        case .choice: statusRow(category)
                        }
                    }
                    .padding(.vertical, MonarchConsentPreferencesTokens.rowPadding)
                }
            }
            if let record {
                Text(
                    MonarchConsentPreferencesTokens.recordLine(
                        record,
                        now: now ?? Date(),
                        locale: locale,
                        labels: labels
                    )
                )
                .monarchTextStyle(.caption)
                .foregroundStyle(theme.colors.textMuted)
            }
            buttons
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: MonarchConsentPreferencesTokens.lineSpacing) {
            Text(labels.title ?? MonarchConsentPreferencesStrings.title)
                .monarchTextStyle(.title)
                .foregroundStyle(theme.colors.textDefault)
                .accessibilityAddTraits(.isHeader)
            Text(labels.description ?? MonarchConsentPreferencesStrings.description)
                .monarchTextStyle(.body)
                .foregroundStyle(theme.colors.textMuted)
            if let policyURL {
                Link(destination: policyURL) {
                    Text(labels.policy ?? MonarchConsentPreferencesStrings.policy)
                        .monarchTextStyle(.body)
                        .underline()
                        .foregroundStyle(theme.colors.textLink)
                        .frame(minHeight: MonarchAccessibility.minimumHitTarget)
                        .contentShape(Rectangle())
                }
            }
        }
    }

    @ViewBuilder private func switchRow(_ category: MonarchConsentPreferencesCategory) -> some View {
        if category.isRequired && mode == .categories {
            alwaysOnRow(category)
        } else {
            plainSwitchRow(category)
        }
    }

    /// A required category: "Always on" sits left of the locked switch, as on the web.
    private func alwaysOnRow(_ category: MonarchConsentPreferencesCategory) -> some View {
        let alwaysOn = labels.alwaysOn ?? MonarchConsentPreferencesStrings.alwaysOn
        return HStack(alignment: .top, spacing: MonarchConsentPreferencesTokens.buttonSpacing) {
            categoryText(category, showsAlwaysOn: false)
                .frame(maxWidth: .infinity, alignment: .leading)
            Text(alwaysOn)
                .monarchTextStyle(.caption)
                .foregroundStyle(theme.colors.textMuted)
                .padding(.top, MonarchConsentPreferencesTokens.lineSpacing)
            Toggle(
                category.label,
                isOn: Binding(
                    get: {
                        MonarchConsentPreferencesTokens.isOn(
                            category,
                            choices: draft,
                            isGlobalPrivacyControl: isGlobalPrivacyControl
                        )
                    },
                    set: { draft[category.id] = $0 }
                )
            )
            .labelsHidden()
            .tint(theme.colors.accentSolid)
            .disabled(
                MonarchConsentPreferencesTokens.isLocked(category, isGlobalPrivacyControl: isGlobalPrivacyControl)
            )
            .accessibilityLabel(category.label)
            .accessibilityHint(category.description + ". " + alwaysOn)
        }
    }

    private func plainSwitchRow(_ category: MonarchConsentPreferencesCategory) -> some View {
        Toggle(
            isOn: Binding(
                get: {
                    MonarchConsentPreferencesTokens.isOn(
                        category,
                        choices: draft,
                        isGlobalPrivacyControl: isGlobalPrivacyControl
                    )
                },
                set: { draft[category.id] = $0 }
            )
        ) {
            categoryText(category)
        }
        .tint(theme.colors.accentSolid)
        .disabled(MonarchConsentPreferencesTokens.isLocked(category, isGlobalPrivacyControl: isGlobalPrivacyControl))
    }

    private func statusRow(_ category: MonarchConsentPreferencesCategory) -> some View {
        let isOn = MonarchConsentPreferencesTokens.isOn(
            category,
            choices: choices,
            isGlobalPrivacyControl: isGlobalPrivacyControl
        )
        let status = MonarchConsentPreferencesTokens.status(
            category,
            choices: choices,
            isGlobalPrivacyControl: isGlobalPrivacyControl,
            labels: labels
        )
        return HStack(alignment: .firstTextBaseline, spacing: MonarchConsentPreferencesTokens.buttonSpacing) {
            categoryText(category)
                .frame(maxWidth: .infinity, alignment: .leading)
            Text(status)
                .monarchTextStyle(.label)
                .foregroundStyle(theme.colors[role: MonarchConsentPreferencesTokens.statusRole(isOn: isOn)])
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(category.label)
        .accessibilityValue(status)
        .accessibilityHint(category.description)
    }

    private func categoryText(_ category: MonarchConsentPreferencesCategory, showsAlwaysOn: Bool = true)
        -> some View
    {
        VStack(alignment: .leading, spacing: MonarchConsentPreferencesTokens.lineSpacing) {
            Text(category.label)
                .monarchTextStyle(.bodyStrong)
                .foregroundStyle(theme.colors.textDefault)
            Text(category.description)
                .monarchTextStyle(.caption)
                .foregroundStyle(theme.colors.textMuted)
            if showsAlwaysOn && category.isRequired && mode == .categories {
                Text(labels.alwaysOn ?? MonarchConsentPreferencesStrings.alwaysOn)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors.textMuted)
            }
            if MonarchConsentPreferencesTokens.isGlobalPrivacyControlLocked(
                category,
                isGlobalPrivacyControl: isGlobalPrivacyControl
            ) {
                Text(labels.globalPrivacyControl ?? MonarchConsentPreferencesStrings.globalPrivacyControl)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors.textMuted)
            }
        }
        .multilineTextAlignment(.leading)
    }

    private var buttons: some View {
        VStack(alignment: .leading, spacing: MonarchConsentPreferencesTokens.buttonSpacing) {
            ViewThatFits(in: .horizontal) {
                HStack(spacing: MonarchConsentPreferencesTokens.buttonSpacing) { choiceButtons }
                VStack(spacing: MonarchConsentPreferencesTokens.buttonSpacing) { choiceButtons }
            }
            if mode == .categories {
                MonarchButton(
                    labels.save ?? MonarchConsentPreferencesStrings.save,
                    variant: .primary,
                    fullWidth: true
                ) {
                    decide(.custom)
                }
            }
            if MonarchConsentPreferencesTokens.showsWithdraw(mode: mode, allowsWithdraw: allowsWithdraw, record: record)
            {
                MonarchButton(labels.withdraw ?? MonarchConsentPreferencesStrings.withdraw, variant: .ghost) {
                    decide(.withdrawn)
                }
            }
        }
    }

    @ViewBuilder private var choiceButtons: some View {
        MonarchButton(labels.rejectAll ?? MonarchConsentPreferencesStrings.rejectAll, fullWidth: true) {
            decide(.rejectAll)
        }
        MonarchButton(labels.acceptAll ?? MonarchConsentPreferencesStrings.acceptAll, fullWidth: true) {
            decide(.acceptAll)
        }
    }

    private func decide(_ method: MonarchConsentMethod) {
        let result = MonarchConsentPreferencesTokens.choices(
            for: method,
            categories: categories,
            draft: draft,
            isGlobalPrivacyControl: isGlobalPrivacyControl
        )
        draft = result
        onDecide(method, result)
    }
}

#if DEBUG
    #Preview("MonarchConsentPreferences") {
        MonarchPreviewMatrix {
            MonarchConsentPreferences(
                categories: [
                    MonarchConsentPreferencesCategory(
                        id: "necessary",
                        label: "Strictly necessary",
                        description: "Sign-in and security.",
                        isRequired: true
                    ),
                    MonarchConsentPreferencesCategory(
                        id: "analytics",
                        label: "Analytics",
                        description: "How the app is used."
                    ),
                    MonarchConsentPreferencesCategory(
                        id: "ads",
                        label: "Advertising",
                        description: "Ads based on your activity.",
                        isSaleOrSharing: true
                    ),
                ],
                isGlobalPrivacyControl: true
            ) { _, _ in }
        }
    }
#endif
