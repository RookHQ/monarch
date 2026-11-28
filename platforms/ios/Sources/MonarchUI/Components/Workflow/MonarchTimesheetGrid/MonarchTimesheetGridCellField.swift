import SwiftUI

/// The compact field of one editable cell of the timesheet grid. Kept here
/// because `MonarchTextField` has a visible label and helper text, which a
/// grid cell cannot hold; it reads the same field tokens.
struct MonarchTimesheetGridCellField: View {
    let key: MonarchTimesheetGridCellKey
    let minutes: Int
    let label: String
    let error: String?
    let isSaving: Bool
    let isNonWorking: Bool
    let maximum: Int
    let labels: MonarchTimesheetGridLabels
    let focus: FocusState<MonarchTimesheetGridCellKey?>.Binding
    let onCommit: @MainActor (Int) -> Void
    let onProblem: @MainActor (String?) -> Void
    let onMove: @MainActor (Int, Int) -> Void

    @State private var draft: String?
    @Environment(\.monarchTheme) private var theme

    private typealias Tokens = MonarchTimesheetGridTokens

    private var isFocused: Bool { focus.wrappedValue == key }

    private var savedText: String {
        minutes > 0 ? Tokens.durationText(minutes, labels: labels) : ""
    }

    var body: some View {
        let shape = MonarchShapes.control
        let appearance = Tokens.cellAppearance(
            isEditable: true,
            isLocked: false,
            isNonWorking: isNonWorking,
            hasError: error != nil,
            isSaving: isSaving
        )
        TextField(
            label,
            text: Binding(get: { draft ?? savedText }, set: { draft = $0 }),
            prompt: Text(verbatim: Tokens.emptyMark).foregroundStyle(theme.colors.textSubtle)
        )
        .textFieldStyle(.plain)
        .autocorrectionDisabled()
        #if os(iOS)
            .keyboardType(.numbersAndPunctuation)
            .textInputAutocapitalization(.never)
        #endif
        .multilineTextAlignment(.center)
        .monarchTextStyle(.body)
        .monarchTabularNumber()
        .foregroundStyle(theme.colors[role: appearance.foreground])
        .focused(focus, equals: key)
        .disabled(isSaving)
        .onSubmit {
            if commit() { onMove(1, 0) }
        }
        .onKeyPress(.upArrow) { move(-1, 0) }
        .onKeyPress(.downArrow) { move(1, 0) }
        .onKeyPress(.escape) { restore() }
        .onChange(of: isFocused) { _, focused in
            if !focused { _ = commit() }
        }
        .onChange(of: minutes) { _, _ in draft = nil }
        .accessibilityLabel(label)
        .accessibilityHint(Tokens.cellHint(error: error, isSaving: isSaving, labels: labels))
        .padding(Tokens.fieldPadding)
        // A text field asks for a wide ideal size; the column takes its width from the day, not from the field.
        .frame(minWidth: 0, idealWidth: 0, maxWidth: .infinity, minHeight: Tokens.minCellHeight)
        .background(shape.fill(theme.colors.bgSurface))
        .overlay(shape.strokeBorder(theme.colors[role: appearance.border], lineWidth: MonarchShapes.borderWidth))
        .monarchFocusRing(isFocused, in: shape)
        .contentShape(Rectangle())
        .onTapGesture { focus.wrappedValue = key }
    }

    /// Reads the draft and saves it. Returns whether the text was accepted.
    @discardableResult
    private func commit() -> Bool {
        guard let text = draft else { return true }
        let entry = Tokens.read(text, maximum: maximum)
        if let problem = Tokens.problem(entry, maximum: maximum, labels: labels) {
            onProblem(problem)
            return false
        }
        onProblem(nil)
        if case .save(let value) = entry {
            if value == minutes { draft = nil }
            onCommit(value)
        }
        return true
    }

    private func move(_ rowStep: Int, _ dayStep: Int) -> KeyPress.Result {
        onMove(rowStep, dayStep)
        return .handled
    }

    private func restore() -> KeyPress.Result {
        guard draft != nil || error != nil else { return .ignored }
        draft = nil
        onProblem(nil)
        return .handled
    }
}
