package dev.monarchui.components.datagrid

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import dev.monarchui.components.datagrid.columnmapping.MonarchColumnMapping
import dev.monarchui.components.datagrid.columnmapping.MonarchColumnMappingConfidence
import dev.monarchui.components.datagrid.columnmapping.MonarchColumnMappingDefaults
import dev.monarchui.components.datagrid.columnmapping.MonarchColumnMappingField
import dev.monarchui.components.datagrid.columnmapping.MonarchColumnMappingLabels
import dev.monarchui.components.datagrid.columnmapping.MonarchColumnMappingSource
import dev.monarchui.components.datagrid.columnmapping.MonarchColumnMappingSuggestion
import dev.monarchui.components.datagrid.columnmapping.MonarchColumnMappingTokens
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private val Fields = listOf(
    MonarchColumnMappingField(id = "name", label = "Name", required = true),
    MonarchColumnMappingField(id = "email", label = "Email", required = true, aliases = listOf("E-mail", "Mail")),
    MonarchColumnMappingField(
        id = "phone",
        label = "Phone number",
        description = "International format, e.g. +44 20 7946 0000",
    ),
    MonarchColumnMappingField(id = "company", label = "Company"),
)

private val Sources = listOf(
    MonarchColumnMappingSource("Full Name", listOf("Sam Lee", "Ana Ruiz")),
    MonarchColumnMappingSource("E-mail", listOf("sam@example.com")),
    MonarchColumnMappingSource("Phone"),
    MonarchColumnMappingSource("Notes"),
)

private val Suggested = MonarchColumnMappingDefaults.mapping(MonarchColumnMappingDefaults.suggest(Sources, Fields))

