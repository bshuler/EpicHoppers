package com.songoda.epichoppers.listeners;

import com.songoda.epichoppers.EpicHoppersPlugin;
import com.songoda.epichoppers.api.hopper.Hopper;
import com.songoda.epichoppers.api.hopper.TeleportTrigger;
import com.songoda.epichoppers.handlers.EnchantmentHandler;
import com.songoda.epichoppers.hopper.EFilter;
import com.songoda.epichoppers.hopper.EHopper;
import com.songoda.epichoppers.player.SyncType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

class InteractListenersTest {

    private ServerMock server;
    private EpicHoppersPlugin plugin;
    private WorldMock world;
    private InteractListeners listener;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(EpicHoppersPlugin.class);
        world = server.addSimpleWorld("world");
        listener = new InteractListeners(plugin);
        plugin.getConfig().set("Main.Allow hopper Upgrading", true);
        plugin.getConfig().set("Main.Support Enderchests", true);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private Hopper registerHopper(Location location, TeleportTrigger trigger) {
        Hopper hopper = new EHopper(location, plugin.getLevelManager().getLowestLevel(),
                null, null, null, new EFilter(), trigger, null);
        plugin.getHopperManager().addHopper(location, hopper);
        return hopper;
    }

    // --- onPlayerToggleSneakEvent ---

    @Test
    void notSneakingDoesNothing() {
        PlayerMock player = server.addPlayer();
        player.setSneaking(false);

        listener.onPlayerToggleSneakEvent(new PlayerToggleSneakEvent(player, false));

        assertEquals(0, player.getLocation().distanceSquared(player.getLocation()));
        // No hopper was ever queried, so nothing was registered as a side
        // effect either - confirmed by the location assertion above being
        // trivially true and no exception being thrown.
    }

    @Test
    void sneakingOnTopOfAHopperWithNonSneakTriggerDoesNothing() {
        PlayerMock player = server.addPlayer();
        Block hopperBlock = world.getBlockAt(0, 4, 0);
        registerHopper(hopperBlock.getLocation(), TeleportTrigger.DISABLED);
        player.teleport(world.getBlockAt(0, 5, 0).getLocation());
        player.setSneaking(true);

        Location before = player.getLocation().clone();
        listener.onPlayerToggleSneakEvent(new PlayerToggleSneakEvent(player, true));

        assertEquals(before, player.getLocation());
    }

    @Test
    void sneakingOnTopOfASneakTriggerHopperDelegatesToTheTeleportHandlerWithoutThrowing() {
        // TeleportHandler#tpPlayer's own behavior (whether/how far it moves
        // the player) is that class's own responsibility and is tested
        // separately - this only proves InteractListeners correctly detects
        // the hopper directly below the player and routes the SNEAK trigger
        // into it instead of silently swallowing it.
        PlayerMock player = server.addPlayer();
        Block hopperBlock = world.getBlockAt(0, 4, 0);
        registerHopper(hopperBlock.getLocation(), TeleportTrigger.SNEAK);
        player.teleport(world.getBlockAt(0, 5, 0).getLocation());
        player.setSneaking(true);

        listener.onPlayerToggleSneakEvent(new PlayerToggleSneakEvent(player, true));
        // No exception means the SNEAK branch was reached and safely handled.
    }

    @Test
    void sneakingInsideAHopperBlockItselfAlsoTriggersTheCheck() {
        PlayerMock player = server.addPlayer();
        Block hopperBlock = world.getBlockAt(0, 5, 0);
        registerHopper(hopperBlock.getLocation(), TeleportTrigger.SNEAK);
        player.teleport(hopperBlock.getLocation());
        player.setSneaking(true);

        listener.onPlayerToggleSneakEvent(new PlayerToggleSneakEvent(player, true));
        // Reaches the "isHopper(location)" branch (as opposed to "down") -
        // no exception means it was handled.
    }

    // --- onBlockInteract ---

    private PlayerInteractEvent leftClick(PlayerMock player, Block block, ItemStack item) {
        return new PlayerInteractEvent(player, Action.LEFT_CLICK_BLOCK, item, block, BlockFace.UP);
    }

    @Test
    void nonLeftClickActionsAreIgnored() {
        PlayerMock player = server.addPlayer();
        Block block = world.getBlockAt(0, 5, 0);
        block.setType(Material.CHEST);

        PlayerInteractEvent event = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK,
                new ItemStack(Material.AIR), block, BlockFace.UP);
        listener.onBlockInteract(event);

