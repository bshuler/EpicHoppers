package com.songoda.epichoppers.hopper;

import com.songoda.epichoppers.EpicHoppersPlugin;
import com.songoda.epichoppers.api.hopper.Hopper;
import com.songoda.epichoppers.api.hopper.TeleportTrigger;
import com.songoda.epichoppers.api.hopper.levels.Level;
import com.songoda.epichoppers.api.hopper.levels.modules.Module;
import com.songoda.epichoppers.hopper.levels.ELevelManager;
import com.songoda.epichoppers.player.MenuType;
import com.songoda.epichoppers.player.PlayerData;
import com.songoda.epichoppers.player.SyncType;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * EHopper.upgrade/upgradeFinal/sync/compile/timeout all route through
 * EpicHoppersPlugin.getInstance().getLevelManager() (the real, singleton
 * manager bound to the loaded plugin), so these tests reset and repopulate
 * that manager directly rather than trying to inject a fake one - there is
 * no seam for that in the source.
 *
 * The GUI-building methods (overview/crafting/filter) are intentionally not
 * covered here: they are almost entirely hardcoded Inventory/ItemStack
 * layout code with no decision logic beyond permission-gated slot choices,
 * and are exercised (for the permission-gated branches) by
 * InventoryListenersTest / InteractListenersTest instead where the GUI is
 * reached through its real trigger path.
 */
class EHopperTest {

    private ServerMock server;
    private EpicHoppersPlugin plugin;
    private ELevelManager levelManager;
    private WorldMock world;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(EpicHoppersPlugin.class);
        world = server.addSimpleWorld("hopper-world");

        levelManager = (ELevelManager) plugin.getLevelManager();
        levelManager.clear();
        levelManager.addLevel(1, 10, 100, 5, 1, false, false, new ArrayList<>());
        levelManager.addLevel(2, 20, 200, 5, 1, false, false, new ArrayList<>());
        levelManager.addLevel(3, 30, 300, 5, 1, false, false, new ArrayList<>());
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private EHopper hopperAtLevel(int levelNumber, Location location) {
        Level level = levelManager.getLevel(levelNumber);
        return new EHopper(location, level, null, null, null, new EFilter(),
                TeleportTrigger.DISABLED, null);
    }

    // ---- upgrade() ----

    @Test
    void upgradeWithXpSucceedsAndDeductsLevelsWhenPlayerHasEnough() {
        PlayerMock player = server.addPlayer();
        player.setLevel(50);
        Location loc = new Location(world, 0, 64, 0);
        EHopper hopper = hopperAtLevel(1, loc);

        hopper.upgrade("XP", player);

        assertEquals(2, hopper.getLevel().getLevel());
        assertEquals(30, player.getLevel()); // level 2's costExperience is 20: 50 - 20 = 30
        assertNotNull(player.nextMessage());
    }

    @Test
    void upgradeWithXpFailsAndSendsCannotAffordWhenPlayerLacksLevels() {
        PlayerMock player = server.addPlayer();
        player.setLevel(1);
        Location loc = new Location(world, 0, 64, 0);
        EHopper hopper = hopperAtLevel(1, loc);

        hopper.upgrade("XP", player);

        assertEquals(1, hopper.getLevel().getLevel());
        assertEquals(1, player.getLevel());
        assertNotNull(player.nextMessage());
    }

    @Test
    void upgradeWithXpInCreativeModeBypassesTheLevelCostButDoesNotDeductLevels() {
        PlayerMock player = server.addPlayer();
        player.setGameMode(GameMode.CREATIVE);
        player.setLevel(0);
        Location loc = new Location(world, 0, 64, 0);
        EHopper hopper = hopperAtLevel(1, loc);

        hopper.upgrade("XP", player);

        assertEquals(2, hopper.getLevel().getLevel());
        assertEquals(0, player.getLevel());
    }

    @Test
    void upgradeWithEcoSendsVaultNotInstalledWhenVaultIsAbsent() {
        PlayerMock player = server.addPlayer();
        Location loc = new Location(world, 0, 64, 0);
        EHopper hopper = hopperAtLevel(1, loc);

        hopper.upgrade("ECO", player);

        assertEquals(1, hopper.getLevel().getLevel());
        assertEquals("Vault is not installed.", player.nextMessage());
    }

    @Test
    void upgradeIsANoOpWhenAlreadyAtTheHighestRegisteredLevel() {
        PlayerMock player = server.addPlayer();
        player.setLevel(999);
        Location loc = new Location(world, 0, 64, 0);
        EHopper hopper = hopperAtLevel(3, loc);

        hopper.upgrade("XP", player);

        assertEquals(3, hopper.getLevel().getLevel());
        assertEquals(999, player.getLevel());
        assertNull(player.nextMessage());
    }

