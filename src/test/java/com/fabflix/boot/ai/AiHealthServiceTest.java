package com.fabflix.boot.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class AiHealthServiceTest {
    private final OllamaReachabilityProbe probe = mock(OllamaReachabilityProbe.class);

    @Test
    void skipsTheProbeEntirelyWhenDisabled() {
        AiHealthResponse response = service("false").health();

        assertFalse(response.enabled());
        assertFalse(response.reachable());
        assertEquals("disabled", response.status());
        verify(probe, never()).isReachable(any());
    }

    @Test
    void treatsAnEmptySwitchAsDisabled() {
        AiHealthResponse response = service("").health();

        assertFalse(response.enabled());
        assertFalse(response.reachable());
        assertEquals("disabled", response.status());
        verify(probe, never()).isReachable(any());
    }

    @Test
    void treatsTrueCaseInsensitivelyAsEnabled() {
        when(probe.isReachable(any())).thenReturn(true);

        AiHealthResponse response = service("TRUE").health();

        assertTrue(response.enabled());
        assertTrue(response.reachable());
        assertEquals("ok", response.status());
        verify(probe).isReachable(Duration.ofSeconds(2));
    }

    @Test
    void reportsOkWhenTheProbeSucceeds() {
        when(probe.isReachable(Duration.ofSeconds(2))).thenReturn(true);

        AiHealthResponse response = service("true").health();

        assertTrue(response.enabled());
        assertTrue(response.reachable());
        assertEquals("ok", response.status());
        verify(probe).isReachable(Duration.ofSeconds(2));
    }

    @Test
    void reportsUnreachableWhenTheProbeFails() {
        when(probe.isReachable(any())).thenReturn(false);

        AiHealthResponse response = service("true").health();

        assertTrue(response.enabled());
        assertFalse(response.reachable());
        assertEquals("unreachable", response.status());
    }

    @Test
    void reportsUnreachableWhenTheProbeThrows() {
        when(probe.isReachable(any())).thenThrow(new IllegalStateException("probe blew up"));

        AiHealthResponse response = service("true").health();

        assertTrue(response.enabled());
        assertFalse(response.reachable());
        assertEquals("unreachable", response.status());
    }

    @Test
    void echoesTheConfiguredModelNamesVerbatim() {
        when(probe.isReachable(any())).thenReturn(true);

        AiHealthResponse response = service("true").health();

        assertEquals("qwen2.5:7b", response.chatModel());
        assertEquals("bge-m3", response.embeddingModel());
    }

    private AiHealthService service(String enabled) {
        return new AiHealthService(probe, enabled, "qwen2.5:7b", "bge-m3");
    }
}
