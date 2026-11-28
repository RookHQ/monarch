package com.rookhq.monarch.components.billing.pricingtable

/** How a `MonarchPricingTable` arranges its plans. */
public enum class MonarchPricingTableLayout {
    /** The grid when the table is at least 640dp wide and the font scale is below 1.3; pages otherwise. */
    Automatic,

    /** One plan per page with its features below, and a plan picker. */
    Paged,

    /** Plans as columns and features as rows. */
    Grid,
}