    // ---- upgradeFinal() ----

    @Test
    void upgradeFinalSendsSuccessMessageWhenNotYetAtHighestLevel() {
        PlayerMock player = server.addPlayer();
        Location loc = new Location(world, 0, 64, 0);
        EHopper hopper = hopperAtLevel(1, loc);

        hopper.upgradeFinal(levelManager.getLevel(2), player);

        assertSame(levelManager.getLevel(2), hopper.getLevel());
        assertNotNull(player.nextMessage());
    }

    @Test
    void upgradeFinalSendsMaxedMessageWhenReachingTheHighestLevel() {
        PlayerMock player = server.addPlayer();
        Location loc = new Location(world, 0, 64, 0);
        EHopper hopper = hopperAtLevel(2, loc);

        hopper.upgradeFinal(levelManager.getLevel(3), player);

        assertSame(levelManager.getLevel(3), hopper.getLevel());
        assertNotNull(player.nextMessage());
    }

    // ---- sync() ----

    @Test
    void syncSucceedsAndSetsSyncedBlockWhenWithinRange() {
        PlayerMock player = server.addPlayer();
        Location loc = new Location(world, 0, 64, 0);
        EHopper hopper = hopperAtLevel(1, loc);
        Block target = world.getBlockAt(2, 64, 0);

        hopper.sync(target, false, player);

        assertSame(target, hopper.getSyncedBlock());
        assertEquals(player.getUniqueId(), hopper.getLastPlayer());
        assertNotNull(player.nextMessage());
    }

    @Test
    void syncFailsAndSendsOutOfRangeWhenBeyondLevelRange() {
        PlayerMock player = server.addPlayer();
        Location loc = new Location(world, 0, 64, 0);
        EHopper hopper = hopperAtLevel(1, loc); // range == 5
        Block target = world.getBlockAt(50, 64, 0);

        hopper.sync(target, false, player);

        assertNull(hopper.getSyncedBlock());
        assertNotNull(player.nextMessage());
    }

    @Test
    void syncBypassesRangeCheckWhenPlayerHasOverridePermission() {
        PlayerMock player = server.addPlayer();
        player.addAttachment(plugin, "EpicHoppers.Override", true);
        Location loc = new Location(world, 0, 64, 0);
        EHopper hopper = hopperAtLevel(1, loc);
        Block target = world.getBlockAt(50, 64, 0);

        hopper.sync(target, false, player);

        assertSame(target, hopper.getSyncedBlock());
    }

    @Test
    void syncSetsFilterEndPointInsteadOfSyncedBlockWhenFiltered() {
        PlayerMock player = server.addPlayer();
        Location loc = new Location(world, 0, 64, 0);
        EFilter filter = new EFilter();
        EHopper hopper = new EHopper(loc, levelManager.getLevel(1), null, null, null, filter,
                TeleportTrigger.DISABLED, null);
        Block target = world.getBlockAt(1, 64, 0);

        hopper.sync(target, true, player);

        assertNull(hopper.getSyncedBlock());
        assertSame(target, filter.getEndPoint());
    }

    // ---- compile() ----

    @Test
    void compileReadsFilterSlotsFromThePlayersOpenTopInventoryAndSkipsGlassAndAirAndBarrier() {
        PlayerMock player = server.addPlayer();
        Location loc = new Location(world, 0, 64, 0);
        EFilter filter = new EFilter();
        EHopper hopper = new EHopper(loc, levelManager.getLevel(1), null, null, null, filter,
                TeleportTrigger.DISABLED, null);

        org.bukkit.inventory.Inventory inv = server.createInventory(null, 54);
        inv.setItem(0, new org.bukkit.inventory.ItemStack(Material.DIAMOND));
        inv.setItem(1, new org.bukkit.inventory.ItemStack(Material.WHITE_STAINED_GLASS_PANE)); // skipped
        inv.setItem(27, new org.bukkit.inventory.ItemStack(Material.COAL));
        inv.setItem(7, new org.bukkit.inventory.ItemStack(Material.BARRIER)); // skipped (sentinel)
        inv.setItem(8, new org.bukkit.inventory.ItemStack(Material.EMERALD));
        player.openInventory(inv);

        hopper.compile(player);

        assertEquals(java.util.List.of(Material.DIAMOND), filter.getWhiteList());
        assertEquals(java.util.List.of(Material.COAL), filter.getBlackList());
        assertEquals(java.util.List.of(Material.EMERALD), filter.getVoidList());
    }

