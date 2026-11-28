import Foundation
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchAiPromptInputFixtures: ComponentFixtures {
    static let id = "ai-prompt-input"

    static let files = [
        MonarchAiAttachmentTrayFile(id: "1", name: "march-ledger.pdf", size: 182_000, contentType: "application/pdf"),
        MonarchAiAttachmentTrayFile(
            id: "2",
            name: "receipt.png",
            size: 64_000,
            contentType: "image/png",
            progress: 0.4
        ),
        MonarchAiAttachmentTrayFile(id: "3", name: "scan.jpg", contentType: "image/jpeg", isFailed: true),
    ]

    static let fixtures: [Fixture] = [
        Fixture("empty") {
            MonarchAiPromptInput("Message the assistant", text: .constant("")) { _ in }
        },
        Fixture("typing, with label, attach, paste and counter") {
            MonarchAiPromptInput(
                "Message the assistant",
                text: .constant("Which invoices are overdue?"),
                maxLength: 4000,
                showsLabel: true,
                countTokens: { $0.count / 4 },
                onAttach: {},
                onReceiveFiles: { _ in },
                onSubmit: { _ in }
            )
        },
        Fixture("near the limit") {
            MonarchAiPromptInput("Message the assistant", text: .constant("123456789"), maxLength: 10) { _ in }
        },
        Fixture("over the limit") {
            MonarchAiPromptInput("Message the assistant", text: .constant("12345678901234"), maxLength: 10) { _ in }
        },
        Fixture("busy with attachments") {
            MonarchAiPromptInput(
                "Message the assistant",
                text: .constant(""),
                attachments: files,
                isBusy: true,
                onStop: {},
                onRemoveAttachment: { _ in },
                onRetryAttachment: { _ in },
                onSubmit: { _ in }
            )
        },
        Fixture("busy without a stop handler") {
            MonarchAiPromptInput("Message the assistant", text: .constant("Hi"), isBusy: true) { _ in }
        },
        Fixture("disabled with a reason") {
            MonarchAiPromptInput(
                "Message the assistant",
                text: .constant(""),
                disabledReason: "Connect a provider to start."
            ) { _ in }
            .disabled(true)
        },
        Fixture("trigger menu, toolbar and footer") {
            MonarchAiPromptInput(
                "Message the assistant",
                text: .constant("/sum"),
                triggerMenu: {
                    MonarchAiSlashCommands(
                        text: .constant("/sum"),
                        commands: [
                            MonarchSlashMenuItem(
                                id: "summarize",
                                label: "summarize",
                                description: "Summarize the thread"
                            )
                        ]
                    ) { _ in nil }
                },
                toolbar: {
                    MonarchAiPromptToolbar {
                        Toggle("Search the web", systemImage: "globe", isOn: .constant(true))
                    }
                },
                footer: {
                    Text("Answers can be wrong. Check important facts.").monarchTextStyle(.caption)
                },
                onSubmit: { _ in }
            )
        },
        Fixture("long text grows to five lines") {
            MonarchAiPromptInput(
                "Message the assistant",
                text: .constant(String(repeating: "Please summarise the overdue invoices. ", count: 12)),
                minLines: 2,
                maxLines: 5
            ) { _ in }
        },
    ]
}

@Suite("MonarchAiPromptInput")
struct MonarchAiPromptInputTests {
    private typealias Tokens = MonarchAiPromptInputTokens

    @Test("The limit is ok, then warning from the share rounded down, then over above the limit")
    func limit() {
        #expect(Tokens.limit(count: 5_000, maxLength: nil, warnAt: 0.9) == .ok)
        #expect(Tokens.limit(count: 8, maxLength: 10, warnAt: 0.9) == .ok)
        #expect(Tokens.limit(count: 9, maxLength: 10, warnAt: 0.9) == .warning)
        #expect(Tokens.limit(count: 10, maxLength: 10, warnAt: 0.9) == .warning)
        #expect(Tokens.limit(count: 11, maxLength: 10, warnAt: 0.9) == .over)
        #expect(Tokens.limit(count: 3, maxLength: 7, warnAt: 0.5) == .warning)
        #expect(Tokens.limit(count: 0, maxLength: 10, warnAt: 0) == .warning)
        #expect(Tokens.limit(count: 9, maxLength: 10, warnAt: 2) == .ok)
    }

