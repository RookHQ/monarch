import SwiftUI

/// The AutoFill kind of a free-text address field.
enum MonarchAddressContentType: Sendable {
    case street
    case city
    case region
    case postalCode
}

extension View {
    /// Tells AutoFill what an address field holds, and types postal codes
    /// in capitals without autocorrection. iOS only; the macOS build host
    /// has no content types for these fields.
    @ViewBuilder
    func monarchAddressContentType(_ type: MonarchAddressContentType) -> some View {
        #if os(iOS)
            switch type {
            case .street:
                textContentType(.fullStreetAddress)
            case .city:
                textContentType(.addressCity)
            case .region:
                textContentType(.addressState)
            case .postalCode:
                textContentType(.postalCode)
                    .textInputAutocapitalization(.characters)
                    .autocorrectionDisabled()
            }
        #else
            self
        #endif
    }
}