    @Test
    void compileProducesEmptyListsWhenTheTopInventoryHasNoRelevantItems() {
        PlayerMock player = server.addPlayer();
        Location loc = new Location(world, 0, 64, 0);
        EFilter filter = new EFilter();
        EHopper hopper = new EHopper(loc, levelManager.getLevel(1), null, null, null, filter,
                TeleportTrigger.DISABLED, null);

        org.bukkit.inventory.Inventory inv = server.createInventory(null, 54);
        player.openInventory(inv);

        hopper.compile(player);

        assertTrue(filter.getWhiteList().isEmpty());
        assertTrue(filter.getBlackList().isEmpty());
        assertTrue(filter.getVoidList().isEmpty());
    }

    // ---- timeout() ----

    @Test
    void timeoutClearsSyncTypeAndNotifiesPlayerAfterTheConfiguredDelay() {
        PlayerMock player = server.addPlayer();
        Location loc = new Location(world, 0, 64, 0);
        EHopper hopper = hopperAtLevel(1, loc);

        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        data.setSyncType(SyncType.FILTERED);

        hopper.timeout(player);
        long delay = plugin.getConfig().getLong("Main.Timeout When Syncing Hoppers");
        server.getScheduler().performTicks(delay + 1);

        assertNull(data.getSyncType());
        assertNotNull(player.nextMessage());
    }

    // ---- plain getters/setters ----

    @Test
    void locationXyzGetLocationAndPlainAccessorsRoundTrip() {
        Location loc = new Location(world, 3, 4, 5);
        EHopper hopper = hopperAtLevel(1, loc);

        assertEquals(3, hopper.getX());
        assertEquals(4, hopper.getY());
        assertEquals(5, hopper.getZ());
        assertEquals(loc, hopper.getLocation());
        assertFalse(hopper.getLocation() == loc); // getLocation() returns a clone

        UUID uuid = UUID.randomUUID();
        hopper.setLastPlayer(uuid);
        assertEquals(uuid, hopper.getLastPlayer());

        hopper.setAutoCrafting(Material.FURNACE);
        assertEquals(Material.FURNACE, hopper.getAutoCrafting());

        hopper.setTeleportTrigger(TeleportTrigger.SNEAK);
        assertEquals(TeleportTrigger.SNEAK, hopper.getTeleportTrigger());

        Block b = world.getBlockAt(9, 9, 9);
        hopper.setSyncedBlock(b);
        assertSame(b, hopper.getSyncedBlock());
    }

    @Test
    void blockConstructorDelegatesToLocationConstructor() {
        Block block = world.getBlockAt(1, 2, 3);
        EHopper hopper = new EHopper(block, levelManager.getLevel(1), null, null, null,
                new EFilter(), TeleportTrigger.DISABLED, null);

        assertEquals(block.getLocation(), hopper.getLocation());
    }

    @Test
    void getHopperCastsTheUnderlyingBlockStateWhenItIsActuallyAHopper() {
        Block block = world.getBlockAt(11, 64, 11);
        block.setType(Material.HOPPER);
        EHopper hopper = new EHopper(block, levelManager.getLevel(1), null, null, null,
                new EFilter(), TeleportTrigger.DISABLED, null);

        org.bukkit.block.Hopper state = hopper.getHopper();
        assertNotNull(state);
    }

    // ---- overview() ----

    private static final Module AUTO_CRAFTING_MODULE = new Module() {
        @Override public String getName() { return "AutoCrafting"; }
        @Override public void run(Hopper hopper) { }
        @Override public List<Material> getBlockedItems(Hopper hopper) { return Collections.emptyList(); }
        @Override public String getDescription() { return "Auto crafting"; }
    };

    @Test
    void overviewIsANoOpWithoutThePermission() {
        PlayerMock player = server.addPlayer();
        Location loc = new Location(world, 0, 64, 0);
        EHopper hopper = hopperAtLevel(1, loc);

        hopper.overview(player);

        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        assertEquals(MenuType.NOT_IN, data.getInMenu());
    }

    @Test
    void overviewOpensA27SlotInventoryAndTracksMenuStateWhenPermitted() {
        PlayerMock player = server.addPlayer();
        player.addAttachment(plugin, "epichoppers.overview", true);
        Location loc = new Location(world, 0, 64, 0);
        EHopper hopper = hopperAtLevel(1, loc);

        hopper.overview(player);

        assertEquals(27, player.getOpenInventory().getTopInventory().getSize());
        assertEquals(MenuType.OVERVIEW, plugin.getPlayerDataManager().getPlayerData(player).getInMenu());
        assertEquals(player.getUniqueId(), hopper.getLastPlayer());
        assertEquals(player.getUniqueId(), hopper.getPlacedBy());
    }

