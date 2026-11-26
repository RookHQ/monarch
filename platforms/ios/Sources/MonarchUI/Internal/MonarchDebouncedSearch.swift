import Observation

/// Where a debounced search stands: of ``MonarchEntityPicker``,
/// ``MonarchAddressAutocomplete``, ``MonarchLocationFields`` and any picker
/// that searches as the person types.
enum MonarchDebouncedSearchPhase: Equatable, Sendable {
    /// Nothing typed: the static list is shown.
    case idle
    /// The text is shorter than the minimum.
    case tooShort
    /// Waiting for the debounce or the answer.
    case loading
    /// The search threw.
    case failed
    /// The answer is in `results`.
    case done
}

/// Runs a debounced search and holds its phase and results.
///
/// The view starts one task per query (`.task(id:)`). SwiftUI cancels the
/// task when the query changes, which ends the wait or the search, and a
/// cancelled search never writes its result, so the results always describe
/// the latest query. The clock is injected so tests never wait.
@MainActor
@Observable
final class MonarchDebouncedSearch<Item: Sendable> {
    /// Where the search stands.
    private(set) var phase: MonarchDebouncedSearchPhase = .idle
    /// The latest answer.
    private(set) var results: [Item] = []

    /// Waits for `debounce` on `clock`, then searches the trimmed `query`.
    /// An empty query is idle when `minLength` is above zero; a shorter one is
    /// too short. Neither is searched.
    func run<C: Clock>(
        _ query: String,
        minLength: Int,
        debounce: Duration,
        clock: C,
        search: @Sendable (String) async throws -> [Item]
    ) async where C.Duration == Duration {
        let trimmed = query.trimmingCharacters(in: .whitespacesAndNewlines)
        if let phase = Self.phaseBeforeSearch(trimmed, minLength: minLength) {
            self.phase = phase
            results = []
            return
        }
        phase = .loading
        do {
            if debounce > .zero { try await clock.sleep(for: debounce) }
            let found = try await search(trimmed)
            guard !Task.isCancelled else { return }
            results = found
            phase = .done
        } catch {
            guard !Task.isCancelled else { return }
            results = []
            phase = .failed
        }
    }

    /// Waits for `debounce` on the continuous clock, then searches.
    func run(
        _ query: String,
        minLength: Int,
        debounce: Duration,
        search: @Sendable (String) async throws -> [Item]
    ) async {
        await run(query, minLength: minLength, debounce: debounce, clock: ContinuousClock(), search: search)
    }

    /// The phase for a query that is not searched, or `nil` when it is.
    nonisolated static func phaseBeforeSearch(_ trimmed: String, minLength: Int) -> MonarchDebouncedSearchPhase? {
        if trimmed.isEmpty && minLength > 0 { return .idle }
        if trimmed.count < minLength { return .tooShort }
        return nil
    }
}
