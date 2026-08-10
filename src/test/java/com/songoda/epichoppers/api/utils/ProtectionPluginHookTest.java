package com.songoda.epichoppers.api.utils;

import com.songoda.epichoppers.EpicHoppersPlugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProtectionPluginHookTest {

    private ServerMock server;
    private EpicHoppersPlugin plugin;
    private WorldMock world;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(EpicHoppersPlugin.class);
        world = server.addSimpleWorld("world");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    /**
     * Minimal implementation that only overrides the two abstract methods,
     * exercising the interface's default method exactly as a real
     * protection-plugin hook implementation would.
     */
    private static class RecordingHook implements ProtectionPluginHook {
        private Location lastChecked;
        private final boolean result;

        RecordingHook(boolean result) {
            this.result = result;
        }

        @Override
        public JavaPlugin getPlugin() {
            return null;
        }

        @Override
        public boolean canBuild(Player player, Location location) {
            this.lastChecked = location;
            return result;
        }
    }

    @Test
    void canBuildByBlockReturnsFalseWhenTheBlockIsNull() {
        RecordingHook hook = new RecordingHook(true);
        PlayerMock player = server.addPlayer();

        assertFalse(hook.canBuild(player, (Block) null));
    }

    @Test
    void canBuildByBlockDelegatesToTheLocationOverloadAndReturnsItsResult() {
        RecordingHook hook = new RecordingHook(true);
        PlayerMock player = server.addPlayer();
        Block block = world.getBlockAt(1, 1, 1);
        block.setType(Material.CHEST);

        assertTrue(hook.canBuild(player, block));
        org.junit.jupiter.api.Assertions.assertEquals(block.getLocation(), hook.lastChecked);
    }

    @Test
    void canBuildByBlockReturnsFalseWhenTheDelegateDenies() {
        RecordingHook hook = new RecordingHook(false);
        PlayerMock player = server.addPlayer();
        Block block = world.getBlockAt(2, 1, 1);
        block.setType(Material.CHEST);

        assertFalse(hook.canBuild(player, block));
    }
}