    @Test
    void overviewShowsAlreadyMaxedWhenAtTheHighestLevel() {
        PlayerMock player = server.addPlayer();
        player.addAttachment(plugin, "epichoppers.overview", true);
        Location loc = new Location(world, 0, 64, 0);
        EHopper hopper = hopperAtLevel(3, loc);

        hopper.overview(player);

        assertEquals(27, player.getOpenInventory().getTopInventory().getSize());
    }

    @Test
    void overviewShowsOnlyTeleportIconWhenTeleportEnabledAndFilterDisabled() {
        levelManager.clear();
        levelManager.addLevel(1, 10, 100, 5, 1, false, true, new ArrayList<>());
        levelManager.addLevel(2, 20, 200, 5, 1, false, false, new ArrayList<>());

        PlayerMock player = server.addPlayer();
        player.addAttachment(plugin, "epichoppers.overview", true);
        Location loc = new Location(world, 0, 64, 0);
        EHopper hopper = hopperAtLevel(1, loc);

        hopper.overview(player);

        org.bukkit.inventory.Inventory inv = player.getOpenInventory().getTopInventory();
        assertEquals(Material.ENDER_PEARL, inv.getItem(4).getType());
    }

    @Test
    void overviewShowsOnlyFilterIconWhenFilterEnabledAndTeleportDisabled() {
        levelManager.clear();
        levelManager.addLevel(1, 10, 100, 5, 1, true, false, new ArrayList<>());
        levelManager.addLevel(2, 20, 200, 5, 1, false, false, new ArrayList<>());

        PlayerMock player = server.addPlayer();
        player.addAttachment(plugin, "epichoppers.overview", true);
        Location loc = new Location(world, 0, 64, 0);
        EHopper hopper = hopperAtLevel(1, loc);

        hopper.overview(player);

        org.bukkit.inventory.Inventory inv = player.getOpenInventory().getTopInventory();
        assertEquals(Material.COMPARATOR, inv.getItem(4).getType());
    }

    @Test
    void overviewShowsBothTeleportAndFilterIconsInSeparateSlotsWhenBothEnabled() {
        levelManager.clear();
        levelManager.addLevel(1, 10, 100, 5, 1, true, true, new ArrayList<>());
        levelManager.addLevel(2, 20, 200, 5, 1, false, false, new ArrayList<>());

        PlayerMock player = server.addPlayer();
        player.addAttachment(plugin, "epichoppers.overview", true);
        Location loc = new Location(world, 0, 64, 0);
        EHopper hopper = hopperAtLevel(1, loc);

        hopper.overview(player);

        org.bukkit.inventory.Inventory inv = player.getOpenInventory().getTopInventory();
        assertEquals(Material.ENDER_PEARL, inv.getItem(3).getType());
        assertEquals(Material.COMPARATOR, inv.getItem(5).getType());
    }

    @Test
    void overviewShowsCraftingIconWhenAnAutoCraftingModuleAndFilterAreBothEnabled() {
        levelManager.clear();
        ArrayList<Module> modules = new ArrayList<>();
        modules.add(AUTO_CRAFTING_MODULE);
        levelManager.addLevel(1, 10, 100, 5, 1, true, false, modules);
        levelManager.addLevel(2, 20, 200, 5, 1, false, false, new ArrayList<>());

        PlayerMock player = server.addPlayer();
        player.addAttachment(plugin, "epichoppers.overview", true);
        Location loc = new Location(world, 0, 64, 0);
        EHopper hopper = hopperAtLevel(1, loc);

        hopper.overview(player);

        org.bukkit.inventory.Inventory inv = player.getOpenInventory().getTopInventory();
        assertEquals(Material.CRAFTING_TABLE, inv.getItem(21).getType());
        assertEquals(Material.TRIPWIRE_HOOK, inv.getItem(23).getType());
    }

    @Test
    void overviewShowsXpAndEconomyUpgradeIconsWhenConfiguredAndPermitted() {
        PlayerMock player = server.addPlayer();
        player.addAttachment(plugin, "epichoppers.overview", true);
        player.addAttachment(plugin, "EpicHoppers.Upgrade.XP", true);
        player.addAttachment(plugin, "EpicHoppers.Upgrade.ECO", true);
        Location loc = new Location(world, 0, 64, 0);
        EHopper hopper = hopperAtLevel(1, loc);

        hopper.overview(player);

        org.bukkit.inventory.Inventory inv = player.getOpenInventory().getTopInventory();
        assertEquals(Material.EXPERIENCE_BOTTLE, inv.getItem(11).getType());
        assertEquals(Material.SUNFLOWER, inv.getItem(15).getType());
    }

