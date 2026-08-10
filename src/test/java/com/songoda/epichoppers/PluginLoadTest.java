package com.songoda.epichoppers;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Proves the real plugin main class can be loaded end-to-end under
 * MockBukkit (onEnable, config generation, locale loading, listener
 * registration). This is the backbone fixture most other test classes in
 * this module build on, since almost all business logic goes through
 * {@code EpicHoppersPlugin.getInstance()}.
 */
class PluginLoadTest {

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
    void pluginLoadsAndBecomesTheSingleton() {
        assertNotNull(plugin);
        assertSame(plugin, EpicHoppersPlugin.getInstance());
    }

    @Test
    void managersAreInitialized() {
        assertNotNull(plugin.getLevelManager());
        assertNotNull(plugin.getHopperManager());
        assertNotNull(plugin.getLocale());
        assertNotNull(plugin.getBoostManager());
        assertNotNull(plugin.getPlayerDataManager());
        assertNotNull(plugin.getCommandManager());
        assertNotNull(plugin.getSettingsManager());
    }

    @Test
    void defaultLevelsAreLoadedFromConfig() {
        assertNotNull(plugin.getLevelManager().getLevel(1));
        assertNotNull(plugin.getLevelManager().getLevel(7));
    }
}
