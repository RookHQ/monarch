import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchEntityPickerFixtures: ComponentFixtures {
    static let id = "entity-picker"

    static let people = [
        MonarchEntityPickerEntity(id: "ana", name: "Ana Silva", description: "Design", hint: "ana@example.com"),
        MonarchEntityPickerEntity(id: "sam", name: "Sam Lee", description: "Engineering", keywords: "Samuel"),
        MonarchEntityPickerEntity(id: "zoe", name: "Zoë Brant", image: nil, isDisabled: true),
        MonarchEntityPickerEntity(
            id: "web",
            name: "Website relaunch",
            description: "Project",
            systemImage: "folder",
            shape: .square
        ),
    ]

    static let grouped = [
        MonarchEntityPickerEntity(id: "ana", name: "Ana Silva", group: "Design", status: .online),
        MonarchEntityPickerEntity(id: "lee", name: "Lee Park", group: "Engineering", status: .away),
        MonarchEntityPickerEntity(id: "sam", name: "Sam Lee", group: "Design", status: .offline, statusLabel: "Out"),
        MonarchEntityPickerEntity(id: "kim", name: "Kim Doe"),
    ]

    @MainActor
    static func list(
        selection: [String] = [],
        query: String = "",
        isMultiple: Bool = false,
        clearOptionTitle: String? = nil,
        isLoading: Bool = false,
        minSearchLength: Int = 0,
        entities: [MonarchEntityPickerEntity] = people,
        search: (@Sendable (String) async throws -> [MonarchEntityPickerEntity])? = nil,
        canCreate: Bool = false
    ) -> MonarchEntityPickerList {
        MonarchEntityPickerList(
            title: "Owner",
            isMultiple: isMultiple,
            selection: selection,
            entities: entities,
            query: .constant(query),
            clearOptionTitle: clearOptionTitle,
            isLoading: isLoading,
            minSearchLength: minSearchLength,
            debounce: .zero,
            labels: MonarchEntityPickerLabels(),
            search: search,
            canCreate: canCreate,
            onChoose: { _ in },
            onCreate: { _ in },
            onClose: {}
        )
    }

    static let fixtures = [
        Fixture("one chosen, with help text") {
            MonarchEntityPicker("Owner", selection: .constant("ana"), entities: people, helper: "Gets the alerts.")
        },
        Fixture("empty with prompt and clear option") {
            MonarchEntityPicker(
                "Owner",
                selection: .constant(nil),
                entities: people,
                prompt: "Choose an owner",
                clearOptionTitle: "Unassigned"
            )
        },
        Fixture("required with error") {
            MonarchEntityPicker(
                "Owner",
                selection: .constant(nil),
                entities: people,
                error: "Choose an owner.",
                isRequired: true
            )
        },
        Fixture("several chosen") {
            MonarchEntityPicker("Reviewers", selection: .constant(["ana", "sam", "web"]), entities: people)
        },
        Fixture("chosen id only in selected entities, long name") {
            MonarchEntityPicker(
                "Account",
                selection: .constant("acme"),
                entities: [],
                selectedEntities: [
                    MonarchEntityPickerEntity(
                        id: "acme",
                        name: "Acme Corporation International Holdings and Subsidiaries Limited",
                        systemImage: "building.2",
                        shape: .square
                    )
                ]
            )
        },
        Fixture("disabled") {
            MonarchEntityPicker("Owner", selection: .constant("sam"), entities: people).disabled(true)
        },
        Fixture("list: chosen, disabled, clear option") {
            list(selection: ["ana"], clearOptionTitle: "Unassigned")
        },
        Fixture("list: multiple") {
            list(selection: ["ana", "web"], isMultiple: true)
        },
        Fixture("list: no match with create") {
            list(query: "Priya", canCreate: true)
        },
        Fixture("list: no match") {
            list(query: "Priya")
        },
        Fixture("list: groups and presence") {
            list(entities: grouped)
        },
        Fixture("list: loading with no entities") {
            list(isLoading: true, entities: [])
        },
        Fixture("list: server search before the first answer") {
            list(minSearchLength: 2, search: { _ in [] })
        },
    ]
}

/// A clock that never waits and records how long it was asked to wait.
private final class MonarchEntityPickerTestClock: Clock, @unchecked Sendable {
    private let lock = NSLock()
    private var recorded: [Duration] = []

    var now: ContinuousClock.Instant { ContinuousClock.now }
    var minimumResolution: Duration { .zero }
    var sleeps: [Duration] { lock.withLock { recorded } }

    func sleep(until deadline: ContinuousClock.Instant, tolerance: Duration?) async throws {
        lock.withLock { recorded.append(deadline - ContinuousClock.now) }
        try Task.checkCancellation()
    }
}

