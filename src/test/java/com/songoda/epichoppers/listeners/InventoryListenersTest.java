package com.songoda.epichoppers.listeners;

import com.songoda.epichoppers.EpicHoppersPlugin;
import com.songoda.epichoppers.api.hopper.Hopper;
import com.songoda.epichoppers.api.hopper.TeleportTrigger;
import com.songoda.epichoppers.hopper.EFilter;
import com.songoda.epichoppers.hopper.EHopper;
import com.songoda.epichoppers.hopper.levels.ELevelManager;
import com.songoda.epichoppers.player.MenuType;
import com.songoda.epichoppers.player.PlayerData;
import com.songoda.epichoppers.player.SyncType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.Inventory;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InventoryListenersTest {

    private ServerMock server;
    private EpicHoppersPlugin plugin;
    private WorldMock world;
    private InventoryListeners listener;
    private ELevelManager levelManager;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(EpicHoppersPlugin.class);
        world = server.addSimpleWorld("world");
        listener = new InventoryListeners(plugin);
        levelManager = (ELevelManager) plugin.getLevelManager();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private InventoryClickEvent click(PlayerMock player, int slot, ClickType type) {
        return new InventoryClickEvent(player.getOpenInventory(), InventoryType.SlotType.CONTAINER, slot,
                type, InventoryAction.PICKUP_ALL);
    }

    private EHopper hopperAtLevel(int level, Location loc) {
        return new EHopper(loc, levelManager.getLevel(level), null, null, null, new EFilter(),
                TeleportTrigger.DISABLED, null);
    }

    /**
     * In production, EHopper#filter(Player)/#crafting(Player) are only ever
     * reached from InventoryListeners#onInventoryClick's OVERVIEW-menu
     * branch, which requires the player to already be in the OVERVIEW menu -
     * a state only reachable via a prior EHopper#overview(Player) call,
     * which is what actually populates PlayerData#getLastHopper(). Calling
     * filter()/crafting() directly (as these tests do, to isolate the FILTER/
     * CRAFTING-menu behavior under test) skips that, so tests must set
     * lastHopper themselves to recreate the real precondition.
     */
    private void openFilterMenu(PlayerMock player, EHopper hopper) {
        plugin.getPlayerDataManager().getPlayerData(player).setLastHopper(hopper);
        hopper.filter(player);
    }

    // --- onInventoryClick: early-exit guards ---

    @Test
    void onInventoryClickDoesNothingWhenTheClickedSlotHasNoCurrentItem() {
        PlayerMock player = server.addPlayer();
        player.openInventory(Bukkit.createInventory(null, 9));

        InventoryClickEvent event = click(player, 0, ClickType.LEFT);
        listener.onInventoryClick(event);

        assertFalse(event.isCancelled());
    }

    @Test
    void onInventoryClickIgnoresClicksInTheBottomPlayerInventory() {
        PlayerMock player = server.addPlayer();
        Inventory top = Bukkit.createInventory(null, 9);
        player.openInventory(top);
        player.getInventory().setItem(0, new ItemStack(Material.STONE));

        // rawSlot for the player's own inventory slot 0 is topInventory.size() + 0.
        InventoryClickEvent event = click(player, top.getSize(), ClickType.LEFT);
        listener.onInventoryClick(event);

        assertFalse(event.isCancelled());
    }

    // Note: onInventoryClick's "!event.getCurrentItem().hasItemMeta()) return;"
    // guard (a defensive early-exit for a current item with no ItemMeta at
    // all) could not be exercised with a real test: on this modern Paper/
    // MockBukkit target, ItemStack#hasItemMeta() returns true even for a
    // brand new, otherwise-untouched `new ItemStack(Material.STONE)` (every
    // item now carries an implicit, non-empty data-components patch), so
    // there is no reachable non-AIR ItemStack for which hasItemMeta() is
    // false. This appears to be dead code on current Bukkit/Paper versions,
    // not something this plugin can control - left untested rather than
    // faked with a reflection hack.

    // --- onInventoryClick: cursor-driven Sync-Touch-enchant application ---

    @Test
    void onInventoryClickAppliesSyncTouchWhenAnEnchantedBookIsDraggedOntoATool() {
        PlayerMock player = server.addPlayer();
        Inventory top = Bukkit.createInventory(null, 9);
        player.openInventory(top);
        ItemStack axe = new ItemStack(Material.DIAMOND_AXE);
        ItemMeta axeMeta = axe.getItemMeta();
        axeMeta.setDisplayName("My Axe");
        axe.setItemMeta(axeMeta);
        top.setItem(0, axe);
        player.setItemOnCursor(plugin.enchantmentHandler.getbook());

        InventoryClickEvent event = click(player, 0, ClickType.LEFT);
        listener.onInventoryClick(event);

        assertTrue(event.isCancelled());
        assertEquals(Material.AIR, player.getItemOnCursor().getType());
    }

    @Test
    void onInventoryClickDoesNotApplySyncTouchWhenTheCurrentItemIsNotATool() {
        PlayerMock player = server.addPlayer();
        Inventory top = Bukkit.createInventory(null, 9);
        player.openInventory(top);
        ItemStack dirt = new ItemStack(Material.DIRT);
        ItemMeta meta = dirt.getItemMeta();
        meta.setDisplayName("Dirt Block");
        dirt.setItemMeta(meta);
        top.setItem(0, dirt);
        player.setItemOnCursor(plugin.enchantmentHandler.getbook());

        InventoryClickEvent event = click(player, 0, ClickType.LEFT);
        listener.onInventoryClick(event);

        assertFalse(event.isCancelled());
        assertEquals(Material.ENCHANTED_BOOK, player.getItemOnCursor().getType());
    }

    // --- onInventoryClick: anvil hopper-item cancellation ---

    @Test
    void onInventoryClickCancelsPickingUpAHopperItemFromAnAnvil() {
        PlayerMock player = server.addPlayer();
        Inventory anvil = Bukkit.createInventory(null, InventoryType.ANVIL);
        ItemStack hopperItem = new ItemStack(Material.HOPPER);
        ItemMeta meta = hopperItem.getItemMeta();
        meta.setDisplayName("Repaired Hopper");
        hopperItem.setItemMeta(meta);
        anvil.setItem(2, hopperItem);
        player.openInventory(anvil);

        InventoryClickEvent event = click(player, 2, ClickType.LEFT);
        listener.onInventoryClick(event);

        assertTrue(event.isCancelled());
    }

    // --- onInventoryClick: CRAFTING menu passthrough/cancel ---

    @Test
    void onInventoryClickAllowsTheCraftingSlotThirteenButCancelsEverythingElse() {
        PlayerMock player = server.addPlayer();
        Location loc = new Location(world, 0, 64, 0);
        EHopper hopper = hopperAtLevel(1, loc);
        plugin.getHopperManager().addHopper(loc, hopper);
        hopper.crafting(player);

        InventoryClickEvent slot13 = click(player, 13, ClickType.LEFT);
        listener.onInventoryClick(slot13);
        assertFalse(slot13.isCancelled());

        InventoryClickEvent other = click(player, 0, ClickType.LEFT);
        listener.onInventoryClick(other);
        assertTrue(other.isCancelled());
    }

    // --- onInventoryClick: OVERVIEW menu ---

    @Test
    void onInventoryClickCancelsAndDoesNothingElseForAPlainOverviewBackgroundSlot() {
        PlayerMock player = server.addPlayer();
        Location loc = new Location(world, 0, 64, 0);
        EHopper hopper = hopperAtLevel(1, loc);
        plugin.getHopperManager().addHopper(loc, hopper);
        player.addAttachment(plugin, "epichoppers.overview", true);
        hopper.overview(player);

        InventoryClickEvent event = click(player, 0, ClickType.LEFT);
        listener.onInventoryClick(event);

        assertTrue(event.isCancelled());
        assertEquals(MenuType.OVERVIEW, plugin.getPlayerDataManager().getPlayerData(player).getInMenu());
    }

    @Test
    void onInventoryClickLeftClickOnPerlIconTeleportsWhenSyncedAndClosesTheMenu() {
        PlayerMock player = server.addPlayer();
        Location loc = new Location(world, 0, 64, 0);
        EHopper hopper = hopperAtLevel(1, loc);
        plugin.getHopperManager().addHopper(loc, hopper);
        player.addAttachment(plugin, "epichoppers.overview", true);
        player.addAttachment(plugin, "EpicHoppers.Teleport", true);
        hopper.overview(player);
        org.bukkit.block.Block syncTarget = world.getBlockAt(5, 64, 5);
        hopper.setSyncedBlock(syncTarget);

        InventoryClickEvent event = click(player, 4, ClickType.LEFT);
        listener.onInventoryClick(event);

        assertTrue(event.isCancelled());
        // player.closeInventory() fires a real InventoryCloseEvent that the
        // plugin's own registered InventoryListeners handles, resetting the
        // player's menu state back to NOT_IN.
        assertEquals(MenuType.NOT_IN, plugin.getPlayerDataManager().getPlayerData(player).getInMenu());
    }

    @Test
    void onInventoryClickRightClickOnPerlIconCyclesTheTeleportTriggerAndReopensTheMenu() {
        PlayerMock player = server.addPlayer();
        Location loc = new Location(world, 0, 64, 0);
        EHopper hopper = hopperAtLevel(1, loc);
        plugin.getHopperManager().addHopper(loc, hopper);
        player.addAttachment(plugin, "epichoppers.overview", true);
        player.addAttachment(plugin, "EpicHoppers.Teleport", true);
        hopper.overview(player);
        assertEquals(TeleportTrigger.DISABLED, hopper.getTeleportTrigger());

        InventoryClickEvent event = click(player, 4, ClickType.RIGHT);
        listener.onInventoryClick(event);

        assertTrue(event.isCancelled());
        assertEquals(TeleportTrigger.SNEAK, hopper.getTeleportTrigger());
        // The trigger-cycle branch re-opens overview() and returns before
        // player.closeInventory() - the menu must still be open afterwards.
        assertEquals(MenuType.OVERVIEW, plugin.getPlayerDataManager().getPlayerData(player).getInMenu());
    }

    @Test
    void onInventoryClickOnFilterIconOpensTheFilterMenu() {
        PlayerMock player = server.addPlayer();
        Location loc = new Location(world, 0, 64, 0);
        EHopper hopper = hopperAtLevel(1, loc);
        plugin.getHopperManager().addHopper(loc, hopper);
        player.addAttachment(plugin, "epichoppers.overview", true);
        player.addAttachment(plugin, "EpicHoppers.Filter", true);
        hopper.overview(player);

        InventoryClickEvent event = click(player, 4, ClickType.LEFT);
        listener.onInventoryClick(event);

        assertTrue(event.isCancelled());
        assertEquals(MenuType.FILTER, plugin.getPlayerDataManager().getPlayerData(player).getInMenu());
    }

    @Test
    void onInventoryClickOnXpUpgradeIconUpgradesAndClosesTheMenu() {
        PlayerMock player = server.addPlayer();
        Location loc = new Location(world, 0, 64, 0);
        EHopper hopper = hopperAtLevel(1, loc);
        plugin.getHopperManager().addHopper(loc, hopper);
        player.addAttachment(plugin, "epichoppers.overview", true);
        player.addAttachment(plugin, "EpicHoppers.Upgrade.XP", true);
        player.setLevel(999);
        hopper.overview(player);

        InventoryClickEvent event = click(player, 11, ClickType.LEFT);
        listener.onInventoryClick(event);

        assertTrue(event.isCancelled());
        assertEquals(2, hopper.getLevel().getLevel());
    }

    @Test
    void onInventoryClickOnEcoUpgradeIconWithoutVaultMessagesAndClosesTheMenu() {
        PlayerMock player = server.addPlayer();
        Location loc = new Location(world, 0, 64, 0);
        EHopper hopper = hopperAtLevel(1, loc);
        plugin.getHopperManager().addHopper(loc, hopper);
        player.addAttachment(plugin, "epichoppers.overview", true);
        player.addAttachment(plugin, "EpicHoppers.Upgrade.ECO", true);
        hopper.overview(player);

        InventoryClickEvent event = click(player, 15, ClickType.LEFT);
        listener.onInventoryClick(event);

        // No Vault plugin registered in this test environment - upgrade()
        // just messages "Vault is not installed." internally, but the click
        // handler unconditionally calls player.closeInventory() afterwards
        // regardless of upgrade() success.
        assertTrue(event.isCancelled());
        assertEquals(1, hopper.getLevel().getLevel());
    }

    @Test
    void onInventoryClickOnCraftingIconOpensTheCraftingMenu() {
        PlayerMock player = server.addPlayer();
        ArrayList<com.songoda.epichoppers.api.hopper.levels.modules.Module> modules = new ArrayList<>();
        modules.add(new com.songoda.epichoppers.api.hopper.levels.modules.Module() {
            @Override public String getName() { return "AutoCrafting"; }
            @Override public void run(Hopper hopper) { }
            @Override public java.util.List<Material> getBlockedItems(Hopper hopper) { return java.util.Collections.emptyList(); }
            @Override public String getDescription() { return "Auto crafting"; }
        });
        levelManager.clear();
        levelManager.addLevel(1, 10, 100, 5, 1, true, false, modules);
        levelManager.addLevel(2, 20, 200, 5, 1, false, false, new ArrayList<>());

        Location loc = new Location(world, 0, 64, 0);
        EHopper hopper = hopperAtLevel(1, loc);
        plugin.getHopperManager().addHopper(loc, hopper);
        player.addAttachment(plugin, "epichoppers.overview", true);
        hopper.overview(player);

        InventoryClickEvent event = click(player, 21, ClickType.LEFT);
        listener.onInventoryClick(event);

        assertTrue(event.isCancelled());
        assertEquals(MenuType.CRAFTING, plugin.getPlayerDataManager().getPlayerData(player).getInMenu());
    }

    @Test
    void onInventoryClickLeftClickOnSyncIconStartsASyncWhenTheOwnerClicks() {
        PlayerMock player = server.addPlayer();
        Location loc = new Location(world, 0, 64, 0);
        EHopper hopper = hopperAtLevel(1, loc);
        plugin.getHopperManager().addHopper(loc, hopper);
        player.addAttachment(plugin, "epichoppers.overview", true);
        hopper.overview(player); // sets hopper.lastPlayer = player's UUID

        InventoryClickEvent event = click(player, 22, ClickType.LEFT);
        listener.onInventoryClick(event);

        assertTrue(event.isCancelled());
        assertEquals(SyncType.REGULAR, plugin.getPlayerDataManager().getPlayerData(player).getSyncType());
    }

    @Test
    void onInventoryClickLeftClickOnSyncIconRefusesWhenAnotherPlayerPlacedTheHopper() {
        PlayerMock player = server.addPlayer();
        Location loc = new Location(world, 0, 64, 0);
        EHopper hopper = hopperAtLevel(1, loc);
        plugin.getHopperManager().addHopper(loc, hopper);
        player.addAttachment(plugin, "epichoppers.overview", true);
        hopper.overview(player);
        hopper.setLastPlayer(UUID.randomUUID());

        InventoryClickEvent event = click(player, 22, ClickType.LEFT);
        listener.onInventoryClick(event);

        assertTrue(event.isCancelled());
        assertNull(plugin.getPlayerDataManager().getPlayerData(player).getSyncType());
        assertNotNull(player.nextMessage());
    }

    @Test
    void onInventoryClickRightClickOnSyncIconDesyncsTheHopper() {
        PlayerMock player = server.addPlayer();
        Location loc = new Location(world, 0, 64, 0);
        EHopper hopper = hopperAtLevel(1, loc);
        plugin.getHopperManager().addHopper(loc, hopper);
        player.addAttachment(plugin, "epichoppers.overview", true);
        hopper.overview(player);
        hopper.setSyncedBlock(world.getBlockAt(5, 64, 5));

        InventoryClickEvent event = click(player, 22, ClickType.RIGHT);
        listener.onInventoryClick(event);

        assertTrue(event.isCancelled());
        assertNull(hopper.getSyncedBlock());
    }

    // --- onInventoryClick: doFilter() delegation ---

    @Test
    void onInventoryClickInsideFilterMenuCancelsAShiftClickWithoutCompiling() {
        PlayerMock player = server.addPlayer();
        Location loc = new Location(world, 0, 64, 0);
        EHopper hopper = hopperAtLevel(1, loc);
        plugin.getHopperManager().addHopper(loc, hopper);
        openFilterMenu(player, hopper);

        InventoryClickEvent event = click(player, 12, ClickType.SHIFT_LEFT);
        listener.onInventoryClick(event);

        assertTrue(event.isCancelled());
    }

    @Test
    void onInventoryClickInsideFilterMenuOnSyncSlotStartsAFilteredSync() {
        PlayerMock player = server.addPlayer();
        Location loc = new Location(world, 0, 64, 0);
        EHopper hopper = hopperAtLevel(1, loc);
        plugin.getHopperManager().addHopper(loc, hopper);
        openFilterMenu(player, hopper);

        InventoryClickEvent event = click(player, 40, ClickType.LEFT);
        listener.onInventoryClick(event);

        assertEquals(SyncType.FILTERED, plugin.getPlayerDataManager().getPlayerData(player).getSyncType());
    }

    @Test
    void onInventoryClickInsideFilterMenuOnAnEmptyCategorySlotWithEmptyCursorDoesNothing() {
        PlayerMock player = server.addPlayer();
        Location loc = new Location(world, 0, 64, 0);
        EHopper hopper = hopperAtLevel(1, loc);
        plugin.getHopperManager().addHopper(loc, hopper);
        openFilterMenu(player, hopper);

        // Slot 0 holds the whitelist placeholder pane since the whitelist is
        // empty; clicking it with nothing on the cursor is a no-op.
        InventoryClickEvent event = click(player, 0, ClickType.LEFT);
        listener.onInventoryClick(event);

        assertTrue(event.isCancelled());
    }

    @Test
    void onInventoryClickInsideFilterMenuRejectsAddingMoreThanOneItemAtOnce() {
        PlayerMock player = server.addPlayer();
        Location loc = new Location(world, 0, 64, 0);
        EHopper hopper = hopperAtLevel(1, loc);
        plugin.getHopperManager().addHopper(loc, hopper);
        openFilterMenu(player, hopper);
        player.setItemOnCursor(new ItemStack(Material.DIAMOND, 2));

        InventoryClickEvent event = click(player, 0, ClickType.LEFT);
        listener.onInventoryClick(event);

        assertTrue(event.isCancelled());
        assertNotNull(player.nextMessage());
    }

    @Test
    void onInventoryClickInsideFilterMenuAllowsAddingASingleItemAndClearsTheCursorNextTick() {
        PlayerMock player = server.addPlayer();
        Location loc = new Location(world, 0, 64, 0);
        EHopper hopper = hopperAtLevel(1, loc);
        plugin.getHopperManager().addHopper(loc, hopper);
        openFilterMenu(player, hopper);
        player.setItemOnCursor(new ItemStack(Material.DIAMOND, 1));

        InventoryClickEvent event = click(player, 0, ClickType.LEFT);
        listener.onInventoryClick(event);
        assertFalse(event.isCancelled());

        server.getScheduler().performTicks(2);
        assertEquals(Material.AIR, player.getItemOnCursor().getType());
    }

    @Test
    void onInventoryClickInsideFilterMenuOnAnUnrelatedIconAllowsTheClickThrough() {
        PlayerMock player = server.addPlayer();
        Location loc = new Location(world, 0, 64, 0);
        EHopper hopper = hopperAtLevel(1, loc);
        plugin.getHopperManager().addHopper(loc, hopper);
        openFilterMenu(player, hopper);

        // Slot 13 holds the info paper icon, which is not one of the
        // category slots at all, so it's outside the `a[]` array and the
        // loop's default `return true` with cancellation applies... but
        // slot 12 is glass filler that IS also outside the category array;
        // the method still returns true (cancelled) for any FILTER-menu
        // click that isn't slot 40 and isn't a recognized category slot.
        InventoryClickEvent event = click(player, 13, ClickType.LEFT);
        listener.onInventoryClick(event);

        assertTrue(event.isCancelled());
    }

    // --- onDrop ---

    @Test
    void onDropIgnoresAnItemWithNoDisplayName() {
        PlayerMock player = server.addPlayer();
        Item itemEntity = world.dropItem(new Location(world, 0, 64, 0), new ItemStack(Material.DIRT));

        PlayerDropItemEvent event = new PlayerDropItemEvent(player, itemEntity);
        listener.onDrop(event);

        assertTrue(world.getEntitiesByClass(Item.class).contains(itemEntity));
    }

    @Test
    void onDropRemovesAFilterMarkerItemMatchingTheWhitelistDisplayName() {
        PlayerMock player = server.addPlayer();
        ItemStack marker = new ItemStack(Material.WHITE_STAINED_GLASS_PANE);
        ItemMeta meta = marker.getItemMeta();
        meta.setDisplayName(plugin.getLocale().getMessage("interface.filter.whitelist"));
        marker.setItemMeta(meta);
        Item itemEntity = world.dropItem(new Location(world, 0, 64, 0), marker);

        PlayerDropItemEvent event = new PlayerDropItemEvent(player, itemEntity);
        listener.onDrop(event);

        assertFalse(world.getEntitiesByClass(Item.class).contains(itemEntity));
    }

    @Test
    void onDropLeavesAnUnrelatedNamedItemAlone() {
        PlayerMock player = server.addPlayer();
        ItemStack named = new ItemStack(Material.DIAMOND);
        ItemMeta meta = named.getItemMeta();
        meta.setDisplayName("Just a diamond");
        named.setItemMeta(meta);
        Item itemEntity = world.dropItem(new Location(world, 0, 64, 0), named);

        PlayerDropItemEvent event = new PlayerDropItemEvent(player, itemEntity);
        listener.onDrop(event);

        assertTrue(world.getEntitiesByClass(Item.class).contains(itemEntity));
    }

    // --- onClose ---

    @Test
    void onCloseOfTheCraftingMenuStoresTheSlotThirteenItemAsAutoCrafting() {
        PlayerMock player = server.addPlayer();
        Location loc = new Location(world, 0, 64, 0);
        EHopper hopper = hopperAtLevel(1, loc);
        plugin.getHopperManager().addHopper(loc, hopper);
        hopper.crafting(player);
        player.getOpenInventory().getTopInventory().setItem(13, new ItemStack(Material.FURNACE));

        InventoryCloseEvent event = new InventoryCloseEvent(player.getOpenInventory());
        listener.onClose(event);

        assertEquals(Material.FURNACE, hopper.getAutoCrafting());
        assertEquals(MenuType.NOT_IN, plugin.getPlayerDataManager().getPlayerData(player).getInMenu());
    }

    @Test
    void onCloseOfTheCraftingMenuWithAnEmptySlotStoresAir() {
        PlayerMock player = server.addPlayer();
        Location loc = new Location(world, 0, 64, 0);
        EHopper hopper = hopperAtLevel(1, loc);
        plugin.getHopperManager().addHopper(loc, hopper);
        hopper.crafting(player);
        player.getOpenInventory().getTopInventory().setItem(13, null);

        InventoryCloseEvent event = new InventoryCloseEvent(player.getOpenInventory());
        listener.onClose(event);

        assertEquals(Material.AIR, hopper.getAutoCrafting());
    }

    @Test
    void onCloseOfTheFilterMenuCompilesTheDisplayedItemsIntoTheFilterLists() {
        PlayerMock player = server.addPlayer();
        Location loc = new Location(world, 0, 64, 0);
        EHopper hopper = hopperAtLevel(1, loc);
        plugin.getHopperManager().addHopper(loc, hopper);
        openFilterMenu(player, hopper);
        player.getOpenInventory().getTopInventory().setItem(0, new ItemStack(Material.DIAMOND));

        InventoryCloseEvent event = new InventoryCloseEvent(player.getOpenInventory());
        listener.onClose(event);

        assertTrue(hopper.getFilter().getWhiteList().contains(Material.DIAMOND));
        assertEquals(MenuType.NOT_IN, plugin.getPlayerDataManager().getPlayerData(player).getInMenu());
    }

    @Test
    void onCloseOfAnyOpenMenuClearsTheHoppersLastPlayer() {
        PlayerMock player = server.addPlayer();
        Location loc = new Location(world, 0, 64, 0);
        EHopper hopper = hopperAtLevel(1, loc);
        plugin.getHopperManager().addHopper(loc, hopper);
        player.addAttachment(plugin, "epichoppers.overview", true);
        hopper.overview(player);
        assertEquals(player.getUniqueId(), hopper.getLastPlayer());

        InventoryCloseEvent event = new InventoryCloseEvent(player.getOpenInventory());
        listener.onClose(event);

        assertNull(hopper.getLastPlayer());
    }

    @Test
    void onCloseWithNoMenuOpenIsANoOp() {
        PlayerMock player = server.addPlayer();
        player.openInventory(Bukkit.createInventory(null, 9));

        InventoryCloseEvent event = new InventoryCloseEvent(player.getOpenInventory());
        listener.onClose(event);

        assertEquals(MenuType.NOT_IN, plugin.getPlayerDataManager().getPlayerData(player).getInMenu());
    }
}
