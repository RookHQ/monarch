import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

private let fixedNow = Date(timeIntervalSinceReferenceDate: 800_000_000)

enum MonarchDomainProgressFixtures: ComponentFixtures {
    static let id = "domain-progress"
    static let fixtures = [
        Fixture("waiting for DNS, no record yet, checked") {
            MonarchDomainProgress(
                "book.example.com",
                stage: .dns,
                dnsFound: MonarchDomainDnsSnapshot(),
                checkedAt: fixedNow.addingTimeInterval(-120),
                now: fixedNow
            )
        },
        Fixture("ownership with a blocking problem") {
            MonarchDomainProgress(
                "book.example.com",
                stage: .ownership,
                problems: [
                    MonarchDomainProblem(
                        "Your domain points somewhere else. Remove the A record for book before adding the CNAME.",
                        code: "conflicting_a"
                    )
                ],
                dnsFound: MonarchDomainDnsSnapshot(a: ["203.0.113.10"], aaaa: ["2001:db8::1"]),
                notify: .inApp
            )
        },
        Fixture("certificate in progress with an info problem") {
            MonarchDomainProgress(
                "book.example.com",
                stage: .certificate,
                problems: [MonarchDomainProblem("The certificate is pending.", severity: .info)],
                dnsFound: MonarchDomainDnsSnapshot(cnames: ["domains.example.net"]),
                notify: .email
            )
        },
        Fixture("page check, not rechecking") {
            MonarchDomainProgress("book.example.com", stage: .page, isRechecking: false)
        },
        Fixture("live") {
            MonarchDomainProgress("book.example.com", stage: .live, checkedAt: fixedNow, now: fixedNow)
        },
        Fixture("long host, custom labels") {
            MonarchDomainProgress(
                "a-very-long-subdomain-name-for-testing.customer-brand-example.co.uk",
                stage: .certificate,
                labels: MonarchDomainProgressLabels(state: { $0.rawValue.uppercased() })
            )
        },
    ]
}

@Suite("MonarchDomainProgress")
struct MonarchDomainProgressTests {
    typealias Tokens = MonarchDomainProgressTokens

    private func states(_ stage: MonarchDomainStage, _ problems: [MonarchDomainProblem] = [])
        -> [MonarchDomainStepState]
    {
        let states = Tokens.states(stage: stage, problems: problems)
        return Tokens.steps.map { states[$0] ?? .todo }
    }

    @Test("Each stage maps to the state of the three steps")
    func stageStates() {
        #expect(states(.dns) == [.current, .todo, .todo])
        #expect(states(.ownership) == [.current, .todo, .todo])
        #expect(states(.certificate) == [.done, .current, .todo])
        #expect(states(.page) == [.done, .done, .current])
        #expect(states(.live) == [.done, .done, .done])
    }

    @Test("A blocking problem marks its step, or the current one, unless the step is done")
    func problems() {
        let blocking = MonarchDomainProblem("Wrong record.")
        let info = MonarchDomainProblem("Pending.", severity: .info)
        #expect(states(.dns, [blocking]) == [.error, .todo, .todo])
        #expect(states(.certificate, [info]) == [.done, .current, .todo])
        #expect(states(.certificate, [MonarchDomainProblem("x", step: .live)]) == [.done, .current, .error])
        #expect(states(.certificate, [MonarchDomainProblem("x", step: .dns)]) == [.done, .current, .todo])
        #expect(states(.live, [blocking]) == [.done, .done, .done])
    }

    @Test("Blocking problems are shown as warnings; otherwise only the first info, as neutral")
    func shownProblems() {
        let a = MonarchDomainProblem("A")
        let b = MonarchDomainProblem("B")
        let i1 = MonarchDomainProblem("I1", severity: .info)
        let i2 = MonarchDomainProblem("I2", severity: .info)
        #expect(Tokens.shownProblems([i1, a, b]).map(\.message) == ["A", "B"])
        #expect(Tokens.shownProblems([i1, i2]).map(\.message) == ["I1"])
        #expect(Tokens.shownProblems([]).isEmpty)
        #expect(Tokens.tone(a) == .warning)
        #expect(Tokens.tone(i1) == .neutral)
    }