private struct MonarchEntityPickerTestError: Error {}

@Suite("MonarchEntityPicker")
@MainActor
struct MonarchEntityPickerTests {
    private typealias Tokens = MonarchEntityPickerTokens
    private let people = MonarchEntityPickerFixtures.people

    @Test("The static filter searches the name, hint and keywords, ignoring case and accents")
    func matching() {
        #expect(Tokens.matches(people[0], query: "ana"))
        #expect(Tokens.matches(people[0], query: "EXAMPLE"))
        #expect(Tokens.matches(people[1], query: "samuel"))
        #expect(Tokens.matches(people[2], query: "zoe"))
        #expect(!Tokens.matches(people[0], query: "Design"))
        #expect(Tokens.matches(people[0], query: "  "))
    }

    @Test("Without a search the list filters the entities")
    func staticContent() {
        let all = Tokens.content(
            entities: people,
            query: "",
            hasSearch: false,
            phase: .idle,
            results: [],
            isLoading: false
        )
        #expect(all == .rows(["ana", "sam", "zoe", "web"]))
        let some = Tokens.content(
            entities: people,
            query: "sa",
            hasSearch: false,
            phase: .idle,
            results: [],
            isLoading: false
        )
        #expect(some == .rows(["sam"]))
        let none = Tokens.content(
            entities: people,
            query: "qq",
            hasSearch: false,
            phase: .idle,
            results: [],
            isLoading: false
        )
        #expect(none == .empty)
        let loading = Tokens.content(
            entities: [],
            query: "",
            hasSearch: false,
            phase: .idle,
            results: [],
            isLoading: true
        )
        #expect(loading == .loading)
    }

    @Test("With a search the phase decides, and the entities show until it runs")
    func searchContent() {
        func content(_ phase: MonarchDebouncedSearchPhase, results: [MonarchEntityPickerEntity] = [])
            -> MonarchEntityPickerContent
        {
            Tokens.content(
                entities: people,
                query: "x",
                hasSearch: true,
                phase: phase,
                results: results,
                isLoading: false
            )
        }
        #expect(content(.idle) == .rows(["ana", "sam", "zoe", "web"]))
        #expect(content(.tooShort) == .tooShort)
        #expect(content(.loading) == .loading)
        #expect(content(.failed) == .failed)
        #expect(content(.done) == .empty)
        #expect(content(.done, results: [people[1]]) == .rows(["sam"]))
    }

    @Test("A short or empty text is not searched")
    func phaseBeforeSearch() {
        typealias Search = MonarchDebouncedSearch<MonarchEntityPickerEntity>
        #expect(Search.phaseBeforeSearch("", minLength: 0) == nil)
        #expect(Search.phaseBeforeSearch("", minLength: 2) == .idle)
        #expect(Search.phaseBeforeSearch("a", minLength: 2) == .tooShort)
        #expect(Search.phaseBeforeSearch("ab", minLength: 2) == nil)
    }

    @Test("The search waits for the debounce on the injected clock, then sends the trimmed text")
    func searchRuns() async {
        let clock = MonarchEntityPickerTestClock()
        let search = MonarchDebouncedSearch<String>()
        await search.run("  ana ", minLength: 2, debounce: .milliseconds(250), clock: clock) { query in
            [query.uppercased()]
        }
        #expect(search.phase == .done)
        #expect(search.results == ["ANA"])
        #expect(clock.sleeps.count == 1)
        #expect(clock.sleeps.allSatisfy { $0 <= .milliseconds(250) })
    }

    @Test("A failed search clears the results; a short text clears them without searching")
    func searchFails() async {
        let clock = MonarchEntityPickerTestClock()
        let search = MonarchDebouncedSearch<String>()
        await search.run("ana", minLength: 0, debounce: .zero, clock: clock) { _ in ["a"] }
        #expect(search.results == ["a"])
        await search.run("anb", minLength: 0, debounce: .zero, clock: clock) { _ in
            throw MonarchEntityPickerTestError()
        }
        #expect(search.phase == .failed)
        #expect(search.results.isEmpty)
        await search.run("a", minLength: 3, debounce: .zero, clock: clock) { _ in
            Issue.record("A short text must not be searched")
            return []
        }
        #expect(search.phase == .tooShort)
        #expect(clock.sleeps.isEmpty)
    }

    @Test("Create is offered for new, non-blank text when there is a handler")
    func create() {
        #expect(Tokens.showsCreate(query: "Priya", shown: people, canCreate: true))
        #expect(!Tokens.showsCreate(query: "ana silva", shown: people, canCreate: true))
        #expect(!Tokens.showsCreate(query: "zoe brant", shown: people, canCreate: true))
        #expect(!Tokens.showsCreate(query: " ", shown: people, canCreate: true))
        #expect(!Tokens.showsCreate(query: "Priya", shown: people, canCreate: false))
    }

