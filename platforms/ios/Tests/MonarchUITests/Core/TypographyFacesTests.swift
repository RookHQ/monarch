import SwiftUI
import Testing

@testable import MonarchUI

@Suite("MonarchTypography faces")
struct TypographyFacesTests {
    private let poppins = MonarchTypography(
        fontFamily: "Poppins",
        faces: [400: "Poppins-Regular", 500: "Poppins-Medium", 600: "Poppins-SemiBold"]
    )

    @Test("no faces means the family is selected by name and weight")
    func emptyFaces() {
        #expect(MonarchTypography(fontFamily: "Poppins").face(for: .medium) == nil)
        #expect(MonarchTypography.system.face(for: .regular) == nil)
    }

    @Test("each weight picks its face")
    func exactWeights() {
        #expect(poppins.face(for: .regular) == "Poppins-Regular")
        #expect(poppins.face(for: .medium) == "Poppins-Medium")
        #expect(poppins.face(for: .semibold) == "Poppins-SemiBold")
    }

    @Test("a weight without a face takes the nearest, a tie the heavier")
    func nearestWeight() {
        #expect(poppins.face(for: .bold) == "Poppins-SemiBold")
        #expect(poppins.face(for: .light) == "Poppins-Regular")
        let twoFaces = MonarchTypography(fontFamily: "X", faces: [400: "X-Regular", 600: "X-SemiBold"])
        #expect(twoFaces.face(for: .medium) == "X-SemiBold")
    }

    @Test("faces take part in equality")
    func equality() {
        #expect(poppins != MonarchTypography(fontFamily: "Poppins"))
    }
}
