import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

private let routing = MonarchDnsRecord(
    type: "CNAME",
    name: "book.example.com",
    value: "domains.example.net",
    purpose: .routing,
    isFound: true
)
private let verification = MonarchDnsRecord(
    type: "TXT",
    name: "_verify.book.example.com",
    value: "verify=4f7k2m9q8d3r6t1w",
    purpose: .verification,
    ttl: "3600",
    isFound: false
)
private let mail = MonarchDnsRecord(type: "MX", name: "example.com", value: "mx.example.net", priority: 10)

enum MonarchDnsRecordTableFixtures: ComponentFixtures {
    static let id = "dns-record-table"
    static let fixtures = [
        Fixture("cards with a zone") {
            MonarchDnsRecordTable([routing, verification, mail], zone: "example.com", layout: .cards)
        },
        Fixture("cards without a zone or a check") {
            MonarchDnsRecordTable(
                [MonarchDnsRecord(type: "A", name: "example.com", value: "203.0.113.10")],
                layout: .cards
            )
        },
        Fixture("table") {
            MonarchDnsRecordTable([routing, verification, mail], zone: "example.com", layout: .table)
        },
        Fixture("table without optional columns") {
            MonarchDnsRecordTable(
                [MonarchDnsRecord(type: "CNAME", name: "www.example.com", value: "example.net")],
                layout: .table
            )
        },
        Fixture("automatic") {
            MonarchDnsRecordTable([routing, verification], zone: "example.com")
        },
        Fixture("certificate and custom purposes, long value") {
            MonarchDnsRecordTable(
                [
                    MonarchDnsRecord(
                        type: "CAA",
                        name: "example.com",
                        value: "0 issue \"letsencrypt.org\"",
                        purpose: .certificate,
                        isFound: false
                    ),
                    MonarchDnsRecord(
                        type: "TXT",
                        name: "mail._domainkey.example.com",
                        value: String(repeating: "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQ", count: 5),
                        purpose: .custom("Signs your outgoing email so it is not marked as spam")
                    ),
                ],
                zone: "example.com",
                layout: .cards
            )
        },
        Fixture("empty") {
            MonarchDnsRecordTable([], layout: .cards)
        },
    ]
}

@Suite("MonarchDnsRecordTable")
struct MonarchDnsRecordTableTests {
    typealias Tokens = MonarchDnsRecordTableTokens

    @Test("Names are shown relative to the zone, @ at the apex, unchanged outside it")
    func relativeName() {
        #expect(Tokens.relativeName("book.example.com", zone: "example.com") == "book")
        #expect(Tokens.relativeName("example.com", zone: "example.com") == "@")
        #expect(Tokens.relativeName("Example.COM.", zone: "example.com") == "@")
        #expect(Tokens.relativeName("_v.Book.Example.com", zone: "example.com.") == "_v.Book")
        #expect(Tokens.relativeName("book.other.com", zone: "example.com") == "book.other.com")
        #expect(Tokens.relativeName("notexample.com", zone: "example.com") == "notexample.com")
        #expect(Tokens.relativeName("book.example.com", zone: nil) == "book.example.com")
        #expect(Tokens.relativeName("book.example.com", zone: "") == "book.example.com")
    }

    @Test("Automatic is cards in a compact width or at accessibility sizes, a table otherwise")
    func form() {
        #expect(Tokens.form(.automatic, isRegularWidth: false, isAccessibilitySize: false) == .cards)
        #expect(Tokens.form(.automatic, isRegularWidth: true, isAccessibilitySize: false) == .table)
        #expect(Tokens.form(.automatic, isRegularWidth: true, isAccessibilitySize: true) == .cards)
        for isRegular in [true, false] {
            for isLarge in [true, false] {
                #expect(Tokens.form(.cards, isRegularWidth: isRegular, isAccessibilitySize: isLarge) == .cards)
                #expect(Tokens.form(.table, isRegularWidth: isRegular, isAccessibilitySize: isLarge) == .table)
            }
        }
    }

