package com.rookhq.monarch.components.forms.entitypicker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.painter.Painter
import com.rookhq.monarch.components.identity.avatar.MonarchAvatarShape
import com.rookhq.monarch.components.identity.presencedot.MonarchPresenceStatus

/**
 * A person, record or other entity a `MonarchEntityPicker` offers.
 *
 * @property id the stable id, passed to `onSelectedChange`.
 * @property name the visible name, in the list and in the field once chosen. Already localised.
 * @property description a second line under the name, such as a role or a company.
 * @property hint short text at the end of the row, such as an email or a code. Matched by the default filter.
 * @property image the picture, loaded by the app. While it is `null` the avatar shows initials.
 * @property shape a circle for people, a square for organisations and records.
 * @property keywords extra words the default filter matches but never shows.
 * @property enabled whether the entity can be chosen. A disabled entity stays visible in the disabled colour.
 * @property icon a decorative glyph that stands for the kind of entity, such as a list or a board. It replaces the
 * avatar in the row and in the field.
 * @property group the heading the entity is listed under, such as "Active". Neighbours with the same group share
 * one small heading; entities without a group list without one.
 * @property status a presence dot on the avatar, such as online or away. `null` shows none.
 */
@Immutable
public class MonarchEntityPickerEntity(
    public val id: String,
    public val name: String,
    public val description: String? = null,
    public val hint: String? = null,
    public val image: Painter? = null,
    public val shape: MonarchAvatarShape = MonarchAvatarShape.Circle,
    public val keywords: String? = null,
    public val enabled: Boolean = true,
    public val icon: (@Composable () -> Unit)? = null,
    public val group: String? = null,
    public val status: MonarchPresenceStatus? = null,
)
