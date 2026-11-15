package com.yozora.aichat.data.remote

import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

class GeminiLiveServiceTest {

    @Test
    fun setupMessageOmitsThinkingConfigAndEnablesSlidingWindowCompression() {
        MockWebServer().use { server ->
            server.start()
            val setupReceived = CountDownLatch(1)
            val setupPayload = AtomicReference<String>()
            server.enqueue(
                MockResponse().withWebSocketUpgrade(
                    RecordingSocketListener(
                        onText = { text ->
                            if (setupPayload.compareAndSet(null, text)) {
                                setupReceived.countDown()
                            }
                        }
                    )
                )
            )

            val errors = CopyOnWriteArrayList<String>()
            val service = GeminiLiveCallService(mockWebSocketUrl(server))
            startCall(service, errors = errors)

            assertTrue("setup message was never received", setupReceived.await(10, TimeUnit.SECONDS))
            assertTrue("unexpected live errors: $errors", errors.isEmpty())

            val setup = JSONObject(setupPayload.get()).getJSONObject("setup")
            assertEquals("models/gemini-3.8-live", setup.getString("model"))
            assertFalse(setup.has("thinkingConfig"))
            assertFalse(setup.has("thinkingLevel"))
            assertFalse(setup.has("historyConfig"))

            val generationConfig = setup.getJSONObject("generationConfig")
            assertFalse(generationConfig.has("thinkingConfig"))
            assertFalse(generationConfig.has("thinkingLevel"))
            assertEquals(
                "AUDIO",
                generationConfig.getJSONArray("responseModalities").getString(0)
            )
            assertEquals(
                "Puck",
                generationConfig.getJSONObject("speechConfig")
                    .getJSONObject("voiceConfig")
                    .getJSONObject("prebuiltVoiceConfig")
                    .getString("voice_name")
            )
            assertEquals(
                "TURN_INCLUDES_AUDIO_ACTIVITY_AND_ALL_VIDEO",
                setup.getJSONObject("realtimeInputConfig").getString("turnCoverage")
            )
            assertTrue(setup.getJSONObject("contextWindowCompression").has("slidingWindow"))
            assertFalse(setup.getJSONObject("sessionResumption").has("handle"))

            service.stop()
        }
    }

    @Test
    fun sessionResumptionUpdateHandleIsStoredAndReportedToCaller() {
        MockWebServer().use { server ->
            server.start()
            val handleReported = CountDownLatch(1)
            val reportedHandle = AtomicReference<String>()
            server.enqueue(
                MockResponse().withWebSocketUpgrade(
                    RecordingSocketListener(
                        onOpen = { socket ->
                            socket.send(
                                """{"sessionResumptionUpdate":{"newHandle":"handle-live-01","resumable":true}}"""
                            )
                        }
                    )
                )
            )

            val errors = CopyOnWriteArrayList<String>()
            val service = GeminiLiveCallService(mockWebSocketUrl(server))
            service.onSessionResumeHandle = { handle ->
                reportedHandle.set(handle)
                handleReported.countDown()
            }
            startCall(service, errors = errors)

            assertTrue("sessionResumptionUpdate was never received", handleReported.await(10, TimeUnit.SECONDS))
            assertTrue("unexpected live errors: $errors", errors.isEmpty())
            assertEquals("handle-live-01", reportedHandle.get())
            assertEquals("handle-live-01", service.currentSessionResumeHandle())

            service.stop()
        }
    }

    @Test
    fun goAwayTriggersReconnectBeforeServerDisconnects() {
        MockWebServer().use { server ->
            server.start()
            val serverDisconnected = AtomicBoolean(false)
            server.enqueue(
                MockResponse().withWebSocketUpgrade(
                    RecordingSocketListener(
                        onOpen = { socket ->
                            socket.send("""{"goAway":{"timeLeft":"10s"}}""")
                        },
                        onClosing = { serverDisconnected.set(true) }
                    )
                )
            )

            val goAwayReported = CountDownLatch(1)
            val timeLeftMs = AtomicLong(-1L)
            val errors = CopyOnWriteArrayList<String>()
            val service = GeminiLiveCallService(mockWebSocketUrl(server))
            service.onSessionGoAway = { left ->
                timeLeftMs.set(left)
                goAwayReported.countDown()
            }
            startCall(service, errors = errors)

            assertTrue("goAway never triggered a reconnect", goAwayReported.await(20, TimeUnit.SECONDS))
            assertTrue("unexpected live errors: $errors", errors.isEmpty())
            assertEquals(10_000L, timeLeftMs.get())
            assertFalse(
                "reconnect was triggered after the server already dropped the socket",
                serverDisconnected.get()
            )

            service.stop()
        }
    }

