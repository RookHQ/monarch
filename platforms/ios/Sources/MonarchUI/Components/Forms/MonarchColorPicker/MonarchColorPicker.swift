import MonarchTokens
import SwiftUI

/// Picks a colour from named swatches, or any colour with the system colour picker.
///
/// The selection is lower-case `#rrggbb` text, or `nil` for no colour, which
/// is the form to store. Turn it into a SwiftUI colour with
/// ``MonarchColorSwatch/color(hex:)``.
///
/// ```swift
/// MonarchColorPicker("Label colour", selection: $hex)
/// ```
///
/// Accessibility: each swatch is a button named by its name and value
/// ("Blue, #2563eb") with the selected trait on the chosen one, which also
/// carries a ring and a check mark, so the choice does not depend on seeing
/// the colour. The custom row is the standard `ColorPicker`; its label
/// includes the selected value as text. Every swatch has a 44-point touch
/// target. Text and swatches scale with Dynamic Type.
public struct MonarchColorPicker: View {
    private let label: String
    @Binding private var selection: String?
    private let swatches: [MonarchColorSwatch]
    private let helper: String?
    private let allowsCustom: Bool
    private let customLabel: String?
    private let emptyLabel: String?

    @Environment(\.self) private var environment
    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled
    @ScaledMetric(relativeTo: .body) private var swatchSize = MonarchColorPickerTokens.swatchSize
    @ScaledMetric(relativeTo: .body) private var checkSize = MonarchColorPickerTokens.checkSize
    @ScaledMetric(relativeTo: .body) private var checkGlyphSize = MonarchColorPickerTokens.checkGlyphSize
    @ScaledMetric(relativeTo: .body) private var cellSize = MonarchColorPickerTokens.cellSize

    /// A colour picker.
    ///
    /// - Parameters:
    ///   - label: The visible label, already localised.
    ///   - selection: The colour as lower-case `#rrggbb`, or `nil` for none.
    ///   - swatches: The presets. An empty array shows only the custom row.
    ///   - helper: Supporting text under the picker.
    ///   - allowsCustom: Whether the row that opens the system colour picker is shown.
    ///   - customLabel: Replaces the built-in "Custom colour" title of that row.
    ///   - emptyLabel: Replaces the built-in "No colour selected" text.
    public init(
        _ label: String,
        selection: Binding<String?>,
        swatches: [MonarchColorSwatch] = MonarchColorSwatch.defaults,
        helper: String? = nil,
        allowsCustom: Bool = true,
        customLabel: String? = nil,
        emptyLabel: String? = nil
    ) {
        self.label = label
        _selection = selection
        self.swatches = swatches
        self.helper = helper
        self.allowsCustom = allowsCustom
        self.customLabel = customLabel
        self.emptyLabel = emptyLabel
    }

