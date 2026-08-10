package com.songoda.epichoppers.hopper;

import com.songoda.epichoppers.EpicHoppersPlugin;
import com.songoda.epichoppers.api.hopper.Hopper;
import org.bukkit.Location;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EHopperManagerTest {

    private ServerMock server;
    private EpicHoppersPlugin plugin;
    private WorldMock world;
    private EHopperManager manager;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(EpicHoppersPlugin.class);
        world = server.addSimpleWorld("world");
        manager = (EHopperManager) plugin.getHopperManager();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void isHopperIsFalseUntilAHopperIsAddedAtThatLocation() {
        Location loc = world.getBlockAt(0, 0, 0).getLocation();
        assertFalse(manager.isHopper(loc));

        Hopper hopper = new EHopper(loc, plugin.getLevelManager().getLowestLevel(), null, null, null,
                new EFilter(), com.songoda.epichoppers.api.hopper.TeleportTrigger.DISABLED, null);
        manager.addHopper(loc, hopper);

        assertTrue(manager.isHopper(loc));
    }

    @Test
    void addHopperRoundsTheLocationToBlockCoordinatesForLookup() {
        Location loc = world.getBlockAt(1, 2, 3).getLocation();
        Hopper hopper = new EHopper(loc, plugin.getLevelManager().getLowestLevel(), null, null, null,
                new EFilter(), com.songoda.epichoppers.api.hopper.TeleportTrigger.DISABLED, null);
        manager.addHopper(loc, hopper);

        Location fractional = loc.clone().add(0.7, 0.2, 0.9);
        assertTrue(manager.isHopper(fractional));
        assertSame(hopper, manager.getHopper(fractional));
    }

    @Test
    void getHopperAutoCreatesADefaultLevelHopperWhenNoneIsRegistered() {
        Location loc = world.getBlockAt(5, 5, 5).getLocation();
        assertFalse(manager.isHopper(loc));

        Hopper created = manager.getHopper(loc);

        assertNotNull(created);
        assertTrue(manager.isHopper(loc));
        assertEquals(plugin.getLevelManager().getLowestLevel(), created.getLevel());
    }

    @Test
    void getHopperByBlockDelegatesToTheBlocksLocation() {
        Location loc = world.getBlockAt(2, 2, 2).getLocation();
        Hopper hopper = new EHopper(loc, plugin.getLevelManager().getLowestLevel(), null, null, null,
                new EFilter(), com.songoda.epichoppers.api.hopper.TeleportTrigger.DISABLED, null);
        manager.addHopper(loc, hopper);

        assertSame(hopper, manager.getHopper(world.getBlockAt(2, 2, 2)));
    }

    @Test
    void removeHopperDeletesTheEntryAndReturnsIt() {
        Location loc = world.getBlockAt(3, 3, 3).getLocation();
        Hopper hopper = new EHopper(loc, plugin.getLevelManager().getLowestLevel(), null, null, null,
                new EFilter(), com.songoda.epichoppers.api.hopper.TeleportTrigger.DISABLED, null);
        manager.addHopper(loc, hopper);

        assertSame(hopper, manager.removeHopper(loc));
        assertFalse(manager.isHopper(loc));
    }

    @Test
    void getHoppersIsUnmodifiable() {
        assertThrowsUnsupported(() -> manager.getHoppers().put(world.getBlockAt(0, 0, 0).getLocation(),
                manager.getHopper(world.getBlockAt(9, 9, 9).getLocation())));
    }

    private void assertThrowsUnsupported(Runnable r) {
        org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class, r::run);
    }

    @Test
    void getHopperFromPlayerFindsTheHopperLastTouchedByThatPlayer() {
        PlayerMock player = server.addPlayer();
        Location loc = world.getBlockAt(4, 4, 4).getLocation();
        Hopper hopper = new EHopper(loc, plugin.getLevelManager().getLowestLevel(), player.getUniqueId(), null,
                null, new EFilter(), com.songoda.epichoppers.api.hopper.TeleportTrigger.DISABLED, null);
        manager.addHopper(loc, hopper);

        assertSame(hopper, manager.getHopperFromPlayer(player));
    }

    @Test
    void getHopperFromPlayerReturnsNullWhenNoHopperMatches() {
        PlayerMock player = server.addPlayer();
        assertNull(manager.getHopperFromPlayer(player));
    }
}
