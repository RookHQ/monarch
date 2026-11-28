package dev.monarchui

/**
 * Marks a Monarch API that may change or be removed in any release, without
 * the deprecation period that stable API gets.
 *
 * Using a marked declaration is an error until the caller opts in with
 * `@OptIn(ExperimentalMonarchApi::class)`. An experimental type never appears
 * in the signature of a stable declaration.
 */
@RequiresOptIn(
    message = "This Monarch API is experimental and can change or be removed without a deprecation period.",
    level = RequiresOptIn.Level.ERROR,
)
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.TYPEALIAS,
    AnnotationTarget.CONSTRUCTOR,
)
@MustBeDocumented
public annotation class ExperimentalMonarchApi
