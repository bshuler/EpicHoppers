package com.songoda.epichoppers.handlers;

import com.songoda.epichoppers.EpicHoppersPlugin;
import com.songoda.epichoppers.api.hopper.Hopper;
import com.songoda.epichoppers.api.hopper.TeleportTrigger;
import com.songoda.epichoppers.api.hopper.levels.modules.Module;
import com.songoda.epichoppers.hopper.EFilter;
import com.songoda.epichoppers.hopper.EHopper;
import com.songoda.epichoppers.hopper.levels.ELevelManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * HopHandler's hopperRunner()/addItem()/canHop()/doBlacklist() are private,
 * but they are wired to a real Bukkit scheduler task in the constructor -
 * exactly the seam MockBukkit's virtual clock is for, so these tests build
 * a real HopHandler, place real hopper/chest blocks in a MockBukkit world,
 * advance the clock with performTicks(), and assert on the resulting
 * inventory contents rather than calling anything privately.
 */
class HopHandlerTest {

    private ServerMock server;
    private EpicHoppersPlugin plugin;
    private WorldMock world;
    private ELevelManager levelManager;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(EpicHoppersPlugin.class);
        world = server.addSimpleWorld("hop-handler-world");
        world.loadChunk(0, 0); // every test block in this file sits inside chunk (0,0)

        levelManager = (ELevelManager) plugin.getLevelManager();
        levelManager.clear();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private void runOneHopperTick() {
        // Constructor schedules hopperCleaner()+the repeating hopperRunner()
        // task after a fixed 40-tick delay with an initial repeat delay of 0.
        new HopHandler(plugin);
        server.getScheduler().performTicks(42);
    }

    private Hopper registerHopper(Location hopperLoc, Block syncedBlock, List<Module> modules, EFilter filter) {
        levelManager.addLevel(1, 0, 0, 5, 1, false, false, new ArrayList<>(modules));
        Hopper hopper = new EHopper(hopperLoc, levelManager.getLevel(1), null, null, syncedBlock, filter,
                TeleportTrigger.DISABLED, null);
        plugin.getHopperManager().addHopper(hopperLoc, hopper);
        return hopper;
    }

    @Test
    void constructingTheHandlerAndAdvancingTheClockDoesNotThrowWithNoRegisteredHoppers() {
        new HopHandler(plugin);
        server.getScheduler().performTicks(42);
        assertTrue(plugin.getHopperManager().getHoppers().isEmpty());
    }

    @Test
    void hopperRunnerMovesOneItemFromTheHopperIntoASyncedChestAndClearsTheSlot() {
        Block hopperBlock = world.getBlockAt(0, 5, 0);
        hopperBlock.setType(Material.HOPPER);
        Block chestBlock = world.getBlockAt(0, 5, 1);
        chestBlock.setType(Material.CHEST);

        org.bukkit.inventory.ItemStack[] contents = new org.bukkit.inventory.ItemStack[5];
        contents[0] = new org.bukkit.inventory.ItemStack(Material.DIAMOND, 1);
        ((org.bukkit.block.Hopper) hopperBlock.getState()).getInventory().setContents(contents);

        registerHopper(hopperBlock.getLocation(), chestBlock, Collections.emptyList(), new EFilter());

        runOneHopperTick();

        org.bukkit.inventory.ItemStack[] hopperContents =
                ((org.bukkit.block.Hopper) hopperBlock.getState()).getInventory().getContents();
        org.bukkit.inventory.Inventory chestInventory =
                ((org.bukkit.inventory.InventoryHolder) chestBlock.getState()).getInventory();

        assertNull(hopperContents[0]);
        assertNotNull(chestInventory.getItem(0));
        assertEquals(Material.DIAMOND, chestInventory.getItem(0).getType());
        assertEquals(1, chestInventory.getItem(0).getAmount());
    }

