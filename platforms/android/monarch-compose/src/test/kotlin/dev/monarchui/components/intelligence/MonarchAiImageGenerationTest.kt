package dev.monarchui.components.intelligence

import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.monarchui.components.intelligence.aiimagegeneration.MonarchAiGeneratedImage
import dev.monarchui.components.intelligence.aiimagegeneration.MonarchAiImageGeneration
import dev.monarchui.components.intelligence.aiimagegeneration.MonarchAiImageGenerationLabels
import dev.monarchui.components.intelligence.aiimagegeneration.MonarchAiImageGenerationStatus
import dev.monarchui.components.intelligence.aiimagegeneration.MonarchAiImageGenerationTokens as Tokens
import dev.monarchui.components.intelligence.aiimagegeneration.MonarchAiImageGenerationTokens.Slot
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.MonarchTestEnvironment
import dev.monarchui.testing.assertIsBefore
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val picture = ColorPainter(Color.Transparent)

internal object MonarchAiImageGenerationFixtures : ComponentFixtures {
    override val id = "ai-image-generation"
    private val narrow = Modifier.width(150.dp)
    override val fixtures = listOf(
        fixture("pending with progress") {
            MonarchAiImageGeneration(
                images = listOf(MonarchAiGeneratedImage("a")),
                status = MonarchAiImageGenerationStatus.Pending,
                onAltChange = { _, _ -> },
                modifier = narrow,
                progress = 0.4f,
            )
        },
        fixture("progressive") {
            MonarchAiImageGeneration(
                images = listOf(MonarchAiGeneratedImage("a", picture, "A lighthouse")),
                status = MonarchAiImageGenerationStatus.Progressive,
                onAltChange = { _, _ -> },
                modifier = narrow,
            )
        },
        fixture("done, chosen") {
            MonarchAiImageGeneration(
                images = listOf(MonarchAiGeneratedImage("a", picture, "A lighthouse")),
                status = MonarchAiImageGenerationStatus.Done,
                onAltChange = { _, _ -> },
                modifier = narrow,
                selection = "a",
                onSelect = {},
            )
        },
        fixture("done, variations") {
            MonarchAiImageGeneration(
                images = listOf(MonarchAiGeneratedImage("a", picture, "A lighthouse")),
                status = MonarchAiImageGenerationStatus.Done,
                onAltChange = { _, _ -> },
                modifier = narrow,
                onVariations = {},
            )
        },
        fixture("done, alt missing, image unavailable") {
            MonarchAiImageGeneration(
                images = listOf(MonarchAiGeneratedImage("a")),
                status = MonarchAiImageGenerationStatus.Done,
                onAltChange = { _, _ -> },
                modifier = narrow,
            )
        },
        fixture("error with retry") {
            MonarchAiImageGeneration(
                images = emptyList(),
                status = MonarchAiImageGenerationStatus.Error,
                onAltChange = { _, _ -> },
                onRetry = {},
            )
        },
        fixture("error without retry") {
            MonarchAiImageGeneration(
                images = emptyList(),
                status = MonarchAiImageGenerationStatus.Error,
                onAltChange = { _, _ -> },
                error = "The image service is busy.",
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchAiImageGenerationTest {
    @get:Rule
    val rule = createComposeRule()

    private val done = listOf(
        MonarchAiGeneratedImage("a", picture, "A lighthouse at dusk"),
        MonarchAiGeneratedImage("b", picture),
    )

    @Test
    fun eachImageIsReadByItsAltTextOrItsPosition() {
        rule.setMonarchContent {
            MonarchAiImageGeneration(done, MonarchAiImageGenerationStatus.Done, { _, _ -> })
        }
        rule.onNodeWithContentDescription("A lighthouse at dusk")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Image))
        rule.onNodeWithContentDescription("Generated image 2").assertExists()
        rule.onNodeWithText("2 images generated").assertExists()
        rule.onNode(hasContentDescription("AI generated images")).assertExists()
    }

    @Test
    fun anEmptyAltFieldShowsItsErrorOnceImagesCanBeSeen() {
        rule.setMonarchContent {
            MonarchAiImageGeneration(done, MonarchAiImageGenerationStatus.Done, { _, _ -> })
        }
        // Image 2 has no alt text: the error is on its field (its TalkBack element) and in the field's polite live
        // region; image 1, which has alt text, shows none.
        val missing = "Describe the image for people who cannot see it."
        rule.onNode(hasContentDescription("Alt text for image 1", substring = true))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Error))
        rule.onNode(
            hasContentDescription("Alt text for image 2", substring = true) and
                SemanticsMatcher.expectValue(SemanticsProperties.Error, missing),
        ).assertExists()
        rule.onNodeWithContentDescription(missing)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        assertTrue(Tokens.altInvalid("  ", MonarchAiImageGenerationStatus.Done))
        assertFalse(Tokens.altInvalid("", MonarchAiImageGenerationStatus.Pending))
        assertFalse(Tokens.altInvalid("A cat", MonarchAiImageGenerationStatus.Progressive))
    }

