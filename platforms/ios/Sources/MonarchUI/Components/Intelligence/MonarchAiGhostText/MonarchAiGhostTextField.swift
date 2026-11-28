import SwiftUI

#if os(iOS)
    // UIKit, because SwiftUI's TextField and TextEditor cannot do what an
    // inline completion needs on iOS 17: they do not report the caret or an
    // input method's marked text, cannot draw text after the caret inside
    // the field, cannot turn off the system's own inline predictions, and
    // do not let Tab be taken from the focus system. UITextView does all of it.
    import UIKit

    /// The text view of ``MonarchAiGhostText`` on iOS.
    struct MonarchAiGhostTextField: UIViewRepresentable {
        @Binding var text: String
        let suggestion: String
        let style: MonarchAiGhostTextFieldStyle
        let events: MonarchAiGhostTextFieldEvents

        func makeCoordinator() -> Coordinator {
            Coordinator(self)
        }

        func makeUIView(context: Context) -> MonarchAiGhostTextView {
            let view = MonarchAiGhostTextView()
            view.delegate = context.coordinator
            view.onAccept = { [coordinator = context.coordinator] in coordinator.parent.events.onAccept($0) }
            view.onDismiss = { [coordinator = context.coordinator] in coordinator.parent.events.onDismiss() }
            view.apply(style)
            view.setText(text, caret: style.caretAfterExternalChange)
            return view
        }

        func updateUIView(_ view: MonarchAiGhostTextView, context: Context) {
            context.coordinator.parent = self
            view.keyHint = style.keyHint
            view.apply(style)
            if view.text != text {
                view.setText(text, caret: style.caretAfterExternalChange)
            }
            view.suggestion = suggestion
        }

        func sizeThatFits(_ proposal: ProposedViewSize, uiView: MonarchAiGhostTextView, context: Context) -> CGSize? {
            guard let width = proposal.width, width.isFinite, width > 0 else { return nil }
            let fitted = uiView.sizeThatFits(CGSize(width: width, height: .greatestFiniteMagnitude))
            return CGSize(width: width, height: fitted.height)
        }

        /// Reports edits, caret moves and focus to the component.
        @MainActor
        final class Coordinator: NSObject, UITextViewDelegate {
            var parent: MonarchAiGhostTextField

            init(_ parent: MonarchAiGhostTextField) {
                self.parent = parent
            }

            func textViewDidChange(_ textView: UITextView) {
                let text = textView.text ?? ""
                if parent.text != text { parent.text = text }
                report(textView)
            }

            func textViewDidChangeSelection(_ textView: UITextView) {
                // UIKit resets the typing attributes when the selection moves.
                (textView as? MonarchAiGhostTextView)?.restoreTypingAttributes()
                report(textView)
            }

            func textViewDidBeginEditing(_ textView: UITextView) {
                parent.events.onFocusChange(true)
            }

            func textViewDidEndEditing(_ textView: UITextView) {
                parent.events.onFocusChange(false)
            }

            private func report(_ textView: UITextView) {
                let selection = textView.selectedRange
                parent.events.onInput(
                    MonarchAiGhostTextInput(
                        text: textView.text ?? "",
                        caret: selection.length == 0 ? selection.location : nil,
                        isComposing: textView.markedTextRange != nil
                    )
                )
            }
        }
    }

    /// A text view that draws a suggestion after the typed text.
    ///
    /// The suggestion is drawn by a second, inert text view laid over the
    /// first with the same font, insets and width. It holds the typed text
    /// in a clear colour followed by the suggestion, so the suggestion
    /// starts where the typed text ends and wraps as the text itself would.
    /// Nothing in the editable text view changes, so nothing moves.
    final class MonarchAiGhostTextView: UITextView {
        /// The suggestion to draw. Empty for none.
        var suggestion = "" {
            didSet {
                guard suggestion != oldValue else { return }
                refreshMirror()
                refreshAccessibility()
            }
        }

        /// Called when the suggestion is accepted by a tap, a key or an accessibility action.
        var onAccept: (@MainActor (MonarchAiGhostTextAcceptKind) -> Void)?
        /// Called when the suggestion is dismissed by a key or an accessibility action.
        var onDismiss: (@MainActor () -> Void)?

        private let mirror = UITextView()
        private let tapDelegate = MonarchAiGhostTextTapDelegate()
        /// What decides how the text is drawn, to tell when it has to be restyled.
        private struct Look: Equatable {
            let fontSize: CGFloat
            let fontFamily: String?
            let lineSpacing: CGFloat
            let textColor: Color
            let suggestionColor: Color
            let isRightToLeft: Bool
        }

        /// The key cap drawn after the suggestion, such as "Tab".
        var keyHint: String?

        private var appliedLook: Look?
        private var baseAttributes: [NSAttributedString.Key: Any] = [:]
        // Set from the theme by the first `apply`; nothing is drawn in a system colour before it.
        private var suggestionColor = UIColor.clear
        private var isRightToLeft = false
        private var acceptActionName = ""
        private var dismissActionName = ""

        init() {
            super.init(frame: .zero, textContainer: nil)
            // The component draws the box; the text view is only the text.
            backgroundColor = .clear
            textContainerInset = .zero
            textContainer.lineFragmentPadding = 0
            isScrollEnabled = false
            setContentCompressionResistancePriority(.defaultLow, for: .horizontal)
            // The system's own inline prediction would be drawn in the same
            // place as the suggestion. The QuickType bar stays.
            inlinePredictionType = .no

            mirror.backgroundColor = .clear
            mirror.textContainerInset = .zero
            mirror.textContainer.lineFragmentPadding = 0
            mirror.isEditable = false
            mirror.isSelectable = false
            mirror.isScrollEnabled = false
            mirror.isUserInteractionEnabled = false
            mirror.isAccessibilityElement = false
            mirror.accessibilityElementsHidden = true
            mirror.isHidden = true
            addSubview(mirror)

            let tap = UITapGestureRecognizer(target: self, action: #selector(acceptByTap))
            tapDelegate.view = self
            tap.delegate = tapDelegate
            addGestureRecognizer(tap)
        }

        @available(*, unavailable)
        required init?(coder: NSCoder) {
            nil
        }

        /// Applies the font, the colours, the direction and the accessibility text.
        func apply(_ style: MonarchAiGhostTextFieldStyle) {
            let font =
                style.fontFamily.flatMap { UIFont(name: $0, size: style.fontSize) }
                ?? UIFont.systemFont(ofSize: style.fontSize)
            let paragraph = NSMutableParagraphStyle()
            paragraph.lineSpacing = style.lineSpacing
            paragraph.baseWritingDirection = style.isRightToLeft ? .rightToLeft : .leftToRight
            let textColor = UIColor(style.textColor)
            let attributes: [NSAttributedString.Key: Any] = [
                .font: font,
                .foregroundColor: textColor,
                .paragraphStyle: paragraph,
            ]
            let suggestionColor = UIColor(style.suggestionColor)
            let look = Look(
                fontSize: style.fontSize,
                fontFamily: style.fontFamily,
                lineSpacing: style.lineSpacing,
                textColor: style.textColor,
                suggestionColor: style.suggestionColor,
                isRightToLeft: style.isRightToLeft
            )
            // The text is restyled only when the look changes, never on every keystroke.
            let isChanged = look != appliedLook
            appliedLook = look
            baseAttributes = attributes
            self.suggestionColor = suggestionColor
            isRightToLeft = style.isRightToLeft
            if isChanged {
                self.font = font
                self.textColor = textColor
                semanticContentAttribute = style.isRightToLeft ? .forceRightToLeft : .forceLeftToRight
                mirror.semanticContentAttribute = semanticContentAttribute
                textAlignment = .natural
                if markedTextRange == nil {
                    textStorage.setAttributes(attributes, range: NSRange(location: 0, length: textStorage.length))
                }
                restoreTypingAttributes()
                refreshMirror()
            }
            if isEditable != style.isEnabled {
                isEditable = style.isEnabled
                isSelectable = style.isEnabled
            }
            accessibilityLabel = style.label
            accessibilityHint = style.hint
            acceptActionName = style.acceptActionName
            dismissActionName = style.dismissActionName
            refreshAccessibility()
        }

        /// Sets the text from outside the field and places the caret.
        func setText(_ text: String, caret: Int) {
            attributedText = NSAttributedString(string: text, attributes: baseAttributes)
            let length = (text as NSString).length
            selectedRange = NSRange(location: min(max(0, caret), length), length: 0)
            restoreTypingAttributes()
            refreshMirror()
            invalidateIntrinsicContentSize()
        }

        /// Keeps newly typed text in the field's font, colour and line spacing.
        func restoreTypingAttributes() {
            typingAttributes = baseAttributes
        }

        override func layoutSubviews() {
            super.layoutSubviews()
            guard !mirror.isHidden else { return }
            let width = bounds.width
            let needed = mirror.sizeThatFits(CGSize(width: width, height: .greatestFiniteMagnitude)).height
            // The mirror may be taller than the text; the part below the field is clipped.
            mirror.frame = CGRect(x: 0, y: 0, width: width, height: max(contentSize.height, needed))
        }

        private func refreshMirror() {
            guard !suggestion.isEmpty else {
                mirror.isHidden = true
                mirror.attributedText = nil
                return
            }
            var hidden = baseAttributes
            // The typed text only takes its room in the mirror; it is drawn by the text view itself.
            hidden[.foregroundColor] = UIColor.clear
            var shown = baseAttributes
            shown[.foregroundColor] = suggestionColor
            let content = NSMutableAttributedString(string: text ?? "", attributes: hidden)
            content.append(NSAttributedString(string: suggestion, attributes: shown))
            if let keyHint, !keyHint.isEmpty, let image = keyCap(keyHint) {
                content.append(NSAttributedString(string: "  ", attributes: shown))
                let attachment = NSTextAttachment()
                attachment.image = image
                let font = baseAttributes[.font] as? UIFont
                attachment.bounds = CGRect(
                    x: 0,
                    y: (font?.descender ?? -4) / 2,
                    width: image.size.width,
                    height: image.size.height
                )
                content.append(NSAttributedString(attachment: attachment))
            }
            mirror.attributedText = content
            mirror.isHidden = false
            setNeedsLayout()
        }

        /// A small outlined key cap with the key name, in the suggestion's colour.
        private func keyCap(_ name: String) -> UIImage? {
            guard let base = baseAttributes[.font] as? UIFont else { return nil }
            let font = base.withSize(base.pointSize * 0.72)
            let attributes: [NSAttributedString.Key: Any] = [.font: font, .foregroundColor: suggestionColor]
            let text = NSAttributedString(string: name, attributes: attributes)
            let size = text.size()
            let inset = CGSize(width: 6, height: 2)
            let box = CGSize(width: ceil(size.width) + inset.width * 2, height: ceil(size.height) + inset.height * 2)
            return UIGraphicsImageRenderer(size: box).image { _ in
                let rect = CGRect(origin: .zero, size: box).insetBy(dx: 0.5, dy: 0.5)
                let path = UIBezierPath(roundedRect: rect, cornerRadius: 5)
                suggestionColor.withAlphaComponent(0.7).setStroke()
                path.lineWidth = 1
                path.stroke()
                text.draw(at: CGPoint(x: inset.width, y: inset.height))
            }
        }

        private func refreshAccessibility() {
            guard !suggestion.isEmpty else {
                accessibilityCustomActions = nil
                return
            }
            accessibilityCustomActions = [
                UIAccessibilityCustomAction(name: acceptActionName) { [weak self] _ in
                    self?.onAccept?(.all)
                    return true
                },
                UIAccessibilityCustomAction(name: dismissActionName) { [weak self] _ in
                    self?.onDismiss?()
                    return true
                },
            ]
        }

        // MARK: Tap

        /// Whether a point of the text view is on the suggestion.
        func isOnSuggestion(_ point: CGPoint) -> Bool {
            guard !suggestion.isEmpty, !mirror.isHidden else { return false }
            let typedLength = ((text ?? "") as NSString).length
            guard let start = mirror.position(from: mirror.beginningOfDocument, offset: typedLength),
                let range = mirror.textRange(from: start, to: mirror.endOfDocument)
            else { return false }
            let slop = MonarchAiGhostTextTokens.tapSlop
            let local = convert(point, to: mirror)
            return mirror.selectionRects(for: range).contains { selection in
                !selection.rect.isEmpty && selection.rect.insetBy(dx: -slop, dy: -slop).contains(local)
            }
        }

        @objc private func acceptByTap() {
            guard !suggestion.isEmpty else { return }
            onAccept?(.all)
        }

        // MARK: Hardware keyboard

        override var keyCommands: [UIKeyCommand]? {
            let inherited = super.keyCommands ?? []
            // Without a suggestion, and while an input method composes, every key is the system's.
            guard !suggestion.isEmpty, markedTextRange == nil else { return inherited }
            let commands = MonarchAiGhostTextTokens.handledKeys(isRightToLeft: isRightToLeft).map { handled in
                let command = UIKeyCommand(
                    input: Self.input(for: handled.key),
                    modifierFlags: Self.flags(for: handled.modifiers),
                    action: #selector(handleKey(_:))
                )
                // Tab would move focus and the arrow would move the caret.
                command.wantsPriorityOverSystemBehavior = true
                return command
            }
            return commands + inherited
        }

        @objc private func handleKey(_ command: UIKeyCommand) {
            guard let key = Self.key(for: command.input) else { return }
            let action = MonarchAiGhostTextTokens.keyAction(
                key,
                modifiers: Self.modifiers(for: command.modifierFlags),
                isRightToLeft: isRightToLeft
            )
            switch action {
            case .accept(let kind): onAccept?(kind)
            case .dismiss: onDismiss?()
            case nil: break
            }
        }

        private static func input(for key: MonarchAiGhostTextTokens.Key) -> String {
            switch key {
            case .tab: "\t"
            case .escape: UIKeyCommand.inputEscape
            case .leftArrow: UIKeyCommand.inputLeftArrow
            case .rightArrow: UIKeyCommand.inputRightArrow
            }
        }

        private static func key(for input: String?) -> MonarchAiGhostTextTokens.Key? {
            switch input {
            case "\t": .tab
            case UIKeyCommand.inputEscape: .escape
            case UIKeyCommand.inputLeftArrow: .leftArrow
            case UIKeyCommand.inputRightArrow: .rightArrow
            default: nil
            }
        }

        private static func flags(for modifiers: MonarchAiGhostTextTokens.Modifiers) -> UIKeyModifierFlags {
            var flags: UIKeyModifierFlags = []
            if modifiers.contains(.control) { flags.insert(.control) }
            if modifiers.contains(.option) { flags.insert(.alternate) }
            if modifiers.contains(.command) { flags.insert(.command) }
            if modifiers.contains(.shift) { flags.insert(.shift) }
            return flags
        }

        private static func modifiers(for flags: UIKeyModifierFlags) -> MonarchAiGhostTextTokens.Modifiers {
            var modifiers: MonarchAiGhostTextTokens.Modifiers = []
            if flags.contains(.control) { modifiers.insert(.control) }
            if flags.contains(.alternate) { modifiers.insert(.option) }
            if flags.contains(.command) { modifiers.insert(.command) }
            if flags.contains(.shift) { modifiers.insert(.shift) }
            return modifiers
        }
    }

    /// Lets the accept tap begin only on the suggestion, and alongside the
    /// text view's own gestures, so a tap anywhere else still places the caret.
    private final class MonarchAiGhostTextTapDelegate: NSObject, UIGestureRecognizerDelegate {
        weak var view: MonarchAiGhostTextView?

        func gestureRecognizerShouldBegin(_ gestureRecognizer: UIGestureRecognizer) -> Bool {
            guard let view else { return false }
            return view.isOnSuggestion(gestureRecognizer.location(in: view))
        }

        func gestureRecognizer(
            _ gestureRecognizer: UIGestureRecognizer,
            shouldRecognizeSimultaneouslyWith otherGestureRecognizer: UIGestureRecognizer
        ) -> Bool {
            true
        }
    }
#else
    /// The field of ``MonarchAiGhostText`` where UIKit is not available: the
    /// macOS host, which exists only so the package builds and tests without
    /// a simulator. It is a SwiftUI `TextField` with the suggestion laid over
    /// it, and assumes the caret is at the end of the text.
    struct MonarchAiGhostTextField: View {
        @Binding var text: String
        let suggestion: String
        let style: MonarchAiGhostTextFieldStyle
        let events: MonarchAiGhostTextFieldEvents

        @FocusState private var isFocused: Bool

        var body: some View {
            TextField(style.label, text: $text, axis: .vertical)
                .textFieldStyle(.plain)
                .monarchTextStyle(MonarchAiGhostTextTokens.textStyle)
                .foregroundStyle(style.textColor)
                .focused($isFocused)
                .disabled(!style.isEnabled)
                .frame(maxWidth: .infinity, alignment: .leading)
                .overlay(alignment: .topLeading) {
                    if !suggestion.isEmpty {
                        // The typed text only takes its room; the field draws it.
                        Text(
                            "\(Text(text).foregroundStyle(.clear))\(Text(suggestion).foregroundStyle(style.suggestionColor))"
                        )
                        .monarchTextStyle(MonarchAiGhostTextTokens.textStyle)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .allowsHitTesting(false)
                        .accessibilityHidden(true)
                    }
                }
                .accessibilityLabel(style.label)
                .accessibilityHint(style.hint)
                .onChange(of: text) { _, newValue in
                    events.onInput(
                        MonarchAiGhostTextInput(
                            text: newValue,
                            caret: (newValue as NSString).length,
                            isComposing: false
                        )
                    )
                }
                .onChange(of: isFocused) { _, newValue in
                    events.onFocusChange(newValue)
                }
                .onKeyPress(.tab) {
                    guard !suggestion.isEmpty else { return .ignored }
                    events.onAccept(.all)
                    return .handled
                }
                .onKeyPress(.escape) {
                    guard !suggestion.isEmpty else { return .ignored }
                    events.onDismiss()
                    return .handled
                }
        }
    }
#endif