    @Test("Several picks keep their order")
    func toggling() {
        #expect(Tokens.toggled([], id: "ana") == ["ana"])
        #expect(Tokens.toggled(["ana"], id: "sam") == ["ana", "sam"])
        #expect(Tokens.toggled(["ana", "sam"], id: "ana") == ["sam"])
    }

    @Test("The field shows one name, the first name and how many more, or nothing")
    func summary() {
        let labels = MonarchEntityPickerLabels()
        #expect(Tokens.summary(names: [], labels: labels) == nil)
        #expect(Tokens.summary(names: ["Ana Silva"], labels: labels) == "Ana Silva")
        #expect(Tokens.summary(names: ["Ana Silva", "Sam Lee", "Zoë"], labels: labels) == "Ana Silva +2")
        let custom = MonarchEntityPickerLabels(summary: { "\($0) and \($1) more" })
        #expect(Tokens.summary(names: ["Ana", "Sam"], labels: custom) == "Ana and 1 more")
        let known = Dictionary(uniqueKeysWithValues: people.map { ($0.id, $0) })
        #expect(Tokens.name(of: "sam", known: known) == "Sam Lee")
        #expect(Tokens.name(of: "user-9", known: known) == "user-9")
    }

    @Test("VoiceOver reads every chosen name or None, and a row's parts")
    func accessibility() {
        let labels = MonarchEntityPickerLabels()
        #expect(Tokens.accessibilityValue(names: ["Ana Silva", "Sam Lee"], labels: labels) == "Ana Silva, Sam Lee")
        #expect(Tokens.accessibilityValue(names: [], labels: labels) == "None")
        #expect(
            Tokens.accessibilityValue(names: [], labels: MonarchEntityPickerLabels(noSelection: "Nobody")) == "Nobody"
        )
        #expect(Tokens.accessibilityLabel(people[0]) == "Ana Silva, Design, ana@example.com")
        #expect(Tokens.accessibilityLabel(people[2]) == "Zoë Brant")
    }

    @Test("Entities without a group come first, then each group in the order it first appears")
    func groups() {
        let sections = Tokens.sections(MonarchEntityPickerFixtures.grouped)
        #expect(sections.map(\.title) == [nil, "Design", "Engineering"])
        #expect(sections.map(\.ids) == [["kim"], ["ana", "sam"], ["lee"]])
        #expect(Tokens.sections(people).map(\.title) == [nil])
        let blank = [MonarchEntityPickerEntity(id: "a", name: "A", group: "")]
        #expect(Tokens.sections(blank).map(\.title) == [nil])
    }

    @Test("VoiceOver reads the presence of a row, built in or replaced")
    func presenceLabel() {
        let grouped = MonarchEntityPickerFixtures.grouped
        #expect(Tokens.accessibilityLabel(grouped[0]) == "Ana Silva, Online")
        #expect(Tokens.accessibilityLabel(grouped[2]) == "Sam Lee, Out")
        #expect(Tokens.accessibilityLabel(grouped[3]) == "Kim Doe")
    }

    @Test("The list messages, built in and replaced")
    func messages() {
        let labels = MonarchEntityPickerLabels()
        #expect(Tokens.message(.rows([]), minLength: 2, labels: labels) == nil)
        #expect(Tokens.message(.loading, minLength: 2, labels: labels) == "Loading…")
        #expect(Tokens.message(.tooShort, minLength: 2, labels: labels) == "Type at least 2 characters to search.")
        #expect(Tokens.message(.failed, minLength: 2, labels: labels) == "The results could not be loaded.")
        #expect(Tokens.message(.empty, minLength: 2, labels: labels) == "No matches")
        let custom = MonarchEntityPickerLabels(tooShort: { "Min \($0)" })
        #expect(Tokens.message(.tooShort, minLength: 3, labels: custom) == "Min 3")
    }

    @Test("Built-in strings resolve")
    func strings() {
        #expect(MonarchEntityPickerStrings.select == "Select…")
        #expect(MonarchEntityPickerStrings.search == "Search...")
        #expect(MonarchEntityPickerStrings.cancel == "Cancel")
        #expect(MonarchEntityPickerStrings.done == "Done")
        #expect(MonarchEntityPickerStrings.create("Priya") == "Create “Priya”")
        #expect(MonarchEntityPickerStrings.clear == "Clear")
        #expect(MonarchEntityPickerStrings.noSelection == "None")
    }
}
