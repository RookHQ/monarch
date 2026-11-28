package com.rookhq.monarch.components.overlays.unsavedchangesguard

/** How the person chose to leave, passed to the `onLeave` of a `MonarchUnsavedChangesGuard`. */
public enum class MonarchUnsavedChangesGuardLeaveReason {
    /** Without saving: "Discard changes". */
    Discard,

    /** After `onSave` returned: "Save and leave". */
    Save,
}
