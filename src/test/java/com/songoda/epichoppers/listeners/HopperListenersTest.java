package com.songoda.epichoppers.listeners;

import com.songoda.epichoppers.EpicHoppersPlugin;
import com.songoda.epichoppers.api.hopper.Hopper;
import com.songoda.epichoppers.api.hopper.TeleportTrigger;
import com.songoda.epichoppers.hopper.EFilter;
import com.songoda.epichoppers.hopper.EHopper;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * source.getHolder() for a real hopper block's inventory is
 * org.bukkit.block.Hopper (a CraftBukkit/MockBukkit block state) - a real
 * bug fixed alongside this test had the instanceof check testing against
 * com.songoda.epichoppers.api.hopper.Hopper instead (the plugin's own
 * interface, never implemented by a block state), which made onHop() never
 * cancel anything.
 */
class HopperListenersTest {

    private ServerMock server;
    private EpicHoppersPlugin plugin;
    private WorldMock world;
    private HopperListeners listener;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(EpicHoppersPlugin.class);
        world = server.addSimpleWorld("world");
        listener = new HopperListeners(plugin);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private Location hopperLoc() {
        Block b = world.getBlockAt(0, 5, 0);
        b.setType(Material.HOPPER);
        return b.getLocation();
    }

    @Test
    void onHopCancelsMovementOutOfARegisteredHopperWithASyncedBlock() {
        Location loc = hopperLoc();
        Block chest = world.getBlockAt(0, 5, 1);
        chest.setType(Material.CHEST);
        Hopper hopper = new EHopper(loc, plugin.getLevelManager().getLowestLevel(), null, null, chest,
                new EFilter(), TeleportTrigger.DISABLED, null);
        plugin.getHopperManager().addHopper(loc, hopper);

        InventoryMoveItemEvent event = new InventoryMoveItemEvent(
                ((org.bukkit.inventory.InventoryHolder) loc.getBlock().getState()).getInventory(),
                new ItemStack(Material.DIAMOND), ((org.bukkit.inventory.InventoryHolder) chest.getState()).getInventory(), true);

        listener.onHop(event);

        assertTrue(event.isCancelled());
    }

    @Test
    void onHopLeavesMovementAloneWhenTheHopperHasNoSyncedBlock() {
        Location loc = hopperLoc();
        Hopper hopper = new EHopper(loc, plugin.getLevelManager().getLowestLevel(), null, null, null,
                new EFilter(), TeleportTrigger.DISABLED, null);
        plugin.getHopperManager().addHopper(loc, hopper);

        Block chest = world.getBlockAt(0, 5, 1);
        chest.setType(Material.CHEST);
        InventoryMoveItemEvent event = new InventoryMoveItemEvent(
                ((org.bukkit.inventory.InventoryHolder) loc.getBlock().getState()).getInventory(),
                new ItemStack(Material.DIAMOND), ((org.bukkit.inventory.InventoryHolder) chest.getState()).getInventory(), true);

        listener.onHop(event);

        assertFalse(event.isCancelled());
    }

    @Test
    void onHopLeavesMovementAloneWhenTheSourceIsNotARegisteredHopper() {
        Block chest = world.getBlockAt(0, 5, 1);
        chest.setType(Material.CHEST);
        Block otherChest = world.getBlockAt(0, 5, 2);
        otherChest.setType(Material.CHEST);

        InventoryMoveItemEvent event = new InventoryMoveItemEvent(((org.bukkit.inventory.InventoryHolder) chest.getState()).getInventory(),
                new ItemStack(Material.DIAMOND), ((org.bukkit.inventory.InventoryHolder) otherChest.getState()).getInventory(), true);

        listener.onHop(event);

        assertFalse(event.isCancelled());
    }
}
