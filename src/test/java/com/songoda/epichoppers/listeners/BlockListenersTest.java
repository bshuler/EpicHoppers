package com.songoda.epichoppers.listeners;

import com.songoda.epichoppers.EpicHoppersPlugin;
import com.songoda.epichoppers.api.hopper.Hopper;
import com.songoda.epichoppers.api.hopper.TeleportTrigger;
import com.songoda.epichoppers.handlers.EnchantmentHandler;
import com.songoda.epichoppers.hopper.EFilter;
import com.songoda.epichoppers.hopper.EHopper;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockListenersTest {

    private ServerMock server;
    private EpicHoppersPlugin plugin;
    private WorldMock world;
    private BlockListeners listener;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(EpicHoppersPlugin.class);
        world = server.addSimpleWorld("world");
        listener = new BlockListeners(plugin);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private BlockPlaceEvent placeEvent(Block block, ItemStack itemInHand, PlayerMock player) {
        Block against = world.getBlockAt(block.getX(), block.getY() - 1, block.getZ());
        BlockState replaced = block.getState();
        return new BlockPlaceEvent(block, replaced, against, itemInHand, player, true);
    }

    private ItemStack namedItem(Material material, String displayName) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(displayName);
        item.setItemMeta(meta);
        return item;
    }

    // --- onBlockPlace ---

    @Test
    void onBlockPlaceIgnoresEnderChestsEntirely() {
        PlayerMock player = server.addPlayer();
        Block block = world.getBlockAt(0, 5, 0);
        block.setType(Material.ENDER_CHEST);

        BlockPlaceEvent event = placeEvent(block, new ItemStack(Material.ENDER_CHEST), player);
        listener.onBlockPlace(event);

        assertFalse(event.isCancelled());
        assertFalse(plugin.getHopperManager().isHopper(block.getLocation()));
    }

    @Test
    void onBlockPlaceIgnoresNonHopperBlocks() {
        PlayerMock player = server.addPlayer();
        Block block = world.getBlockAt(0, 5, 0);
        block.setType(Material.CHEST);

        BlockPlaceEvent event = placeEvent(block, new ItemStack(Material.CHEST), player);
        listener.onBlockPlace(event);

        assertFalse(event.isCancelled());
        assertFalse(plugin.getHopperManager().isHopper(block.getLocation()));
    }

    @Test
    void onBlockPlaceRegistersAHopperWhenTheItemHasADisplayName() {
        PlayerMock player = server.addPlayer();
        Block block = world.getBlockAt(0, 5, 0);
        block.setType(Material.HOPPER);

        BlockPlaceEvent event = placeEvent(block, namedItem(Material.HOPPER, "Basic Hopper"), player);
        listener.onBlockPlace(event);

        assertFalse(event.isCancelled());
        Hopper hopper = plugin.getHopperManager().getHopper(block.getLocation());
        assertTrue(hopper != null);
        assertEquals(player.getUniqueId(), hopper.getPlacedBy());
    }

    @Test
    void onBlockPlaceDoesNotRegisterAHopperWhenTheItemHasNoDisplayName() {
        PlayerMock player = server.addPlayer();
        Block block = world.getBlockAt(0, 5, 0);
        block.setType(Material.HOPPER);

        BlockPlaceEvent event = placeEvent(block, new ItemStack(Material.HOPPER), player);
        listener.onBlockPlace(event);

        assertFalse(event.isCancelled());
        assertFalse(plugin.getHopperManager().isHopper(block.getLocation()));
    }

    @Test
    void onBlockPlaceCancelsPlacementOnceTheChunkHopperLimitIsExceeded() {
        plugin.getConfig().set("Main.Max Hoppers Per Chunk", 1);
        PlayerMock player = server.addPlayer();

        Block first = world.getBlockAt(0, 5, 0);
        first.setType(Material.HOPPER);
        Block second = world.getBlockAt(1, 5, 0);
        second.setType(Material.HOPPER);

        BlockPlaceEvent event = placeEvent(second, namedItem(Material.HOPPER, "Basic Hopper"), player);
        listener.onBlockPlace(event);

        assertTrue(event.isCancelled());
        assertFalse(plugin.getHopperManager().isHopper(second.getLocation()));
    }

    // --- onBlockBreak ---

    @Test
    void onBlockBreakOfALevelZeroHopperDropsNothingExtraAndDeregisters() {
        // The default config only ever defines Level-1 through Level-7 (see
        // EpicHoppersPlugin.setupConfig()) - there is no Level-0 entry, so a
        // "level 0" hopper is otherwise unreachable through normal play.
        // Registering one directly through the LevelManager is the only way
        // to exercise onBlockBreak's non-cancelling branch at all.
        plugin.getLevelManager().addLevel(0, 0, 0, 10, 1, false, false, new ArrayList<>());

        PlayerMock player = server.addPlayer();
        Block block = world.getBlockAt(0, 5, 0);
        block.setType(Material.HOPPER);

        Hopper hopper = new EHopper(block.getLocation(), plugin.getLevelManager().getLevel(0),
                player.getUniqueId(), player.getUniqueId(), null, new EFilter(), TeleportTrigger.DISABLED, null);
        plugin.getHopperManager().addHopper(block.getLocation(), hopper);

        BlockBreakEvent event = new BlockBreakEvent(block, player);
        listener.onBlockBreak(event);

        assertFalse(event.isCancelled());
        assertFalse(plugin.getHopperManager().isHopper(block.getLocation()));
    }

    @Test
    void onBlockBreakOfAnUpgradedHopperCancelsAndDropsTheLeveledItemInstead() {
        // Every configured level (Level-1..Level-7) is non-zero, so any
        // hopper backed by the default config always takes this branch.
        PlayerMock player = server.addPlayer();
        Block block = world.getBlockAt(0, 5, 0);
        block.setType(Material.HOPPER);

        Hopper hopper = new EHopper(block.getLocation(), plugin.getLevelManager().getLowestLevel(),
                player.getUniqueId(), player.getUniqueId(), null, new EFilter(), TeleportTrigger.DISABLED, null);
        plugin.getHopperManager().addHopper(block.getLocation(), hopper);

        BlockBreakEvent event = new BlockBreakEvent(block, player);
        listener.onBlockBreak(event);

        assertTrue(event.isCancelled());
        assertEquals(Material.AIR, block.getType());
        assertFalse(plugin.getHopperManager().isHopper(block.getLocation()));
    }

    @Test
    void onBlockBreakDropsFilterListItemsAndResetsThePlayersSyncType() {
        PlayerMock player = server.addPlayer();
        plugin.getPlayerDataManager().getPlayerData(player).setSyncType(com.songoda.epichoppers.player.SyncType.REGULAR);

        Block block = world.getBlockAt(0, 5, 0);
        block.setType(Material.HOPPER);

        EFilter filter = new EFilter();
        filter.getWhiteList().add(Material.DIAMOND);
        filter.getBlackList().add(Material.COAL);
        filter.getVoidList().add(Material.IRON_INGOT);

        Hopper hopper = new EHopper(block.getLocation(), plugin.getLevelManager().getLowestLevel(),
                player.getUniqueId(), player.getUniqueId(), null, filter, TeleportTrigger.DISABLED, null);
        plugin.getHopperManager().addHopper(block.getLocation(), hopper);

        BlockBreakEvent event = new BlockBreakEvent(block, player);
        listener.onBlockBreak(event);

        boolean sawDiamond = false, sawCoal = false, sawIron = false;
        for (org.bukkit.entity.Entity ent : world.getEntitiesByClass(org.bukkit.entity.Item.class)) {
            Material type = ((org.bukkit.entity.Item) ent).getItemStack().getType();
            if (type == Material.DIAMOND) sawDiamond = true;
            if (type == Material.COAL) sawCoal = true;
            if (type == Material.IRON_INGOT) sawIron = true;
        }
        assertTrue(sawDiamond && sawCoal && sawIron);
        assertNull(plugin.getPlayerDataManager().getPlayerData(player).getSyncType());
    }

    // --- handleSyncTouch (invoked via onBlockBreak) ---

    @Test
    void handleSyncTouchRoutesNormalDropsIntoTheSyncedChestAndSuppressesNaturalDrop() {
        PlayerMock player = server.addPlayer();
        Block chest = world.getBlockAt(5, 5, 5);
        chest.setType(Material.CHEST);
        ItemStack tool = new EnchantmentHandler().createSyncTouch(new ItemStack(Material.DIAMOND_PICKAXE), chest);
        player.getInventory().setItemInMainHand(tool);

        Block target = world.getBlockAt(0, 5, 0);
        target.setType(Material.DIRT);
        // MockBukkit's BlockMock does not compute real loot-table drops -
        // getDrops() is empty unless explicitly seeded.
        ((org.mockbukkit.mockbukkit.block.BlockMock) target).setDrops(java.util.List.of(new ItemStack(Material.DIRT)));

        BlockBreakEvent event = new BlockBreakEvent(target, player);
        listener.onBlockBreak(event);

        assertFalse(event.isDropItems());
        Inventory chestInventory = ((InventoryHolder) chest.getState()).getInventory();
        boolean sawSomething = false;
        for (ItemStack is : chestInventory.getContents()) {
            if (is != null) sawSomething = true;
        }
        assertTrue(sawSomething);
    }

    @Test
    void handleSyncTouchHarvestsASingleBlockWhenTheToolHasSilkTouch() {
        PlayerMock player = server.addPlayer();
        Block chest = world.getBlockAt(5, 5, 5);
        chest.setType(Material.CHEST);
        ItemStack tool = new EnchantmentHandler().createSyncTouch(new ItemStack(Material.DIAMOND_PICKAXE), chest);
        ItemMeta meta = tool.getItemMeta();
        meta.addEnchant(org.bukkit.enchantments.Enchantment.SILK_TOUCH, 1, true);
        tool.setItemMeta(meta);
        player.getInventory().setItemInMainHand(tool);

        Block target = world.getBlockAt(0, 5, 0);
        target.setType(Material.STONE);

        BlockBreakEvent event = new BlockBreakEvent(target, player);
        listener.onBlockBreak(event);

        Inventory chestInventory = ((InventoryHolder) chest.getState()).getInventory();
        assertEquals(Material.STONE, chestInventory.getItem(0).getType());
    }

    @Test
    void handleSyncTouchDoesNothingWhenThePlayerIsNotHoldingASyncTool() {
        PlayerMock player = server.addPlayer();
        player.getInventory().setItemInMainHand(new ItemStack(Material.DIAMOND_PICKAXE));

        Block target = world.getBlockAt(0, 5, 0);
        target.setType(Material.STONE);

        BlockBreakEvent event = new BlockBreakEvent(target, player);
        listener.onBlockBreak(event);

        assertTrue(event.isDropItems());
    }

    @Test
    void handleSyncTouchDoesNothingForTheUnboundSyncTouchVariant() {
        // The unbound variant (no target block) still matches Methods.isSync()
        // - it shares the same "Sync Touch" lore marker text - but only has
        // one lore line, so the explicit lore.size() != 2 guard must reject it.
        PlayerMock player = server.addPlayer();
        ItemStack tool = new EnchantmentHandler().createSyncTouch(new ItemStack(Material.DIAMOND_PICKAXE), null);
        player.getInventory().setItemInMainHand(tool);

        Block target = world.getBlockAt(0, 5, 0);
        target.setType(Material.STONE);

        BlockBreakEvent event = new BlockBreakEvent(target, player);
        listener.onBlockBreak(event);

        assertTrue(event.isDropItems());
    }

    @Test
    void handleSyncTouchDoesNothingWhenTheSyncedLocationIsNoLongerAChest() {
        PlayerMock player = server.addPlayer();
        Block chest = world.getBlockAt(5, 5, 5);
        chest.setType(Material.CHEST);
        ItemStack tool = new EnchantmentHandler().createSyncTouch(new ItemStack(Material.DIAMOND_PICKAXE), chest);
        player.getInventory().setItemInMainHand(tool);
        // The chest is gone by the time the player breaks something else.
        chest.setType(Material.AIR);

        Block target = world.getBlockAt(0, 5, 0);
        target.setType(Material.STONE);

        BlockBreakEvent event = new BlockBreakEvent(target, player);
        listener.onBlockBreak(event);

        assertTrue(event.isDropItems());
    }

    @Test
    void handleSyncTouchIgnoresBlacklistedBlockTypes() {
        PlayerMock player = server.addPlayer();
        Block chest = world.getBlockAt(5, 5, 5);
        chest.setType(Material.CHEST);
        ItemStack tool = new EnchantmentHandler().createSyncTouch(new ItemStack(Material.DIAMOND_PICKAXE), chest);
        player.getInventory().setItemInMainHand(tool);

        Block target = world.getBlockAt(0, 5, 0);
        target.setType(Material.SPAWNER);

        BlockBreakEvent event = new BlockBreakEvent(target, player);
        listener.onBlockBreak(event);

        assertTrue(event.isDropItems());
    }
}
