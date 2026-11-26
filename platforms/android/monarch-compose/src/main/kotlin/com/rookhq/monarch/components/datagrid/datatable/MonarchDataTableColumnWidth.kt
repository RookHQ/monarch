package com.rookhq.monarch.components.datagrid.datatable

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp

/**
 * The narrowest a column of the table layout of a `MonarchDataTable` gets. A column is never narrower than its
 * longest word (a value or its title), so no word breaks in the middle; the table scrolls sideways only when the
 * longest words of all columns do not fit. `null`, the default, adds no further minimum. Provide a width to keep
 * short columns from getting narrower than it; the columns share the width by their `weight`.
 */
public val LocalMonarchDataTableMinColumnWidth: ProvidableCompositionLocal<Dp?> = staticCompositionLocalOf { null }
