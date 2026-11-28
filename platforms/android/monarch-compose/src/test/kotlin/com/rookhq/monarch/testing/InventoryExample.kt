package com.rookhq.monarch.testing

import androidx.compose.runtime.Composable

/**
 * One compiled example of an inventory file. Instances are generated from
 * `inventory/<id>.json` by the `generateComponentTestSources` task.
 *
 * @property id the inventory id.
 * @property title the title of the example.
 * @property content the example, as written in the inventory file.
 */
internal class InventoryExample(val id: String, val title: String, val content: @Composable () -> Unit)
