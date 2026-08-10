package com.songoda.epichoppers.hopper.levels.modules;

import com.songoda.epichoppers.EpicHoppersPlugin;
import com.songoda.epichoppers.api.hopper.TeleportTrigger;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * ModuleBlockBreak#run(Hopper) is pure state-machine logic keyed off the
 * hopper's own block, with no Bukkit event or menu involved - it is driven
 * directly, once per "tick", by whatever calls Module#run (a repeating
 * scheduler task in production). These tests replicate that by calling
 * run() repeatedly, exactly like production ticking would.
 */
class ModuleBlockBreakTest {

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
        return new EHopper(loc, levelManager.getLevel(1), null, null, null, new EFilter(),
                TeleportTrigger.DISABLED, null);
    }

    @Test
    void firstRunOnANewBlockJustStartsTrackingItAndDoesNothingElse() {
        Location loc = new Location(world, 0, 64, 0);
        EHopper hopper = hopperAt(loc);
        Block above = world.getBlockAt(0, 65, 0);
        above.setType(Material.STONE);

        new ModuleBlockBreak(2).run(hopper);

        assertEquals(Material.STONE, above.getType());
    }

    @Test
    void ticksBelowTheConfiguredAmountLeaveTheBlockAboveAlone() {
        Location loc = new Location(world, 1, 64, 0);
        EHopper hopper = hopperAt(loc);
        Block above = world.getBlockAt(1, 65, 0);
        above.setType(Material.STONE);

        ModuleBlockBreak module = new ModuleBlockBreak(3);
        module.run(hopper); // tick 1: starts tracking
        module.run(hopper); // tick 2: tick(1) < amount(3), no break yet

        assertEquals(Material.STONE, above.getType());
    }

    @Test
    void onceTheConfiguredAmountOfTicksPassAirAboveIsSimplySkipped() {
        Location loc = new Location(world, 2, 64, 0);
        EHopper hopper = hopperAt(loc);
        Block above = world.getBlockAt(2, 65, 0);
        above.setType(Material.AIR);

        ModuleBlockBreak module = new ModuleBlockBreak(1);
        module.run(hopper); // tick 1: starts tracking
        module.run(hopper); // tick 2: tick(1) >= amount(1), but above is AIR

        assertEquals(Material.AIR, above.getType());
    }

    @Test
    void onceTheConfiguredAmountOfTicksPassABlacklistedBlockAboveIsLeftAlone() {
        Location loc = new Location(world, 3, 64, 0);
        EHopper hopper = hopperAt(loc);
        Block above = world.getBlockAt(3, 65, 0);
        above.setType(Material.BEDROCK);
        plugin.getConfig().set("Main.BlockBreak Blacklisted Blocks", java.util.List.of("BEDROCK"));

        ModuleBlockBreak module = new ModuleBlockBreak(1);
        module.run(hopper);
        module.run(hopper);

        assertEquals(Material.BEDROCK, above.getType());
    }

    @Test
    void onceTheConfiguredAmountOfTicksPassANonBlacklistedBlockAboveIsBrokenAndTrackingReset() {
        Location loc = new Location(world, 4, 64, 0);
        EHopper hopper = hopperAt(loc);
        Block above = world.getBlockAt(4, 65, 0);
        above.setType(Material.STONE);
        plugin.getConfig().set("Main.BlockBreak Blacklisted Blocks", java.util.List.of());
        plugin.getConfig().set("Main.BlockBreak Particle Type", "FLAME");

        ModuleBlockBreak module = new ModuleBlockBreak(1);
        module.run(hopper); // tick 1: starts tracking
        module.run(hopper); // tick 2: tick(1) >= amount(1) -> breaks & resets tracking

        assertNotEquals(Material.STONE, above.getType());

        // tracking was reset (block removed from the map), so the very next
        // run on the same location starts tracking fresh rather than
        // immediately breaking again, even though the block is now AIR.
        module.run(hopper);
        assertEquals(Material.AIR, above.getType());
    }

    @Test
    void getNameReturnsBlockBreak() {
        assertEquals("BlockBreak", new ModuleBlockBreak(1).getName());
    }

    @Test
    void getBlockedItemsIsAlwaysNull() {
        Location loc = new Location(world, 5, 64, 0);
        EHopper hopper = hopperAt(loc);
        assertEquals(null, new ModuleBlockBreak(1).getBlockedItems(hopper));
    }

    @Test
    void getDescriptionInterpolatesTheConfiguredAmount() {
        String description = new ModuleBlockBreak(5).getDescription();
        assertEquals("§7Block Break: §6Every 5 ticks", description);
    }
}
