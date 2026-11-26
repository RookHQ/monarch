package com.rookhq.monarch.components.intelligence.aimodelpicker

import androidx.compose.runtime.Immutable

/**
 * One model a person can choose in a `MonarchAiModelPicker`.
 *
 * @property id the value passed to `onSelectionChange`. Unique among the models.
 * @property provider the provider or family the model is listed under, such as "Rook". Already localised.
 * @property name the display name.
 * @property description one short line, such as "Best for long documents".
 * @property capabilities the capability badges, in order.
 * @property contextTokens the context window in tokens, shown compactly: "200K context".
 * @property speed the relative speed.
 * @property costTier the relative cost from 1 (lowest) to 3 (highest), shown as $, $$ or $$$ and read in words.
 * Values outside 1 to 3 are clamped.
 * @property requiresKey the model runs on the workspace's own provider key.
 * @property keyConfigured that key is set up. A model that needs a missing key cannot be chosen; with
 * `onConfigureKey` choosing it asks for the key instead.
 * @property enabled whether the model is available, for example in the current plan.
 */
@Immutable
public class MonarchAiModelOption(
    public val id: String,
    public val provider: String,
    public val name: String,
    public val description: String? = null,
    public val capabilities: List<MonarchAiModelCapability> = emptyList(),
    public val contextTokens: Long? = null,
    public val speed: MonarchAiModelSpeed? = null,
    public val costTier: Int? = null,
    public val requiresKey: Boolean = false,
    public val keyConfigured: Boolean = false,
    public val enabled: Boolean = true,
)