    @Test("Counts include only checked records")
    func counts() {
        let counts = Tokens.counts([routing, verification, mail])
        #expect(counts.found == 1)
        #expect(counts.checked == 2)
        #expect(Tokens.counts([]).found == 0)
        #expect(Tokens.counts([]).checked == 0)
        #expect(Tokens.hasTTL([routing, verification]))
        #expect(!Tokens.hasTTL([routing, mail]))
        #expect(Tokens.hasPriority([mail]))
        #expect(!Tokens.hasPriority([routing]))
    }

    @Test("A checked record has a status in words and a tone; an unchecked one has none")
    func status() {
        let labels = MonarchDnsRecordTableLabels()
        #expect(Tokens.status(routing, labels: labels) == MonarchStatusDefinition("Found", tone: .success))
        #expect(Tokens.status(verification, labels: labels) == MonarchStatusDefinition("Waiting", tone: .neutral))
        #expect(Tokens.status(mail, labels: labels) == nil)
        let custom = MonarchDnsRecordTableLabels(found: "OK", waiting: "Pending")
        #expect(Tokens.status(routing, labels: custom)?.label == "OK")
        #expect(Tokens.status(verification, labels: custom)?.label == "Pending")
    }

    @Test("A record id defaults to its type, name and value")
    func recordID() {
        #expect(routing.id == "CNAME:book.example.com:domains.example.net")
        #expect(MonarchDnsRecord(id: "r1", type: "A", name: "a", value: "b").id == "r1")
    }

    @Test("Built-in strings resolve and every one can be replaced")
    func strings() {
        let labels = MonarchDnsRecordTableLabels()
        #expect(labels.resolvedType == "Type")
        #expect(labels.resolvedName == "Name")
        #expect(labels.resolvedValue == "Value")
        #expect(labels.resolvedTTL == "TTL")
        #expect(labels.resolvedPriority == "Priority")
        #expect(labels.resolvedStatus == "Status")
        #expect(labels.purposeText(.routing) == "Points your address here")
        #expect(labels.purposeText(.verification) == "Proves you own the domain")
        #expect(labels.purposeText(nil) == "Proves you own the domain")
        #expect(labels.purposeText(.certificate) == "Allows the HTTPS certificate")
        #expect(labels.purposeText(.custom("Sends email")) == "Sends email")
        #expect(labels.copyNameText(routing, shown: "book") == "name of the CNAME record")
        #expect(labels.copyValueText(routing, shown: "book") == "value of the CNAME record book")
        #expect(labels.fullNameText("book.example.com") == "Full name: book.example.com")
        #expect(labels.labelText(zone: "example.com") == "DNS records for example.com")
        #expect(labels.labelText(zone: nil) == "DNS records")
        #expect(labels.summaryText(found: 1, total: 2) == "1 of 2 records found.")

        let custom = MonarchDnsRecordTableLabels(
            type: "Typ",
            purpose: { _ in "Zweck" },
            copyName: { record, _ in "Name \(record.type)" },
            copyValue: { _, shown in "Wert \(shown)" },
            fullName: { "Voll: \($0)" },
            label: { _ in "Einträge" },
            summary: { found, total in "\(found)/\(total)" }
        )
        #expect(custom.resolvedType == "Typ")
        #expect(custom.purposeText(.routing) == "Zweck")
        #expect(custom.copyNameText(routing, shown: "book") == "Name CNAME")
        #expect(custom.copyValueText(routing, shown: "book") == "Wert book")
        #expect(custom.fullNameText("x") == "Voll: x")
        #expect(custom.labelText(zone: "example.com") == "Einträge")
        #expect(custom.summaryText(found: 1, total: 2) == "1/2")
    }

    @Test("A table row is read as its type and shown name")
    func rowLabel() {
        #expect(Tokens.rowLabel(routing, shown: "book") == "CNAME book")
    }
}