    @Test("The DNS snapshot is one line")
    func snapshot() {
        let empty = "no record yet"
        #expect(Tokens.snapshotText(MonarchDomainDnsSnapshot(), empty: empty) == empty)
        #expect(
            Tokens.snapshotText(MonarchDomainDnsSnapshot(cnames: ["x.net", "y.net"], a: ["1.2.3.4"]), empty: empty)
                == "CNAME → x.net"
        )
        #expect(
            Tokens.snapshotText(MonarchDomainDnsSnapshot(a: ["1.2.3.4", "5.6.7.8"]), empty: empty)
                == "A → 1.2.3.4, 5.6.7.8"
        )
        #expect(
            Tokens.snapshotText(MonarchDomainDnsSnapshot(a: ["1.2.3.4"], aaaa: ["::1"]), empty: empty)
                == "A → 1.2.3.4 · AAAA → ::1"
        )
        #expect(Tokens.snapshotText(MonarchDomainDnsSnapshot(aaaa: ["::1"]), empty: empty) == "AAAA → ::1")
    }

    @Test("The recheck interval is clamped, and stops once live")
    func recheck() {
        #expect(MonarchDomainProgress.recheckInterval(for: nil) == .seconds(15))
        #expect(MonarchDomainProgress.recheckInterval(for: .dns) == .seconds(30))
        #expect(MonarchDomainProgress.recheckInterval(for: .dns, suggested: .seconds(1)) == .seconds(15))
        #expect(MonarchDomainProgress.recheckInterval(for: .page, suggested: .seconds(60)) == .seconds(60))
        #expect(MonarchDomainProgress.recheckInterval(for: .certificate, suggested: .seconds(3_600)) == .seconds(300))
        #expect(MonarchDomainProgress.recheckInterval(for: .live) == nil)
        #expect(MonarchDomainProgress.recheckInterval(for: .dns, isStopped: true) == nil)
    }

    @Test("Titles and details follow the step state")
    func text() {
        let labels = MonarchDomainProgressLabels()
        let host = "book.example.com"
        let dns = Tokens.items(host: host, stage: .dns, problems: [], labels: labels)
        #expect(dns.map(\.title) == ["Waiting for your DNS record", "HTTPS certificate", "Live"])
        #expect(
            dns.map(\.detail) == [
                "Add the record shown below at your DNS provider.",
                "Starts automatically once DNS is found.",
                "We load book.example.com over HTTPS before calling it live.",
            ]
        )
        let ownership = Tokens.items(host: host, stage: .ownership, problems: [], labels: labels)
        #expect(ownership[0].title == "Ownership record needed")
        let certificate = Tokens.items(host: host, stage: .certificate, problems: [], labels: labels)
        #expect(certificate[0].title == "DNS record found")
        #expect(certificate[0].detail == "Your domain points here.")
        #expect(certificate[1].detail == "Being issued now, usually 1 to 5 minutes.")
        let page = Tokens.items(host: host, stage: .page, problems: [], labels: labels)
        #expect(page[1].title == "HTTPS certificate issued")
        #expect(page[1].detail == "Secure connection works.")
        #expect(page[2].detail == "Checking that your site loads on book.example.com.")
        let live = Tokens.items(host: host, stage: .live, problems: [], labels: labels)
        #expect(live[2].detail == "Your site loads on book.example.com.")
    }

    @Test("Spoken step values and announcements name the active step")
    func accessibility() {
        let labels = MonarchDomainProgressLabels()
        let host = "book.example.com"
        let certificate = Tokens.items(host: host, stage: .certificate, problems: [], labels: labels)
        #expect(Tokens.activeIndex(certificate) == 1)
        #expect(Tokens.announcement(certificate, labels: labels) == "Step 2 of 3: HTTPS certificate, in progress.")
        #expect(
            Tokens.stepValue(certificate[1], labels: labels)
                == "in progress, Being issued now, usually 1 to 5 minutes."
        )
        #expect(Tokens.stepValue(certificate[0], labels: labels) == "done, Your domain points here.")
        let live = Tokens.items(host: host, stage: .live, problems: [], labels: labels)
        #expect(Tokens.activeIndex(live) == 2)
        #expect(Tokens.announcement(live, labels: labels) == "Step 3 of 3: Live, done.")
        let error = Tokens.items(host: host, stage: .dns, problems: [MonarchDomainProblem("x")], labels: labels)
        #expect(
            Tokens.announcement(error, labels: labels) == "Step 1 of 3: Waiting for your DNS record, needs attention."
        )
        #expect(Tokens.stepValue(error[2], labels: labels).hasPrefix("not started, "))
        #expect(Tokens.announcement([], labels: labels).isEmpty)
    }

    @Test("Built-in strings resolve and every one can be replaced")
    func strings() {
        let labels = MonarchDomainProgressLabels()
        #expect(labels.listLabelText("a.com") == "Progress for a.com")
        #expect(labels.dnsSeenText(host: "a.com", seen: "CNAME → b.net") == "Public DNS for a.com shows CNAME → b.net.")
        #expect(labels.resolvedNoRecord == "no record yet")
        #expect(labels.recheckingText(nil) == "We keep checking automatically, even after you close this page.")
        #expect(labels.recheckingText(.email).hasSuffix("and email you when it is live."))
        #expect(labels.recheckingText(.inApp).hasSuffix("and notify you here when it is live."))
        #expect(labels.checkedText("2 minutes ago") == "Checked 2 minutes ago.")
        for state in MonarchDomainStepState.allCases {
            #expect(!labels.stateText(state).hasPrefix("monarch."))
        }

        let custom = MonarchDomainProgressLabels(
            title: { step, _, _ in "T-\(step.rawValue)" },
            detail: { _, state, host in "\(state.rawValue)@\(host)" },
            state: { $0.rawValue },
            listLabel: { "L \($0)" },
            announcement: { position, count, title, state in "\(position)/\(count) \(title) \(state)" },
            dnsSeen: { host, seen in "\(host)=\(seen)" },
            noRecord: "none",
            rechecking: { _ in "R" },
            checked: { "C \($0)" }
        )
        let items = Tokens.items(host: "h", stage: .certificate, problems: [], labels: custom)
        #expect(items.map(\.title) == ["T-dns", "T-certificate", "T-live"])
        #expect(items[1].detail == "current@h")
        #expect(Tokens.announcement(items, labels: custom) == "2/3 T-certificate current")
        #expect(custom.listLabelText("h") == "L h")
        #expect(custom.dnsSeenText(host: "h", seen: "s") == "h=s")
        #expect(custom.resolvedNoRecord == "none")
        #expect(custom.recheckingText(.email) == "R")
        #expect(custom.checkedText("x") == "C x")
    }

    @Test("The last check is relative to the injected now")
    func relativeTime() {
        let locale = Locale(identifier: "en_US")
        #expect(
            Tokens.relativeTime(fixedNow.addingTimeInterval(-120), now: fixedNow, locale: locale) == "2 minutes ago"
        )
        #expect(Tokens.relativeTime(fixedNow, now: fixedNow, locale: locale) == "now")
    }

    @Test("Every state has its own marker colours and a symbol or progress")
    func markers() {
        let markers = MonarchDomainStepState.allCases.map { Tokens.marker($0) }
        #expect(Set(markers.map { "\($0.background.map { "\($0)" } ?? "")" }).count == markers.count)
        #expect(Tokens.symbol(.done) == "checkmark")
        #expect(Tokens.symbol(.error) == "exclamationmark")
        #expect(Tokens.symbol(.current) == nil)
        #expect(Tokens.symbol(.todo) == nil)
        #expect(Tokens.titleRole(.todo) == \.textMuted)
        #expect(Tokens.titleRole(.current) == \.textDefault)
        #expect(Tokens.connectorRole(after: .done) == \.successSolid)
        #expect(Tokens.connectorRole(after: .current) == \.borderDefault)
    }
}
