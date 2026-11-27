package com.rookhq.monarch.components.intelligence

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiJSON
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiPartState
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiUiPart
import com.rookhq.monarch.components.intelligence.aigenerativeui.MonarchAiGenerativeUi
import com.rookhq.monarch.components.intelligence.aigenerativeui.MonarchAiGenerativeUiLabels
import com.rookhq.monarch.components.intelligence.aigenerativeui.MonarchAiGenerativeUiTokens
import com.rookhq.monarch.components.intelligence.aigenerativeui.MonarchAiPartialJson
import com.rookhq.monarch.components.intelligence.aigenerativeui.MonarchAiUiAction
import com.rookhq.monarch.components.intelligence.aigenerativeui.MonarchAiUiEmit
import com.rookhq.monarch.components.intelligence.aigenerativeui.MonarchAiUiRegistry
import com.rookhq.monarch.components.intelligence.aigenerativeui.MonarchAiUiRejection
import com.rookhq.monarch.components.intelligence.aigenerativeui.MonarchAiUiSanitizer
import com.rookhq.monarch.components.intelligence.aigenerativeui.MonarchAiUiStreaming
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

private data class Weather(val city: String, val temperature: Double?)

private fun weather(json: MonarchAiJSON): Weather? {
    val city = json["city"]?.stringValue ?: return null
    return Weather(city, (json["temperature"] as? MonarchAiJSON.Number)?.value)
}

private fun text(value: String) = MonarchAiJSON.Text(value)
private fun number(value: Double) = MonarchAiJSON.Number(value)
private fun obj(vararg members: Pair<String, MonarchAiJSON>) = MonarchAiJSON.Object(linkedMapOf(*members))

private fun registry(): MonarchAiUiRegistry = MonarchAiUiRegistry().apply {
    register("Weather", ::weather, description = "Current weather for a city.") { props, _ ->
        MonarchText(text = props.temperature?.let { "${props.city}: ${it.toInt()}°" } ?: props.city)
    }
    register("Booking", { it["slot"]?.stringValue }, streaming = MonarchAiUiStreaming.OnComplete) { slot, emit ->
        if (emit.connected) {
            MonarchButton(text = "Book $slot", onClick = { emit("submit", obj("slot" to text(slot))) })
        } else {
            MonarchText(text = slot)
        }
    }
    register("Positive", { (it["value"] as? MonarchAiJSON.Number)?.value }, validate = { it > 0 }) { value, _ ->
        MonarchText(text = value.toString())
    }
}

private val lisbon = MonarchAiUiPart("Weather", obj("city" to text("Lisbon"), "temperature" to number(21.0)))

