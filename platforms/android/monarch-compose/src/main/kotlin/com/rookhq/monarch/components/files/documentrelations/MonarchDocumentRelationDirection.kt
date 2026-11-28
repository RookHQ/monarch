package com.rookhq.monarch.components.files.documentrelations

/** Which way a `MonarchDocumentRelation` points. */
public enum class MonarchDocumentRelationDirection {
    /** This document to the target, read as the relation's type: "Parent of Q3 plan". */
    Outgoing,

    /** The target to this document, read as the type's inverse: a parent-of relation reads "Child of". */
    Incoming,
}
