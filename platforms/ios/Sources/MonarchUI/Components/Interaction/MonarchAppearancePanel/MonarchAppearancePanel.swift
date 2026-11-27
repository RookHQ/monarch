import MonarchTokens
import SwiftUI

/// A settings panel for appearance: theme, contrast, accent, motion and
/// density, each a labelled row with a one-line description.
///
/// ```swift
/// @State private var preferences = MonarchAppearancePreferences()
///
/// MonarchAppearancePanel(selection: $preferences)
/// ContentView()
///     .monarchTheme(preferences.appearance)
/// ```
///
/// The panel only changes its binding. Scheme, contrast and accent are the
/// inputs of the Monarch theme, so the app passes
/// ``MonarchAppearancePreferences/appearance`` to `.monarchTheme(_:)`. The
/// defaults follow the device: system scheme, system contrast and system
/// motion. Motion and density are the app's to apply, for example with
/// ``MonarchAppearancePreferences/reducesMotion(systemReducesMotion:)`` and a
/// smaller `controlSize`. The app stores the choices, for example with
/// `@AppStorage`.
///
/// The theme row is a ``MonarchThemeToggle``; contrast, motion and density
/// are segmented `Picker`s; the accent row is a menu `Picker`. Rows are
/// divided by a subtle line, as on the web.
///
/// Accessibility: each row's label and description are read together, then
/// its control. The controls are standard pickers, so VoiceOver reads each
/// choice and marks the current one as selected; the menu picker is named by
/// its row and its value is the current accent. Choices are words, not
/// colours. Text wraps and scales with Dynamic Type.
public struct MonarchAppearancePanel: View {
    private let title: String?
    @Binding private var selection: MonarchAppearancePreferences
    private let rows: [MonarchAppearancePanelRow]
    private let accents: [MonarchAccent]
    private let labels: MonarchAppearancePanelLabels

    @Environment(\.monarchTheme) private var theme

    /// An appearance panel.
    ///
    /// - Parameters:
    ///   - title: A heading above the rows, already localised.
    ///   - selection: The choices. Pass `selection.appearance` to `.monarchTheme(_:)`.
    ///   - rows: The rows to show, in order. The default is theme,
    ///     contrast, motion and density, as on the web; add `.accent` to let
    ///     people choose the accent.
    ///   - accents: The accents offered by the accent row.
    ///   - labels: Replacements for the built-in strings.
    public init(
        _ title: String? = nil,
        selection: Binding<MonarchAppearancePreferences>,
        rows: [MonarchAppearancePanelRow] = MonarchAppearancePanelRow.standard,
        accents: [MonarchAccent] = MonarchTokens.Accents.all,
        labels: MonarchAppearancePanelLabels = MonarchAppearancePanelLabels()
    ) {
        self.title = title
        _selection = selection
        self.rows = rows
        self.accents = accents
        self.labels = labels
    }

    /// The content of the view.
    public var body: some View {
        let visible = MonarchAppearancePanelTokens.visibleRows(rows)
        VStack(alignment: .leading, spacing: 0) {
            if let title {
                Text(title)
                    .monarchTextStyle(.subheading)
                    .foregroundStyle(theme.colors.textDefault)
                    .accessibilityAddTraits(.isHeader)
                    .padding(.bottom, MonarchAppearancePanelTokens.titleSpacing)
            }
            ForEach(Array(visible.enumerated()), id: \.element) { index, section in
                if index > 0 {
                    MonarchDivider()
                }
                row(section)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .contain)
    }

    private func row(_ section: MonarchAppearancePanelRow) -> some View {
        let name = MonarchAppearancePanelTokens.title(section, labels: labels)
        let description = MonarchAppearancePanelTokens.description(section, labels: labels)
        return VStack(alignment: .leading, spacing: MonarchAppearancePanelTokens.controlSpacing) {
            VStack(alignment: .leading, spacing: MonarchAppearancePanelTokens.textSpacing) {
                Text(name)
                    .monarchTextStyle(.bodyStrong)
                    .foregroundStyle(theme.colors.textDefault)
                Text(description)
                    .monarchTextStyle(.label)
                    .foregroundStyle(theme.colors.textMuted)
            }
            .fixedSize(horizontal: false, vertical: true)
            .accessibilityElement(children: .combine)
            control(section, name: name)
        }
        .padding(.vertical, MonarchAppearancePanelTokens.rowPadding)
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    @ViewBuilder
    private func control(_ section: MonarchAppearancePanelRow, name: String) -> some View {
        switch section {
        case .theme:
            MonarchThemeToggle(
                selection: $selection.scheme,
                labels: MonarchAppearancePanelTokens.themeToggleLabels(labels)
            )
        case .contrast:
            segmented(name, selection: $selection.contrast, choices: MonarchAppearancePanelTokens.contrastOrder) {
                MonarchAppearancePanelTokens.name($0, labels: labels)
            }
        case .motion:
            segmented(name, selection: $selection.motion, choices: MonarchAppearancePanelTokens.motionOrder) {
                MonarchAppearancePanelTokens.name($0, labels: labels)
            }
        case .density:
            segmented(name, selection: $selection.density, choices: MonarchAppearancePanelTokens.densityOrder) {
                MonarchAppearancePanelTokens.name($0, labels: labels)
            }
        case .accent:
            accentPicker(name)
        }
    }

    private func segmented<Value: Hashable>(
        _ name: String,
        selection: Binding<Value>,
        choices: [Value],
        title: @escaping (Value) -> String
    ) -> some View {
        Picker(name, selection: selection) {
            ForEach(choices, id: \.self) { choice in
                Text(title(choice)).tag(choice)
            }
        }
        .pickerStyle(.segmented)
        .labelsHidden()
        .controlSize(.large)
    }

    private func accentPicker(_ name: String) -> some View {
        let choices = MonarchAppearancePanelTokens.accentChoices(accents, current: selection.accent)
        let id = Binding<String>(
            get: { selection.accent.id },
            set: { newID in
                if let accent = MonarchAppearancePanelTokens.accent(id: newID, in: choices) {
                    selection.accent = accent
                }
            }
        )
        return MonarchSelectField(
            name,
            selection: id,
            valueText: choices.first { $0.id == selection.accent.id }?.label,
            style: .compact
        ) {
            ForEach(choices, id: \.id) { accent in
                Text(accent.label).tag(accent.id)
            }
        }
    }
}

#if DEBUG
    #Preview("MonarchAppearancePanel") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchAppearancePanel("Appearance", selection: .constant(MonarchAppearancePreferences()))
                MonarchAppearancePanel(
                    selection: .constant(
                        MonarchAppearancePreferences(
                            scheme: .dark,
                            contrast: .more,
                            accent: MonarchTokens.Accents.calendar,
                            motion: .reduce,
                            density: .compact
                        )
                    ),
                    rows: [.accent, .contrast]
                )
                .disabled(true)
            }
        }
    }
#endif
