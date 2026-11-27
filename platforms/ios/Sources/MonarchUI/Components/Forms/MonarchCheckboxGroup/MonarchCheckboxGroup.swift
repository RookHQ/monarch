import SwiftUI

/// A labelled set of checkboxes for picking any number of choices, with an
/// optional "Select all" that is checked, mixed or clear.
///
/// ```swift
/// MonarchCheckboxGroup(
///     "Notify me about",
///     selection: $topics,
///     options: [
///         MonarchCheckboxGroupOption(id: "comments", label: "Comments"),
///         MonarchCheckboxGroupOption(id: "mentions", label: "Mentions", description: "When someone tags you."),
///         MonarchCheckboxGroupOption(id: "billing", label: "Billing", isDisabled: true),
///     ],
///     helper: "Sent by email.",
///     showsSelectAll: true
/// )
/// ```
///
/// Each choice is a standard `Toggle` with the Monarch checkbox style; select
/// all is a standard `Toggle(sources:isOn:)`, so its mixed state is the
/// system's. A group cannot enforce a minimum by itself: check the count and
/// pass `error`.
///
/// Accessibility: the group is a container named by `label`, with the help
/// text, the error (read as "Error: …") and "Required" as its hint; a new
/// error is announced. Each checkbox is read with its label and description
/// and has a touch target of at least 44 points. The mixed state is the
/// system's mixed value and a minus glyph, not colour alone.
public struct MonarchCheckboxGroup: View {
    private let label: String
    @Binding private var selection: Set<String>
    private let options: [MonarchCheckboxGroupOption]
    private let helper: String?
    private let error: String?
    private let layout: MonarchCheckboxGroupLayout
    private let isRequired: Bool
    private let showsSelectAll: Bool
    private let labels: MonarchCheckboxGroupLabels

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize

    /// A checkbox group.
    ///
    /// - Parameters:
    ///   - label: The visible group label, already localised. It also names the group.
    ///   - selection: The ids of the checked choices.
    ///   - options: The choices, in order.
    ///   - helper: Help text under the label.
    ///   - error: An error message, such as "Choose at least one topic". Shown with an icon and announced.
    ///   - layout: One choice per line, or side by side with wrapping.
    ///   - isRequired: Shows a mark after the label and adds "Required" to the hint.
    ///   - showsSelectAll: Adds a "Select all" checkbox above the choices.
    ///   - labels: Replacements for the built-in strings.
    public init(
        _ label: String,
        selection: Binding<Set<String>>,
        options: [MonarchCheckboxGroupOption],
        helper: String? = nil,
        error: String? = nil,
        layout: MonarchCheckboxGroupLayout = .vertical,
        isRequired: Bool = false,
        showsSelectAll: Bool = false,
        labels: MonarchCheckboxGroupLabels = MonarchCheckboxGroupLabels()
    ) {
        self.label = label
        _selection = selection
        self.options = options
        self.helper = helper
        self.error = error
        self.layout = layout
        self.isRequired = isRequired
        self.showsSelectAll = showsSelectAll
        self.labels = labels
    }

    /// The content of the view.
    public var body: some View {
        MonarchFormField(label, helper: helper, error: error, isRequired: isRequired, labels: labels.field) {
            VStack(alignment: .leading, spacing: MonarchCheckboxGroupTokens.itemSpacing) {
                if showsSelectAll { selectAll }
                choices
                    .padding(.leading, showsSelectAll ? MonarchCheckboxGroupTokens.nestedIndent : 0)
            }
            .accessibilityElement(children: .contain)
        }
    }

    private var selectAll: some View {
        let enabled = MonarchCheckboxGroupTokens.enabledOptions(options)
        return Toggle(sources: enabled.map(binding), isOn: \.self) {
            Text(labels.selectAll ?? MonarchCheckboxGroupStrings.selectAll)
        }
        .toggleStyle(.monarchCheckbox)
        .disabled(enabled.isEmpty)
    }

    @ViewBuilder private var choices: some View {
        if MonarchCheckboxGroupTokens.stacks(layout: layout, isAccessibilitySize: dynamicTypeSize.isAccessibilitySize) {
            VStack(alignment: .leading, spacing: MonarchCheckboxGroupTokens.itemSpacing) {
                ForEach(options) { choice($0) }
            }
        } else {
            MonarchFlowLayout(spacing: MonarchCheckboxGroupTokens.rowSpacing, alignment: .top) {
                ForEach(options) { choice($0) }
            }
        }
    }

    private func choice(_ option: MonarchCheckboxGroupOption) -> some View {
        Toggle(isOn: binding(option)) {
            VStack(alignment: .leading, spacing: MonarchCheckboxGroupTokens.descriptionSpacing) {
                Text(option.label)
                if let description = option.description, !description.isEmpty {
                    Text(description)
                        .monarchTextStyle(.caption)
                        .foregroundStyle(
                            theme.colors[
                                role: MonarchCheckboxGroupTokens.descriptionColor(
                                    isEnabled: isEnabled && !option.isDisabled
                                )
                            ]
                        )
                }
            }
        }
        .toggleStyle(.monarchCheckbox)
        .disabled(option.isDisabled)
    }

    private func binding(_ option: MonarchCheckboxGroupOption) -> Binding<Bool> {
        Binding(
            get: { selection.contains(option.id) },
            set: { isOn in
                selection = MonarchCheckboxGroupTokens.setting(option.id, isOn: isOn, in: selection)
            }
        )
    }
}

#if DEBUG
    #Preview("MonarchCheckboxGroup") {
        let options = [
            MonarchCheckboxGroupOption(id: "comments", label: "Comments"),
            MonarchCheckboxGroupOption(id: "mentions", label: "Mentions", description: "When someone tags you."),
            MonarchCheckboxGroupOption(id: "billing", label: "Billing", isDisabled: true),
        ]
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchCheckboxGroup(
                    "Notify me about",
                    selection: .constant(["comments"]),
                    options: options,
                    helper: "Sent by email.",
                    showsSelectAll: true
                )
                MonarchCheckboxGroup(
                    "Days",
                    selection: .constant([]),
                    options: options,
                    error: "Choose at least one.",
                    layout: .horizontal,
                    isRequired: true
                )
                MonarchCheckboxGroup("Disabled", selection: .constant(["comments"]), options: options)
                    .disabled(true)
            }
        }
    }
#endif