    @Test
    fun typingAltTextHandsItToTheApp() {
        val edits = mutableListOf<Pair<String, String>>()
        rule.setMonarchContent {
            MonarchAiImageGeneration(done.drop(1), MonarchAiImageGenerationStatus.Done, { id, alt ->
                edits += id to alt
            })
        }
        rule.onNodeWithContentDescription("Alt text for image 1", substring = true).performTextInput("A boat")
        assertEquals("b" to "A boat", edits.last())
    }

    @Test
    fun choosingAnImageAndAskingForVariationsCallBackWithItsId() {
        var chosen: String? = null
        var varied: String? = null
        rule.setMonarchContent {
            var selection by remember { mutableStateOf<String?>(null) }
            MonarchAiImageGeneration(
                images = done.take(1),
                status = MonarchAiImageGenerationStatus.Done,
                onAltChange = { _, _ -> },
                modifier = Modifier.width(200.dp),
                selection = selection,
                onSelect = {
                    chosen = it
                    selection = it
                },
                onVariations = { varied = it },
            )
        }
        rule.onNodeWithContentDescription("Use image 1").assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.onNodeWithContentDescription("Use image 1").assertIsSelected()
        rule.onNodeWithContentDescription("More like this, image 1").assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        assertEquals("a", chosen)
        assertEquals("a", varied)
    }

    @Test
    fun whilePendingThereArePlaceholdersAndNoActionsAndTheFieldIsDisabled() {
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent {
            MonarchAiImageGeneration(
                images = listOf(MonarchAiGeneratedImage("a", picture)),
                status = MonarchAiImageGenerationStatus.Pending,
                onAltChange = { _, _ -> },
                progress = 0.4f,
                onSelect = {},
            )
        }
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText("Generating images… 40%").assertExists()
        rule.onNodeWithText("Use image").assertDoesNotExist()
        rule.onNodeWithContentDescription("Alt text for image 1", substring = true).assertIsNotEnabled()
        rule.onNodeWithContentDescription("Generated image 1").assertDoesNotExist()
    }

    @Test
    fun anErrorOffersARetry() {
        var retried = 0
        rule.setMonarchContent {
            MonarchAiImageGeneration(
                images = emptyList(),
                status = MonarchAiImageGenerationStatus.Error,
                onAltChange = { _, _ -> },
                onRetry = { retried++ },
            )
        }
        rule.onNodeWithText("The images could not be generated.").assertExists()
        rule.onNodeWithText("Try again").performClick()
        assertEquals(1, retried)
    }

    @Test
    fun completionIsAnnouncedOnceThroughAPoliteLiveRegion() {
        var status by mutableStateOf(MonarchAiImageGenerationStatus.Pending)
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent {
            MonarchAiImageGeneration(done, status, { _, _ -> })
        }
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithContentDescription("2 images generated.").assertDoesNotExist()
        status = MonarchAiImageGenerationStatus.Done
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithContentDescription("2 images generated.")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        val finished = MonarchAiImageGenerationStatus.Done
        assertTrue(Tokens.announcesCompletion(MonarchAiImageGenerationStatus.Progressive, finished))
        assertFalse(Tokens.announcesCompletion(finished, finished))
        assertFalse(Tokens.announcesCompletion(null, finished))
    }

