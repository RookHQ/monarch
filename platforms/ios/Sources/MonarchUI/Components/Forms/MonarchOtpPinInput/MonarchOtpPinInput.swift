import SwiftUI

/// A one-time code or PIN, drawn as one cell per character.
///
/// Underneath it is one standard `TextField` (a `SecureField` when masked)
/// with the one-time-code content type, so the system offers the code from a
/// message, paste works, and VoiceOver meets a single field.
///
/// ```swift
/// MonarchOtpPinInput("Verification code", text: $code) { code in
///     verify(code)
/// }
/// ```
///
/// The binding only ever receives the cleaned code: digits, or digits and the
/// letters A to Z in upper case, never longer than `length`.
///
/// Accessibility: one element whose label is `label` and whose value is the
/// code (hidden by the system when masked). The hint gives the code length,
/// then the error or the helper text. A new error is announced. The error is
/// an icon and text, not colour alone. The cells are at least 44 points high
/// and the whole row focuses the field. The cells keep their left-to-right
/// order in right-to-left layouts, because a code is read left to right.
public struct MonarchOtpPinInput: View {
    private let label: String
    @Binding private var text: String
    private let length: Int
    private let helper: String?
    private let error: String?
    private let mode: MonarchOtpPinInputMode
    private let size: MonarchOtpPinInputSize
    private let isMasked: Bool
    private let lengthHint: String?
    private let groupSize: Int?
    private let onComplete: (@MainActor (String) -> Void)?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled
    @FocusState private var isFocused: Bool
    @State private var draft = ""
    @ScaledMetric private var cellWidth: CGFloat
    @ScaledMetric private var cellHeight: CGFloat
    @ScaledMetric(relativeTo: .body) private var dotSize = MonarchOtpPinInputTokens.dotSize

    /// A one-time code input.
    ///
    /// - Parameters:
    ///   - label: The visible label, already localised.
    ///   - text: The code. It receives only cleaned text.
    ///   - length: The number of characters, at least 1.
    ///   - helper: Supporting text under the cells.
    ///   - error: An error message, for example after a failed check. Replaces the helper text.
    ///   - mode: The characters the code accepts.
    ///   - size: The size of the cells.
    ///   - isMasked: Whether to show dots instead of the characters, for a PIN.
    ///   - lengthHint: Replaces the built-in spoken "Code length: 6".
    ///   - groupSize: Draws a wider gap after every this many cells, such as 3 for "123 456".
    ///   - onComplete: Runs each time typing, pasting or autofill brings the code to `length` characters.
    public init(
        _ label: String,
        text: Binding<String>,
        length: Int = 6,
        helper: String? = nil,
        error: String? = nil,
        mode: MonarchOtpPinInputMode = .numeric,
        size: MonarchOtpPinInputSize = .md,
        isMasked: Bool = false,
        lengthHint: String? = nil,
        groupSize: Int? = nil,
        onComplete: (@MainActor (String) -> Void)? = nil
    ) {
        self.label = label
        _text = text
        self.length = MonarchOtpPinInputTokens.length(length)
        self.helper = helper
        self.error = error
        self.mode = mode
        self.size = size
        self.isMasked = isMasked
        self.lengthHint = lengthHint
        self.groupSize = groupSize
        self.onComplete = onComplete
        _cellWidth = ScaledMetric(wrappedValue: MonarchOtpPinInputTokens.cellWidth(size), relativeTo: .body)
        _cellHeight = ScaledMetric(wrappedValue: MonarchOtpPinInputTokens.cellHeight(size), relativeTo: .body)
    }

    private var activeError: String? {
        guard let error, !error.isEmpty else { return nil }
        return error
    }

    private var code: String {
        MonarchOtpPinInputTokens.clean(text, mode: mode, length: length)
    }

    /// The content of the view.
    public var body: some View {
        VStack(alignment: .leading, spacing: MonarchOtpPinInputTokens.stackSpacing) {
            Text(label)
                .monarchTextStyle(.label)
                .foregroundStyle(isEnabled ? theme.colors.textDefault : theme.colors.textDisabled)
                .accessibilityHidden(true)
            cells
                .overlay { input }
                .contentShape(Rectangle())
                .onTapGesture { isFocused = true }
            supportingText
        }
        .onAppear { draft = code }
        .onChange(of: draft) { _, typed in
            let cleaned = MonarchOtpPinInputTokens.clean(typed, mode: mode, length: length)
            if cleaned != typed { draft = cleaned }
            if let change = MonarchOtpPinInputTokens.change(from: code, typed: typed, mode: mode, length: length) {
                text = change.value
                if change.isComplete { onComplete?(change.value) }
            }
        }
        .onChange(of: text) {
            if draft != code { draft = code }
        }
        .onChange(of: activeError) { _, newError in
            if let newError {
                AccessibilityNotification.Announcement(MonarchOtpPinInputStrings.error(newError)).post()
            }
        }
    }

