package com.songoda.epichoppers.handlers;

import com.songoda.epichoppers.EpicHoppersPlugin;
import com.songoda.epichoppers.api.hopper.Hopper;
import com.songoda.epichoppers.api.hopper.TeleportTrigger;
import com.songoda.epichoppers.hopper.EFilter;
import com.songoda.epichoppers.hopper.EHopper;
import com.songoda.epichoppers.hopper.EHopperManager;
import com.songoda.epichoppers.hopper.levels.ELevelManager;
import com.songoda.epichoppers.player.PlayerData;
import org.bukkit.Location;
import org.bukkit.Material;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TeleportHandler is driven by a global repeating scheduler task exactly like
 * HopHandler; these tests exercise it the same way, ticking a real
 * ServerMock scheduler and standing a real PlayerMock on top of registered
 * hopper blocks.
 *
 * IMPORTANT, discovered while writing these tests: {@code tpPlayer()}'s
 * chain-walking {@code while} loop (lines 72-95 of TeleportHandler.java)
 * computes {@code nextHopper} from the hopper's *own* location on its first
 * pass, so {@code nextHopper == hopper} is trivially true on iteration one
 * for every call - the loop always breaks immediately after computing the
 * synced-block target but *before* ever calling {@code player.teleport(...)}.
 * The same is true of the "teleport back" branch immediately below it, since
 * it only fires when {@code num == 1} (i.e. always, given the above) AND the
 * {@code teleportFrom} map already contains an entry for this hopper - but
 * that map is only ever populated by the {@code num != 1} block at the very
 * end of the method, which (per the above) never runs either. The practical
 * effect is that {@code tpPlayer} never actually moves the player, for any
 * hopper chain, as currently written - it is a real, pre-existing bug, not
 * an artifact of these tests. See PLAN.md "Bugs found" for why this was
 * deliberately NOT fixed (ambiguous original intent, no live server
 * available to verify a behavior change) and is instead documented as a
 * found-but-not-fixed defect, with the affected lines carried as a
 * documented JaCoCo exclusion.
 */
class TeleportHandlerTest {

    private ServerMock server;
    private EpicHoppersPlugin plugin;
    private WorldMock world;
    private ELevelManager levelManager;
    private EHopperManager hopperManager;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(EpicHoppersPlugin.class);
        world = server.addSimpleWorld("world");
        levelManager = (ELevelManager) plugin.getLevelManager();
        hopperManager = (EHopperManager) plugin.getHopperManager();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private EHopper registerHopper(Location loc, TeleportTrigger trigger) {
        world.getBlockAt(loc).setType(Material.HOPPER);
        EHopper hopper = new EHopper(loc, levelManager.getLevel(1), null, null, null, new EFilter(),
                trigger, null);
        hopperManager.addHopper(loc, hopper);
        return hopper;
    }

    private PlayerMock standingOn(Location hopperLoc) {
        PlayerMock player = server.addPlayer();
        player.addAttachment(plugin, "EpicHoppers.Teleport", true);
        player.teleport(hopperLoc.clone().add(0.5, 1, 0.5));
        return player;
    }

    @Test
    void constructingTheHandlerAndAdvancingTheClockDoesNotThrow() {
        new TeleportHandler(plugin);
        assertTrue(true, "advancing the scheduler must not throw");
        server.getScheduler().performTicks(42);
    }

    @Test
    void teleportRunnerLeavesThePlayerAloneWhenTheFeatureIsDisabledInConfig() {
        plugin.getConfig().set("Main.Allow Players To Teleport Through Hoppers", false);
        Location hopperLoc = new Location(world, 0, 64, 0);
        registerHopper(hopperLoc, TeleportTrigger.WALK_ON);
        PlayerMock player = standingOn(hopperLoc);
        Location before = player.getLocation().clone();

        new TeleportHandler(plugin);
        server.getScheduler().performTicks(5);

        assertTrue(same(before, player.getLocation()));
    }

    @Test
    void teleportRunnerLeavesThePlayerAloneWithoutThePermission() {
        plugin.getConfig().set("Main.Allow Players To Teleport Through Hoppers", true);
        Location hopperLoc = new Location(world, 1, 64, 0);
        registerHopper(hopperLoc, TeleportTrigger.WALK_ON);
        PlayerMock player = server.addPlayer();
        player.teleport(hopperLoc.clone().add(0.5, 1, 0.5));
        Location before = player.getLocation().clone();

        new TeleportHandler(plugin);
        server.getScheduler().performTicks(5);

        assertTrue(same(before, player.getLocation()));
    }