    @Test
    fun builtInStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchAiImageGeneration(
                images = done.drop(1),
                status = MonarchAiImageGenerationStatus.Done,
                onAltChange = { _, _ -> },
                modifier = Modifier.width(200.dp),
                onSelect = {},
                labels = MonarchAiImageGenerationLabels(
                    done = { "$it image(s) prête(s)" },
                    imageName = { "Image $it" },
                    select = "Choisir",
                    selectName = { "Choisir l'image $it" },
                ),
            )
        }
        rule.onNodeWithText("1 image(s) prête(s)").assertExists()
        rule.onNodeWithContentDescription("Image 1").assertExists()
        rule.onNodeWithContentDescription("Choisir l'image 1").assertExists()
    }

    @Test
    fun slotsAndActionsFollowTheStatus() {
        val loaded = MonarchAiGeneratedImage("a", picture)
        val missing = MonarchAiGeneratedImage("b")
        assertEquals(Slot.Placeholder, Tokens.slot(loaded, MonarchAiImageGenerationStatus.Pending))
        assertEquals(Slot.Refining, Tokens.slot(loaded, MonarchAiImageGenerationStatus.Progressive))
        assertEquals(Slot.Image, Tokens.slot(loaded, MonarchAiImageGenerationStatus.Done))
        assertEquals(Slot.Placeholder, Tokens.slot(missing, MonarchAiImageGenerationStatus.Progressive))
        assertEquals(Slot.Unavailable, Tokens.slot(missing, MonarchAiImageGenerationStatus.Done))
        val finished = MonarchAiImageGenerationStatus.Done
        assertTrue(Tokens.showsActions(loaded, finished, canSelect = true, canVary = false))
        assertFalse(Tokens.showsActions(loaded, finished, canSelect = false, canVary = false))
        assertFalse(Tokens.showsActions(missing, finished, canSelect = true, canVary = true))
        assertNull(Tokens.clamped(Float.NaN))
        assertEquals(1f, Tokens.clamped(2f))
        assertEquals("40%", Tokens.percent(0.4f, Locale.US))
        assertEquals(1, Tokens.columns(320.dp, 1f))
        assertEquals(2, Tokens.columns(400.dp, 1f))
        assertEquals(1, Tokens.columns(400.dp, LargestFontScale))
    }

    @Test
    fun theUnavailableSlotIsNamed() {
        rule.setMonarchContent {
            MonarchAiImageGeneration(
                listOf(MonarchAiGeneratedImage("a")),
                MonarchAiImageGenerationStatus.Done,
                { _, _ -> },
            )
        }
        rule.onNodeWithContentDescription("Preview unavailable").assertExists()
    }

    @Test
    fun actionsFollowTheLayoutDirection() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchAiImageGeneration(
                done.take(1),
                MonarchAiImageGenerationStatus.Done,
                { _, _ -> },
                // The label above the alt field makes the item taller than the 470dp test window; in a scroll,
                // as on a screen, the action row is placed (a FlowRow does not place what overflows its height).
                Modifier.width(280.dp).verticalScroll(rememberScrollState()),
                onSelect = {},
                onVariations = {},
            )
        }
        rule.onNodeWithContentDescription("Use image 1")
            .assertIsBefore(rule.onNodeWithContentDescription("More like this, image 1"), LayoutDirection.Rtl)
    }

    @Test
    fun theStatusLineIsFullyVisibleAtTheLargestFontScale() {
        rule.setMonarchContent(MonarchTestEnvironment(fontScale = LargestFontScale)) {
            MonarchAiImageGeneration(emptyList(), MonarchAiImageGenerationStatus.Done, { _, _ -> })
        }
        rule.onNodeWithText("0 images generated", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun theChosenImageHasTheSolidAssistantRingInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.aiSolid, Tokens.ringColor(true, colors), theme.id)
            assertEquals(colors.borderSubtle, Tokens.ringColor(false, colors), theme.id)
        }
        assertTrue(Tokens.ringWidth(true, 1.dp) > Tokens.ringWidth(false, 1.dp))
    }
}
