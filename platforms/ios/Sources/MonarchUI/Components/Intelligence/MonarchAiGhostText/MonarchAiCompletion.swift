import Foundation
import Observation
import os

/// Inline completion state for one text field: it debounces requests,
/// cancels the stale ones, never suggests while an input method is
/// composing, and accepts a suggestion whole or word by word.
///
/// ``MonarchAiGhostText`` wires it to a field. Keep one yourself to read or
/// drive it, or call `update(text:cursor:)` from your own editor:
///
/// ```swift
/// @State private var completion = MonarchAiCompletion { text, cursor in
///     try await myService.complete(text, at: cursor)
/// }
/// ```
///
/// `complete` receives the text and the cursor, as a UTF-16 offset into the
/// text, and returns the text to insert at the cursor, or `nil` or an empty
/// string for no suggestion. It runs in a task that is cancelled when its
/// answer is no longer wanted, so use `Task.isCancelled` or cancellable calls.
/// Monarch does no networking: the closure decides where the request goes.
@MainActor
@Observable
public final class MonarchAiCompletion {
    /// Where the completion is.
    public private(set) var status: MonarchAiCompletionStatus = .idle
    /// The suggested continuation. Empty when there is none.
    public private(set) var suggestion = ""
    /// The text the suggestion, or the pending request, belongs to.
    public private(set) var text = ""
    /// The cursor the suggestion goes in at, as a UTF-16 offset into `text`.
    public private(set) var cursor = 0
    /// Whether suggestions are on. Change it with `setEnabled(_:)`.
    public private(set) var isEnabled: Bool

    /// Called when `complete` throws, not when it is cancelled.
    @ObservationIgnored public var onError: (@MainActor (any Error) -> Void)?

    @ObservationIgnored private let complete: @Sendable (String, Int) async throws -> String?
    @ObservationIgnored private let debounce: Duration
    @ObservationIgnored private let minimumCharacters: Int
    @ObservationIgnored private let sleep: @Sendable (Duration) async throws -> Void
    @ObservationIgnored private var sequence = 0
    @ObservationIgnored private var isComposing = false
    @ObservationIgnored private var task: Task<Void, Never>?
    /// The request in flight, so it can be cancelled when the completion is released.
    @ObservationIgnored private let pending = MonarchAiCompletionRequest()

    /// A completion.
    ///
    /// - Parameters:
    ///   - debounce: How long typing must pause before a request is sent.
    ///   - minimumCharacters: The fewest characters before the cursor, without the white space around
    ///     them, for a request.
    ///   - isEnabled: Whether suggestions are on.
    ///   - complete: Asks the model for a continuation of the text at the cursor.
    public convenience init(
        debounce: Duration = .milliseconds(300),
        minimumCharacters: Int = 3,
        isEnabled: Bool = true,
        complete: @escaping @Sendable (String, Int) async throws -> String?
    ) {
        self.init(
            debounce: debounce,
            minimumCharacters: minimumCharacters,
            isEnabled: isEnabled,
            sleep: { try await Task.sleep(for: $0) },
            complete: complete
        )
    }

    /// The designated initialiser, with the timer the tests replace.
    init(
        debounce: Duration,
        minimumCharacters: Int,
        isEnabled: Bool,
        sleep: @escaping @Sendable (Duration) async throws -> Void,
        complete: @escaping @Sendable (String, Int) async throws -> String?
    ) {
        self.debounce = debounce
        self.minimumCharacters = minimumCharacters
        self.isEnabled = isEnabled
        self.sleep = sleep
        self.complete = complete
    }

    deinit {
        pending.cancel()
    }

    /// Tells the completion what the field holds now. It debounces a
    /// request, keeps the rest of a suggestion the person is typing through,
    /// and does nothing when neither value changed.
    ///
    /// - Parameters:
    ///   - text: The whole text of the field.
    ///   - cursor: The cursor, as a UTF-16 offset into `text`.
    public func update(text: String, cursor: Int) {
        let new = text as NSString
        let cursor = min(max(0, cursor), new.length)
        guard isEnabled, !isComposing else {
            cancel()
            if status != .idle || self.text != text || self.cursor != cursor {
                commit(.idle, suggestion: "", text: text, cursor: cursor)
            }
            return
        }
        // Nothing changed: a caret event, or the echo of our own insertion.
        if self.text == text && self.cursor == cursor { return }

        // Typing through the suggestion keeps the rest of it: no new request.
        if status == .ready && cursor > self.cursor {
            let old = self.text as NSString
            let typed = new.substring(with: NSRange(location: self.cursor, length: cursor - self.cursor))
            let isSameAround =
                new.substring(to: self.cursor) == old.substring(to: self.cursor)
                && new.substring(from: cursor) == old.substring(from: self.cursor)
            let shown = suggestion as NSString
            let typedLength = (typed as NSString).length
            if isSameAround && typedLength < shown.length && shown.hasPrefix(typed) {
                commit(.ready, suggestion: shown.substring(from: typedLength), text: text, cursor: cursor)
                return
            }
        }

        cancel()
        let before = new.substring(to: cursor).trimmingCharacters(in: .whitespacesAndNewlines)
        guard before.count >= max(0, minimumCharacters) else {
            commit(.idle, suggestion: "", text: text, cursor: cursor)
            return
        }
        commit(.waiting, suggestion: "", text: text, cursor: cursor)
        request(text: text, cursor: cursor)
    }

    /// Drops the suggestion and cancels any request. It stays away until the text changes.
    public func dismiss() {
        cancel()
        if status != .idle { commit(.idle, suggestion: "", text: text, cursor: cursor) }
    }

