package dev.monarchui.components.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.components.interaction.reorderablelist.MonarchReorderableListLabels
import dev.monarchui.components.navigation.favoriteslist.MonarchFavoriteItem
import dev.monarchui.components.navigation.favoriteslist.MonarchFavoritesList
import dev.monarchui.components.navigation.favoriteslist.MonarchFavoritesListLabels
import dev.monarchui.components.navigation.favoriteslist.MonarchFavoritesListTokens
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.MonarchTestEnvironment
import dev.monarchui.testing.TestIcon
import dev.monarchui.testing.assertIsBefore
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private val Items = listOf(
    MonarchFavoriteItem("Roadmap", id = "roadmap", current = true),
    MonarchFavoriteItem("Q3 planning", id = "q3"),
    MonarchFavoriteItem("Design reviews", id = "design"),
)

internal object MonarchFavoritesListFixtures : ComponentFixtures {
    override val id = "favorites-list"
    override val fixtures = listOf(
        fixture("browsing, read only") { MonarchFavoritesList(
            items = Items,
            onSelect = {},
            removeIcon = null,
            headingIcon = null,
        ) },
        fixture("browsing, editable, with icons") {
            MonarchFavoritesList(
                items = Items.map {
                    MonarchFavoriteItem(it.title, it.id, icon = { TestGlyph() }, current = it.current)
                },
                onSelect = {},
                onRemove = {},
                onReorder = {},
                headingIcon = { TestGlyph() },
                removeIcon = null,
            )
        },
        fixture("editing, reorder and remove") {
            MonarchFavoritesList(
                items = Items,
                onSelect = {},
                editing = true,
                onRemove = {},
                onReorder = {},
                removeIcon = null,
                headingIcon = null,
            )
        },
        fixture("editing, remove only, icon") {
            MonarchFavoritesList(
                items = Items,
                onSelect = {},
                editing = true,
                onRemove = {},
                removeIcon = { TestGlyph() },
                headingIcon = null,
            )
        },
        fixture("empty with a message") {
            MonarchFavoritesList(
                items = emptyList(),
                onSelect = {},
                onRemove = {},
                emptyMessage = "Pin pages to find them here.",
                removeIcon = null,
                headingIcon = null,
            )
        },
        fixture("long names") {
            MonarchFavoritesList(
                items = listOf(
                    MonarchFavoriteItem("Customer onboarding research for the enterprise plan", id = "a"),
                    MonarchFavoriteItem("Weekly metrics", id = "b", current = true),
                ),
                onSelect = {},
                onRemove = {},
                removeIcon = null,
                headingIcon = null,
            )
        },
    )
}

@androidx.compose.runtime.Composable
private fun TestGlyph() {
    MonarchIcon(imageVector = TestIcon, contentDescription = null)
}

@RunWith(RobolectricTestRunner::class)
class MonarchFavoritesListTest {
    @get:Rule
    val rule = createComposeRule()