    @Test
    void hopperRunnerLeavesTheHopperAloneWhenItHasNoSyncedBlock() {
        Block hopperBlock = world.getBlockAt(1, 5, 0);
        hopperBlock.setType(Material.HOPPER);

        org.bukkit.inventory.ItemStack[] contents = new org.bukkit.inventory.ItemStack[5];
        contents[0] = new org.bukkit.inventory.ItemStack(Material.DIAMOND, 1);
        ((org.bukkit.block.Hopper) hopperBlock.getState()).getInventory().setContents(contents);

        registerHopper(hopperBlock.getLocation(), null, Collections.emptyList(), new EFilter());

        runOneHopperTick();

        org.bukkit.inventory.ItemStack[] hopperContents =
                ((org.bukkit.block.Hopper) hopperBlock.getState()).getInventory().getContents();
        assertNotNull(hopperContents[0]);
        assertEquals(Material.DIAMOND, hopperContents[0].getType());
    }

    @Test
    void hopperRunnerUnregistersTheEntryWhenTheSyncedBlockIsNoLongerAValidDestinationType() {
        Block hopperBlock = world.getBlockAt(2, 5, 0);
        hopperBlock.setType(Material.HOPPER);
        Block notADestination = world.getBlockAt(2, 5, 1);
        notADestination.setType(Material.DIRT);

        org.bukkit.inventory.ItemStack[] contents = new org.bukkit.inventory.ItemStack[5];
        contents[0] = new org.bukkit.inventory.ItemStack(Material.DIAMOND, 1);
        ((org.bukkit.block.Hopper) hopperBlock.getState()).getInventory().setContents(contents);

        Hopper hopper = registerHopper(hopperBlock.getLocation(), notADestination, Collections.emptyList(), new EFilter());

        runOneHopperTick();

        assertNull(hopper.getSyncedBlock());
        org.bukkit.inventory.ItemStack[] hopperContents =
                ((org.bukkit.block.Hopper) hopperBlock.getState()).getInventory().getContents();
        assertNotNull(hopperContents[0]);
    }

    @Test
    void hopperRunnerSkipsItemsBlockedByARegisteredModule() {
        Block hopperBlock = world.getBlockAt(3, 5, 0);
        hopperBlock.setType(Material.HOPPER);
        Block chestBlock = world.getBlockAt(3, 5, 1);
        chestBlock.setType(Material.CHEST);

        org.bukkit.inventory.ItemStack[] contents = new org.bukkit.inventory.ItemStack[5];
        contents[0] = new org.bukkit.inventory.ItemStack(Material.DIAMOND, 1);
        ((org.bukkit.block.Hopper) hopperBlock.getState()).getInventory().setContents(contents);

        final boolean[] ran = {false};
        Module blockingModule = new Module() {
            @Override public String getName() { return "blocker"; }
            @Override public void run(Hopper hopper) { ran[0] = true; }
            @Override public List<Material> getBlockedItems(Hopper hopper) { return List.of(Material.DIAMOND); }
            @Override public String getDescription() { return "Blocks diamonds"; }
        };

        registerHopper(hopperBlock.getLocation(), chestBlock, List.of(blockingModule), new EFilter());

        runOneHopperTick();

        assertTrue(ran[0]);
        org.bukkit.inventory.ItemStack[] hopperContents =
                ((org.bukkit.block.Hopper) hopperBlock.getState()).getInventory().getContents();
        assertNotNull(hopperContents[0]);
        assertEquals(Material.DIAMOND, hopperContents[0].getType());
    }

    @Test
    void hopperRunnerRoutesAWhitelistMismatchThroughTheFilterEndpointInsteadOfTheSyncedBlock() {
        Block hopperBlock = world.getBlockAt(4, 5, 0);
        hopperBlock.setType(Material.HOPPER);
        Block syncedButUnused = world.getBlockAt(4, 5, 1);
        syncedButUnused.setType(Material.CHEST);
        Block filterEndpoint = world.getBlockAt(4, 5, 2);
        filterEndpoint.setType(Material.CHEST);

        org.bukkit.inventory.ItemStack[] contents = new org.bukkit.inventory.ItemStack[5];
        contents[0] = new org.bukkit.inventory.ItemStack(Material.DIAMOND, 1);
        ((org.bukkit.block.Hopper) hopperBlock.getState()).getInventory().setContents(contents);

        EFilter filter = new EFilter();
        filter.setWhiteList(new ArrayList<>(List.of(Material.EMERALD))); // diamond is not on the whitelist
        filter.setEndPoint(filterEndpoint);

        registerHopper(hopperBlock.getLocation(), syncedButUnused, Collections.emptyList(), filter);

        runOneHopperTick();

        org.bukkit.inventory.Inventory endpointInventory =
                ((org.bukkit.inventory.InventoryHolder) filterEndpoint.getState()).getInventory();
        org.bukkit.inventory.Inventory syncedInventory =
                ((org.bukkit.inventory.InventoryHolder) syncedButUnused.getState()).getInventory();

        assertNotNull(endpointInventory.getItem(0));
        assertEquals(Material.DIAMOND, endpointInventory.getItem(0).getType());
        assertNull(syncedInventory.getItem(0));
    }