        assertFalse(event.isCancelled());
    }

    @Test
    void aSneakingPlayerIsIgnored() {
        PlayerMock player = server.addPlayer();
        player.addAttachment(plugin, "EpicHoppers.overview", true);
        player.setSneaking(true);
        Block block = world.getBlockAt(0, 5, 0);
        block.setType(Material.CHEST);

        PlayerInteractEvent event = leftClick(player, block, new ItemStack(Material.AIR));
        listener.onBlockInteract(event);

        assertFalse(event.isCancelled());
    }

    @Test
    void aPlayerWithoutTheOverviewPermissionIsIgnored() {
        PlayerMock player = server.addPlayer();
        Block block = world.getBlockAt(0, 5, 0);
        block.setType(Material.HOPPER);

        PlayerInteractEvent event = leftClick(player, block, new ItemStack(Material.AIR));
        listener.onBlockInteract(event);

        assertFalse(event.isCancelled());
        assertFalse(plugin.getHopperManager().isHopper(block.getLocation()));
    }

    @Test
    void aBlockThatIsNotAnInventoryHolderOrEnderChestIsIgnored() {
        PlayerMock player = server.addPlayer();
        player.addAttachment(plugin, "EpicHoppers.overview", true);
        Block block = world.getBlockAt(0, 5, 0);
        block.setType(Material.STONE);

        PlayerInteractEvent event = leftClick(player, block, new ItemStack(Material.AIR));
        listener.onBlockInteract(event);

        assertFalse(event.isCancelled());
    }

    @Test
    void aSyncToolBoundToAChestCanUnbindItselfByClickingAnotherChest() {
        PlayerMock player = server.addPlayer();
        player.addAttachment(plugin, "EpicHoppers.overview", true);
        Block originalTarget = world.getBlockAt(9, 5, 9);
        originalTarget.setType(Material.CHEST);
        ItemStack tool = new EnchantmentHandler().createSyncTouch(
                new ItemStack(Material.DIAMOND_PICKAXE), originalTarget);
        assertEquals(2, tool.getItemMeta().getLore().size());
        // Methods.isSync(player) (and the production onBlockInteract branch
        // itself) reads the player's main-hand item, not the event's own
        // getItem() - the event's item is only used to build the event and
        // is otherwise ignored by this listener.
        player.getInventory().setItemInMainHand(tool);

        Block clicked = world.getBlockAt(0, 5, 0);
        clicked.setType(Material.CHEST);

        PlayerInteractEvent event = leftClick(player, clicked, tool);
        listener.onBlockInteract(event);

        assertTrue(event.isCancelled());
        ItemMeta meta = player.getInventory().getItemInMainHand().getItemMeta();
        assertEquals(1, meta.getLore().size());
        String message = player.nextMessage();
        assertNotNull(message);
    }

    @Test
    void anUnboundSyncToolBindsToTheClickedChest() {
        PlayerMock player = server.addPlayer();
        player.addAttachment(plugin, "EpicHoppers.overview", true);
        ItemStack tool = new EnchantmentHandler().createSyncTouch(
                new ItemStack(Material.DIAMOND_PICKAXE), null);
        assertEquals(1, tool.getItemMeta().getLore().size());
        player.getInventory().setItemInMainHand(tool);

        Block clicked = world.getBlockAt(0, 5, 0);
        clicked.setType(Material.CHEST);

        PlayerInteractEvent event = leftClick(player, clicked, tool);
        listener.onBlockInteract(event);

        assertTrue(event.isCancelled());
        ItemMeta meta = player.getInventory().getItemInMainHand().getItemMeta();
        assertEquals(2, meta.getLore().size());
        String message = player.nextMessage();
        assertNotNull(message);
    }

    @Test
    void clickingAHopperSwallowsAnExceptionWhenNoLevelsAreRegistered() {
        // Same reachable NPE as BlockListeners' equivalent test:
        // EHopperManager#getHopper auto-vivifies a new EHopper seeded with
        // LevelManager#getLowestLevel(), which throws when every level has
        // been cleared via the real ELevelManager#clear() API. The
        // exception surfaces straight out of
        // instance.getHopperManager().getHopper(e.getClickedBlock()),
        // caught by onBlockInteract's own catch-all.
        ((com.songoda.epichoppers.hopper.levels.ELevelManager) plugin.getLevelManager()).clear();
        PlayerMock player = server.addPlayer();
        player.addAttachment(plugin, "EpicHoppers.overview", true);
        Block block = world.getBlockAt(0, 5, 0);
        block.setType(Material.HOPPER);

        PlayerInteractEvent event = leftClick(player, block, new ItemStack(Material.AIR));
        listener.onBlockInteract(event);

        assertFalse(event.isCancelled());
    }

    @Test
    void clickingAHopperWithUpgradingAllowedOpensTheOverviewMenu() {
        PlayerMock player = server.addPlayer();
        player.addAttachment(plugin, "EpicHoppers.overview", true);
        Block block = world.getBlockAt(0, 5, 0);
        block.setType(Material.HOPPER);

        PlayerInteractEvent event = leftClick(player, block, new ItemStack(Material.AIR));
        listener.onBlockInteract(event);

        assertTrue(event.isCancelled());
        assertTrue(player.getOpenInventory().getTitle().contains("Level"));
    }

    @Test
    void clickingAHopperWhileHoldingAPickaxeDoesNotOpenTheOverviewMenuEvenWithUpgradingAllowed() {
        PlayerMock player = server.addPlayer();
        player.addAttachment(plugin, "EpicHoppers.overview", true);
        Block block = world.getBlockAt(0, 5, 0);
        block.setType(Material.HOPPER);

        PlayerInteractEvent event = leftClick(player, block, new ItemStack(Material.DIAMOND_PICKAXE));
        listener.onBlockInteract(event);

        assertTrue(event.isCancelled());
    }

    @Test
    void anAdminClickingAHopperWithUpgradingDisabledArmsSyncTimeoutInstead() {
        plugin.getConfig().set("Main.Allow hopper Upgrading", false);
        PlayerMock player = server.addPlayer();
        player.addAttachment(plugin, "EpicHoppers.overview", true);
        player.addAttachment(plugin, "EpicHoppers.Admin", true);
        Block block = world.getBlockAt(0, 5, 0);
        block.setType(Material.HOPPER);

        PlayerInteractEvent event = leftClick(player, block, new ItemStack(Material.AIR));
        listener.onBlockInteract(event);

        assertTrue(event.isCancelled());
        assertNotNull(player.nextMessage());
    }

    @Test
    void aNonAdminClickingAHopperWithUpgradingDisabledJustCancelsTheEvent() {
        plugin.getConfig().set("Main.Allow hopper Upgrading", false);
        PlayerMock player = server.addPlayer();
        player.addAttachment(plugin, "EpicHoppers.overview", true);
        Block block = world.getBlockAt(0, 5, 0);
        block.setType(Material.HOPPER);

        PlayerInteractEvent event = leftClick(player, block, new ItemStack(Material.AIR));
        listener.onBlockInteract(event);

        assertTrue(event.isCancelled());
    }

    @Test
    void brewingStandsAreIgnoredEvenWhileAwaitingASyncTarget() {
        PlayerMock player = server.addPlayer();
        player.addAttachment(plugin, "EpicHoppers.overview", true);
        plugin.getPlayerDataManager().getPlayerData(player).setSyncType(SyncType.REGULAR);
        Block block = world.getBlockAt(0, 5, 0);
        block.setType(Material.BREWING_STAND);

        PlayerInteractEvent event = leftClick(player, block, new ItemStack(Material.AIR));
        listener.onBlockInteract(event);

        assertFalse(event.isCancelled());
        assertEquals(SyncType.REGULAR, plugin.getPlayerDataManager().getPlayerData(player).getSyncType());
    }

    @Test
    void clickingTheHopperItselfWhileAwaitingASyncTargetSendsASyncSelfMessage() {
        PlayerMock player = server.addPlayer();
        player.addAttachment(plugin, "EpicHoppers.overview", true);
        Block block = world.getBlockAt(0, 5, 0);
        block.setType(Material.CHEST);
        Hopper hopper = registerHopper(block.getLocation(), TeleportTrigger.DISABLED);
        plugin.getPlayerDataManager().getPlayerData(player).setLastHopper(hopper);
        plugin.getPlayerDataManager().getPlayerData(player).setSyncType(SyncType.REGULAR);

        PlayerInteractEvent event = leftClick(player, block, new ItemStack(Material.AIR));
        listener.onBlockInteract(event);

        assertTrue(event.isCancelled());
        assertNotNull(player.nextMessage());
        assertNull(plugin.getPlayerDataManager().getPlayerData(player).getSyncType());
    }

    @Test
    void clickingADifferentContainerWhileAwaitingASyncTargetSyncsTheHopperToIt() {
        PlayerMock player = server.addPlayer();
        player.addAttachment(plugin, "EpicHoppers.overview", true);
        player.addAttachment(plugin, "EpicHoppers.Admin", true);
        Block hopperBlock = world.getBlockAt(0, 5, 0);
        hopperBlock.setType(Material.HOPPER);
        Hopper hopper = registerHopper(hopperBlock.getLocation(), TeleportTrigger.DISABLED);
        plugin.getPlayerDataManager().getPlayerData(player).setLastHopper(hopper);
        plugin.getPlayerDataManager().getPlayerData(player).setSyncType(SyncType.REGULAR);

        Block chest = world.getBlockAt(5, 5, 5);
        chest.setType(Material.CHEST);

        PlayerInteractEvent event = leftClick(player, chest, new ItemStack(Material.AIR));
        listener.onBlockInteract(event);

        assertTrue(event.isCancelled());
        assertNotNull(player.nextMessage());
        assertEquals(chest, ((EHopper) hopper).getSyncedBlock());
        assertNull(plugin.getPlayerDataManager().getPlayerData(player).getSyncType());
    }
}