    private fun announcement(): String? = rule.onAllNodes(
        androidx.compose.ui.test.SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion),
    ).fetchSemanticsNodes().firstNotNullOfOrNull {
        it.config.getOrNull(SemanticsProperties.ContentDescription)?.firstOrNull()
    }

    @Test
    fun rowsOpenTheirDestinationAndTheCurrentOneIsSelected() {
        val opened = mutableListOf<String>()
        rule.setMonarchContent { MonarchFavoritesList(
            items = Items,
            onSelect = { opened += it.id },
            removeIcon = null,
            headingIcon = null,
        ) }
        rule.onNodeWithText("Roadmap").assertIsSelected()
        rule.onNodeWithText("Q3 planning").assertIsNotSelected().performClick()
        assertEquals(listOf("q3"), opened)
        rule.onNode(isHeading()).assertExists()
        rule.onNodeWithText("Favorites", useUnmergedTree = true).assertExists()
        // Read only: no Edit button and no remove action.
        rule.onNodeWithContentDescription("Edit favorites").assertDoesNotExist()
        val actions = rule.onNodeWithText("Q3 planning").fetchSemanticsNode().config
            .getOrNull(SemanticsActions.CustomActions)
        assertTrue(actions.isNullOrEmpty())
    }

    @Test
    fun editModeShowsRemoveButtonsStopsOpeningAndRemovingAnnounces() {
        val items = mutableStateListOf(*Items.toTypedArray())
        val opened = mutableListOf<String>()
        rule.setMonarchContent {
            MonarchFavoritesList(
                items = items,
                onSelect = { opened += it.id },
                onRemove = { items.remove(it) },
                removeIcon = null,
                headingIcon = null,
            )
        }
        rule.onNodeWithContentDescription("Edit favorites").performClick()
        rule.onNodeWithContentDescription("Done").assertExists()
        rule.onNodeWithText("Q3 planning").performClick()
        assertEquals(emptyList(), opened)
        rule.onNodeWithContentDescription("Remove Q3 planning from favorites").performClick()
        assertEquals(listOf("roadmap", "design"), items.map { it.id })
        assertEquals("Q3 planning removed from favorites.", announcement())
        rule.onNodeWithContentDescription("Done").performClick()
        rule.onNodeWithContentDescription("Edit favorites").assertExists()
    }

    @Test
    fun outsideEditModeRemoveIsATalkBackAction() {
        val removed = mutableListOf<String>()
        rule.setMonarchContent { MonarchFavoritesList(
            items = Items,
            onSelect = {},
            onRemove = { removed += it.id },
            removeIcon = null,
            headingIcon = null,
        ) }
        val action = rule.onNodeWithText("Design reviews").fetchSemanticsNode()
            .config[SemanticsActions.CustomActions].single()
        assertEquals("Remove Design reviews from favorites", action.label)
        action.action()
        assertEquals(listOf("design"), removed)
    }

    @Test
    fun editModeWithReorderMovesRowsThroughTheHandle() {
        val orders = mutableListOf<List<String>>()
        rule.setMonarchContent {
            MonarchFavoritesList(
                items = Items,
                onSelect = {},
                editing = true,
                onReorder = { orders += it.map { i -> i.id } },
                removeIcon = null,
                headingIcon = null,
            )
        }
        rule.onNodeWithContentDescription("Reorder Roadmap").fetchSemanticsNode()
            .config[SemanticsActions.CustomActions].first { it.label == "Move down" }.action()
        rule.waitForIdle()
        assertEquals(listOf(listOf("q3", "roadmap", "design")), orders)
        // Without onRemove there are no remove buttons.
        rule.onNodeWithContentDescription("Remove Roadmap from favorites").assertDoesNotExist()
    }

    @Test
    fun removingTheLastItemEndsAHoistedEditMode() {
        var editing by mutableStateOf(true)
        val items = mutableStateListOf(Items.first())
        val changes = mutableListOf<Boolean>()
        rule.setMonarchContent {
            MonarchFavoritesList(
                items = items,
                onSelect = {},
                editing = editing,
                onEditingChange = {
                    changes += it
                    editing = it
                },
                onRemove = { items.remove(it) },
                emptyMessage = "Nothing pinned.",
                removeIcon = null,
                headingIcon = null,
            )
        }
        rule.onNodeWithContentDescription("Remove Roadmap from favorites").performClick()
        rule.waitForIdle()
        assertEquals(listOf(false), changes)
        rule.onNodeWithText("Nothing pinned.").assertExists()
        rule.onNodeWithContentDescription("Done").assertDoesNotExist()
    }

    @Test
    fun anEmptyListWithoutAMessageDrawsNothing() {
        rule.setMonarchContent { MonarchFavoritesList(
            items = emptyList(),
            onSelect = {},
            onRemove = {},
            removeIcon = null,
            headingIcon = null,
        ) }
        rule.onNodeWithText("Favorites", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun targetsAre48dpAndTheEditButtonIsAtTheEnd() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) {
            MonarchFavoritesList(
                items = Items,
                onSelect = {},
                onRemove = {},
                editing = environment.fontScale > 1f,
                removeIcon = null,
                headingIcon = null,
            )
        }
        LayoutDirection.entries.forEach { direction ->
            environment.layoutDirection = direction
            rule.waitForIdle()
            rule.onNodeWithText("Roadmap").assertTouchTargetAtLeast48dp(rule.density)
            rule.onNodeWithContentDescription("Edit favorites").assertTouchTargetAtLeast48dp(rule.density)
            rule.onNodeWithText("Favorites", useUnmergedTree = true)
                .assertIsBefore(rule.onNodeWithContentDescription("Edit favorites"), direction)
        }
        environment.layoutDirection = LayoutDirection.Ltr
        environment.fontScale = LargestFontScale
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Remove Roadmap from favorites").assertTouchTargetAtLeast48dp(rule.density)
        rule.onNodeWithText("Roadmap", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun theBuiltInStringsCanBeReplaced() {
        val items = mutableStateListOf(*Items.toTypedArray())
        rule.setMonarchContent {
            MonarchFavoritesList(
                items = items,
                onSelect = {},
                editing = true,
                onRemove = { items.remove(it) },
                onReorder = {},
                labels = MonarchFavoritesListLabels(
                    title = "Favoris",
                    done = "Terminé",
                    remove = "Retirer",
                    removeLabel = { "Retirer $it des favoris" },
                    removed = { "$it retiré." },
                    reorder = MonarchReorderableListLabels(handle = { "Déplacer $it" }),
                ),
                removeIcon = null,
                headingIcon = null,
            )
        }
        rule.onNodeWithText("Favoris", useUnmergedTree = true).assertExists()
        rule.onNodeWithContentDescription("Terminé").assertExists()
        rule.onNodeWithContentDescription("Déplacer Roadmap").assertExists()
        assertEquals(3, rule.onAllNodesWithText("Retirer", useUnmergedTree = true).fetchSemanticsNodes().size)
        rule.onNodeWithContentDescription("Retirer Roadmap des favoris").performClick()
        assertEquals("Roadmap retiré.", announcement())
    }

    @Test
    fun theRowColoursAndTheRulesComeFromTheTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val current = MonarchFavoritesListTokens.rowColors(current = true, colors = colors)
            assertEquals(colors.accentSubtle, current.container)
            assertEquals(colors.textDefault, current.content)
            val resting = MonarchFavoritesListTokens.rowColors(current = false, colors = colors)
            assertEquals(colors.textMuted, resting.content)
            assertEquals(colors.textMuted, MonarchFavoritesListTokens.mutedColor(colors))
        }
        assertEquals(FontWeight.Medium, MonarchFavoritesListTokens.titleWeight(true))
        assertEquals(FontWeight.Normal, MonarchFavoritesListTokens.titleWeight(false))
        assertTrue(MonarchFavoritesListTokens.editable(canRemove = false, canReorder = true))
        assertTrue(!MonarchFavoritesListTokens.editable(canRemove = false, canReorder = false))
        assertTrue(MonarchFavoritesListTokens.inEdit(editable = true, requested = true, count = 1))
        assertTrue(!MonarchFavoritesListTokens.inEdit(editable = true, requested = true, count = 0))
        assertTrue(!MonarchFavoritesListTokens.inEdit(editable = false, requested = true, count = 2))
        assertEquals(listOf("b", "c", "a"), MonarchFavoritesListTokens.moved(listOf("a", "b", "c"), 0, 2))
        assertEquals(listOf("a", "b"), MonarchFavoritesListTokens.moved(listOf("a", "b"), 0, 5))
    }
}
