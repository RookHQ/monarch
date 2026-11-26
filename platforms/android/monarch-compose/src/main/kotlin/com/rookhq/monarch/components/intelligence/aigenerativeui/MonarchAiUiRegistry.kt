package com.rookhq.monarch.components.intelligence.aigenerativeui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiJSON

/**
 * The allow-list of components that `MonarchAiGenerativeUi` may render, by
 * the name the model uses.
 *
 * ```kotlin
 * data class Weather(val city: String, val temperature: Double?)
 *
 * fun weather(json: MonarchAiJSON): Weather? {
 *     val city = json["city"]?.stringValue ?: return null
 *     return Weather(city, (json["temperature"] as? MonarchAiJSON.Number)?.value)
 * }
 *
 * val registry = remember {
 *     MonarchAiUiRegistry().apply {
 *         register("Weather", ::weather, description = "Current weather for a city.") { props, _ ->
 *             WeatherCard(props)
 *         }
 *     }
 * }
 * ```
 *
 * Only names registered here resolve; any other name is refused. Props are
 * untrusted data: they are sanitised, then decoded by the component's own
 * decoder into its own typed model, then checked by the optional validator.
 * Nothing the model sends is evaluated, and no component is chosen by
 * anything but the exact name. Build the registry once (in `remember` or
 * outside composition) and register everything before it is shown: the
 * registry is not observable, so a later registration does not recompose.
 */
@Stable
public class MonarchAiUiRegistry {
    private val entries = LinkedHashMap<String, MonarchAiUiEntry>()

    /** The registered names, in the order they were first registered. */
    public val names: List<String> get() = entries.keys.toList()

    /** Whether [name] is registered. */
    public operator fun contains(name: String): Boolean = name in entries

    internal fun entry(name: String): MonarchAiUiEntry? = entries[name]

    /**
     * Registers a component. Registering a name again replaces the earlier
     * component and keeps its place; an empty name is ignored.
     *
     * @param name the name the model uses, such as `"Weather"`. Matched exactly.
     * @param decode turns the sanitised props into the component's typed model. Return `null` (or throw) to
     * refuse them. It receives only plain data: objects, arrays, strings, finite numbers, booleans and `null`.
     * @param description one sentence for the model: when to use this component. Listed by [manifest].
     * @param streaming whether it renders while props stream, or waits for the complete part.
     * @param validate checks the decoded props. Return `false` to refuse them.
     * @param content draws the component from the props and the action channel.
     */
    public fun <P : Any> register(
        name: String,
        decode: (MonarchAiJSON) -> P?,
        description: String? = null,
        streaming: MonarchAiUiStreaming = MonarchAiUiStreaming.Progressive,
        validate: ((P) -> Boolean)? = null,
        content: @Composable (props: P, emit: MonarchAiUiEmit) -> Unit,
    ) {
        if (name.isEmpty()) return
        entries[name] = MonarchAiUiEntry(description, streaming) { json ->
            val props = runCatching { decode(json) }.getOrNull()
            val valid = props != null && (validate == null || runCatching { validate(props) }.getOrDefault(false))
            if (props == null || !valid) {
                MonarchAiUiPrepared.Refused(MonarchAiUiRejection.InvalidProps)
            } else {
                MonarchAiUiPrepared.Ready { emit -> content(props, emit) }
            }
        }
    }

    /**
     * A compact list of what may be rendered, one line per component, to put
     * in the model's system prompt: `- Weather: Current weather for a city.`
     */
    public fun manifest(): String =
        MonarchAiGenerativeUiTokens.manifest(entries.map { (name, entry) -> name to entry.description })
}

/** One registered component, with its props type erased. */
internal class MonarchAiUiEntry(
    val description: String?,
    val streaming: MonarchAiUiStreaming,
    val prepare: (MonarchAiJSON) -> MonarchAiUiPrepared,
)

/** Props decoded and checked: the content to draw, or why not. */
internal sealed interface MonarchAiUiPrepared {
    /** The props are valid. */
    class Ready(val content: @Composable (MonarchAiUiEmit) -> Unit) : MonarchAiUiPrepared

    /** The props were refused. */
    class Refused(val reason: MonarchAiUiRejection) : MonarchAiUiPrepared
}
