package dev.monarchui.buildlogic

import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.TestDescriptor
import org.gradle.api.tasks.testing.TestListener
import org.gradle.api.tasks.testing.TestResult
import java.time.Duration
import java.util.Timer
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.timer

/** The Gradle property that overrides the per-test limit, in minutes. */
const val TEST_LIMIT_PROPERTY = "monarch.testTimeoutMinutes"

/** The default limit for one test method. The slowest test today takes well under a minute. */
private const val DEFAULT_TEST_LIMIT_MINUTES = 5L

private const val CHECK_EVERY_MS = 15_000L

/**
 * Fails fast when a single test hangs. A hung Compose layout under Robolectric
 * spins the test JVM without output, and the task timeout alone waits for the
 * whole task budget. This watchdog checks the tests in flight every 15 seconds.
 * When one has run longer than the limit (5 minutes, or
 * `-Pmonarch.testTimeoutMinutes=N`) it prints every test in flight with its
 * running time, then stops the test JVMs of this build, so the task fails at
 * once with those names in the log.
 */
fun Test.failFastOnHungTests() {
    val limit = Duration.ofMinutes(
        project.providers.gradleProperty(TEST_LIMIT_PROPERTY).orNull?.toLongOrNull()
            ?: DEFAULT_TEST_LIMIT_MINUTES,
    )
    val taskPath = path
    val running = ConcurrentHashMap<String, Long>()
    var watchdog: Timer? = null
    val log = logger

    fun stop() {
        watchdog?.cancel()
        watchdog = null
        running.clear()
    }

    addTestListener(
        object : TestListener {
            override fun beforeSuite(suite: TestDescriptor) {
                if (suite.parent != null || watchdog != null) return
                watchdog = timer("monarch-test-watchdog", daemon = true, period = CHECK_EVERY_MS) {
                    val now = System.nanoTime()
                    val hung = running.filterValues { Duration.ofNanos(now - it) > limit }
                    if (hung.isEmpty()) return@timer
                    log.error("$taskPath: a test has run longer than ${limit.toMinutes()} min. Tests in flight:")
                    running.forEach { (name, start) ->
                        log.error("  $name (${Duration.ofNanos(now - start).toSeconds()} s)")
                    }
                    log.error("$taskPath: stopping the test JVM. Raise the limit with -P$TEST_LIMIT_PROPERTY=N.")
                    // Only this daemon's own test workers; nothing outside this build is touched.
                    ProcessHandle.current().descendants()
                        .filter { it.info().commandLine().orElse("").contains("Gradle Test Executor") }
                        .forEach { it.destroyForcibly() }
                    stop()
                }
            }

            override fun afterSuite(suite: TestDescriptor, result: TestResult) {
                if (suite.parent == null) stop()
            }

            override fun beforeTest(testDescriptor: TestDescriptor) {
                running["${testDescriptor.className}.${testDescriptor.name}"] = System.nanoTime()
            }

            override fun afterTest(testDescriptor: TestDescriptor, result: TestResult) {
                running.remove("${testDescriptor.className}.${testDescriptor.name}")
            }
        },
    )
}
