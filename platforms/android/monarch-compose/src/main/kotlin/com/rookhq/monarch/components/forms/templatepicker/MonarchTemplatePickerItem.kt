package com.rookhq.monarch.components.forms.templatepicker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable

/**
 * One template in a `MonarchTemplatePicker`. The app owns what applying it
 * does.
 *
 * @property id a stable id, passed to `onSelect`.
 * @property name the short name, already localised. It is the card's name for TalkBack.
 * @property description one or two lines on what the template is for.
 * @property category the id of one of the picker's categories.
 * @property badge a short marker such as "Built-in" or "New", read with the card.
 * @property keywords extra words the search matches, besides the name and the description.
 * @property enabled whether the template can be chosen.
 * @property preview a thumbnail drawn by the app, such as an `Image` of a `Painter` or a small layout. It is hidden
 * from TalkBack.
 */
@Immutable
public class MonarchTemplatePickerItem(
    public val id: String,
    public val name: String,
    public val description: String? = null,
    public val category: String? = null,
    public val badge: String? = null,
    public val keywords: List<String> = emptyList(),
    public val enabled: Boolean = true,
    public val preview: (@Composable () -> Unit)? = null,
)