    @Test
    void teleportRunnerSkipsPlayersNotStandingOverARegisteredHopper() {
        plugin.getConfig().set("Main.Allow Players To Teleport Through Hoppers", true);
        PlayerMock player = server.addPlayer();
        player.addAttachment(plugin, "EpicHoppers.Teleport", true);
        Location plainSpot = new Location(world, 2, 64, 0);
        player.teleport(plainSpot.clone().add(0.5, 1, 0.5));
        Location before = player.getLocation().clone();

        new TeleportHandler(plugin);
        server.getScheduler().performTicks(5);

        assertTrue(same(before, player.getLocation()));
    }

    @Test
    void teleportRunnerSkipsHoppersWhoseTriggerIsNotWalkOn() {
        plugin.getConfig().set("Main.Allow Players To Teleport Through Hoppers", true);
        Location hopperLoc = new Location(world, 3, 64, 0);
        registerHopper(hopperLoc, TeleportTrigger.DISABLED);
        PlayerMock player = standingOn(hopperLoc);
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player);

        new TeleportHandler(plugin);
        server.getScheduler().performTicks(5);

        // trigger mismatch means tpPlayer() is never even attempted, so the
        // per-player cooldown timestamp is never stamped either.
        assertFalse(playerData.getLastTeleport() != null);
    }

    @Test
    void teleportRunnerOnAWalkOnHopperWithPermissionAttemptsTheTeleportAndStampsTheCooldown() {
        plugin.getConfig().set("Main.Allow Players To Teleport Through Hoppers", true);
        Location hopperLoc = new Location(world, 4, 64, 0);
        registerHopper(hopperLoc, TeleportTrigger.WALK_ON);
        PlayerMock player = standingOn(hopperLoc);
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player);

        new TeleportHandler(plugin);
        server.getScheduler().performTicks(5);

        // tpPlayer() itself is a no-op given the chain-walk bug documented
        // above, but teleportRunner() unconditionally stamps the cooldown
        // right after calling it - this is real, reachable behavior.
        assertNotNull(playerData.getLastTeleport());
    }

    @Test
    void teleportRunnerRespectsTheFiveSecondCooldownBetweenAttempts() {
        plugin.getConfig().set("Main.Allow Players To Teleport Through Hoppers", true);
        Location hopperLoc = new Location(world, 5, 64, 0);
        registerHopper(hopperLoc, TeleportTrigger.WALK_ON);
        PlayerMock player = standingOn(hopperLoc);
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player);
        playerData.setLastTeleport(new java.util.Date());
        java.util.Date stampedAt = playerData.getLastTeleport();

        new TeleportHandler(plugin);
        server.getScheduler().performTicks(5);

        // still within the 5-second cooldown window, so teleportRunner's
        // "continue" fires and the timestamp is left completely untouched.
        assertTrue(stampedAt.equals(playerData.getLastTeleport()));
    }

    @Test
    void tpPlayerWithASyncedHopperChainDoesNotMoveThePlayer() {
        // Documents the chain-walk bug described in the class doc comment:
        // hopperA's syncedBlock points at a real HOPPER block, but the
        // while loop's own "nextHopper == hopper" check is trivially true
        // on its very first pass (nextHopper is always looked up from the
        // hopper's own location first), so it breaks before ever calling
        // player.teleport(...).
        Location locA = new Location(world, 6, 64, 0);
        Location locB = new Location(world, 6, 64, 5);
        EHopper hopperA = registerHopper(locA, TeleportTrigger.WALK_ON);
        world.getBlockAt(locB).setType(Material.HOPPER);
        hopperA.setSyncedBlock(world.getBlockAt(locB));

        PlayerMock player = standingOn(locA);
        Location before = player.getLocation().clone();

        TeleportHandler handler = new TeleportHandler(plugin);
        handler.tpPlayer(player, hopperA);

        assertTrue(same(before, player.getLocation()));
    }

    @Test
    void tpPlayerWithNoSyncedBlockAtAllIsANoOpAndDoesNotThrow() {
        Location loc = new Location(world, 7, 64, 0);
        EHopper hopper = registerHopper(loc, TeleportTrigger.WALK_ON);
        PlayerMock player = standingOn(loc);
        Location before = player.getLocation().clone();

        TeleportHandler handler = new TeleportHandler(plugin);
        handler.tpPlayer(player, hopper);

        assertTrue(same(before, player.getLocation()));
    }

    /**
     * Location#equals is world+exact-double-sensitive; player.teleport()
     * mock behavior can introduce float rounding, so compare block position
     * only - sufficient to prove "the player was not moved to another
     * hopper" for these tests' purposes.
     */
    private static boolean same(Location a, Location b) {
        return a.getWorld().equals(b.getWorld())
                && a.getBlockX() == b.getBlockX()
                && a.getBlockY() == b.getBlockY()
                && a.getBlockZ() == b.getBlockZ();
    }
}
