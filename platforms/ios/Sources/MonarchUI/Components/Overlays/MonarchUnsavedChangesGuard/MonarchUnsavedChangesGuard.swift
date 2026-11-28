import SwiftUI

/// Warns before leaving a screen or a sheet throws away unsaved changes.
///
/// ```swift
/// Form { ... }
///     .toolbar {
///         ToolbarItem(placement: .cancellationAction) {
///             Button("Cancel") { if isDirty { isAsking = true } else { dismiss() } }
///         }
///     }
///     .monarchUnsavedChangesGuard(
///         isPresented: $isAsking,
///         hasChanges: isDirty,
///         onSave: { try await save() },
///         onLeave: { _ in dismiss() }
///     )
/// ```
///
/// While `hasChanges` is true the guard:
/// - stops a sheet from being swiped away (`interactiveDismissDisabled`);
/// - on iOS, replaces the navigation bar's back button with one that asks
///   first, which also turns off the back swipe;
/// - asks with a Monarch dialog (the web's card, three actions in the web's order) whenever `isPresented` becomes true, so
///   the app's own Cancel or Close button asks too.
///
/// The question offers Save and leave (when `onSave` is given), Discard
/// changes and Keep editing. While the save runs the screen cannot be left;
/// if it throws, an alert shows the error and the person stays. `onLeave`
/// is called after a discard or a successful save: dismiss or navigate there.
///
/// Accessibility: the question is a modal Monarch dialog, so
/// VoiceOver stays inside it, reads the title and message and the escape
/// gesture keeps editing. The destructive choice has the destructive role, which is also
/// its name, not only its colour. Saving shows a labelled progress indicator
/// and is announced; a failure is shown in an alert.
public struct MonarchUnsavedChangesGuard: ViewModifier {
    @Binding private var isPresented: Bool
    private let hasChanges: Bool
    private let labels: MonarchUnsavedChangesGuardLabels
    private let guardsBackButton: Bool
    private let onSave: (@MainActor () async throws -> Void)?
    private let onLeave: @MainActor (MonarchUnsavedChangesGuardLeaveReason) -> Void

    @State private var isSaving = false
    @State private var failure: String?
    @Environment(\.monarchTheme) private var theme

    /// An unsaved-changes guard.
    ///
    /// - Parameters:
    ///   - isPresented: Whether the question is shown. Set it to true from the app's own Cancel
    ///     or Close button when there are changes; the guarding back button sets it too.
    ///   - hasChanges: Whether there are unsaved changes. While false nothing is blocked.
    ///   - labels: Replacements for the built-in strings.
    ///   - guardsBackButton: Whether the navigation bar's back button asks first while there are
    ///     changes. Has no effect outside iOS.
    ///   - onSave: Saves the changes. When given, Save and leave is offered. A thrown error keeps
    ///     the person on the screen and is shown in an alert.
    ///   - onLeave: Leaves the screen, after a discard or a successful save.
    public init(
        isPresented: Binding<Bool>,
        hasChanges: Bool,
        labels: MonarchUnsavedChangesGuardLabels = MonarchUnsavedChangesGuardLabels(),
        guardsBackButton: Bool = true,
        onSave: (@MainActor () async throws -> Void)? = nil,
        onLeave: @escaping @MainActor (MonarchUnsavedChangesGuardLeaveReason) -> Void
    ) {
        _isPresented = isPresented
        self.hasChanges = hasChanges
        self.labels = labels
        self.guardsBackButton = guardsBackButton
        self.onSave = onSave
        self.onLeave = onLeave
    }

    /// The guarded content.
    public func body(content: Content) -> some View {
        let text = MonarchUnsavedChangesGuardTokens.text(labels)
        let isLocked = MonarchUnsavedChangesGuardTokens.isLocked(hasChanges: hasChanges, isSaving: isSaving)
        guarded(content, isLocked: isLocked, text: text)
            .interactiveDismissDisabled(isLocked)
            .overlay {
                if isSaving {
                    ProgressView(text.saving)
                        .monarchTextStyle(.label)
                        .foregroundStyle(theme.colors[role: MonarchUnsavedChangesGuardTokens.savingText])
                        .padding(MonarchUnsavedChangesGuardTokens.savingPadding)
                        .monarchElevation(
                            MonarchUnsavedChangesGuardTokens.savingElevation,
                            fill: MonarchUnsavedChangesGuardTokens.savingFill,
                            in: MonarchShapes.container
                        )
                }
            }
            .monarchDialog(
                isPresented: $isPresented,
                title: text.title,
                message: text.message,
                content: { EmptyView() },
                footer: { dialogFooter(text: text) }
            )
            .alert(
                failure ?? text.saveFailed,
                isPresented: Binding(get: { failure != nil }, set: { if !$0 { failure = nil } }),
                actions: { Button(text.keepEditing, role: .cancel) {} }
            )
    }

    @ViewBuilder
    private func guarded(_ content: Content, isLocked: Bool, text: MonarchUnsavedChangesGuardText) -> some View {
        #if os(iOS)
            let replacesBack = guardsBackButton && isLocked
            content
                .navigationBarBackButtonHidden(replacesBack)
                .toolbar {
                    if replacesBack {
                        ToolbarItem(placement: .topBarLeading) {
                            Button {
                                isPresented = true
                            } label: {
                                Label(text.back, systemImage: MonarchUnsavedChangesGuardTokens.backSymbol)
                                    .labelStyle(.titleAndIcon)
                            }
                            .disabled(isSaving)
                        }
                    }
                }
        #else
            content
        #endif
    }

    /// The footer of the question, in the web's order: Discard changes and Keep editing on the first row,
    /// Save and leave on the second.
    @ViewBuilder
    private func dialogFooter(text: MonarchUnsavedChangesGuardText) -> some View {
        let canSave = onSave != nil
        HStack(spacing: MonarchUnsavedChangesGuardTokens.footerSpacing) {
            MonarchButton(text.discard, variant: .danger) {
                isPresented = false
                onLeave(.discard)
            }
            Spacer(minLength: 0)
            MonarchButton(text.keepEditing) { isPresented = false }
        }
        if canSave {
            MonarchButton(text.save, variant: .primary) {
                isPresented = false
                Task { await save(text: text) }
            }
        }
    }

    private func save(text: MonarchUnsavedChangesGuardText) async {
        guard let onSave, !isSaving else { return }
        isSaving = true
        AccessibilityNotification.Announcement(text.saving).post()
        do {
            try await onSave()
            isSaving = false
            onLeave(.save)
        } catch {
            isSaving = false
            failure = MonarchUnsavedChangesGuardTokens.failureMessage(for: error, fallback: text.saveFailed)
        }
    }
}

#if DEBUG
    #Preview("MonarchUnsavedChangesGuard") {
        MonarchPreviewMatrix {
            NavigationStack {
                Text("Edit the contact")
                    .monarchTextStyle(.body)
                    .navigationTitle("Contact")
                    .monarchUnsavedChangesGuard(isPresented: .constant(false), hasChanges: true, onLeave: { _ in })
            }
            .frame(minHeight: MonarchAccessibility.minimumHitTarget * 3)
        }
    }
#endif
