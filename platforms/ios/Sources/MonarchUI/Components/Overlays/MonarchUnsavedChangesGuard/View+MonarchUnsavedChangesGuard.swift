import SwiftUI

extension View {
    /// Warns before leaving this screen or sheet throws away unsaved changes.
    /// See ``MonarchUnsavedChangesGuard``.
    ///
    /// - Parameters:
    ///   - isPresented: Whether the question is shown. Set it from the app's own Cancel button.
    ///   - hasChanges: Whether there are unsaved changes.
    ///   - labels: Replacements for the built-in strings.
    ///   - guardsBackButton: Whether the back button asks first while there are changes (iOS).
    ///   - onSave: Saves the changes; when given, Save and leave is offered.
    ///   - onLeave: Leaves the screen, after a discard or a successful save.
    /// - Returns: The guarded view.
    public func monarchUnsavedChangesGuard(
        isPresented: Binding<Bool>,
        hasChanges: Bool,
        labels: MonarchUnsavedChangesGuardLabels = MonarchUnsavedChangesGuardLabels(),
        guardsBackButton: Bool = true,
        onSave: (@MainActor () async throws -> Void)? = nil,
        onLeave: @escaping @MainActor (MonarchUnsavedChangesGuardLeaveReason) -> Void
    ) -> some View {
        modifier(
            MonarchUnsavedChangesGuard(
                isPresented: isPresented,
                hasChanges: hasChanges,
                labels: labels,
                guardsBackButton: guardsBackButton,
                onSave: onSave,
                onLeave: onLeave
            )
        )
    }
}