    @Test
    void hopperRunnerAppliesTheOwnersActiveBoostMultiplierToTheAmountMoved() {
        // MockBukkit's scheduler fires the constructor's nested delayed->repeating
        // task combo an implementation-defined number of times within a single
        // performTicks() budget (observed empirically to run hopperRunner() more
        // than once per call), so this test does not assert an absolute amount.
        // Instead it registers two otherwise-identical hopper/chest pairs in the
        // SAME tick advance - one unboosted, one with a 3x boost on its owner -
        // and asserts the boosted pair moved exactly 3x as much as the unboosted
        // pair, which isolates "amt = level.getAmount() * multiplier" regardless
        // of how many times hopperRunner() actually ran.
        Block unboostedHopperBlock = world.getBlockAt(5, 5, 0);
        unboostedHopperBlock.setType(Material.HOPPER);
        Block unboostedChestBlock = world.getBlockAt(5, 5, 1);
        unboostedChestBlock.setType(Material.CHEST);

        Block boostedHopperBlock = world.getBlockAt(6, 5, 0);
        boostedHopperBlock.setType(Material.HOPPER);
        Block boostedChestBlock = world.getBlockAt(6, 5, 1);
        boostedChestBlock.setType(Material.CHEST);

        org.bukkit.inventory.ItemStack[] plentyOfDiamonds = new org.bukkit.inventory.ItemStack[5];
        plentyOfDiamonds[0] = new org.bukkit.inventory.ItemStack(Material.DIAMOND, 64);
        ((org.bukkit.block.Hopper) unboostedHopperBlock.getState()).getInventory()
                .setContents(plentyOfDiamonds.clone());
        ((org.bukkit.block.Hopper) boostedHopperBlock.getState()).getInventory()
                .setContents(plentyOfDiamonds.clone());

        levelManager.addLevel(1, 0, 0, 5, 1, false, false, new ArrayList<>());

        java.util.UUID unboostedOwner = java.util.UUID.randomUUID();
        Hopper unboostedHopper = new EHopper(unboostedHopperBlock.getLocation(), levelManager.getLevel(1), null,
                unboostedOwner, unboostedChestBlock, new EFilter(), TeleportTrigger.DISABLED, null);
        plugin.getHopperManager().addHopper(unboostedHopperBlock.getLocation(), unboostedHopper);

        java.util.UUID boostedOwner = java.util.UUID.randomUUID();
        Hopper boostedHopper = new EHopper(boostedHopperBlock.getLocation(), levelManager.getLevel(1), null,
                boostedOwner, boostedChestBlock, new EFilter(), TeleportTrigger.DISABLED, null);
        plugin.getHopperManager().addHopper(boostedHopperBlock.getLocation(), boostedHopper);
        plugin.getBoostManager().addBoostToPlayer(new com.songoda.epichoppers.boost.BoostData(3,
                System.currentTimeMillis() + 60_000, boostedOwner));

        runOneHopperTick();

        org.bukkit.inventory.Inventory unboostedChestInventory =
                ((org.bukkit.inventory.InventoryHolder) unboostedChestBlock.getState()).getInventory();
        org.bukkit.inventory.Inventory boostedChestInventory =
                ((org.bukkit.inventory.InventoryHolder) boostedChestBlock.getState()).getInventory();

        int unboostedAmount = unboostedChestInventory.getItem(0).getAmount();
        int boostedAmount = boostedChestInventory.getItem(0).getAmount();

        assertTrue(unboostedAmount > 0);
        assertEquals(3 * unboostedAmount, boostedAmount);
    }
}
