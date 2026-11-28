package dev.monarchui.components.intelligence.aiimagegeneration

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchAiImageGeneration`.
 * Every `null` keeps the built-in string `monarch_aiimagegeneration_<key>`.
 * Positions passed to the functions start at 1.
 *
 * @property label the TalkBack name of the whole group, "AI generated images".
 * @property pending the status line while nothing is ready, from the formatted percentage if known:
 * "Generating images… 40%".
 * @property progressive the status line while previews refine, from the formatted percentage if known:
 * "Refining images… 80%".
 * @property done the status line when finished, from the number of images: "4 images generated".
 * @property error the message when generation failed, unless `error` is passed to the component.
 * @property announceDone announced once when generation finishes, from the number of images.
 * @property retry the retry button, "Try again".
 * @property imageName the TalkBack name of an image whose alt text is empty: "Generated image 2".
 * @property unavailable shown in a slot whose image is missing after generation, "Preview unavailable".
 * @property altLabel the label of an alt text field: "Alt text for image 2".
 * @property altRequired the error under an empty alt text field.
 * @property select the button that chooses an image, "Use image".
 * @property selectName the TalkBack name of that button: "Use image 2".
 * @property selected the mark on the chosen image, "Selected".
 * @property variations the button that asks for more images like one, "More like this".
 * @property variationsName the TalkBack name of that button: "More like this, image 2".
 */
@Immutable
public class MonarchAiImageGenerationLabels(
    public val label: String? = null,
    public val pending: ((percent: String?) -> String)? = null,
    public val progressive: ((percent: String?) -> String)? = null,
    public val done: ((count: Int) -> String)? = null,
    public val error: String? = null,
    public val announceDone: ((count: Int) -> String)? = null,
    public val retry: String? = null,
    public val imageName: ((position: Int) -> String)? = null,
    public val unavailable: String? = null,
    public val altLabel: ((position: Int) -> String)? = null,
    public val altRequired: String? = null,
    public val select: String? = null,
    public val selectName: ((position: Int) -> String)? = null,
    public val selected: String? = null,
    public val variations: String? = null,
    public val variationsName: ((position: Int) -> String)? = null,
)
