package com.rookhq.monarch.components.identity.appaccesspicker

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.painter.Painter

/**
 * One app that can be granted in a `MonarchAppAccessPicker`.
 *
 * @property id the stable id, such as the app's slug; what the picker's value holds.
 * @property name the app's name, already localised.
 * @property description one line about what the app is for.
 * @property logo the app's logo, loaded by the app. `null` shows the initial on a tinted tile.
 * @property badge a short tag after the name, such as a plan or "Beta".
 * @property enabled `false` when the app cannot be changed here, for example when everyone has it.
 */
@Immutable
public class MonarchAppAccessOption(
    public val id: String,
    public val name: String,
    public val description: String? = null,
    public val logo: Painter? = null,
    public val badge: String? = null,
    public val enabled: Boolean = true,
)