    /// The real field. Its text and caret are drawn clear, because the cells
    /// show the code; it still takes the keyboard, autofill, paste and VoiceOver.
    private var input: some View {
        Group {
            if isMasked {
                SecureField("", text: $draft, prompt: Text(verbatim: ""))
            } else {
                TextField("", text: $draft, prompt: Text(verbatim: ""))
            }
        }
        .textFieldStyle(.plain)
        .foregroundStyle(Color.clear)
        .tint(Color.clear)
        .focused($isFocused)
        .textContentType(.oneTimeCode)
        .autocorrectionDisabled()
        #if os(iOS)
            .keyboardType(mode == .numeric ? .numberPad : .asciiCapable)
            .textInputAutocapitalization(mode == .alphanumeric ? .characters : .never)
        #endif
        .accessibilityLabel(label)
        .accessibilityHint(
            MonarchOtpPinInputTokens.accessibilityHint(
                length: length,
                lengthHint: lengthHint,
                helper: helper,
                error: activeError
            )
        )
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private var cells: some View {
        let characters = Array(code)
        return HStack(spacing: MonarchOtpPinInputTokens.cellSpacing) {
            ForEach(0..<length, id: \.self) { index in
                if MonarchOtpPinInputTokens.hasSeparator(before: index, groupSize: groupSize) {
                    Spacer(minLength: 0).frame(maxWidth: MonarchOtpPinInputTokens.cellSpacing)
                }
                cell(index, character: index < characters.count ? characters[index] : nil, count: characters.count)
            }
        }
        // A code is read left to right in every language.
        .environment(\.layoutDirection, .leftToRight)
        .accessibilityHidden(true)
    }

    private func cell(_ index: Int, character: Character?, count: Int) -> some View {
        let shape = RoundedRectangle(cornerRadius: MonarchOtpPinInputTokens.cornerRadius, style: .continuous)
        let isActive =
            MonarchOtpPinInputTokens.activeIndex(count: count, length: length, isFocused: isFocused) == index
        let foreground = theme.colors[role: MonarchOtpPinInputTokens.foreground(isEnabled: isEnabled)]
        let border = MonarchOtpPinInputTokens.border(hasError: activeError != nil, isEnabled: isEnabled)
        return ZStack {
            if let character {
                if isMasked {
                    Circle().fill(foreground).frame(width: dotSize, height: dotSize)
                } else {
                    Text(String(character))
                        .monarchTextStyle(MonarchOtpPinInputTokens.textStyle(size))
                        .foregroundStyle(foreground)
                        .lineLimit(1)
                        .minimumScaleFactor(MonarchOtpPinInputTokens.minimumScaleFactor)
                }
            } else if MonarchOtpPinInputTokens.showsCaret(at: index, count: count, length: length, isFocused: isFocused)
            {
                Capsule()
                    .fill(foreground)
                    .frame(
                        width: MonarchOtpPinInputTokens.caretWidth,
                        height: cellHeight * MonarchOtpPinInputTokens.caretHeightRatio
                    )
            }
        }
        .frame(minWidth: 0, maxWidth: cellWidth, minHeight: cellHeight)
        .background(shape.fill(theme.colors[role: MonarchOtpPinInputTokens.background(isEnabled: isEnabled)]))
        .overlay(shape.strokeBorder(theme.colors[role: border], lineWidth: MonarchOtpPinInputTokens.borderWidth))
        .monarchFocusRing(isActive, in: shape)
    }

    @ViewBuilder private var supportingText: some View {
        if let activeError {
            HStack(alignment: .firstTextBaseline, spacing: MonarchOtpPinInputTokens.messageIconSpacing) {
                MonarchIcon(systemName: MonarchOtpPinInputTokens.errorSymbol, size: .sm)
                Text(activeError).monarchTextStyle(.caption)
            }
            .foregroundStyle(theme.colors.dangerStrong)
            .accessibilityHidden(true)
        } else if let helper, !helper.isEmpty {
            Text(helper)
                .monarchTextStyle(.caption)
                .foregroundStyle(theme.colors.textMuted)
                .accessibilityHidden(true)
        }
    }
}

#if DEBUG
    #Preview("MonarchOtpPinInput") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchOtpPinInput("Verification code", text: .constant("123"), helper: "Sent to your phone.")
                MonarchOtpPinInput("Code", text: .constant("482913"), error: "That code has expired.", groupSize: 3)
                MonarchOtpPinInput("PIN", text: .constant("12"), length: 4, size: .lg, isMasked: true)
                MonarchOtpPinInput("Invite code", text: .constant("AB12"), mode: .alphanumeric).disabled(true)
            }
        }
    }
#endif