    /// Accepts the whole suggestion.
    ///
    /// - Returns: What to insert, or `nil` when there is no suggestion.
    @discardableResult
    public func accept() -> MonarchAiCompletionAcceptance? {
        take(wordOnly: false)
    }

    /// Accepts the next word and keeps the rest showing.
    ///
    /// - Returns: What to insert, or `nil` when there is no suggestion.
    @discardableResult
    public func acceptWord() -> MonarchAiCompletionAcceptance? {
        take(wordOnly: true)
    }

    /// Call with `true` when an input method starts composing (marked text)
    /// and `false` when it ends. While composing, nothing is requested or shown.
    public func setComposing(_ isComposing: Bool) {
        guard self.isComposing != isComposing else { return }
        self.isComposing = isComposing
        if isComposing { dismiss() }
    }

    /// Turns suggestions on or off. Turning them off clears the current one.
    public func setEnabled(_ isEnabled: Bool) {
        guard self.isEnabled != isEnabled else { return }
        self.isEnabled = isEnabled
        if !isEnabled { dismiss() }
    }

    /// The next word of a suggestion: any leading white space, then the
    /// first word with the punctuation attached to it. Text without spaces,
    /// such as Chinese or Japanese, is split at the system's word boundaries.
    public static func nextWord(of suggestion: String) -> String {
        guard !suggestion.isEmpty else { return "" }
        let lead = suggestion.prefix { $0.isWhitespace }
        let run = suggestion.dropFirst(lead.count).prefix { !$0.isWhitespace }
        guard !run.isEmpty else { return suggestion }
        let text = String(run)
        var wordEnd: String.Index?
        text.enumerateSubstrings(in: text.startIndex..., options: [.byWords, .substringNotRequired]) {
            _,
            range,
            _,
            stop in
            // Punctuation before a word travels with it: "(hello" is one step.
            wordEnd = range.upperBound
            stop = true
        }
        guard let wordEnd else { return String(lead) + text }
        // Punctuation after it too, so "word," never leaves a lone comma behind.
        let tail = text[wordEnd...].prefix { !$0.isLetter && !$0.isNumber }
        return String(lead) + String(text[..<wordEnd]) + String(tail)
    }

    /// Shows a suggestion at the end of a text without a request. For previews and tests.
    func show(_ suggestion: String, after text: String) {
        cancel()
        commit(.ready, suggestion: suggestion, text: text, cursor: (text as NSString).length)
    }

    /// Waits until no request is waiting or in flight. For tests.
    func settled() async {
        await task?.value
    }

    private func commit(_ status: MonarchAiCompletionStatus, suggestion: String, text: String, cursor: Int) {
        if self.status != status { self.status = status }
        if self.suggestion != suggestion { self.suggestion = suggestion }
        if self.text != text { self.text = text }
        if self.cursor != cursor { self.cursor = cursor }
    }

    private func cancel() {
        sequence += 1
        task?.cancel()
        task = nil
        pending.cancel()
    }

    private func request(text: String, cursor: Int) {
        let id = sequence
        let sleep = sleep
        let debounce = debounce
        let complete = complete
        let task = Task { @MainActor [weak self] in
            do { try await sleep(debounce) } catch { return }
            // The completion is not kept alive while the request is in flight.
            guard self?.begin(id, text: text, cursor: cursor) == true else { return }
            let outcome: Result<String?, any Error>
            do {
                outcome = .success(try await complete(text, cursor))
            } catch {
                outcome = .failure(error)
            }
            self?.finish(id, outcome: outcome, text: text, cursor: cursor)
        }
        self.task = task
        pending.set(task)
    }

    /// The debounce ended: the request is sent unless a newer edit replaced it.
    private func begin(_ id: Int, text: String, cursor: Int) -> Bool {
        guard sequence == id, !Task.isCancelled else { return false }
        commit(.loading, suggestion: "", text: text, cursor: cursor)
        return true
    }

    /// The request ended. A newer edit, a dismissal or a cancellation makes its answer stale.
    private func finish(_ id: Int, outcome: Result<String?, any Error>, text: String, cursor: Int) {
        guard sequence == id, !Task.isCancelled else { return }
        switch outcome {
        case .success(let result):
            let suggestion = result ?? ""
            commit(suggestion.isEmpty ? .idle : .ready, suggestion: suggestion, text: text, cursor: cursor)
        case .failure(let error):
            guard !(error is CancellationError) else { return }
            commit(.error, suggestion: "", text: text, cursor: cursor)
            onError?(error)
        }
    }

    private func take(wordOnly: Bool) -> MonarchAiCompletionAcceptance? {
        guard status == .ready, !suggestion.isEmpty else { return nil }
        let inserted = wordOnly ? Self.nextWord(of: suggestion) : suggestion
        let rest = (suggestion as NSString).substring(from: (inserted as NSString).length)
        let current = text as NSString
        let text = current.substring(to: cursor) + inserted + current.substring(from: cursor)
        let cursor = cursor + (inserted as NSString).length
        commit(rest.isEmpty ? .idle : .ready, suggestion: rest, text: text, cursor: cursor)
        return MonarchAiCompletionAcceptance(inserted: inserted, text: text, cursor: cursor)
    }
}

/// The request in flight. It is `Sendable`, so the completion can cancel it
/// from its `deinit`, which is not on the main actor.
private final class MonarchAiCompletionRequest: Sendable {
    private let task = OSAllocatedUnfairLock<Task<Void, Never>?>(initialState: nil)

    func set(_ task: Task<Void, Never>) {
        self.task.withLock { $0 = task }
    }

    func cancel() {
        let task = task.withLock { task in
            defer { task = nil }
            return task
        }
        task?.cancel()
    }
}
