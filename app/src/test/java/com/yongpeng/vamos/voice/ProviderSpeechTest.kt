// Modified by Vamos (2026): brand and package migration.
package com.yongpeng.vamos.voice

import com.yongpeng.vamos.core.model.ModelError
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderSpeechTest {
    private class FakeRecorder(private val ms: Int, private val starts: Boolean = true) : Recorder {
        override val level: StateFlow<Float> = MutableStateFlow(0f)
        var cancelled = 0
        override fun start() = starts
        override suspend fun stop() = ShortArray(Wav.RATE * ms / 1000)
        override fun cancel() { cancelled++ }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private fun events(s: SpeechSession) = runBlocking {
        withTimeout(5_000) { s.events.filter { it !is SpeechEvent.Level }.toList() }
    }

    @Test fun releaseReturnsWhileRecorderStopIsSuspended() = kotlinx.coroutines.test.runTest {
        val stopped = CompletableDeferred<Unit>()
        val releaseStop = CompletableDeferred<Unit>()
        val recorder = object : Recorder {
            override val level = MutableStateFlow(0f)
            override fun start() = true
            override suspend fun stop(): ShortArray {
                stopped.complete(Unit)
                releaseStop.await()
                return ShortArray(0)
            }
            override fun cancel() = Unit
        }
        val session = ProviderSpeech(recorder, { error("short recording must not transcribe") }, backgroundScope)
        session.release()
        runCurrent()
        assertTrue(stopped.isCompleted)
        assertFalse(releaseStop.isCompleted)
        // The same caller can continue while audio shutdown is still waiting.
        releaseStop.complete(Unit)
        runCurrent()
    }

    @Test
    fun releaseConvertsThenGivesFinal() {
        var got: ByteArray? = null
        val s = ProviderSpeech(FakeRecorder(1_000), { wav -> got = wav; "你好" }, scope)
        assertFalse(s.live)
        s.release()
        assertEquals(listOf(SpeechEvent.Converting, SpeechEvent.Final("你好")), events(s))
        assertEquals(44 + Wav.RATE * 2, got!!.size)
    }

    @Test
    fun modelErrorBecomesProviderFailure() {
        val err = ModelError.Network()
        val s = ProviderSpeech(FakeRecorder(1_000), { throw err }, scope)
        s.release()
        val out = events(s)
        assertEquals(SpeechEvent.Converting, out[0])
        assertEquals(SpeechEvent.Failed(SpeechFailure.Provider(err)), out[1])
        assertEquals(2, out.size)
    }

    @Test
    fun stalledTranscriptionTimesOutAsNetwork() {
        val s = ProviderSpeech(FakeRecorder(1_000), { awaitCancellation() }, scope, timeoutMs = 100)
        s.release()
        assertEquals(listOf(SpeechEvent.Converting, SpeechEvent.Failed(SpeechFailure.Network)), events(s))
    }

    @Test
    fun defaultTimeoutIsThirtySeconds() {
        assertEquals(30_000L, ProviderSpeech.TRANSCRIBE_TIMEOUT_MS)
    }

    @Test
    fun tooShortRecordingIsEmptyFinalWithoutCalling() {
        var called = false
        val s = ProviderSpeech(FakeRecorder(300), { called = true; "x" }, scope)
        s.release()
        assertEquals(listOf(SpeechEvent.Final("")), events(s))
        assertFalse(called)
    }

    @Test
    fun micThatWontStartFails() {
        val s = ProviderSpeech(FakeRecorder(1_000, starts = false), { "x" }, scope)
        assertEquals(listOf(SpeechEvent.Failed(SpeechFailure.MicUnavailable)), events(s))
    }

    @Test
    fun cancelTwiceIsSafeAndSilencesEvents() {
        val gate = CompletableDeferred<Unit>()
        val rec = FakeRecorder(1_000)
        val s = ProviderSpeech(rec, { gate.await(); "迟到的" }, scope)
        s.release()
        s.cancel()
        s.cancel()
        gate.complete(Unit)
        val out = events(s)
        assertTrue("got $out", out.none { it is SpeechEvent.Final || it is SpeechEvent.Failed })
        assertTrue(rec.cancelled >= 1)
    }
}
