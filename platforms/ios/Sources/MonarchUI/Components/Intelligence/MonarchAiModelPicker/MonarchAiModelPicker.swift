import SwiftUI

/// Lets people choose which model answers: a button showing the current
/// model that opens a sheet listing the models by provider, with their
/// capabilities, context size, speed, cost and the state of their own
/// provider keys.
///
/// ```swift
/// MonarchAiModelPicker(selection: $modelID, models: models) { model in
///     showKeySettings(for: model)
/// }
/// ```
///
/// Choosing a model sets the selection and closes the sheet. A model that
/// runs on the workspace's own key and has none set up calls
/// `onConfigureKey` instead; without `onConfigureKey` it cannot be chosen,
/// like a disabled model. A `Menu` cannot show badges and two lines per
/// model, so the list is a `List` in a sheet.
///
/// Accessibility: the button is named "Model" and its value is the selected
/// model and provider. Each model is a button named by the model and its key
/// badge, with the description, capabilities, context, speed and cost in
/// words as its value; the selected one has the selected trait and a check
/// mark. A model whose key is missing has the hint "Select to add an API
/// key."; one that cannot be chosen is dimmed and reads "Unavailable".
public struct MonarchAiModelPicker: View {
    @Binding private var selection: String?
    private let models: [MonarchAiModelOption]
    private let size: MonarchButtonSize
    private let labels: MonarchAiModelPickerLabels
    private let onConfigureKey: (@MainActor (MonarchAiModelOption) -> Void)?

    @Environment(\.monarchTheme) private var theme
    @State private var isPresented = false

    /// A model picker.
    ///
    /// - Parameters:
    ///   - selection: The id of the selected model, `nil` for none.
    ///   - models: The models in display order. Grouped by provider in first-seen order.
    ///   - size: The height and padding of the button.
    ///   - startsOpen: Presents the list when the view appears, for a screen that opens on the choice.
    ///   - labels: Replacements for the built-in strings.
    ///   - onConfigureKey: Runs when someone chooses a model whose key is
    ///     missing, to add the key. Without it such models cannot be chosen.
    public init(
        selection: Binding<String?>,
        models: [MonarchAiModelOption],
        size: MonarchButtonSize = .md,
        startsOpen: Bool = false,
        labels: MonarchAiModelPickerLabels = MonarchAiModelPickerLabels(),
        onConfigureKey: (@MainActor (MonarchAiModelOption) -> Void)? = nil
    ) {
        _selection = selection
        self.models = models
        self.size = size
        _isPresented = State(initialValue: startsOpen)
        self.labels = labels
        self.onConfigureKey = onConfigureKey
    }

    private typealias Tokens = MonarchAiModelPickerTokens

    /// The content of the view.
    public var body: some View {
        let selected = models.first { $0.id == selection }
        Button {
            isPresented = true
        } label: {
            HStack(spacing: Tokens.triggerSpacing) {
                if let selected {
                    Text(selected.name)
                    Text(selected.provider)
                        .foregroundStyle(theme.colors.textMuted)
                } else {
                    Text(labels.placeholder ?? MonarchAiModelPickerStrings.placeholder)
                        .foregroundStyle(theme.colors.textMuted)
                }
                MonarchIcon(systemName: Tokens.chevronSymbol, size: .sm)
            }
        }
        .buttonStyle(.monarch(.secondary, size: size))
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(labels.label ?? MonarchAiModelPickerStrings.label)
        .accessibilityValue(Tokens.triggerValue(selected, labels: labels))
        .accessibilityAddTraits(.isButton)
        // A dropdown under the trigger, as on the web; a compact screen keeps it a popover instead of a sheet.
        .popover(isPresented: $isPresented, attachmentAnchor: .rect(.bounds), arrowEdge: .top) {
            MonarchAiModelPickerList(
                models: models,
                selection: selection,
                canConfigureKey: onConfigureKey != nil,
                labels: labels,
                onChoose: choose,
                onDone: { isPresented = false }
            )
            .frame(width: Tokens.popoverWidth)
            .frame(minHeight: Tokens.popoverMinimumHeight, maxHeight: Tokens.popoverMaximumHeight)
            .presentationCompactAdaptation(.popover)
        }
    }

    private func choose(_ model: MonarchAiModelOption) {
        switch Tokens.state(model, canConfigureKey: onConfigureKey != nil) {
        case .blocked:
            return
        case .needsKey:
            isPresented = false
            onConfigureKey?(model)
        case .available:
            isPresented = false
            selection = model.id
        }
    }
}

#if DEBUG
    #Preview("MonarchAiModelPicker") {
        let models = [
            MonarchAiModelOption(
                id: "rook-large",
                provider: "Rook",
                name: "Rook Large",
                description: "Best for long documents",
                capabilities: [.vision, .tools, .reasoning],
                contextTokens: 200_000,
                speed: .balanced,
                costTier: 3
            ),
            MonarchAiModelOption(id: "rook-fast", provider: "Rook", name: "Rook Fast", speed: .fast, costTier: 1),
        ]
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchAiModelPicker(selection: .constant("rook-large"), models: models)
                MonarchAiModelPicker(selection: .constant(nil), models: models, size: .sm)
                MonarchAiModelPickerList(
                    models: models,
                    selection: "rook-large",
                    canConfigureKey: false,
                    labels: MonarchAiModelPickerLabels(),
                    onChoose: { _ in },
                    onDone: {}
                )
            }
        }
    }
#endif
