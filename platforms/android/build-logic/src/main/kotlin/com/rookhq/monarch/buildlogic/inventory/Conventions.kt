package com.rookhq.monarch.buildlogic.inventory

import java.io.File

/**
 * The rules of docs/ADDING-COMPONENTS.md that can be checked by reading files.
 * Every message names the file and the rule.
 */
object Conventions {
    fun check(
        entries: List<InventoryEntry>,
        fixtures: List<FixtureSet>,
        pages: List<CataloguePageSource>,
        layout: MonarchLayout,
        errors: MutableList<String>,
    ) {
        entries.forEach { coverage(it, layout, errors) }
        entries.forEach { layer(it, errors) }
        parityFixtures(entries, layout, errors)
        fixtures(entries, fixtures, layout, errors)
        pages(entries, pages, errors)
        strings(layout, errors)
    }

    private fun coverage(entry: InventoryEntry, layout: MonarchLayout, errors: MutableList<String>) {
        val where = "inventory/${entry.id}.json"
        val coverage = entry.coverage
        if (entry.shipsCode) {
            if (entry.name == null) errors += "$where: coverage $coverage needs a Kotlin name"
            if (entry.tests.isEmpty()) errors += "$where: coverage $coverage needs at least one test class in tests"
        } else if (entry.name != null) {
            errors += "$where: coverage $coverage ships no Kotlin type, so name must be null"
        }
        if (coverage == "web-only") {
            if (entry.kind != "none") errors += "$where: coverage web-only needs kind none"
        } else {
            if (entry.wrapsNative == null) errors += "$where: wrapsNative must name the Compose or Material 3 API"
            if (entry.examples.isEmpty()) errors += "$where: coverage $coverage needs at least one compiled example"
        }
        if (coverage == "native" && (entry.kind != "native" || entry.category != "A")) {
            errors += "$where: coverage native needs kind native and category A"
        }
        if (coverage == "compose" && entry.kind != "recipe") errors += "$where: coverage compose needs kind recipe"
        val name = entry.name
        if (coverage == "build" && name != null && entry.family != Families.FOUNDATIONS) {
            val path = "components/${Families.packageOf(entry.family)}/${componentPackage(name)}/$name.kt"
            if (!File(layout.composeMain, path).isFile) {
                errors += "$where: expected the component at ${layout.relative(File(layout.composeMain, path))}"
            }
        }
    }

    /**
     * CROSS-PLATFORM.md section 8: an example bound to a cross-platform scenario fixture is the first one, names
     * its own id, the web example's title and a template, and the fixture is generated. Its code is written by
     * `scripts/native/fixtures.mjs` (whose `--check` runs in `npm run verify`); here only the binding is checked.
     */
    private fun parityFixtures(entries: List<InventoryEntry>, layout: MonarchLayout, errors: MutableList<String>) {
        val generated = layout.parityFixtures.takeIf { it.isFile }?.readText().orEmpty()
        entries.forEach { entry ->
            val where = "inventory/${entry.id}.json"
            (entry.raw["examples"] as List<*>).forEachIndexed { index, item ->
                val example = item as Map<*, *>
                val fixture = example["fixture"] as String?
                if (fixture == null) {
                    if (example.containsKey("template") || example.containsKey("setupTemplate")) {
                        errors += "$where: example $index has a template but no fixture"
                    }
                    return@forEachIndexed
                }
                if (index != 0) errors += "$where: only the first example binds a fixture"
                if (fixture != entry.id || example["webExample"] == null || example["template"] == null) {
                    errors += "$where: a bound example needs fixture \"${entry.id}\", webExample and template"
                }
                if ("id = \"$fixture\"," !in generated) {
                    errors += "$where: fixture \"$fixture\" is not in ${layout.relative(layout.parityFixtures)}; " +
                        "run npm run native:fixtures"
                }
            }
        }
    }

    /**
     * ADR 0017: a pattern is an experimental domain composite, so an entry in
     * the patterns layer is always experimental. The schema already limits
     * `layer` to `components` and `patterns` and requires it.
     */
    private fun layer(entry: InventoryEntry, errors: MutableList<String>) {
        if (entry.layer == "patterns" && entry.status != "experimental") {
            errors += "inventory/${entry.id}.json: layer patterns needs status experimental, not ${entry.status} " +
                "(ADR 0017); make it experimental or move it to layer components"
        }
    }

    private fun fixtures(
        entries: List<InventoryEntry>,
        fixtures: List<FixtureSet>,
        layout: MonarchLayout,
        errors: MutableList<String>,
    ) {
        val ids = entries.map { it.id }.toSet()
        fixtures.forEach { set ->
            val where = layout.relative(set.file)
            when {
                set.id == null -> errors += "$where: ${set.objectName} needs `override val id = \"<inventory id>\"`"
                set.id !in ids ->
                    errors += "$where: ${set.objectName} has id \"${set.id}\", but inventory/${set.id}.json does not exist"
            }
            if (!set.objectName.endsWith("Fixtures")) errors += "$where: ${set.objectName} must be named <Name>Fixtures"
        }
        fixtures.groupBy { it.objectName }.filterValues { it.size > 1 }.keys.forEach {
            errors += "$it is declared more than once"
        }
        val covered = fixtures.mapNotNull { it.id }.toSet()
        entries.filter { it.shipsCode && it.id !in covered }.forEach { entry ->
            errors += "inventory/${entry.id}.json: coverage ${entry.coverage} needs fixtures: declare " +
                "`object <Name>Fixtures : ComponentFixtures` with id \"${entry.id}\" in the component's test file"
        }
    }

    private fun pages(entries: List<InventoryEntry>, pages: List<CataloguePageSource>, errors: MutableList<String>) {
        val byFamily = pages.groupBy { it.familyPackage }.mapValues { (_, list) -> list.map { it.objectName }.toSet() }
        entries.filter { it.coverage != "web-only" }.forEach { entry ->
            val family = Families.packageOf(entry.family)
            val candidates = Sources.pageCandidates(entry)
            if (candidates.none { it in byFamily[family].orEmpty() }) {
                errors += "inventory/${entry.id}.json: needs a catalogue page: " +
                    "sample/.../sections/$family/${candidates.first()}.kt with `object ${candidates.first()} : CataloguePage`"
            }
        }
    }

    private val stringName = Regex("""<(?:string|plurals|string-array)\s+name="([^"]+)"""")

    private fun strings(layout: MonarchLayout, errors: MutableList<String>) {
        layout.composeRes.listFiles().orEmpty().sortedBy { it.name }.forEach { file ->
            val where = layout.relative(file)
            if (file.name == "strings.xml") {
                errors += "$where: use one file per component, strings_<component>.xml"
                return@forEach
            }
            val component = Regex("""^strings_([a-z0-9_]+)\.xml$""").find(file.name)?.groupValues?.get(1) ?: return@forEach
            stringName.findAll(file.readText()).map { it.groupValues[1] }.forEach { key ->
                if (!key.startsWith("monarch_${component}_")) {
                    errors += "$where: string \"$key\" must start with monarch_${component}_"
                }
            }
        }
    }
}