    @Test
    fun reconnectingSessionSendsStoredResumeHandleAndHistoryConfig() {
        MockWebServer().use { server ->
            server.start()
            val initialSetups = CopyOnWriteArrayList<String>()
            val reconnectSetups = CopyOnWriteArrayList<String>()
            val handleReported = CountDownLatch(1)
            val reconnectSetupReceived = CountDownLatch(1)

            server.enqueue(
                MockResponse().withWebSocketUpgrade(
                    RecordingSocketListener(
                        onOpen = { socket ->
                            socket.send("""{"sessionResumptionUpdate":{"newHandle":"handle-live-02"}}""")
                        },
                        onText = { text -> initialSetups += text }
                    )
                )
            )
            server.enqueue(
                MockResponse().withWebSocketUpgrade(
                    RecordingSocketListener(
                        onText = { text ->
                            reconnectSetups += text
                            reconnectSetupReceived.countDown()
                        }
                    )
                )
            )

            val errors = CopyOnWriteArrayList<String>()
            val service = GeminiLiveCallService(mockWebSocketUrl(server))
            service.onSessionResumeHandle = { handleReported.countDown() }
            startCall(service, errors = errors)

            assertTrue("sessionResumptionUpdate was never received", handleReported.await(10, TimeUnit.SECONDS))
            val storedHandle = service.currentSessionResumeHandle()
            service.stop()

            startCall(
                service = service,
                configuration = liveConfig(resumeHandle = storedHandle, priorContext = "Earlier chat"),
                errors = errors
            )
            assertTrue("reconnect setup message was never received", reconnectSetupReceived.await(10, TimeUnit.SECONDS))
            assertTrue("unexpected live errors: $errors", errors.isEmpty())

            val reconnectSetup = JSONObject(reconnectSetups[0]).getJSONObject("setup")
            assertEquals("handle-live-02", reconnectSetup.getJSONObject("sessionResumption").getString("handle"))
            assertTrue(
                reconnectSetup.getJSONObject("historyConfig")
                    .getBoolean("initialHistoryInClientContent")
            )

            service.stop()
        }
    }

    private fun mockWebSocketUrl(server: MockWebServer): String {
        return server.url("/live").toString().replaceFirst("http", "ws")
    }

    private fun liveConfig(
        resumeHandle: String? = null,
        priorContext: String? = null
    ) = GeminiLiveCallConfig(
        apiKey = "test-key",
        modelId = GeminiLiveCallService.DEFAULT_MODEL_ID,
        systemInstruction = "System prompt",
        voiceName = "Puck",
        priorConversationContext = priorContext,
        resumeHandle = resumeHandle
    )

    private fun startCall(
        service: GeminiLiveCallService,
        configuration: GeminiLiveCallConfig = liveConfig(),
        errors: CopyOnWriteArrayList<String>
    ) {
        service.start(
            config = configuration,
            onStatus = {},
            onError = { message -> errors += message },
            onUserTranscript = {},
            onModelTranscriptDelta = {},
            onModelTurnComplete = {}
        )
    }

    private class RecordingSocketListener(
        private val onOpen: (WebSocket) -> Unit = {},
        private val onText: (String) -> Unit = {},
        private val onClosing: () -> Unit = {},
        private val onFailure: (Throwable) -> Unit = {}
    ) : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            onOpen(webSocket)
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            onText(text)
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            onClosing()
            webSocket.close(code, reason)
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            onFailure(t)
        }
    }
}
