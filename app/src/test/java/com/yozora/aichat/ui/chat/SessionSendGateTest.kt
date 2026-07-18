package com.yozora.aichat.ui.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionSendGateTest {
    @Test
    fun rapidClaimsAcceptOnlyOneAttemptPerSession() {
        var nextToken = 0
        val gate = SessionSendGate { "token-${++nextToken}" }

        val accepted = List(10) { gate.claim("chat-a") }

        assertEquals("token-1", accepted.first())
        assertTrue(accepted.drop(1).all { it == null })
        assertTrue(gate.isSending("chat-a"))
    }

    @Test
    fun sessionsAreIndependent() {
        var nextToken = 0
        val gate = SessionSendGate { "token-${++nextToken}" }

        val tokenA = gate.claim("chat-a")
        val tokenB = gate.claim("chat-b")

        assertEquals("token-1", tokenA)
        assertEquals("token-2", tokenB)
        assertTrue(gate.isSending("chat-a"))
        assertTrue(gate.isSending("chat-b"))
    }

    @Test
    fun staleTokenCannotReleaseNewAttempt() {
        var nextToken = 0
        val gate = SessionSendGate { "token-${++nextToken}" }
        val first = gate.claim("chat")!!
        gate.release("chat", first)
        val second = gate.claim("chat")!!

        gate.release("chat", first)

        assertTrue(gate.isSending("chat"))
        assertNull(gate.claim("chat"))
        gate.release("chat", second)
        assertFalse(gate.isSending("chat"))
    }

    @Test
    fun sameSessionCanSendAgainAfterCompletion() {
        var nextToken = 0
        val gate = SessionSendGate { "token-${++nextToken}" }
        val first = gate.claim("chat")!!
        gate.release("chat", first)

        assertEquals("token-2", gate.claim("chat"))
    }
}