internal object MonarchColumnMappingFixtures : ComponentFixtures {
    override val id = "column-mapping"
    override val fixtures = listOf(
        fixture("suggested") {
            MonarchColumnMapping(sources = Sources, mapping = Suggested, fields = Fields, onMappingChange = {})
        },
        fixture("missing and duplicate") {
            MonarchColumnMapping(
                sources = Sources.drop(2),
                mapping = mapOf("Phone" to "phone", "Notes" to "phone"),
                fields = Fields,
                onMappingChange = {},
            )
        },
        fixture("disabled, nothing mapped") {
            MonarchColumnMapping(
                sources = Sources.take(1),
                mapping = emptyMap(),
                fields = Fields,
                onMappingChange = {},
                enabled = false,
            )
        },
        fixture("issues hidden, no bulk actions") {
            MonarchColumnMapping(
                sources = Sources.take(1),
                mapping = emptyMap(),
                fields = Fields,
                onMappingChange = {},
                showIssues = false,
                showBulkActions = false,
            )
        },
        fixture("blank and long headers") {
            MonarchColumnMapping(
                sources = listOf(
                    MonarchColumnMappingSource(""),
                    MonarchColumnMappingSource(
                        "Primary contact person at the customer's head office",
                        listOf("A very long example value that wraps onto another line"),
                    ),
                ),
                mapping = emptyMap(),
                fields = Fields,
                onMappingChange = {},
                showIssues = false,
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchColumnMappingTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun suggestsLikeTheWeb() {
        val suggestions = MonarchColumnMappingDefaults.suggest(Sources, Fields)
        assertEquals(
            MonarchColumnMappingSuggestion("name", MonarchColumnMappingConfidence.Similar, 0.75),
            suggestions["Full Name"],
        )
        assertEquals(MonarchColumnMappingConfidence.Exact, suggestions["E-mail"]?.confidence)
        assertEquals(MonarchColumnMappingConfidence.Exact, suggestions["Phone"]?.confidence)
        assertEquals("phone", suggestions["Phone"]?.fieldId)
        assertFalse("Notes" in suggestions)
        assertEquals(mapOf("Full Name" to "name", "E-mail" to "email", "Phone" to "phone"), Suggested)
    }

    @Test
    fun eachFieldIsSuggestedOnceAndCloseMatchesNeedTheThreshold() {
        val suggestions = MonarchColumnMappingDefaults.suggest(
            listOf(MonarchColumnMappingSource("Email"), MonarchColumnMappingSource("Work email")),
            Fields,
        )
        assertEquals(listOf("Email"), suggestions.keys.toList())
        val typo = MonarchColumnMappingDefaults.suggest(
            listOf(MonarchColumnMappingSource("Adress")),
            listOf(MonarchColumnMappingField(id = "address", label = "Address")),
        )
        assertEquals(MonarchColumnMappingConfidence.Similar, typo["Adress"]?.confidence)
        assertEquals(0.86, typo["Adress"]?.score)
        val strict = MonarchColumnMappingDefaults.suggest(
            listOf(MonarchColumnMappingSource("Adress")),
            listOf(MonarchColumnMappingField(id = "address", label = "Address")),
            threshold = 0.9,
        )
        assertTrue(strict.isEmpty())
    }

    @Test
    fun similarityNormalisesNamesLikeTheWeb() {
        assertEquals("emailaddress", MonarchColumnMappingDefaults.normalize("E-mail Address"))
        assertEquals("cafe", MonarchColumnMappingDefaults.normalize("Café"))
        assertEquals(1.0, MonarchColumnMappingDefaults.similarity("first_name", "First Name"))
        assertEquals(1.0, MonarchColumnMappingDefaults.similarity("phoneNumber", "Phone number"))
        assertEquals(0.0, MonarchColumnMappingDefaults.similarity("", "Name"))
    }

    @Test
    fun issuesFindMissingAndDuplicateFields() {
        val issues = MonarchColumnMappingDefaults.issues(
            mapOf("Mail" to "email", "E-mail" to "email", "Notes" to "other"),
            Fields,
        )
        assertEquals(listOf("name"), issues.missing.map { it.id })
        assertEquals(mapOf("email" to listOf("Mail", "E-mail")), issues.duplicates)
        assertFalse(issues.isEmpty)
        assertTrue(MonarchColumnMappingDefaults.issues(mapOf("a" to "name", "b" to "email"), Fields).isEmpty)
    }

    @Test
    fun picksAreNamedByTheirColumnAndShowSuggestions() {
        rule.setMonarchContent {
            MonarchColumnMapping(sources = Sources.take(2), mapping = Suggested, fields = Fields, onMappingChange = {})
        }
        rule.onNodeWithContentDescription("Column mapping").assertExists()
        rule.onNodeWithContentDescription("Field for E-mail").assertExists()
        rule.onNode(hasText("Email (required)")).assertExists()
        rule.onNodeWithText("Matched").assertExists()
        rule.onNodeWithText("Suggested").assertExists()
        rule.onNodeWithText("Sam Lee, Ana Ruiz", substring = true).assertExists()
        rule.onNodeWithText("2 of 2 columns mapped")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
    }

    @Test
    fun choosingAFieldUpdatesTheMapping() {
        var latest: Map<String, String> = emptyMap()
        rule.setMonarchContent {
            var mapping by remember { mutableStateOf(mapOf("Notes" to "company")) }
            MonarchColumnMapping(
                sources = listOf(MonarchColumnMappingSource("Notes")),
                mapping = mapping,
                fields = Fields,
                onMappingChange = {
                    mapping = it
                    latest = it
                },
                showBulkActions = false,
            )
        }
        rule.onNodeWithContentDescription("Field for Notes").performClick()
        rule.onNode(hasText("Don’t import") and hasClickAction()).performClick()
        assertEquals(emptyMap(), latest)
        rule.onNodeWithContentDescription("Field for Notes").performClick()
        rule.onNode(hasText("Phone number") and hasClickAction()).performClick()
        assertEquals(mapOf("Notes" to "phone"), latest)
        rule.onNodeWithText("International format, e.g. +44 20 7946 0000").assertExists()
    }

    @Test
    fun missingAndDuplicateFieldsAreErrors() {
        rule.setMonarchContent {
            MonarchColumnMapping(
                sources = listOf(MonarchColumnMappingSource("Mail"), MonarchColumnMappingSource("E-mail")),
                mapping = mapOf("Mail" to "email", "E-mail" to "email"),
                fields = Fields,
                onMappingChange = {},
                showBulkActions = false,
            )
        }
        rule.onNodeWithText("Map a column to: Name.")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        // A duplicate is the error of its field, naming the other column: spoken with the field, and announced by
        // the field's live region.
        rule.onNode(hasContentDescription("Field for Mail") and hasError("Email is also chosen for E-mail."))
            .assertExists()
        rule.onNode(hasContentDescription("Email is also chosen for E-mail.") and politeLiveRegion).assertExists()
        rule.onNodeWithContentDescription("Column mapping")
            .performScrollToNode(hasContentDescription("Field for E-mail"))
        rule.onNode(hasContentDescription("Field for E-mail") and hasError("Email is also chosen for Mail."))
            .assertExists()
    }

    private fun hasError(message: String) = SemanticsMatcher.expectValue(SemanticsProperties.Error, message)

    private val politeLiveRegion = SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)

    @Test
    fun hiddenIssuesShowNoErrors() {
        rule.setMonarchContent {
            MonarchColumnMapping(
                sources = listOf(MonarchColumnMappingSource("Mail"), MonarchColumnMappingSource("E-mail")),
                mapping = mapOf("Mail" to "email", "E-mail" to "email"),
                fields = Fields,
                onMappingChange = {},
                showIssues = false,
            )
        }
        rule.onNodeWithText("Map a column to: Name.").assertDoesNotExist()
        rule.onNodeWithText("Email is also chosen for E-mail.").assertDoesNotExist()
        // The error line is hidden from TalkBack, so check the field carries no error either.
        val anyError = SemanticsMatcher.keyIsDefined(SemanticsProperties.Error)
        rule.onNode(hasContentDescription("Field for E-mail") and anyError).assertDoesNotExist()
    }

    @Test
    fun bulkActionsResetAndClearTheMapping() {
        var latest: Map<String, String>? = null
        rule.setMonarchContent {
            MonarchColumnMapping(
                sources = Sources,
                mapping = mapOf("Notes" to "name"),
                fields = Fields,
                onMappingChange = { latest = it },
            )
        }
        rule.onNodeWithText("Use suggestions").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(Suggested, latest)
        rule.onNodeWithText("Don’t import any").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(emptyMap(), latest)
    }

    @Test
    fun bulkActionsHideWhenTheyWouldDoNothing() {
        rule.setMonarchContent {
            MonarchColumnMapping(sources = Sources, mapping = emptyMap(), fields = Fields, onMappingChange = {})
        }
        rule.onNodeWithText("Use suggestions").assertExists()
        rule.onNodeWithText("Don’t import any").assertDoesNotExist()
    }

    @Test
    fun disabledPickersAndButtonsCannotBeUsed() {
        var changes = 0
        rule.setMonarchContent {
            MonarchColumnMapping(
                sources = Sources.take(1),
                mapping = emptyMap(),
                fields = Fields,
                onMappingChange = { changes++ },
                enabled = false,
            )
        }
        rule.onNodeWithText("Use suggestions").assertIsNotEnabled().performClick()
        rule.onNodeWithContentDescription("Field for Full Name").assertIsNotEnabled()
        assertEquals(0, changes)
    }

    @Test
    fun aBlankHeaderIsItsPositionAndStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchColumnMapping(
                sources = listOf(MonarchColumnMappingSource("Name"), MonarchColumnMappingSource("")),
                mapping = mapOf("Name" to "name"),
                fields = Fields,
                onMappingChange = {},
                labels = MonarchColumnMappingLabels(
                    label = "Columnas",
                    fieldFor = { "Campo para $it" },
                    summary = { mapped, total -> "$mapped de $total" },
                    missing = { "Falta: $it" },
                    exactMatch = "Exacto",
                    skipAll = "Ninguna",
                    noSamples = "Sin valores",
                ),
            )
        }
        rule.onNodeWithContentDescription("Columnas").assertExists()
        rule.onNodeWithContentDescription("Campo para #2").assertExists()
        rule.onNodeWithText("1 de 2").assertExists()
        rule.onNodeWithText("Falta: Email").assertExists()
        rule.onNodeWithText("Exacto").assertExists()
        rule.onNodeWithText("Ninguna").assertExists()
        assertTrue(rule.onAllNodesWithText("Sin valores", substring = true).fetchSemanticsNodes().isNotEmpty())
    }