    @Test
    void overviewIncludesBoostedStatsLoreWhenTheOwnerHasAnActiveBoost() {
        PlayerMock player = server.addPlayer();
        player.addAttachment(plugin, "epichoppers.overview", true);
        Location loc = new Location(world, 0, 64, 0);
        EHopper hopper = hopperAtLevel(1, loc);
        hopper.overview(player); // placedBy becomes this player's UUID

        plugin.getBoostManager().addBoostToPlayer(new com.songoda.epichoppers.boost.BoostData(
                2, System.currentTimeMillis() + 60_000, player.getUniqueId()));

        hopper.overview(player);

        org.bukkit.inventory.Inventory inv = player.getOpenInventory().getTopInventory();
        List<String> lore = inv.getItem(13).getItemMeta().getLore();
        assertTrue(lore.stream().anyMatch(line -> line.contains("2")));
    }

    // ---- crafting() ----

    @Test
    void craftingOpensA27SlotInventoryShowingAirWhenNoAutoCraftingMaterialIsSet() {
        PlayerMock player = server.addPlayer();
        Location loc = new Location(world, 0, 64, 0);
        EHopper hopper = hopperAtLevel(1, loc);

        hopper.crafting(player);

        org.bukkit.inventory.Inventory inv = player.getOpenInventory().getTopInventory();
        assertEquals(27, inv.getSize());
        assertEquals(Material.AIR, inv.getItem(13).getType());
        assertEquals(MenuType.CRAFTING, plugin.getPlayerDataManager().getPlayerData(player).getInMenu());
    }

    @Test
    void craftingShowsTheConfiguredAutoCraftingMaterial() {
        PlayerMock player = server.addPlayer();
        Location loc = new Location(world, 0, 64, 0);
        EHopper hopper = new EHopper(loc, levelManager.getLevel(1), null, null, null,
                new EFilter(), TeleportTrigger.DISABLED, Material.FURNACE);

        hopper.crafting(player);

        org.bukkit.inventory.Inventory inv = player.getOpenInventory().getTopInventory();
        assertEquals(Material.FURNACE, inv.getItem(13).getType());
    }

    // ---- filter() ----

    @Test
    void filterOpensA54SlotInventoryWithEmptyListsShowingOnlyPlaceholders() {
        PlayerMock player = server.addPlayer();
        Location loc = new Location(world, 0, 64, 0);
        EHopper hopper = hopperAtLevel(1, loc);

        hopper.filter(player);

        org.bukkit.inventory.Inventory inv = player.getOpenInventory().getTopInventory();
        assertEquals(54, inv.getSize());
        assertEquals(Material.WHITE_STAINED_GLASS_PANE, inv.getItem(0).getType());
        assertEquals(Material.BLACK_STAINED_GLASS_PANE, inv.getItem(27).getType());
        assertEquals(Material.BARRIER, inv.getItem(7).getType());
        assertEquals(MenuType.FILTER, plugin.getPlayerDataManager().getPlayerData(player).getInMenu());
    }

    @Test
    void filterFillsWhiteBlackAndVoidSlotsFromTheFilterListsInOrder() {
        PlayerMock player = server.addPlayer();
        Location loc = new Location(world, 0, 64, 0);
        EFilter ef = new EFilter();
        ef.setWhiteList(new ArrayList<>(List.of(Material.DIAMOND, Material.GOLD_INGOT)));
        ef.setBlackList(new ArrayList<>(List.of(Material.COAL)));
        ef.setVoidList(new ArrayList<>(List.of(Material.DIRT, Material.SAND, Material.GRAVEL)));
        EHopper hopper = new EHopper(loc, levelManager.getLevel(1), null, null, null, ef,
                TeleportTrigger.DISABLED, null);

        hopper.filter(player);

        org.bukkit.inventory.Inventory inv = player.getOpenInventory().getTopInventory();
        assertEquals(Material.DIAMOND, inv.getItem(0).getType());
        assertEquals(Material.GOLD_INGOT, inv.getItem(1).getType());
        assertEquals(Material.COAL, inv.getItem(27).getType());
        assertEquals(Material.DIRT, inv.getItem(7).getType());
        assertEquals(Material.SAND, inv.getItem(8).getType());
        assertEquals(Material.GRAVEL, inv.getItem(16).getType());
    }
}
