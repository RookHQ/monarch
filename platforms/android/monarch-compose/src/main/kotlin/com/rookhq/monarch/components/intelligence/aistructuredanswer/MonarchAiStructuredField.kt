package com.rookhq.monarch.components.intelligence.aistructuredanswer

import androidx.compose.runtime.Immutable
import com.rookhq.monarch.foundation.MonarchTone

/**
 * One field of a `MonarchAiStructuredAnswer`: which value of the data it
 * shows, under which label, and how.
 *
 * @property key the key of the value in the answer's data.
 * @property label the visible label, already localised.
 * @property type how the value is shown.
 * @property format writes a `Number` field, for example as a percentage. `null` uses the answer's locale.
 * @property tone the tone of each badge of a `Badge` field, from its value. `null` is `MonarchTone.Neutral`.
 * @property wide takes a whole row in the grid layout. `null` makes `List` and `Markdown` fields wide.
 */
@Immutable
public class MonarchAiStructuredField(
    public val key: String,
    public val label: String,
    public val type: MonarchAiStructuredFieldType = MonarchAiStructuredFieldType.Text,
    public val format: ((Double) -> String)? = null,
    public val tone: ((value: String) -> MonarchTone)? = null,
    public val wide: Boolean? = null,
)
