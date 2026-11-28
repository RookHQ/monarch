package dev.monarchui.components.search.facetedfilterpanel

/** Whether a group is open and shows all its options, with the callbacks that change it. */
internal class FacetedFilterPanelGroupState(
    val open: Boolean,
    val onOpenChange: (Boolean) -> Unit,
    val showAll: Boolean,
    val onShowAllChange: (Boolean) -> Unit,
)