internal object MonarchAiGenerativeUiFixtures : ComponentFixtures {
    override val id = "ai-generative-ui"
    override val fixtures = listOf(
        fixture("rendered") { MonarchAiGenerativeUi(lisbon, registry()) },
        fixture("unknown component") { MonarchAiGenerativeUi(MonarchAiUiPart("WebView", obj()), registry()) },
        fixture("invalid props") {
            MonarchAiGenerativeUi(MonarchAiUiPart("Weather", obj("temperature" to number(21.0))), registry())
        },
        fixture("waiting for the complete part") {
            MonarchAiGenerativeUi(
                MonarchAiUiPart("Booking", text("{\"slot\": \"10"), state = MonarchAiPartState.Streaming),
                registry(),
            )
        },
        fixture("action channel") {
            MonarchAiGenerativeUi(MonarchAiUiPart("Booking", obj("slot" to text("10:00"))), registry(), onAction = {})
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchAiGenerativeUiTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun anAllowedComponentRendersFromItsTypedProps() {
        rule.setMonarchContent { MonarchAiGenerativeUi(lisbon, registry()) }
        rule.onNodeWithText("Lisbon: 21°").assertExists()
    }

    @Test
    fun anUnknownNameShowsANoticeAndIsReportedOnce() {
        val rejections = mutableListOf<MonarchAiUiRejection>()
        var tick by mutableStateOf(0)
        rule.setMonarchContent {
            // Recomposing with the same part must not report it again.
            MonarchText(text = "tick $tick")
            MonarchAiGenerativeUi(
                MonarchAiUiPart("WebView", obj()),
                registry(),
                onReject = { reason, _ -> rejections += reason },
            )
        }
        tick++
        rule.waitForIdle()
        rule.onNodeWithText("This part of the answer can’t be shown here.").assertExists()
        assertEquals(listOf(MonarchAiUiRejection.UnknownComponent), rejections)
    }

    @Test
    fun propsThatDoNotDecodeOrFailValidationAreRefused() {
        val registry = registry()
        val missingCity = MonarchAiUiPart("Weather", obj("temperature" to number(21.0)))
        val negative = MonarchAiUiPart("Positive", obj("value" to number(-1.0)))
        val notAnObject = MonarchAiUiPart("Weather", text("{\"city\":\"Lisbon\"}"))
        listOf(missingCity, negative, notAnObject).forEach { part ->
            assertEquals(
                MonarchAiGenerativeUiTokens.Outcome.Rejected(MonarchAiUiRejection.InvalidProps),
                MonarchAiGenerativeUiTokens.outcome(part, registry),
                part.toString(),
            )
        }
        rule.setMonarchContent {
            MonarchAiGenerativeUi(
                missingCity,
                registry,
                labels = MonarchAiGenerativeUiLabels(invalid = "Données invalides"),
            )
        }
        rule.onNodeWithText("Données invalides").assertExists()
    }

    @Test
    fun aStreamingPartRendersAsSoonAsItDecodesAndIsNeverRefusedForItsProps() {
        val registry = registry()
        val early = MonarchAiUiPart("Weather", text("{\"temperature\": 2"), state = MonarchAiPartState.Streaming)
        val enough = MonarchAiUiPart("Weather", text("{\"city\": \"Lis"), state = MonarchAiPartState.Streaming)
        assertEquals(MonarchAiGenerativeUiTokens.Outcome.Pending, MonarchAiGenerativeUiTokens.outcome(early, registry))
        rule.setMonarchContent { MonarchAiGenerativeUi(enough, registry) }
        rule.onNodeWithText("Lis").assertExists()
    }

    @Test
    fun aComponentThatWaitsForTheCompletePartShowsANamedProgressIndicator() {
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent {
            MonarchAiGenerativeUi(
                MonarchAiUiPart("Booking", text("{\"slot\": \"10:00\"}"), state = MonarchAiPartState.Streaming),
                registry(),
            )
        }
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithContentDescription("Loading").assertExists()
        rule.onNodeWithText("10:00").assertDoesNotExist()
    }

    @Test
    fun actionsReachTheHostSanitisedAndNamed() {
        val actions = mutableListOf<MonarchAiUiAction>()
        val part = MonarchAiUiPart("Booking", obj("slot" to text("10:00")), id = "p1", toolCallId = "call-1")
        rule.setMonarchContent { MonarchAiGenerativeUi(part, registry(), onAction = { actions += it }) }
        rule.onNodeWithText("Book 10:00").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(
            listOf(MonarchAiUiAction("submit", obj("slot" to text("10:00")), "Booking", "p1", "call-1")),
            actions,
        )
        val unsafe = MonarchAiGenerativeUiTokens.action("open", obj("href" to text("javascript:alert(1)")), part)
        assertEquals(obj(), unsafe.payload)
    }

    @Test
    fun withoutOnActionTheChannelIsNotConnected() {
        rule.setMonarchContent {
            MonarchAiGenerativeUi(MonarchAiUiPart("Booking", obj("slot" to text("9:00"))), registry())
        }
        rule.onNodeWithText("9:00").assertExists()
        rule.onNodeWithText("Book 9:00").assertDoesNotExist()
        var sent = 0
        MonarchAiUiEmit { _, _ -> sent++ }.invoke("")
        assertEquals(0, sent)
        assertFalse(MonarchAiUiEmit().connected)
    }

    @Test
    fun theRegistryIsAnOrderedAllowListWithAManifest() {
        val registry = registry()
        registry.register("", { it }) { _, _ -> }
        registry.register("Weather", ::weather) { _, _ -> }
        assertEquals(listOf("Weather", "Booking", "Positive"), registry.names)
        assertTrue("Booking" in registry)
        assertFalse("WebView" in registry)
        assertEquals("- Weather\n- Booking\n- Positive", registry.manifest())
        assertEquals(
            "- Weather: Current weather for a city.",
            MonarchAiUiRegistry().apply { register("Weather", ::weather, "Current weather for a city.") { _, _ -> } }
                .manifest(),
        )
    }

    @Test
    fun theSanitiserDropsUnsafeLinksAndLimitsSize() {
        val clean = MonarchAiUiSanitizer.sanitize(
            obj(
                "href" to text("javascript:alert(1)"),
                "link" to text("https://rook.example/a"),
                "note" to text("data:text/html,<b>x</b>"),
                "src" to text("/images/a.png"),
                "label" to text("Time: 10:00"),
                "count" to number(Double.NaN),
                "long" to text("x".repeat(MonarchAiUiSanitizer.MAX_STRING + 5)),
            ),
        )
        assertIs<MonarchAiJSON.Object>(clean)
        assertNull(clean["href"])
        assertNull(clean["note"])
        assertNull(clean["count"])
        assertEquals(text("https://rook.example/a"), clean["link"])
        assertEquals(text("/images/a.png"), clean["src"])
        assertEquals(text("Time: 10:00"), clean["label"])
        assertEquals(MonarchAiUiSanitizer.MAX_STRING, clean["long"]?.stringValue?.length)
        assertNull(MonarchAiUiSanitizer.sanitize(text("not an object")))
        assertEquals(obj(), MonarchAiUiSanitizer.sanitize(MonarchAiJSON.Null))
        var deep: MonarchAiJSON = obj("leaf" to text("x"))
        repeat(MonarchAiUiSanitizer.MAX_DEPTH + 2) { deep = obj("child" to deep) }
        val cleaned = MonarchAiUiSanitizer.sanitize(deep)?.toJsonString().orEmpty()
        assertTrue(cleaned.isNotEmpty() && cleaned.length < deep.toJsonString().length)
        val many = MonarchAiJSON.Array(List(MonarchAiUiSanitizer.MAX_ITEMS + 10) { number(it.toDouble()) })
        val kept = (MonarchAiUiSanitizer.sanitize(obj("items" to many))?.get("items") as MonarchAiJSON.Array).values
        assertEquals(MonarchAiUiSanitizer.MAX_ITEMS, kept.size)
        assertFalse(MonarchAiUiSanitizer.safeUrl(" java\tscript:alert(1)"))
    }

    @Test
    fun partialJsonIsReadAsFarAsItGoes() {
        assertEquals(obj("city" to text("Lis")), MonarchAiPartialJson.parse("{\"city\": \"Lis"))
        assertEquals(obj("city" to MonarchAiJSON.Null), MonarchAiPartialJson.parse("{\"city\":"))
        assertEquals(obj("ci" to MonarchAiJSON.Null), MonarchAiPartialJson.parse("{\"ci"))
        assertEquals(obj("a" to number(1.0)), MonarchAiPartialJson.parse("{\"a\": 1, \"b\": tr"))
        assertEquals(
            obj("items" to MonarchAiJSON.Array(listOf(number(1.0), number(2.0)))),
            MonarchAiPartialJson.parse("{\"items\": [1, 2,"),
        )
        assertEquals(obj("q" to text("say \"hi")), MonarchAiPartialJson.parse("{\"q\": \"say \\\"hi"))
        assertNull(MonarchAiPartialJson.parse("   "))
        assertEquals(obj("done" to MonarchAiJSON.Bool(true)), MonarchAiPartialJson.parse("{\"done\": true}"))
    }

    @Test
    fun theNoticeWrapsAtTheLargestFontScaleAndInRightToLeft() {
        rule.setMonarchContent(fontScale = LargestFontScale, layoutDirection = LayoutDirection.Rtl) {
            MonarchAiGenerativeUi(MonarchAiUiPart("WebView", obj()), registry())
        }
        rule.onNodeWithText("This part of the answer can’t be shown here.", useUnmergedTree = true)
            .assertTextFullyVisible()
    }
}
