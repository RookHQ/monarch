/// Holds one running task and cancels it when it is replaced or when the
/// holder goes away, so a component's async work never outlives the
/// component.
///
/// ```swift
/// @State private var search = MonarchTaskHolder()
/// search.replace { await load(query) }
/// ```
@MainActor
final class MonarchTaskHolder {
    // Read in `deinit`, which is not on the main actor; a `Task` is Sendable.
    private(set) nonisolated(unsafe) var task: Task<Void, Never>?

    init() {}

    /// Cancels the running task, if any, and starts `operation`.
    func replace(_ operation: @escaping @MainActor @Sendable () async -> Void) {
        task?.cancel()
        task = Task { await operation() }
    }

    /// Cancels the running task, if any.
    func cancel() {
        task?.cancel()
        task = nil
    }

    /// Whether a task is held and not cancelled.
    var isRunning: Bool {
        guard let task else { return false }
        return !task.isCancelled
    }

    deinit {
        task?.cancel()
    }
}
