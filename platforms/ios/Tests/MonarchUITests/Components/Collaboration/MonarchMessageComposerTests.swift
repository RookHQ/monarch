import Foundation
import SwiftUI
import Testing

@testable import MonarchUI

private let modes = [
    MonarchMessageComposerMode("reply", label: "Reply", placeholder: "Reply to the customer"),
    MonarchMessageComposerMode("note", label: "Internal note", isNote: true, sendLabel: "Add note"),
]
private let people = [
    MonarchMentionItem(id: "u1", label: "Ana Lopez", detail: "Design"),
    MonarchMentionItem(id: "u2", label: "Ben Okafor"),
]

enum MonarchMessageComposerFixtures: ComponentFixtures {
    static let id = "message-composer"
    static let fixtures = [
        Fixture("empty with placeholder") {
            MonarchMessageComposer("Message #design", text: .constant(""), placeholder: "Message #design") { _ in }
        },
        Fixture("reply, attachments, count and draft saved") {
            MonarchMessageComposer(
                "Reply",
                text: .constant("Sounds good, see the brief"),
                replyTo: MonarchMessageComposerReply(author: "Ana Lopez", excerpt: "Can we ship on Friday?"),
                draftStatus: .saved,
                maxLength: 500,
                hasAttachments: true,
                onAttach: {},
                onDismissReply: {}
            ) {
                MonarchAttachmentChip("brief.pdf", size: 240_000)
                MonarchAttachmentChip("mockup.png", status: .uploading, progress: 0.4)
            } actions: {
                Button("Schedule", systemImage: "clock") {}.labelStyle(.iconOnly)
            } onSend: { _ in
            }
        },
        Fixture("editing") {
            MonarchMessageComposer(
                "Edit message",
                text: .constant("Shipping on Friday."),
                isEditing: true,
                onCancelEdit: {}
            ) { _ in }
        },
        Fixture("modes with the note selected, sending") {
            MonarchMessageComposer(
                "Ticket reply",
                text: .constant("Customer is on the enterprise plan"),
                modes: modes,
                selectedMode: .constant("note"),
                isSending: true
            ) { _ in }
        },
        Fixture("mentions and over the limit, draft error") {
            MonarchMessageComposer(
                "Message",
                text: .constant("A message that is longer than the limit allows @An"),
                mentions: MonarchMessageComposerMentions(people) { _ in },
                draftStatus: .error,
                maxLength: 20
            ) { _ in }
        },
        Fixture("disabled with a reason") {
            MonarchMessageComposer(
                "Message",
                text: .constant(""),
                disabledReason: "Only admins can post in this channel."
            ) { _ in }
            .disabled(true)
        },
        Fixture("long text and long reply") {
            MonarchMessageComposer(
                "Message",
                text: .constant(String(repeating: "A long draft that wraps over several lines. ", count: 8)),
                replyTo: MonarchMessageComposerReply(
                    author: "Maximilian Alexander von Hohenberg-Lichtenstein",
                    excerpt: String(repeating: "A quoted message. ", count: 10)
                ),
                showsEmojiButton: false
            ) { _ in }
        },
    ]
}

@Suite("MonarchMessageComposer")
@MainActor
struct MonarchMessageComposerTests {
    typealias Tokens = MonarchMessageComposerTokens
    let labels = MonarchMessageComposerLabels()
    let english = Locale(identifier: "en_GB")