    @Test
    fun longHeadersWrapAtTheLargestFontScale() {
        val header = "Primary contact person at the head office"
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchColumnMapping(
                sources = listOf(MonarchColumnMappingSource(header)),
                mapping = emptyMap(),
                fields = Fields,
                onMappingChange = {},
                showIssues = false,
            )
        }
        rule.onNodeWithText(header, useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun theCountOnlyIncludesKnownFields() {
        val t = MonarchColumnMappingTokens
        assertEquals(1, t.mappedCount(Sources, mapOf("Phone" to "phone", "Notes" to "gone"), Fields))
        assertTrue(t.sameAsSuggested(Sources, Suggested, MonarchColumnMappingDefaults.suggest(Sources, Fields)))
        assertEquals(mapOf("a" to "x"), t.choose(mapOf("a" to "x", "b" to "y"), "b", ""))
        assertEquals(MonarchTone.Success, t.badgeTone(MonarchColumnMappingConfidence.Exact))
        assertEquals(MonarchTone.Info, t.badgeTone(MonarchColumnMappingConfidence.Similar))
    }

    @Test
    fun theColoursComeFromTheirRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val c = MonarchColors(theme.colors)
            val t = MonarchColumnMappingTokens
            assertEquals(c.textDefault, t.headerColor(c), theme.id)
            assertEquals(c.textMuted, t.mutedColor(c), theme.id)
            assertEquals(c.dangerSubtle, t.notice(c).container, theme.id)
            assertEquals(c.dangerStrong, t.notice(c).content, theme.id)
            assertEquals(c.dangerBorder, t.notice(c).border, theme.id)
        }
    }
}
