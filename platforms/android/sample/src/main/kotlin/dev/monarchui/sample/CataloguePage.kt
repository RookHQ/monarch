package dev.monarchui.sample

import androidx.compose.runtime.Composable

/**
 * One page of the catalogue: one component, or one documented use of a
 * Material 3 component.
 *
 * Declare an `object <Name>Section : CataloguePage` in
 * `sections/<family>/<Name>Section.kt`. Do not register it anywhere: Gradle
 * finds every such object and generates the navigation (`CatalogueFamilies`).
 * The folder is the family the page is listed under.
 */
interface CataloguePage {
    /** The title in the list and the top bar: the title of the inventory entry. */
    val title: String

    /**
     * The page, laid out in a column with the standard gap. No scrolling, no
     * padding and no title: the catalogue adds them. Make the page
     * interactive where the component is. Use `CatalogueCaption` for a
     * heading inside the page.
     */
    @Composable
    fun Content()
}

/**
 * A family of the catalogue and its pages.
 *
 * @property title the display name of the family, for example "Data display".
 * @property pages the pages, in alphabetical order of their object names.
 */
class CatalogueFamily(val title: String, val pages: List<CataloguePage>)
