package dev.monarchui.components.files.documentrelations

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.res.stringResource
import dev.monarchui.R

/**
 * A kind of relation and its label.
 *
 * @property value the stable value, passed to `onAdd` and stored in `MonarchDocumentRelation.type`.
 * @property label the visible name, such as "Parent of". Already localised.
 * @property inverse the `value` of the type as seen from the target (parent-of and child-of). `null` is the type
 * itself.
 */
@Immutable
public class MonarchDocumentRelationType(
    public val value: String,
    public val label: String,
    public val inverse: String? = null,
) {
    /** The built-in relation types. */
    public companion object {
        /**
         * The web's seven types with their built-in labels from `strings_documentrelations.xml`: related to,
         * parent of and child of, supersedes and superseded by, implements and specified by.
         */
        @Composable
        public fun defaults(): List<MonarchDocumentRelationType> = listOf(
            MonarchDocumentRelationType(
                "related-to",
                stringResource(R.string.monarch_documentrelations_type_related_to),
                "related-to",
            ),
            MonarchDocumentRelationType(
                "parent-of",
                stringResource(R.string.monarch_documentrelations_type_parent_of),
                "child-of",
            ),
            MonarchDocumentRelationType(
                "child-of",
                stringResource(R.string.monarch_documentrelations_type_child_of),
                "parent-of",
            ),
            MonarchDocumentRelationType(
                "supersedes",
                stringResource(R.string.monarch_documentrelations_type_supersedes),
                "superseded-by",
            ),
            MonarchDocumentRelationType(
                "superseded-by",
                stringResource(R.string.monarch_documentrelations_type_superseded_by),
                "supersedes",
            ),
            MonarchDocumentRelationType(
                "implements",
                stringResource(R.string.monarch_documentrelations_type_implements),
                "specified-by",
            ),
            MonarchDocumentRelationType(
                "specified-by",
                stringResource(R.string.monarch_documentrelations_type_specified_by),
                "implements",
            ),
        )
    }
}
