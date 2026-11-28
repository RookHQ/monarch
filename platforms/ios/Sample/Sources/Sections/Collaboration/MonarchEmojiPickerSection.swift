import MonarchUI
import SwiftUI

struct MonarchEmojiPickerSection: CataloguePage {
    static let title = "EmojiPicker"

    @Environment(\.monarchTheme) private var theme
    @State private var message = ""
    @State private var recents: [String] = []
    @State private var skinTone = MonarchEmojiSkinTone.none
    @State private var isPresented = false

    var body: some View {
        Text(message.isEmpty ? "Pick an emoji" : message)
            .monarchTextStyle(.title)
            .foregroundStyle(theme.colors.textDefault)

        CatalogueCaption("Inline, with recents and a remembered skin tone")
        MonarchEmojiPicker(recents: recents, skinTone: $skinTone) { emoji, _ in
            message += emoji
            recents = MonarchEmojiPicker.pushingRecent(emoji, to: recents)
        }

        CatalogueCaption("In a sheet")
        Button("Add reaction", systemImage: "face.smiling") { isPresented = true }
            .buttonStyle(.monarch())
            .sheet(isPresented: $isPresented) {
                MonarchEmojiPicker(recents: recents, skinTone: $skinTone) { emoji, _ in
                    message += emoji
                    recents = MonarchEmojiPicker.pushingRecent(emoji, to: recents)
                    isPresented = false
                }
                .padding(theme.spacing.space4)
                .presentationDetents([.medium, .large])
            }
    }
}
