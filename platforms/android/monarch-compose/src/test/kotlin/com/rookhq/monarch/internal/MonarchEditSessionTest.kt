package com.rookhq.monarch.internal

import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The edit session shared by MonarchInlineEditText and MonarchEditableCell. */
@RunWith(RobolectricTestRunner::class)
class MonarchEditSessionTest {
    private val changes = mutableListOf<Boolean>()

    private fun session(): MonarchEditSession = MonarchEditSession(
        MonarchEditSessionState(mutableStateOf(false), mutableStateOf(""), mutableStateOf(false), mutableStateOf(null)),
        CoroutineScope(Dispatchers.Unconfined),
    ) { changes += it }

    @Test
    fun openEditAndCloseReportOnlyRealChanges() {
        val session = session()
        session.open("Draft")
        session.open("Again")
        assertEquals("Again", session.state.draft.value)
        session.refuse("Too long")
        assertEquals("Too long", session.state.failure.value)
        session.edit("Shorter")
        assertNull(session.state.failure.value)
        session.close()
        session.close()
        assertEquals(listOf(true, false), changes)
    }

    @Test
    fun aSaveClosesOnSuccessAndKeepsTheEditorWithTheMessageOnFailure() {
        val session = session()
        session.open("A")
        val gate = CompletableDeferred<Unit>()
        session.persist(message = { "failed" }) { gate.await() }
        assertTrue(session.saving)
        assertTrue(session.editing)
        gate.complete(Unit)
        assertFalse(session.saving)
        assertFalse(session.editing)

        session.open("B")
        session.persist(message = { "Could not save: ${it.message}" }) { error("offline") }
        assertFalse(session.saving)
        assertTrue(session.editing)
        assertEquals("Could not save: offline", session.state.failure.value)
    }

    @Test
    fun aMenuOrDialogClosesWhileItSaves() {
        val session = session()
        session.open("A")
        val gate = CompletableDeferred<Unit>()
        session.persist(closeWhileSaving = true, message = { "failed" }) { gate.await() }
        assertFalse(session.editing)
        assertTrue(session.saving)
        gate.complete(Unit)
        assertFalse(session.saving)
    }
}
