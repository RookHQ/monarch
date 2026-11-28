import SwiftUI

/// One page of the catalogue: the examples of one component.
///
/// Add a page by adding one file,
/// `Sample/Sources/Sections/<Family>/<Name>Section.swift`, that declares
/// `struct <Name>Section: CataloguePage`. Do not add it to any list:
/// `Scripts/generate.py` finds it and writes `CatalogueRegistry`. The family
/// in the navigation is the name of the folder.
protocol CataloguePage: View {
    /// The title in the list and in the navigation bar.
    static var title: String { get }
    /// Pages are created by the registry.
    init()
}