    @Test("Announcements happen on the way up only")
    func announcements() {
        let labels = MonarchAiPromptInputLabels()
        #expect(
            Tokens.announcement(from: .ok, to: .warning, count: 9, maxLength: 10, labels: labels) == "1 character left"
        )
        #expect(
            Tokens.announcement(from: .ok, to: .warning, count: 3600, maxLength: 4000, labels: labels)
                == "400 characters left"
        )
        #expect(
            Tokens.announcement(from: .warning, to: .over, count: 13, maxLength: 10, labels: labels)
                == "3 characters over the limit"
        )
        #expect(
            Tokens.announcement(from: .ok, to: .over, count: 11, maxLength: 10, labels: labels)
                == "1 character over the limit"
        )
        #expect(Tokens.announcement(from: .over, to: .warning, count: 10, maxLength: 10, labels: labels) == nil)
        #expect(Tokens.announcement(from: .warning, to: .ok, count: 1, maxLength: 10, labels: labels) == nil)
        #expect(Tokens.announcement(from: .ok, to: .ok, count: 1, maxLength: 10, labels: labels) == nil)
        #expect(Tokens.announcement(from: .ok, to: .warning, count: 9, maxLength: nil, labels: labels) == nil)
        let custom = MonarchAiPromptInputLabels(counterWarning: { "noch \($0)" })
        #expect(Tokens.announcement(from: .ok, to: .warning, count: 9, maxLength: 10, labels: custom) == "noch 1")
    }

    @Test("The counter shows the count and is read as the count of the limit, or by how much it is over")
    func counter() {
        let labels = MonarchAiPromptInputLabels()
        #expect(Tokens.counterText(count: 120, maxLength: 4000, labels: labels) == "120 / 4000")
        #expect(Tokens.counterLabel(count: 120, maxLength: 4000, labels: labels) == "120 of 4000 characters")
        #expect(Tokens.counterLabel(count: 4002, maxLength: 4000, labels: labels) == "2 characters over the limit")
        #expect(Tokens.tokensText(30, labels: labels) == "≈ 30 tokens")
        let custom = MonarchAiPromptInputLabels(counter: { "\($0)-\($1)" }, tokens: { "~\($0)" })
        #expect(Tokens.counterText(count: 1, maxLength: 2, labels: custom) == "1-2")
        #expect(Tokens.tokensText(3, labels: custom) == "~3")
    }

    @Test("Colours: the counter warns and errs, the box shows danger over the limit and accent while dragging")
    func colours() {
        #expect(Tokens.counterForeground(.ok) == \.textMuted)
        #expect(Tokens.counterForeground(.warning) == \.warningStrong)
        #expect(Tokens.counterForeground(.over) == \.dangerStrong)
        #expect(Tokens.border(limit: .ok, isDragging: false, isEnabled: true) == \.borderInput)
        #expect(Tokens.border(limit: .over, isDragging: false, isEnabled: true) == \.dangerBorder)
        #expect(Tokens.border(limit: .over, isDragging: true, isEnabled: true) == \.accentBorder)
        #expect(Tokens.border(limit: .over, isDragging: true, isEnabled: false) == \.borderSubtle)
        #expect(Tokens.background(isDragging: false, isEnabled: true) == \.bgSurface)
        #expect(Tokens.background(isDragging: true, isEnabled: true) == \.accentSubtle)
        #expect(Tokens.background(isDragging: true, isEnabled: false) == \.bgSubtle)
        #expect(Tokens.foreground(isEnabled: false) == \.textDisabled)
    }

    @Test("Sending needs text or attachments, within the limit, enabled and not busy")
    func canSubmit() {
        #expect(Tokens.canSubmit(text: "Hi", hasAttachments: false, maxLength: nil, isEnabled: true, isBusy: false))
        #expect(!Tokens.canSubmit(text: "  \n ", hasAttachments: false, maxLength: nil, isEnabled: true, isBusy: false))
        #expect(Tokens.canSubmit(text: "", hasAttachments: true, maxLength: nil, isEnabled: true, isBusy: false))
        #expect(!Tokens.canSubmit(text: "Hello", hasAttachments: false, maxLength: 4, isEnabled: true, isBusy: false))
        #expect(Tokens.canSubmit(text: "Hell", hasAttachments: false, maxLength: 4, isEnabled: true, isBusy: false))
        #expect(!Tokens.canSubmit(text: "Hi", hasAttachments: false, maxLength: nil, isEnabled: false, isBusy: false))
        #expect(!Tokens.canSubmit(text: "Hi", hasAttachments: false, maxLength: nil, isEnabled: true, isBusy: true))
    }

    @Test("A failed send gives the text back unless something new was typed")
    func restored() {
        #expect(Tokens.restored(current: "", sent: "Hello") == "Hello")
        #expect(Tokens.restored(current: "New", sent: "Hello") == "New")
    }

    @Test("Line range is at least one line and never inverted")
    func lines() {
        #expect(Tokens.lines(min: 1, max: 8) == 1...8)
        #expect(Tokens.lines(min: 0, max: 3) == 1...3)
        #expect(Tokens.lines(min: 4, max: 2) == 4...4)
    }

    @Test("Return sends with returnKey; Command-Return always; Shift-Return adds a line")
    func keys() {
        #expect(Tokens.sends(submitOn: .returnKey, modifiers: []))
        #expect(!Tokens.sends(submitOn: .returnKey, modifiers: .shift))
        #expect(Tokens.sends(submitOn: .returnKey, modifiers: .command))
        #expect(!Tokens.sends(submitOn: .commandReturn, modifiers: []))
        #expect(Tokens.sends(submitOn: .commandReturn, modifiers: .command))
    }

    @Test("Built-in strings resolve")
    func strings() {
        let all = [
            MonarchAiPromptInputStrings.placeholder, MonarchAiPromptInputStrings.send,
            MonarchAiPromptInputStrings.stop, MonarchAiPromptInputStrings.attach,
            MonarchAiPromptInputStrings.dropHint, MonarchAiPromptInputStrings.counter(1, 2),
            MonarchAiPromptInputStrings.counterLabel(1, 2), MonarchAiPromptInputStrings.counterWarning(2),
            MonarchAiPromptInputStrings.counterOver(2), MonarchAiPromptInputStrings.tokens(2),
        ]
        for string in all {
            #expect(!string.hasPrefix("monarch."))
        }
        #expect(MonarchAiPromptInputStrings.placeholder == "Send a message…")
        #expect(MonarchAiPromptInputStrings.attach == "Attach files")
    }

    @Test("The app's focus state can be passed in, and the input lays out with it")
    @MainActor
    func externalFocus() {
        struct Screen: View {
            @FocusState var isFocused: Bool
            var body: some View {
                MonarchAiPromptInput("Message", text: .constant(""), focus: $isFocused) { _ in }
            }
        }
        #expect(Host.layout(Screen()).height > 0)
    }
}
