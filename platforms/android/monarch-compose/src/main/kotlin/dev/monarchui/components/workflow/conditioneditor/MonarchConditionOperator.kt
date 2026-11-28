package dev.monarchui.components.workflow.conditioneditor

/** How a [MonarchCondition] compares the field's value. */
public enum class MonarchConditionOperator {
    /** Is: equal, ignoring case for text and comparing the day for dates. */
    Eq,

    /** Is not. */
    Neq,

    /** Is any of a list of values. */
    In,

    /** Is none of a list of values. */
    Nin,

    /** Contains one of the values: a substring of text, or a member of a list. */
    Contains,

    /** Contains none of the values. */
    NotContains,

    /** Is greater than. */
    Gt,

    /** Is at least. */
    Gte,

    /** Is less than. */
    Lt,

    /** Is at most. */
    Lte,

    /** Is on an earlier day. */
    Before,

    /** Is on a later day. */
    After,

    /** Is yes. Takes no value. */
    IsTrue,

    /** Is no. Takes no value. */
    IsFalse,

    /** Has no value. Takes no value. */
    IsEmpty,

    /** Has a value. Takes no value. */
    IsNotEmpty,
    ;

    /** Whether the operator takes a value: "is empty" and "is yes" do not. */
    public val takesValue: Boolean get() = this !in NoValue

    /** Whether the operator takes a list of values ("is any of", "is none of"). */
    public val takesList: Boolean get() = this == In || this == Nin

    public companion object {
        private val NoValue = setOf(IsTrue, IsFalse, IsEmpty, IsNotEmpty)

        /**
         * The operators offered for [field]: its own `operators`, or the defaults for its type. Only [Eq] when the
         * field is unknown.
         */
        public fun forField(field: MonarchConditionField?): List<MonarchConditionOperator> =
            field?.operators ?: field?.type?.let(::defaultsFor) ?: listOf(Eq)

        /** The default operators of a field type, in the order they are offered. */
        public fun defaultsFor(type: MonarchConditionFieldType): List<MonarchConditionOperator> = when (type) {
            MonarchConditionFieldType.Text -> listOf(Eq, Neq, Contains, NotContains, IsEmpty, IsNotEmpty)
            MonarchConditionFieldType.Number -> listOf(Eq, Neq, Gt, Gte, Lt, Lte, IsEmpty, IsNotEmpty)
            MonarchConditionFieldType.Date -> listOf(Eq, Before, After, IsEmpty, IsNotEmpty)
            MonarchConditionFieldType.Select -> listOf(Eq, Neq, In, Nin, IsEmpty, IsNotEmpty)
            MonarchConditionFieldType.MultiSelect,
            MonarchConditionFieldType.Tags,
            -> listOf(Contains, NotContains, IsEmpty, IsNotEmpty)
            MonarchConditionFieldType.Boolean -> listOf(IsTrue, IsFalse)
        }
    }
}
