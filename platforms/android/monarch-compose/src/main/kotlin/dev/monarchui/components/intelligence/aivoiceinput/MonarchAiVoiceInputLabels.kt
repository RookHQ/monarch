package dev.monarchui.components.intelligence.aivoiceinput

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchAiVoiceInput`. Every
 * `null` uses the built-in string named after it in `strings_aivoiceinput.xml`.
 *
 * @property dictate the button in toggle mode: "Dictate".
 * @property holdToTalk the button in push-to-talk mode: "Hold to talk".
 * @property holdHint the TalkBack action of the push-to-talk button, read as "Double-tap to" and the action:
 * "start or stop listening".
 * @property starting the status while waiting for the microphone: "Starting microphone…".
 * @property listening the status while listening, also announced: "Listening…".
 * @property stopped announced when listening stops: "Stopped listening."
 * @property denied shown when microphone access is off.
 * @property noSpeech shown when nothing was heard.
 * @property noMicrophone shown when there is no microphone.
 * @property unavailable shown when recognition is not available.
 * @property error shown for any other failure.
 * @property transcript read before the live transcript: "Transcript".
 * @property openSettings the action that opens the app's settings page when access is off: "Open settings".
 */
@Immutable
public class MonarchAiVoiceInputLabels(
    public val dictate: String? = null,
    public val holdToTalk: String? = null,
    public val holdHint: String? = null,
    public val starting: String? = null,
    public val listening: String? = null,
    public val stopped: String? = null,
    public val denied: String? = null,
    public val noSpeech: String? = null,
    public val noMicrophone: String? = null,
    public val unavailable: String? = null,
    public val error: String? = null,
    public val transcript: String? = null,
    public val openSettings: String? = null,
)
