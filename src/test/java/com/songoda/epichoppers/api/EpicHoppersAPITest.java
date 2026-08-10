package com.songoda.epichoppers.api;

import com.songoda.epichoppers.EpicHoppersPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * EpicHoppersPlugin#onEnable() already calls
 * {@link EpicHoppersAPI#setImplementation(EpicHoppers)} and #onDisable()
 * already calls {@link EpicHoppersAPI#clearImplementation()} - MockBukkit's
 * load()/unmock() cycle drives both of those for every test in this suite,
 * which is also what keeps this class's static implementation field clean
 * between test classes. These tests exercise the static API directly against
 * whatever state that real load() left behind, rather than trying to
 * fabricate a scenario the plugin lifecycle wouldn't actually produce.
 */
class EpicHoppersAPITest {

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
    void getImplementationReturnsTheInstanceSetDuringOnEnable() {
        assertSame(plugin, EpicHoppersAPI.getImplementation());
    }

    @Test
    void setImplementationThrowsWhenAnImplementationIsAlreadyRegistered() {
        // onEnable() already registered `plugin` as the implementation;
        // calling setImplementation again without clearing first must throw.
        assertThrows(IllegalArgumentException.class, () -> EpicHoppersAPI.setImplementation(plugin));
    }

    @Test
    void clearImplementationAllowsARegistrationToBeMadeAgain() {
        EpicHoppersAPI.clearImplementation();
        assertNull(EpicHoppersAPI.getImplementation());

        EpicHoppersAPI.setImplementation(plugin);

        assertSame(plugin, EpicHoppersAPI.getImplementation());
    }

    @Test
    void canBeInstantiated() {
        // EpicHoppersAPI is a static-only utility class, never instantiated
        // anywhere in the codebase; this only exists to exercise the
        // implicit default constructor for coverage purposes.
        new EpicHoppersAPI();
    }
}
