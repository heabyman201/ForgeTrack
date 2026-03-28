package com.forgecompose.workouttracker

import android.content.Context
import com.forgecompose.workouttracker.health.HrRepository
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Node
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HrPhoneWatchConnectionTest {

    private lateinit var context: Context
    private lateinit var messageClient: MessageClient

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        messageClient = mockk(relaxed = true)
    }

    @Test
    fun `phone sends start HR command to all connected watch nodes`() = runTest {
        val watchOne = mockNode("watch-1")
        val watchTwo = mockNode("watch-2")
        val payloadOne = slot<ByteArray>()
        val payloadTwo = slot<ByteArray>()
        every { messageClient.sendMessage("watch-1", "/hr_control", capture(payloadOne)) } returns Tasks.forResult(1)
        every { messageClient.sendMessage("watch-2", "/hr_control", capture(payloadTwo)) } returns Tasks.forResult(1)

        val repository = HrRepository(
            context = context,
            messageClientProvider = { messageClient },
            connectedNodesProvider = { listOf(watchOne, watchTwo) }
        )
        val result = repository.sendHrRecordingCommand(active = true)

        assertTrue(result.isSuccess)
        verify(exactly = 1) { messageClient.sendMessage("watch-1", "/hr_control", any()) }
        verify(exactly = 1) { messageClient.sendMessage("watch-2", "/hr_control", any()) }
        assertEquals("start", String(payloadOne.captured, Charsets.UTF_8))
        assertEquals("start", String(payloadTwo.captured, Charsets.UTF_8))
    }

    @Test
    fun `phone updates BPM when watch sends HR message`() {
        val repository = HrRepository(
            context = context,
            messageClientProvider = { messageClient },
            connectedNodesProvider = { emptyList() }
        )

        repository.onMessageReceived(fakeMessageEvent(path = "/hr", payload = "128"))
        assertEquals(128, repository.hr.value)

        repository.onMessageReceived(fakeMessageEvent(path = "/hr", payload = "invalid"))
        assertEquals(0, repository.hr.value)

        repository.onMessageReceived(fakeMessageEvent(path = "/other_path", payload = "145"))
        assertEquals(0, repository.hr.value)
    }

    private fun mockNode(id: String): Node {
        val node = mockk<Node>()
        every { node.id } returns id
        return node
    }

    private fun fakeMessageEvent(path: String, payload: String): MessageEvent {
        return object : MessageEvent {
            override fun getRequestId(): Int = 1
            override fun getPath(): String = path
            override fun getData(): ByteArray = payload.toByteArray(Charsets.UTF_8)
            override fun getSourceNodeId(): String = "watch-node"
        }
    }
}
