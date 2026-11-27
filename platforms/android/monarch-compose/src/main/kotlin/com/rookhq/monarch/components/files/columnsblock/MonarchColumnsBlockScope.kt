package com.rookhq.monarch.components.files.columnsblock

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable

/**
 * The receiver of the content of a `MonarchColumnsBlock`, where each column
 * is declared, as items are declared in a `LazyColumn`.
 */
public sealed interface MonarchColumnsBlockScope {
    /**
     * Adds one column. Its content is laid out top to bottom, and TalkBack
     * reads the whole column before the next one.
     *
     * @param content the content of the column.
     */
    public fun column(content: @Composable ColumnScope.() -> Unit)
}

/** Collects the columns declared in a `MonarchColumnsBlockScope`. */
internal class MonarchColumnsBlockScopeImpl : MonarchColumnsBlockScope {
    val columns = mutableListOf<@Composable ColumnScope.() -> Unit>()

    override fun column(content: @Composable ColumnScope.() -> Unit) {
        columns += content
    }
}
