package com.songoda.epichoppers.hopper.levels.modules;

import com.songoda.epichoppers.EpicHoppersPlugin;
import com.songoda.epichoppers.api.hopper.TeleportTrigger;
import com.songoda.epichoppers.hopper.EFilter;
import com.songoda.epichoppers.hopper.EHopper;
import com.songoda.epichoppers.hopper.levels.ELevelManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.ItemMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ModuleSuction#run(Hopper) is pure state-machine logic driven directly by a
 * repeating scheduler task in production (like ModuleBlockBreak); these
 * tests drive it the same way, with real dropped Item entities and a real
 * HOPPER block (MockBukkit's HopperStateMock backs Hopper#getHopper() with a
 * genuine, live Container inventory).
 */
class ModuleSuctionTest {

    private ServerMock server;
    private EpicHoppersPlugin plugin;
    private WorldMock world;
    private ELevelManager levelManager;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(EpicHoppersPlugin.class);
        world = server.addSimpleWorld("world");
        levelManager = (ELevelManager) plugin.getLevelManager();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private EHopper hopperAt(Location loc) {
        world.getBlockAt(loc).setType(Material.HOPPER);
        return new EHopper(loc, levelManager.getLevel(1), null, null, null, new EFilter(),
                TeleportTrigger.DISABLED, null);
    }

    private Item dropAt(Location loc, ItemStack stack, int ticksLived, boolean onGround) {
        Item item = world.dropItem(loc, stack);
        ItemMock mock = (ItemMock) item;
        mock.setTicksLived(ticksLived);
        mock.setOnGround(onGround);
        return item;
    }

    @Test
    void suckedUpItemIsRemovedFromTheWorldAndAddedToTheHopper() {
        Location loc = new Location(world, 0, 64, 0);
        EHopper hopper = hopperAt(loc);
        Item item = dropAt(loc.clone().add(1.3, 0, 0), new ItemStack(Material.DIAMOND, 3), 20, true);

        new ModuleSuction(2).run(hopper);

        assertTrue(item.isDead());
        assertTrue(hopper.getHopper().getInventory().contains(Material.DIAMOND));
    }

    @Test
    void freshlySpawnedItemsBelowTheTenTickGraceAreIgnored() {
        Location loc = new Location(world, 1, 64, 0);
        EHopper hopper = hopperAt(loc);
        Item item = dropAt(loc.clone().add(1.3, 0, 0), new ItemStack(Material.DIAMOND, 3), 5, true);

        new ModuleSuction(2).run(hopper);

        assertFalse(item.isDead());
        assertFalse(hopper.getHopper().getInventory().contains(Material.DIAMOND));
    }

    @Test
    void itemsNotYetOnTheGroundAreIgnoredEvenIfEverythingElseMatches() {
        Location loc = new Location(world, 2, 64, 0);
        EHopper hopper = hopperAt(loc);
        Item item = dropAt(loc.clone().add(1.3, 0, 0), new ItemStack(Material.DIAMOND, 3), 20, false);

        new ModuleSuction(2).run(hopper);

        assertFalse(item.isDead());
        assertFalse(hopper.getHopper().getInventory().contains(Material.DIAMOND));
        // marked grabbed regardless, since the on-ground check happens after pickup-delay is set
        assertTrue(item.hasMetadata("grabbed"));
    }

    @Test
    void shulkerBoxItemsAreNeverSuckedUp() {
        Location loc = new Location(world, 3, 64, 0);
        EHopper hopper = hopperAt(loc);
        Item item = dropAt(loc.clone().add(1.3, 0, 0), new ItemStack(Material.SHULKER_BOX, 1), 20, true);

        new ModuleSuction(2).run(hopper);

        assertFalse(item.isDead());
    }

    @Test
    void shopPluginPriceTagsAreNeverSuckedUp() {
        Location loc = new Location(world, 4, 64, 0);
        EHopper hopper = hopperAt(loc);
        ItemStack stack = new ItemStack(Material.DIAMOND);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName("***5.00");
        stack.setItemMeta(meta);
        Item item = dropAt(loc.clone().add(1.3, 0, 0), stack, 20, true);

        new ModuleSuction(2).run(hopper);

        assertFalse(item.isDead());
    }

    @Test
    void itemsAlreadyMarkedGrabbedByAnEarlierPassAreSkipped() {
        Location loc = new Location(world, 5, 64, 0);
        EHopper hopper = hopperAt(loc);
        Item item = dropAt(loc.clone().add(1.3, 0, 0), new ItemStack(Material.DIAMOND, 3), 20, true);
        item.setMetadata("grabbed", new FixedMetadataValue(plugin, ""));

        new ModuleSuction(2).run(hopper);

        assertFalse(item.isDead());
        assertFalse(hopper.getHopper().getInventory().contains(Material.DIAMOND));
    }

    @Test
    void aFullNonMatchingHopperInventoryLeavesTheItemOnTheGround() {
        Location loc = new Location(world, 6, 64, 0);
        EHopper hopper = hopperAt(loc);
        ItemStack filler = new ItemStack(Material.COBBLESTONE, 64);
        for (int i = 0; i < hopper.getHopper().getInventory().getSize(); i++) {
            hopper.getHopper().getInventory().setItem(i, filler.clone());
        }
        Item item = dropAt(loc.clone().add(1.3, 0, 0), new ItemStack(Material.DIAMOND, 3), 20, true);

        new ModuleSuction(2).run(hopper);

        assertFalse(item.isDead());
    }

    @Test
    void aFullButRoomyMatchingHopperInventoryStillSucksTheItemUp() {
        Location loc = new Location(world, 9, 64, 0);
        EHopper hopper = hopperAt(loc);
        ItemStack partial = new ItemStack(Material.DIAMOND, 32);
        for (int i = 0; i < hopper.getHopper().getInventory().getSize(); i++) {
            hopper.getHopper().getInventory().setItem(i, partial.clone());
        }
        Item item = dropAt(loc.clone().add(1.3, 0, 0), new ItemStack(Material.DIAMOND, 3), 20, true);

        new ModuleSuction(2).run(hopper);

        assertTrue(item.isDead());
    }

    @Test
    void entitiesThatAreNotDroppedItemsAreIgnored() {
        Location loc = new Location(world, 7, 64, 0);
        EHopper hopper = hopperAt(loc);
        world.spawn(loc.clone().add(1.3, 0, 0), org.bukkit.entity.Zombie.class);

        new ModuleSuction(2).run(hopper);

        // no exception, nothing to assert on the zombie - this just proves
        // the instanceof guard skips non-Item entities cleanly.
        assertEquals(1, world.getEntitiesByClass(org.bukkit.entity.Zombie.class).size());
    }

    @Test
    void getNameReturnsSuction() {
        assertEquals("Suction", new ModuleSuction(1).getName());
    }

    @Test
    void getBlockedItemsIsAlwaysNull() {
        Location loc = new Location(world, 8, 64, 0);
        EHopper hopper = hopperAt(loc);
        assertEquals(null, new ModuleSuction(1).getBlockedItems(hopper));
    }

    @Test
    void getDescriptionInterpolatesTheConfiguredAmount() {
        String description = new ModuleSuction(3).getDescription();
        assertEquals("§7Suction: §63", description);
    }
}