    @Test("Sending needs text or attachments, the enabled state, no send in flight and the limit")
    func canSend() {
        #expect(Tokens.canSend(text: "Hi", hasAttachments: false, maxLength: nil, isEnabled: true, isSending: false))
        #expect(
            !Tokens.canSend(text: "  \n ", hasAttachments: false, maxLength: nil, isEnabled: true, isSending: false)
        )
        #expect(Tokens.canSend(text: "", hasAttachments: true, maxLength: nil, isEnabled: true, isSending: false))
        #expect(!Tokens.canSend(text: "Hi", hasAttachments: false, maxLength: nil, isEnabled: false, isSending: false))
        #expect(!Tokens.canSend(text: "Hi", hasAttachments: false, maxLength: nil, isEnabled: true, isSending: true))
        #expect(!Tokens.canSend(text: "Hello", hasAttachments: false, maxLength: 4, isEnabled: true, isSending: false))
        #expect(Tokens.canSend(text: "Hell", hasAttachments: false, maxLength: 4, isEnabled: true, isSending: false))
    }

    @Test("A failed send shows the error's own text, else the override, else the built-in line")
    func sendFailure() {
        struct Described: LocalizedError { var errorDescription: String? { "The channel is archived." } }
        struct Plain: Error {}
        let fallback = MonarchMessageComposerStrings.sendFailed
        #expect(
            MonarchFailureMessage.message(for: Described(), override: "x", fallback: fallback)
                == "The channel is archived."
        )
        #expect(MonarchFailureMessage.message(for: Plain(), override: "Not sent", fallback: fallback) == "Not sent")
        #expect(MonarchFailureMessage.message(for: Plain(), override: nil, fallback: fallback) == fallback)
        #expect(MonarchMessageComposerLabels(sendFailed: "Not sent").sendFailed == "Not sent")
    }

    @Test("The send title follows sending, the mode and editing")
    func sendTitle() {
        #expect(Tokens.sendTitle(isSending: false, isEditing: false, mode: nil, labels: labels) == "Send")
        #expect(Tokens.sendTitle(isSending: false, isEditing: true, mode: nil, labels: labels) == "Save")
        #expect(Tokens.sendTitle(isSending: true, isEditing: false, mode: nil, labels: labels) == "Sending…")
        #expect(Tokens.sendTitle(isSending: false, isEditing: false, mode: modes[1], labels: labels) == "Add note")
        #expect(Tokens.sendTitle(isSending: false, isEditing: false, mode: modes[0], labels: labels) == "Send")
    }

    @Test("The context shows editing over a reply, or nothing")
    func contextTitle() {
        let reply = MonarchMessageComposerReply(author: "Ana")
        #expect(Tokens.contextTitle(isEditing: false, replyTo: nil, labels: labels) == nil)
        #expect(Tokens.contextTitle(isEditing: false, replyTo: reply, labels: labels) == "Replying to Ana")
        #expect(Tokens.contextTitle(isEditing: true, replyTo: reply, labels: labels) == "Editing message")
    }

    @Test("The count shows used and limit, and is spoken as characters left or over")
    func count() {
        #expect(Tokens.countText("Hello", maxLength: nil, locale: english) == nil)
        #expect(Tokens.countText("Hello", maxLength: 1_000, locale: english) == "5/1,000")
        #expect(Tokens.countLabel("Hello", maxLength: 12, labels: labels) == "7 characters left")
        #expect(Tokens.countLabel("Hello", maxLength: 3, labels: labels) == "2 characters over the limit")
        #expect(Tokens.countForeground("Hello", maxLength: 3) == \.dangerStrong)
        #expect(Tokens.countForeground("Hello", maxLength: 10) == \.textMuted)
        #expect(Tokens.isOverLimit("Hello", maxLength: 4))
        #expect(!Tokens.isOverLimit("Hello", maxLength: nil))
    }

    @Test("A note has the warning look; disabled wins")
    func colours() {
        #expect(Tokens.background(isNote: false, isEnabled: true) == \.bgSurface)
        #expect(Tokens.background(isNote: true, isEnabled: true) == \.warningSubtle)
        #expect(Tokens.background(isNote: true, isEnabled: false) == \.bgSubtle)
        #expect(Tokens.border(isNote: false, isEnabled: true) == \.borderInput)
        #expect(Tokens.border(isNote: true, isEnabled: true) == \.warningBorder)
        #expect(Tokens.border(isNote: true, isEnabled: false) == \.borderSubtle)
        #expect(Tokens.foreground(isEnabled: true) == \.textDefault)
        #expect(Tokens.foreground(isEnabled: false) == \.textDisabled)
    }

    @Test("The selected mode is the matching one, or the first")
    func mode() {
        #expect(Tokens.mode("note", in: modes)?.value == "note")
        #expect(Tokens.mode(nil, in: modes)?.value == "reply")
        #expect(Tokens.mode("missing", in: modes)?.value == "reply")
        #expect(Tokens.mode("note", in: []) == nil)
    }

    @Test("Typing is reported at most once per interval")
    func typing() {
        let start = Date(timeIntervalSinceReferenceDate: 800_000_000)
        #expect(Tokens.shouldSignalTyping(now: start, lastSignal: nil, interval: 3))
        #expect(!Tokens.shouldSignalTyping(now: start.addingTimeInterval(2.9), lastSignal: start, interval: 3))
        #expect(Tokens.shouldSignalTyping(now: start.addingTimeInterval(3), lastSignal: start, interval: 3))
    }

    @Test("The message is trimmed, keeps the mode and only mentions still in the text")
    func message() {
        let picked = [
            MonarchMessageComposerPicked(trigger: "@", item: people[0]),
            MonarchMessageComposerPicked(trigger: "@", item: people[1]),
            MonarchMessageComposerPicked(trigger: "@", item: people[0]),
        ]
        let message = Tokens.message(text: "  Hi @Ana Lopez, see this\n", picked: picked, mode: modes[1])
        #expect(message.text == "Hi @Ana Lopez, see this")
        #expect(message.mentions.map(\.id) == ["u1"])
        #expect(message.mode == "note")
        #expect(Tokens.message(text: "Hi", picked: [], mode: nil).mode == nil)
    }

    @Test("A picked mention is written as trigger and label")
    func mentionText() {
        #expect(Tokens.mentionText(people[0], trigger: "@") == "@Ana Lopez")
        let query = MonarchMentionQuery.find(in: "Hi @An")
        let replaced = query?.replacing(in: "Hi @An", with: Tokens.mentionText(people[0], trigger: "@"))
        #expect(replaced == "Hi @Ana Lopez ")
    }

    @Test("An emoji is added at the end, after a space when needed")
    func insertingEmoji() {
        #expect(Tokens.inserting("🎉", into: "") == "🎉")
        #expect(Tokens.inserting("🎉", into: "Done") == "Done 🎉")
        #expect(Tokens.inserting("🎉", into: "Done ") == "Done 🎉")
    }

    @Test("Overrides replace the built-in strings")
    func overrides() {
        let custom = MonarchMessageComposerLabels(
            send: "Senden",
            replyingTo: { "Antwort an \($0)" },
            remaining: { "\($0) übrig" }
        )
        #expect(Tokens.sendTitle(isSending: false, isEditing: false, mode: nil, labels: custom) == "Senden")
        #expect(
            Tokens.contextTitle(isEditing: false, replyTo: MonarchMessageComposerReply(author: "Ana"), labels: custom)
                == "Antwort an Ana"
        )
        #expect(Tokens.countLabel("Hi", maxLength: 5, labels: custom) == "3 übrig")
    }

    @Test("The built-in strings come from the component's strings table")
    func strings() {
        #expect(MonarchMessageComposerStrings.cancel == "Cancel")
        #expect(MonarchMessageComposerStrings.attach == "Attach files")
        #expect(MonarchMessageComposerStrings.emoji == "Add emoji")
        #expect(MonarchMessageComposerStrings.modes == "Message type")
        #expect(MonarchMessageComposerStrings.dismissReply == "Stop replying")
        #expect(MonarchMessageComposerStrings.draftSaving == "Saving draft…")
        #expect(MonarchMessageComposerStrings.draftSaved == "Draft saved")
        #expect(MonarchMessageComposerStrings.draftError == "Draft not saved")
        #expect(MonarchMessageComposerStrings.sendFailed == "Message not sent. Try again.")
    }

    #if os(iOS)
        @Test("The composer is at least two touch targets high")
        func minimumHeight() {
            let size = Host.layout(MonarchMessageComposer("Message", text: .constant("")) { _ in })
            #expect(size.height >= MonarchAccessibility.minimumHitTarget * 2)
        }
    #endif
}
