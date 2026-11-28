/// A family of components in the catalogue's navigation.
struct CatalogueFamily: Identifiable {
    let name: String
    let entries: [CatalogueEntry]

    var id: String { name }
}
