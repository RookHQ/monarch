import SwiftUI

/// Related toggle buttons in one group: text formatting, list filters, view
/// options. Any number can be on, or at most one.
///
/// ```swift
/// MonarchToggleGroup("Formatting", selection: $styles, options: [
///     MonarchToggleGroupOption("bold", label: "Bold", systemImage: "bold", isIconOnly: true),
///     MonarchToggleGroupOption("italic", label: "Italic", systemImage: "italic", isIconOnly: true),
/// ])
///
/// MonarchToggleGroup("View", selection: $view, options: views, isRequired: true)
/// ```
///
/// Every item is a standard `Toggle` with ``MonarchToggleButtonStyle``, so
/// on items look and behave like standalone toggle buttons. A horizontal
/// group wraps onto a new row when the items do not fit.
///
/// Accessibility: the group is a container named by its title. Each item is
/// read as a switch with its label, its count and its on or off value.
/// An icon-only item is read by its label. With `isRequired`, turning off
/// the selected item has no effect, so one item always stays on.
public struct MonarchToggleGroup<Value: Hashable>: View {
    private enum Selection {
        case single(Binding<Value?>, isRequired: Bool)
        case multiple(Binding<Set<Value>>)
    }

    private let title: String
    private let selection: Selection
    private let options: [MonarchToggleGroupOption<Value>]
    private let variant: MonarchToggleButtonVariant
    private let size: MonarchButtonSize
    private let axis: Axis

    /// A group where any number of items can be on.
    ///
    /// - Parameters:
    ///   - title: The name of the group, already localised. Spoken, not shown.
    ///   - selection: The values of the items that are on.
    ///   - options: The items, in order.
    ///   - variant: How items are drawn while off.
    ///   - size: The height and padding of every item.
    ///   - axis: A wrapping row, or a column.
    public init(
        _ title: String,
        selection: Binding<Set<Value>>,
        options: [MonarchToggleGroupOption<Value>],
        variant: MonarchToggleButtonVariant = .ghost,
        size: MonarchButtonSize = .md,
        axis: Axis = .horizontal
    ) {
        self.title = title
        self.selection = .multiple(selection)
        self.options = options
        self.variant = variant
        self.size = size
        self.axis = axis
    }

    /// A group where at most one item is on.
    ///
    /// - Parameters:
    ///   - title: The name of the group, already localised. Spoken, not shown.
    ///   - selection: The value of the item that is on, or `nil`.
    ///   - options: The items, in order.
    ///   - variant: How items are drawn while off.
    ///   - size: The height and padding of every item.
    ///   - isRequired: Whether turning off the selected item is ignored.
    ///   - axis: A wrapping row, or a column.
    public init(
        _ title: String,
        selection: Binding<Value?>,
        options: [MonarchToggleGroupOption<Value>],
        variant: MonarchToggleButtonVariant = .ghost,
        size: MonarchButtonSize = .md,
        isRequired: Bool = false,
        axis: Axis = .horizontal
    ) {
        self.title = title
        self.selection = .single(selection, isRequired: isRequired)
        self.options = options
        self.variant = variant
        self.size = size
        self.axis = axis
    }

    @Environment(\.monarchToggleGroupTone) private var tone

    /// The content of the view.
    public var body: some View {
        let layout =
            axis == .horizontal
            ? AnyLayout(MonarchFlowLayout(spacing: MonarchToggleGroupTokens.itemSpacing))
            : AnyLayout(VStackLayout(alignment: .leading, spacing: MonarchToggleGroupTokens.itemSpacing))
        layout {
            ForEach(options, id: \.value) { option in
                Toggle(isOn: isOn(option.value)) {
                    MonarchToggleGroupItemLabel(option: option)
                }
                .toggleStyle(.monarchButton(variant, size: size, tone: tone))
                .disabled(option.isDisabled)
                .accessibilityLabel(
                    MonarchToggleGroupTokens.accessibilityLabel(label: option.label, count: option.count)
                )
            }
        }
        .accessibilityElement(children: .contain)
        .accessibilityLabel(title)
    }

    private func isOn(_ value: Value) -> Binding<Bool> {
        switch selection {
        case .multiple(let binding):
            Binding(
                get: { binding.wrappedValue.contains(value) },
                set: {
                    binding.wrappedValue = MonarchToggleGroupTokens.toggled(
                        binding.wrappedValue,
                        value: value,
                        isOn: $0
                    )
                }
            )
        case .single(let binding, let isRequired):
            Binding(
                get: { binding.wrappedValue == value },
                set: {
                    binding.wrappedValue = MonarchToggleGroupTokens.toggled(
                        binding.wrappedValue,
                        value: value,
                        isOn: $0,
                        isRequired: isRequired
                    )
                }
            )
        }
    }
}

private struct MonarchToggleGroupToneKey: EnvironmentKey {
    static let defaultValue: MonarchTone = .accent
}

extension EnvironmentValues {
    /// The tone of an item that is on. Components inside the module set it to neutral where the web shows a grey fill.
    var monarchToggleGroupTone: MonarchTone {
        get { self[MonarchToggleGroupToneKey.self] }
        set { self[MonarchToggleGroupToneKey.self] = newValue }
    }
}

/// The symbol, label and count of one item.
private struct MonarchToggleGroupItemLabel<Value: Hashable>: View {
    let option: MonarchToggleGroupOption<Value>

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled

    var body: some View {
        let showsText = MonarchToggleGroupTokens.showsText(
            isIconOnly: option.isIconOnly,
            systemImage: option.systemImage
        )
        HStack(spacing: MonarchToggleGroupTokens.contentSpacing) {
            if let systemImage = option.systemImage {
                MonarchIcon(systemName: systemImage, size: .sm)
            }
            if showsText {
                Text(option.label)
                if let count = option.count, !count.isEmpty {
                    Text(count)
                        .foregroundStyle(theme.colors[role: MonarchToggleGroupTokens.count(isEnabled: isEnabled)])
                }
            }
        }
    }
}

#if DEBUG
    #Preview("MonarchToggleGroup") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchToggleGroup(
                    "Formatting",
                    selection: .constant(Set(["bold"])),
                    options: [
                        MonarchToggleGroupOption("bold", label: "Bold", systemImage: "bold", isIconOnly: true),
                        MonarchToggleGroupOption("italic", label: "Italic", systemImage: "italic", isIconOnly: true),
                        MonarchToggleGroupOption(
                            "underline",
                            label: "Underline",
                            systemImage: "underline",
                            isIconOnly: true
                        ),
                    ]
                )
                MonarchToggleGroup(
                    "Status",
                    selection: .constant(Optional("open")),
                    options: [
                        MonarchToggleGroupOption("open", label: "Open", count: "12"),
                        MonarchToggleGroupOption("closed", label: "Closed", count: "3"),
                        MonarchToggleGroupOption("archived", label: "Archived", isDisabled: true),
                    ],
                    variant: .outline,
                    isRequired: true
                )
            }
        }
    }
#endif
