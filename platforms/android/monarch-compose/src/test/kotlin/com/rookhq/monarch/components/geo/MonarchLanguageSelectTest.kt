package com.rookhq.monarch.components.geo

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.rookhq.monarch.components.geo.countryselect.CountrySelectData
import com.rookhq.monarch.components.geo.languageselect.LanguageSelectData
import com.rookhq.monarch.components.geo.languageselect.MonarchLanguageSelect
import com.rookhq.monarch.components.geo.languageselect.MonarchLanguageSelectLabelStyle
import com.rookhq.monarch.components.geo.languageselect.MonarchLanguageSelectLabels
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal object MonarchLanguageSelectFixtures : ComponentFixtures {
    override val id = "language-select"
    override val fixtures = listOf(
        fixture("endonym") {
            MonarchLanguageSelect(selected = "de", onSelectedChange = {}, locale = Locale.US)
        },
        fixture("localized with tag") {
            MonarchLanguageSelect(
                selected = "ja",
                onSelectedChange = {},
                locale = Locale.US,
                labelStyle = MonarchLanguageSelectLabelStyle.Localized,
                showTag = true,
            )
        },
        fixture("empty, short list") {
            MonarchLanguageSelect(
                selected = null,
                onSelectedChange = {},
                locale = Locale.US,
                languages = listOf("en", "pt-BR", "ar"),
                helperText = "The language of your emails.",
            )
        },
        fixture("error") {
            MonarchLanguageSelect(selected = null, onSelectedChange = {}, locale = Locale.US, errorText = "Choose one.")
        },
        fixture("disabled") {
            MonarchLanguageSelect(selected = "fr", onSelectedChange = {}, locale = Locale.US, enabled = false)
        },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchLanguageSelectTest {
    @get:Rule
    val rule = createComposeRule()

    private fun row(text: String) = hasText(text) and SemanticsMatcher.keyIsDefined(SemanticsProperties.Selected)

    @Test
    fun tagsAreCanonicalAndInvalidTagsAreDropped() {
        assertEquals("en-US", LanguageSelectData.canonical("EN-us"))
        assertEquals("zh-Hans", LanguageSelectData.canonical("zh-hans"))
        assertNull(LanguageSelectData.canonical(""))
        assertNull(LanguageSelectData.canonical("12"))
        val tags = LanguageSelectData.TAGS
        assertEquals(tags.size, tags.mapNotNull(LanguageSelectData::canonical).size)
    }

    @Test
    fun namesAreCapitalisedInTheirLocale() {
        assertEquals("German", LanguageSelectData.name("de", Locale.US))
        assertEquals("Deutsch", LanguageSelectData.endonym("de"))
        assertEquals("Français", LanguageSelectData.endonym("fr"))
        assertEquals("qqq", LanguageSelectData.name("qqq", Locale.US).lowercase(Locale.ROOT))
    }

    @Test
    fun optionsShowBothNamesAndFindTheEnglishName() {
        val de = LanguageSelectData.option("de", Locale.US, MonarchLanguageSelectLabelStyle.Endonym, showTag = false)
        assertEquals("Deutsch", de.label)
        assertEquals("German", de.description)
        assertNull(de.hint)
        val localized = LanguageSelectData.option("de", Locale.US, MonarchLanguageSelectLabelStyle.Localized, true)
        assertEquals("German", localized.label)
        assertEquals("Deutsch", localized.description)
        assertEquals("de", localized.hint)
        val en = LanguageSelectData.option("en", Locale.US, MonarchLanguageSelectLabelStyle.Endonym, false)
        assertNull(en.description)
        val inFrench = LanguageSelectData.option("de", Locale.FRANCE, MonarchLanguageSelectLabelStyle.Endonym, false)
        assertTrue(CountrySelectData.matches(inFrench, "german"))
        assertTrue(CountrySelectData.matches(inFrench, "allem"))
    }

    @Test
    fun optionsAreDistinctSortedAndKeepAnUnlistedSelection() {
        val options = LanguageSelectData.options(
            listOf("fr", "FR", "de", "bad tag", "en"),
            "it",
            Locale.US,
            MonarchLanguageSelectLabelStyle.Endonym,
            showTag = false,
        )
        assertEquals(listOf("en", "fr", "de", "it"), options.map { it.id })
    }

    @Test
    fun searchingByTheInterfaceNameChoosesTheCanonicalTag() {
        var chosen: String? = null
        rule.setMonarchContent {
            var tag by remember { mutableStateOf<String?>(null) }
            MonarchLanguageSelect(
                selected = tag,
                onSelectedChange = {
                    tag = it
                    chosen = it
                },
                locale = Locale.US,
                priority = listOf("EN-gb"),
            )
        }
        rule.onNodeWithContentDescription("Language").assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.waitForIdle()
        rule.onNode(hasText("Suggested") and isHeading()).assertExists()
        rule.onNode(hasSetTextAction()).performTextInput("japan")
        rule.waitForIdle()
        rule.onNode(row("日本語")).performClick()
        rule.waitForIdle()
        assertEquals("ja", chosen)
    }

    @Test
    fun theBuiltInStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchLanguageSelect(
                selected = null,
                onSelectedChange = {},
                locale = Locale.US,
                labels = MonarchLanguageSelectLabels(label = "Langue", empty = "Aucune"),
            )
        }
        rule.onNodeWithContentDescription("Langue").performClick()
        rule.waitForIdle()
        rule.onNode(hasSetTextAction()).performTextInput("qqqq")
        rule.waitForIdle()
        rule.onNodeWithText("Aucune").assertExists()
    }

    @Test
    fun disabledIsNotEnabledAndTextFitsAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchLanguageSelect(selected = "pt-BR", onSelectedChange = {}, locale = Locale.US, enabled = false)
        }
        rule.onNode(hasText("Português", substring = true)).assertIsNotEnabled()
        rule.onNodeWithText("Language", useUnmergedTree = true).assertTextFullyVisible()
    }
}
