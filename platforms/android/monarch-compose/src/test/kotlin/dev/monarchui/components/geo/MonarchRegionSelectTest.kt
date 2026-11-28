package dev.monarchui.components.geo

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import dev.monarchui.R
import dev.monarchui.components.geo.regionselect.MonarchRegionSelect
import dev.monarchui.components.geo.regionselect.MonarchRegionSelectLabels
import dev.monarchui.components.geo.regionselect.MonarchRegionSelectRegion
import dev.monarchui.components.geo.regionselect.RegionSelectData
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import kotlinx.coroutines.CompletableDeferred
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private val provinces = listOf(
    MonarchRegionSelectRegion("PT-11", "Lisboa"),
    MonarchRegionSelectRegion("PT-13", "Porto"),
)

internal object MonarchRegionSelectFixtures : ComponentFixtures {
    override val id = "region-select"
    override val fixtures = listOf(
        fixture("built-in list, chosen") {
            MonarchRegionSelect(country = "US", selected = "US-CA", onSelectedChange = {})
        },
        fixture("short list with codes") {
            MonarchRegionSelect(country = "GB", selected = null, onSelectedChange = {}, showCode = true)
        },
        fixture("app regions") {
            MonarchRegionSelect(country = "PT", selected = "PT-13", onSelectedChange = {}, regions = provinces)
        },
        fixture("text entry") {
            MonarchRegionSelect(country = "PT", selected = "Lisboa", onSelectedChange = {})
        },
        fixture("no country") {
            MonarchRegionSelect(country = null, selected = null, onSelectedChange = {})
        },
        fixture("load failed") {
            MonarchRegionSelect(
                country = "PT",
                selected = null,
                onSelectedChange = {},
                loadRegions = { error("offline") },
            )
        },
        fixture("error") {
            MonarchRegionSelect(country = "CA", selected = null, onSelectedChange = {}, errorText = "Choose one.")
        },
        fixture("disabled") {
            MonarchRegionSelect(country = "AU", selected = "AU-NSW", onSelectedChange = {}, enabled = false)
        },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchRegionSelectTest {
    @get:Rule
    val rule = createComposeRule()

    private fun row(text: String) = hasText(text) and SemanticsMatcher.keyIsDefined(SemanticsProperties.Selected)

    @Test
    fun theBuiltInListsUseIsoCodes() {
        val us = RegionSelectData.builtIn("us")
        assertEquals(51, us?.size)
        assertEquals("US-CA", us?.first { it.name == "California" }?.code)
        assertEquals(13, RegionSelectData.builtIn("CA")?.size)
        assertEquals(8, RegionSelectData.builtIn("AU")?.size)
        assertEquals(listOf("GB-ENG", "GB-NIR", "GB-SCT", "GB-WLS"), RegionSelectData.builtIn("GB")?.map { it.code })
        assertEquals(36, RegionSelectData.builtIn("IN")?.size)
        assertNull(RegionSelectData.builtIn("PT"))
    }

    @Test
    fun eachCountryHasItsOwnWordForItsRegions() {
        assertEquals(R.string.monarch_regionselect_label_us, RegionSelectData.labelRes("us"))
        assertEquals(R.string.monarch_regionselect_label_gb, RegionSelectData.labelRes("GB"))
        assertEquals(R.string.monarch_regionselect_label, RegionSelectData.labelRes("PT"))
        assertEquals(R.string.monarch_regionselect_label, RegionSelectData.labelRes(null))
        val option = RegionSelectData.option(MonarchRegionSelectRegion("US-CA", "California"), showCode = true)
        assertEquals("CA", option.hint)
        assertEquals("US-CA", option.keywords)
        assertNull(RegionSelectData.option(MonarchRegionSelectRegion("US-CA", "California"), false).hint)
    }

    @Test
    fun aBuiltInListIsSearchedAndChoosesTheIsoCode() {
        var chosen: String? = null
        rule.setMonarchContent {
            var region by remember { mutableStateOf<String?>(null) }
            MonarchRegionSelect(
                country = "us",
                selected = region,
                onSelectedChange = {
                    region = it
                    chosen = it
                },
            )
        }
        rule.onNodeWithContentDescription("State").assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.waitForIdle()
        rule.onNode(hasSetTextAction()).performTextInput("calif")
        rule.waitForIdle()
        rule.onNode(row("California")).performClick()
        rule.waitForIdle()
        assertEquals("US-CA", chosen)
        rule.onNodeWithText("California").assertExists()
    }

    @Test
    fun withoutACountryTheFieldIsDisabledAndSaysWhy() {
        rule.setMonarchContent {
            MonarchRegionSelect(country = null, selected = null, onSelectedChange = {})
        }
        rule.onNodeWithContentDescription("Region").assertIsNotEnabled()
        rule.onNodeWithText("Choose a country first", useUnmergedTree = true).assertExists()
    }

    @Test
    fun aCountryWithoutAListGetsAnAddressRegionTextField() {
        var typed: String? = "unset"
        rule.setMonarchContent {
            MonarchRegionSelect(country = "PT", selected = null, onSelectedChange = { typed = it })
        }
        rule.onNode(hasSetTextAction())
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ContentType, ContentType.AddressRegion))
            .performTextInput("Lisboa")
        assertEquals("Lisboa", typed)
    }

    @Test
    fun theLoaderRunsOncePerCountryAndCanBeRetried() {
        var calls = 0
        var fail = true
        val answer = CompletableDeferred<List<MonarchRegionSelectRegion>>()
        rule.setMonarchContent {
            MonarchRegionSelect(
                country = "PT",
                selected = null,
                onSelectedChange = {},
                loadRegions = {
                    calls++
                    if (fail) error("offline") else answer.await()
                },
            )
        }
        rule.waitForIdle()
        assertEquals(1, calls)
        rule.onNodeWithText("Couldn’t load the regions.", substring = true).assertExists()
        fail = false
        rule.onNode(hasText("Try again", substring = true) and hasClickAction()).performClick()
        rule.waitForIdle()
        assertEquals(2, calls)
        rule.onNodeWithText("Couldn’t load the regions.", substring = true).assertDoesNotExist()
        answer.complete(provinces)
        rule.waitForIdle()
        rule.onNode(hasSetTextAction()).performClick()
        rule.waitForIdle()
        rule.onNode(row("Porto")).assertExists()
        assertEquals(2, calls)
    }

    @Test
    fun theBuiltInStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchRegionSelect(
                country = null,
                selected = null,
                onSelectedChange = {},
                labels = MonarchRegionSelectLabels(label = "Province", chooseCountryFirst = "Pick a country"),
            )
        }
        rule.onNodeWithContentDescription("Province").assertExists()
        rule.onNodeWithText("Pick a country", useUnmergedTree = true).assertExists()
    }

    @Test
    fun theAppsRegionsWinAndTextFitsAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchRegionSelect(country = "PT", selected = "PT-13", onSelectedChange = {}, regions = provinces)
        }
        rule.onNodeWithText("Porto").assertExists()
        rule.onNodeWithText("Region", useUnmergedTree = true).assertTextFullyVisible()
        rule.onNode(hasText("Region") and isHeading()).assertDoesNotExist()
    }
}
