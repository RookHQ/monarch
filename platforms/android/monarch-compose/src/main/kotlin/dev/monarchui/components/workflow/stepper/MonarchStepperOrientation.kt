package dev.monarchui.components.workflow.stepper

/** The direction in which a `MonarchStepper` lays out its steps. */
public enum class MonarchStepperOrientation {
    /** Steps side by side with lines between them, for the top of a flow. */
    Horizontal,

    /** Steps stacked with lines between them, for a side panel or a long setup checklist. Never compact by itself. */
    Vertical,
}
