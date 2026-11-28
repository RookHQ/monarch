import SwiftUI

/// One organisation of a ``MonarchLogoCloud``: its name, an optional logo image and an optional link.
public struct MonarchLogoCloudLogo: Identifiable {
    /// The organisation's name. It is the spoken label, and the wordmark when there is no image.
    public let name: String
    /// The logo. Without one the name is shown as a wordmark.
    public let image: Image?
    /// Where a tap on the logo goes, such as a customer story.
    public let destination: URL?

    /// The name, which identifies the logo in the cloud.
    public var id: String { name }

    /// A logo.
    ///
    /// - Parameters:
    ///   - name: The organisation's name, shown when there is no image and always read by VoiceOver.
    ///   - image: The logo, loaded by the app.
    ///   - destination: Where a tap on the logo goes. Without one the logo is not interactive.
    public init(_ name: String, image: Image? = nil, destination: URL? = nil) {
        self.name = name
        self.image = image
        self.destination = destination
    }
}