    /// The content of the view.
    public var body: some View {
        let selected = MonarchColorPickerTokens.selectedHex(selection)
        VStack(alignment: .leading, spacing: MonarchColorPickerTokens.stackSpacing) {
            Text(label)
                .monarchTextStyle(.label)
                .foregroundStyle(theme.colors[role: MonarchColorPickerTokens.title(isEnabled: isEnabled)])
                .accessibilityAddTraits(.isHeader)
            if !swatches.isEmpty {
                LazyVGrid(
                    columns: [
                        GridItem(.adaptive(minimum: cellSize), spacing: MonarchColorPickerTokens.gridSpacing)
                    ],
                    alignment: .leading,
                    spacing: MonarchColorPickerTokens.gridSpacing
                ) {
                    ForEach(swatches) { swatch in
                        swatchButton(swatch, isSelected: swatch.hex == selected)
                    }
                }
            }
            if allowsCustom {
                customRow
            }
            if let helper, !helper.isEmpty {
                Text(helper)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors[role: MonarchColorPickerTokens.detail(isEnabled: isEnabled)])
            }
        }
        .accessibilityElement(children: .contain)
    }

    private func swatchButton(_ swatch: MonarchColorSwatch, isSelected: Bool) -> some View {
        Button {
            selection = swatch.hex
        } label: {
            Circle()
                .fill(swatch.color)
                .overlay(
                    Circle().strokeBorder(
                        theme.colors[role: MonarchColorPickerTokens.swatchBorder],
                        lineWidth: MonarchShapes.borderWidth
                    )
                )
                .frame(width: swatchSize, height: swatchSize)
                .overlay {
                    if isSelected { checkMark }
                }
                .padding(MonarchColorPickerTokens.ringGap + MonarchColorPickerTokens.ringWidth)
                .overlay {
                    if isSelected {
                        Circle().strokeBorder(
                            theme.colors[role: MonarchColorPickerTokens.ring],
                            lineWidth: MonarchColorPickerTokens.ringWidth
                        )
                    }
                }
                .frame(width: cellSize, height: cellSize)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(MonarchColorPickerTokens.swatchLabel(swatch))
        .accessibilityAddTraits(isSelected ? .isSelected : [])
    }

    private var checkMark: some View {
        Image(systemName: MonarchColorPickerTokens.checkSymbol)
            .font(.system(size: checkGlyphSize, weight: .bold))
            .foregroundStyle(theme.colors[role: MonarchColorPickerTokens.checkGlyph])
            .frame(width: checkSize, height: checkSize)
            .background(Circle().fill(theme.colors[role: MonarchColorPickerTokens.checkBackground]))
            .accessibilityHidden(true)
    }

    /// The custom colour as the web draws it: a swatch of the colour, a hex field and the selected value below.
    private var customRow: some View {
        let shape = RoundedRectangle(cornerRadius: MonarchTokens.Radius.md, style: .continuous)
        return VStack(alignment: .leading, spacing: MonarchColorPickerTokens.stackSpacing) {
            HStack(spacing: MonarchColorPickerTokens.stackSpacing) {
                shape
                    .fill(selection.flatMap(MonarchColorSwatch.color(hex:)) ?? theme.colors.bgSubtle)
                    .overlay(shape.strokeBorder(theme.colors.borderDefault, lineWidth: MonarchShapes.borderWidth))
                    .frame(width: MonarchAccessibility.minimumHitTarget, height: MonarchAccessibility.minimumHitTarget)
                    .accessibilityHidden(true)
                TextField(customLabel ?? MonarchColorPickerStrings.custom, text: hexText)
                    .textFieldStyle(.plain)
                    .monarchTextStyle(.code)
                    .foregroundStyle(theme.colors[role: MonarchColorPickerTokens.title(isEnabled: isEnabled)])
                    .autocorrectionDisabled()
                    #if os(iOS)
                        .textInputAutocapitalization(.never)
                    #endif
                    .monarchFieldBox(hasError: false, isFocused: false)
                    .frame(maxWidth: 160)
            }
            Text(
                MonarchColorPickerStrings.selected(
                    MonarchColorPickerTokens.valueText(selection: selection, emptyLabel: emptyLabel)
                )
            )
            .monarchTextStyle(.caption)
            .foregroundStyle(theme.colors[role: MonarchColorPickerTokens.detail(isEnabled: isEnabled)])
        }
    }

    /// The text of the hex field: what is selected, and a valid hex typed there selects it.
    private var hexText: Binding<String> {
        Binding(
            get: { selection ?? "" },
            set: { text in
                if let hex = MonarchColorPickerTokens.normalizedHex(text) {
                    selection = hex
                } else if text.isEmpty {
                    selection = nil
                }
            }
        )
    }

    /// The selection as a colour for the system picker. While there is no
    /// selection the picker opens on the theme's text colour; nothing is
    /// written until the person picks.
    private var customColor: Binding<Color> {
        Binding(
            get: { selection.flatMap(MonarchColorSwatch.color(hex:)) ?? theme.colors.textDefault },
            set: { selection = MonarchColorPickerTokens.hex(of: $0, in: environment) }
        )
    }
}

#if DEBUG
    #Preview("MonarchColorPicker") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchColorPicker("Label colour", selection: .constant("#2563eb"), helper: "Shown on the board.")
                MonarchColorPicker("No selection", selection: .constant(nil), allowsCustom: false)
                MonarchColorPicker("Disabled", selection: .constant("#16a34a")).disabled(true)
            }
        }
    }
#endif
