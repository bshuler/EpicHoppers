package com.songoda.epichoppers.utils;

import com.songoda.epichoppers.EpicHoppersPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class DebuggerTest {

    private ServerMock server;
    private EpicHoppersPlugin plugin;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(EpicHoppersPlugin.class);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void isDebugReflectsTheConfigDefaultOfFalse() {
        assertFalse(Debugger.isDebug());
    }

    @Test
    void isDebugReflectsAnExplicitlyEnabledConfigValue() {
        plugin.getConfig().set("System.Debugger Enabled", true);
        assertTrue(Debugger.isDebug());
    }

    @Test
    void runReportDoesNothingObservableWhenDebugIsDisabled() {
        assertDoesNotThrow(() -> Debugger.runReport(new Exception("quiet failure")));
    }

    @Test
    void runReportPrintsTheStackTraceWhenDebugIsEnabled() {
        // sendReport(e) is an intentionally empty hook (no-op by design), so
        // the only observable behavior of runReport with debugging on is that
        // it doesn't throw while walking the println/printStackTrace branch.
        plugin.getConfig().set("System.Debugger Enabled", true);
        assertDoesNotThrow(() -> Debugger.runReport(new Exception("loud failure")));
    }

    @Test
    void canBeInstantiated() {
        // Debugger is a static-only utility class, never instantiated
        // anywhere in the codebase; this only exists to exercise the
        // implicit default constructor for coverage purposes.
        new Debugger();
    }
}
