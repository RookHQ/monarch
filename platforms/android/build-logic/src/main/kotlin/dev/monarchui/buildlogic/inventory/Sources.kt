package dev.monarchui.buildlogic.inventory

import java.io.File

/** A `ComponentFixtures` object found in the test sources. */
class FixtureSet(val objectName: String, val packageName: String, val id: String?, val file: File) {
    val qualifiedName: String get() = "$packageName.$objectName"

    /** `MonarchBadgeFixtures` is declared beside the test class `MonarchBadgeTest`. */
    val testClass: String get() = objectName.removeSuffix("Fixtures") + "Test"
}

/** A `CataloguePage` object found in the sample. */
class CataloguePageSource(val objectName: String, val packageName: String, val familyPackage: String, val file: File)

/** Finds the per-component declarations that the generated registries list. */
object Sources {
    private val packageLine = Regex("""^package\s+([\w.]+)""", RegexOption.MULTILINE)
    private val fixturesObject = Regex("""^(?:internal\s+)?object\s+(\w+)\s*:\s*ComponentFixtures\b""", RegexOption.MULTILINE)
    private val fixturesId = Regex("""override\s+val\s+id(?:\s*:\s*String)?\s*=\s*"([^"]+)"""")
    private val pageObject = Regex("""^(?:internal\s+)?object\s+(\w+)\s*:\s*CataloguePage\b""", RegexOption.MULTILINE)

    fun fixtures(layout: MonarchLayout): List<FixtureSet> =
        kotlinFiles(layout.composeTest).flatMap { file ->
            val text = file.readText()
            val packageName = packageLine.find(text)?.groupValues?.get(1).orEmpty()
            fixturesObject.findAll(text).map { match ->
                val id = fixturesId.find(text, match.range.last)?.groupValues?.get(1)
                FixtureSet(match.groupValues[1], packageName, id, file)
            }.toList()
        }.sortedBy { it.qualifiedName }

    fun pages(layout: MonarchLayout, errors: MutableList<String>): List<CataloguePageSource> =
        kotlinFiles(layout.sampleSections).flatMap { file ->
            val where = layout.relative(file)
            val parts = file.relativeTo(layout.sampleSections).invariantSeparatorsPath.split('/')
            val text = file.readText()
            val objects = pageObject.findAll(text).map { it.groupValues[1] }.toList()
            when {
                objects.isEmpty() -> emptyList()
                parts.size != 2 || Families.titleOf(parts[0]) == null -> {
                    errors += "$where: catalogue pages live at sections/<family>/<Name>Section.kt, " +
                        "where <family> is one of ${Families.packages.sorted().joinToString(", ")}"
                    emptyList()
                }
                else -> objects.map { name ->
                    if ("$name.kt" != parts[1]) errors += "$where: the file must be named after $name"
                    CataloguePageSource(name, packageLine.find(text)?.groupValues?.get(1).orEmpty(), parts[0], file)
                }
            }
        }.sortedWith(compareBy({ it.familyPackage }, { it.objectName }))

    /** The hand-written files of one entry: sources, previews, strings, tests and the catalogue page. */
    fun filesOf(entry: InventoryEntry, layout: MonarchLayout, fixtures: List<FixtureSet>): List<File> {
        val family = Families.packageOf(entry.family)
        val found = mutableListOf<File>()
        entry.name?.let { name ->
            val folder = "components/$family/${componentPackage(name)}"
            found += kotlinFiles(File(layout.composeMain, folder))
            found += kotlinFiles(File(layout.composeDebug, folder))
        }
        found += fixtures.filter { it.id == entry.id }.map { it.file }
        found += pageCandidates(entry).map { File(layout.sampleSections, "$family/$it.kt") }.filter { it.isFile }
        return found.distinct()
    }

    /** The names a catalogue page of [entry] may have. */
    fun pageCandidates(entry: InventoryEntry): List<String> =
        listOfNotNull(entry.name, pascal(entry.id)).distinct().map { "${it}Section" }
}
