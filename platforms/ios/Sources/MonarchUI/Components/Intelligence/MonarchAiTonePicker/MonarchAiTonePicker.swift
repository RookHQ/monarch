import SwiftUI

/// Chooses the tone the assistant writes in.
///
/// The tones are Professional, Technical, Casual and Executive unless you
/// pass your own. Pass a binding for `customText` to add a Custom option
/// with a text field for the person's own description.
///
/// ```swift
/// MonarchAiTonePicker(selection: $tone)
/// MonarchAiTonePicker(selection: $tone, customText: $custom, variant: .compact)
/// ```
///
/// The `cards` variant is a column of rows, each with a radio mark, the name
/// and a one-line description. The `compact` variant is the standard menu
/// `Picker`. Disable either with the standard `disabled(_:)` modifier.
///
/// Accessibility: in `cards` each row is a button read as the tone name,
/// with the description as its hint and the selected trait on the current
/// tone; the selected row also shows a filled radio mark and a thicker
/// border, so the choice does not depend on colour. The group is labelled
/// with `label`. Rows are at least 44 points high and wrap at large text
/// sizes. `compact` keeps the system picker's behaviour.
public struct MonarchAiTonePicker: View {
    @Binding private var selection: String
    private let customText: Binding<String>?
    private let tones: [MonarchAiTone]
    private let variant: MonarchAiTonePickerVariant
    private let label: String?
    private let customLabel: String?
    private let customDetail: String?
    private let customFieldLabel: String?
    private let customPrompt: String?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled

    /// A tone picker.
    ///
    /// - Parameters:
    ///   - selection: The id of the selected tone, or
    ///     ``MonarchAiTone/customID`` for the Custom option.
    ///   - customText: The person's own description of the tone. Passing a
    ///     binding adds the Custom option and its text field.
    ///   - tones: The tones to offer.
    ///   - variant: Rows with descriptions, or a menu picker.
    ///   - label: Replaces the built-in name of the choice, "Tone".
    ///   - customLabel: Replaces the built-in name of the Custom option.
    ///   - customDetail: Replaces the built-in description of the Custom option.
    ///   - customFieldLabel: Replaces the built-in label of the custom text field.
    ///   - customPrompt: Replaces the built-in placeholder of the custom text field.
    public init(
        selection: Binding<String>,
        customText: Binding<String>? = nil,
        tones: [MonarchAiTone] = MonarchAiTone.defaults,
        variant: MonarchAiTonePickerVariant = .cards,
        label: String? = nil,
        customLabel: String? = nil,
        customDetail: String? = nil,
        customFieldLabel: String? = nil,
        customPrompt: String? = nil
    ) {
        _selection = selection
        self.customText = customText
        self.tones = tones
        self.variant = variant
        self.label = label
        self.customLabel = customLabel
        self.customDetail = customDetail
        self.customFieldLabel = customFieldLabel
        self.customPrompt = customPrompt
    }

    private var title: String { label ?? MonarchAiTonePickerStrings.label }

    private var options: [MonarchAiTone] {
        MonarchAiTonePickerTokens.options(
            tones: tones,
            custom: customText == nil
                ? nil
                : MonarchAiTone(
                    id: MonarchAiTone.customID,
                    label: customLabel ?? MonarchAiTonePickerStrings.custom,
                    detail: customDetail ?? MonarchAiTonePickerStrings.customDetail
                )
        )
    }

    /// The content of the view.
    public var body: some View {
        VStack(alignment: .leading, spacing: MonarchAiTonePickerTokens.stackSpacing) {
            switch variant {
            case .cards: cards
            case .compact: menu
            }
            if let customText,
                MonarchAiTonePickerTokens.showsCustomField(selection: selection, allowsCustom: true)
            {
                MonarchTextField(
                    customFieldLabel ?? MonarchAiTonePickerStrings.customField,
                    text: customText,
                    prompt: customPrompt ?? MonarchAiTonePickerStrings.customPrompt
                )
            }
        }
    }

    private var cards: some View {
        VStack(alignment: .leading, spacing: MonarchAiTonePickerTokens.stackSpacing) {
            Text(title)
                .monarchTextStyle(.label)
                .foregroundStyle(isEnabled ? theme.colors.textDefault : theme.colors.textDisabled)
                .accessibilityHidden(true)
            ForEach(options) { tone in
                MonarchAiToneRow(tone: tone, isSelected: tone.id == selection) {
                    selection = tone.id
                }
            }
        }
        .accessibilityElement(children: .contain)
        .accessibilityLabel(title)
    }

    private var menu: some View {
        HStack(spacing: MonarchAiTonePickerTokens.menuSpacing) {
            Text(title)
                .monarchTextStyle(.label)
                .foregroundStyle(isEnabled ? theme.colors.textDefault : theme.colors.textDisabled)
                .accessibilityHidden(true)
            MonarchSelectField(
                title,
                selection: $selection,
                valueText: options.first { $0.id == selection }?.label,
                style: .compact
            ) {
                ForEach(options) { tone in
                    Text(tone.label).tag(tone.id)
                }
            }
        }
    }
}

/// One tone as a row: a radio mark, the name and the description.
private struct MonarchAiToneRow: View {
    let tone: MonarchAiTone
    let isSelected: Bool
    let action: @MainActor () -> Void

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled

    var body: some View {
        let appearance = MonarchAiTonePickerTokens.appearance(isSelected: isSelected, isEnabled: isEnabled)
        let shape = MonarchShapes.control
        Button(action: action) {
            HStack(alignment: .firstTextBaseline, spacing: MonarchAiTonePickerTokens.markSpacing) {
                MonarchIcon(systemName: MonarchAiTonePickerTokens.symbol(isSelected: isSelected), size: .sm)
                    .foregroundStyle(theme.colors[role: appearance.mark])
                VStack(alignment: .leading, spacing: MonarchAiTonePickerTokens.textSpacing) {
                    Text(tone.label)
                        .monarchTextStyle(.label)
                        .foregroundStyle(theme.colors[role: appearance.label])
                    if !tone.detail.isEmpty {
                        Text(tone.detail)
                            .monarchTextStyle(.caption)
                            .foregroundStyle(theme.colors[role: appearance.detail])
                    }
                }
                .multilineTextAlignment(.leading)
            }
            .padding(.horizontal, MonarchAiTonePickerTokens.horizontalPadding)
            .padding(.vertical, MonarchAiTonePickerTokens.verticalPadding)
            .frame(maxWidth: .infinity, minHeight: MonarchAccessibility.minimumHitTarget, alignment: .leading)
            .background(shape.fill(theme.colors[role: appearance.background]))
            .overlay(
                shape.strokeBorder(
                    theme.colors[role: appearance.border],
                    lineWidth: MonarchAiTonePickerTokens.borderWidth(isSelected: isSelected)
                )
            )
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(tone.label)
        .accessibilityHint(tone.detail)
        .accessibilityAddTraits(isSelected ? [.isButton, .isSelected] : .isButton)
        .monarchAnimation(.fast, value: isSelected)
    }
}

#if DEBUG
    #Preview("MonarchAiTonePicker") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchAiTonePicker(selection: .constant("technical"))
                MonarchAiTonePicker(selection: .constant(MonarchAiTone.customID), customText: .constant(""))
                MonarchAiTonePicker(selection: .constant("casual"), variant: .compact)
                MonarchAiTonePicker(selection: .constant("casual")).disabled(true)
            }
        }
    }
#endif
