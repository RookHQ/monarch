package dev.monarchui.sample.sections.intelligence

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.datadisplay.card.MonarchCard
import dev.monarchui.components.intelligence.aiconversation.MonarchAiJSON
import dev.monarchui.components.intelligence.aiconversation.MonarchAiPartState
import dev.monarchui.components.intelligence.aiconversation.MonarchAiUiPart
import dev.monarchui.components.intelligence.aigenerativeui.MonarchAiGenerativeUi
import dev.monarchui.components.intelligence.aigenerativeui.MonarchAiUiRegistry
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage

/** The catalogue page of AiGenerativeUi. */
object MonarchAiGenerativeUiSection : CataloguePage {
    override val title = "AiGenerativeUi"

    private class Slot(val time: String)

    private const val STREAMED = "{\"time\": \"10:00\", \"note\": \"Bring the contract\"}"

    @Composable
    override fun Content() {
        var booked by rememberSaveable { mutableStateOf("") }
        var received by rememberSaveable { mutableStateOf(0) }
        val registry = remember {
            MonarchAiUiRegistry().apply {
                register(
                    name = "BookingSlot",
                    decode = { json -> json["time"]?.stringValue?.let(::Slot) },
                    description = "A meeting slot the person can book.",
                ) { slot, emit ->
                    MonarchCard {
                        MonarchText(text = "Slot at ${slot.time}")
                        if (emit.connected) {
                            MonarchButton(
                                text = "Book",
                                onClick = {
                                    emit("submit", MonarchAiJSON.Object(mapOf("time" to MonarchAiJSON.Text(slot.time))))
                                },
                            )
                        }
                    }
                }
            }
        }
        CatalogueCaption("An allowed component with an action")
        MonarchAiGenerativeUi(
            part = MonarchAiUiPart("BookingSlot", MonarchAiJSON.Object(mapOf("time" to MonarchAiJSON.Text("10:00")))),
            registry = registry,
            onAction = { action -> booked = action.payload?.get("time")?.stringValue.orEmpty() },
        )
        if (booked.isNotEmpty()) MonarchText(text = "Booked: $booked")
        CatalogueCaption("Streaming props")
        val streaming = received < STREAMED.length
        MonarchAiGenerativeUi(
            part = MonarchAiUiPart(
                component = "BookingSlot",
                props = if (streaming) MonarchAiJSON.Text(STREAMED.take(received)) else MonarchAiJSON.parse(STREAMED),
                state = if (streaming) MonarchAiPartState.Streaming else MonarchAiPartState.Done,
            ),
            registry = registry,
        )
        MonarchButton(
            text = if (streaming) "Stream more" else "Start again",
            onClick = { received = if (streaming) received + STEP else 0 },
        )
        CatalogueCaption("A name that is not allowed")
        MonarchAiGenerativeUi(MonarchAiUiPart("WebView", MonarchAiJSON.Object(emptyMap())), registry)
        CatalogueCaption("Props that are not valid")
        MonarchAiGenerativeUi(MonarchAiUiPart("BookingSlot", MonarchAiJSON.Object(emptyMap())), registry)
    }

    private const val STEP = 8
}
