import MonarchUI
import SwiftUI

struct MonarchDeviceFrameSection: CataloguePage {
    static let title = "Device frame"

    @Environment(\.monarchTheme) private var theme

    var body: some View {
        CatalogueCaption("Phone, with a status bar. Read as one image.")
        MonarchDeviceFrame("Preview of a link page on a phone", statusBarTime: "9:41") {
            linkPage
        }
        .frame(maxWidth: 240)

        CatalogueCaption("Interactive: the content scrolls")
        MonarchDeviceFrame(
            "Live preview of a list",
            screenSize: CGSize(width: 240, height: 320),
            isInteractive: true,
            isFlat: true
        ) {
            VStack(alignment: .leading, spacing: theme.spacing.space3) {
                ForEach(1..<21, id: \.self) { index in
                    Text("Row \(index)").monarchTextStyle(.body)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(theme.spacing.space4)
            .padding(.top, theme.spacing.space6)
        }

        CatalogueCaption("Tablet")
        MonarchDeviceFrame("Preview of an email on a tablet", device: .tablet) {
            Text("Welcome to the team")
                .monarchTextStyle(.heading)
                .padding(theme.spacing.space6)
        }
    }

    private var linkPage: some View {
        VStack(spacing: theme.spacing.space3) {
            MonarchIcon(systemName: "person.crop.circle.fill", size: .lg)
            Text("Ada Lovelace").monarchTextStyle(.subheading)
            ForEach(["Writing", "Talks", "Contact"], id: \.self) { link in
                Text(link)
                    .monarchTextStyle(.label)
                    .frame(maxWidth: .infinity)
                    .padding(theme.spacing.space2)
                    .background(theme.colors.bgMuted, in: RoundedRectangle(cornerRadius: theme.radius.md))
            }
        }
        .padding(theme.spacing.space4)
    }
}
