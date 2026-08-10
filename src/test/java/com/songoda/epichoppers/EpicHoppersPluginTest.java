package com.songoda.epichoppers;

import com.songoda.epichoppers.api.hopper.TeleportTrigger;
import com.songoda.epichoppers.api.hopper.levels.Level;
import com.songoda.epichoppers.api.utils.ProtectionPluginHook;
import com.songoda.epichoppers.hopper.EFilter;
import com.songoda.epichoppers.hopper.EHopper;
import com.songoda.epichoppers.hopper.levels.ELevelManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the small pure-logic surface of EpicHoppersPlugin that has no
 * existing test file at all: canBuild()/registerProtectionHook() (the
 * protection-hook API this plugin exposes to other plugins),
 * getLevelFromItem() (EpicHoppersAPI's item-to-level decoder), and
 * getIslandId() (a thin OfflinePlayer#getUniqueId() wrapper).
 * <p>
 * isInFaction()/getFactionId()/isInTown()/getTownId() are deliberately
 * exercised too, but only their "hook not present" branch: the 9
 * protection-plugin hooks (Factions/Towny/etc., see the class doc comment
 * in CLAUDE.md) were relocated to legacy-hooks/ and their registration
 * calls removed from onEnable(), so the {@code factionsHook}/
 * {@code townyHook} fields can never be assigned anything but their default
 * null - the "hook present" branch of each of these four methods is
 * permanently dead code given the plugin's current onEnable(), not a gap in
 * these tests.
 * <p>
 * The onEnable() storage-rehydration closure (reading persisted hoppers and
 * boosts back out of Storage/StorageRow on startup) and the 9
 * protection-hook constant-string/service branches are intentionally not
 * covered here - see PLAN.md "Coverage exclusions" for why.
 */
class EpicHoppersPluginTest {

    private ServerMock server;
    private EpicHoppersPlugin plugin;
    private WorldMock world;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(EpicHoppersPlugin.class);
        world = server.addSimpleWorld("world");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private static class StubHook implements ProtectionPluginHook {
        private final JavaPlugin hookPlugin;
        private final boolean canBuildResult;

        StubHook(JavaPlugin hookPlugin, boolean canBuildResult) {
            this.hookPlugin = hookPlugin;
            this.canBuildResult = canBuildResult;
        }

        @Override
        public JavaPlugin getPlugin() {
            return hookPlugin;
        }

        @Override
        public boolean canBuild(Player player, Location location) {
            return canBuildResult;
        }
    }

    // ---- canBuild() ----

    @Test
    void canBuildAlwaysAllowsAPlayerWithTheBypassPermission() {
        PlayerMock player = server.addPlayer();
        player.addAttachment(plugin, "EpicHoppers.bypass", true);

        assertTrue(plugin.canBuild(player, new Location(world, 0, 64, 0)));
    }

    @Test
    void canBuildAllowsWithNoRegisteredHooksAndNoBypassPermission() {
        PlayerMock player = server.addPlayer();

        assertTrue(plugin.canBuild(player, new Location(world, 0, 64, 0)));
    }

    @Test
    void canBuildIsRefusedWhenARegisteredHookRefuses() {
        PlayerMock player = server.addPlayer();
        plugin.registerProtectionHook(new StubHook(plugin, false));

        assertFalse(plugin.canBuild(player, new Location(world, 0, 64, 0)));
    }

    @Test
    void canBuildIsAllowedWhenEveryRegisteredHookAllows() {
        PlayerMock player = server.addPlayer();
        plugin.registerProtectionHook(new StubHook(plugin, true));

        assertTrue(plugin.canBuild(player, new Location(world, 0, 64, 0)));
    }

    // ---- isInFaction / getFactionId / isInTown / getTownId ----
    // factionsHook/townyHook are never assigned given the hooks were
    // relocated out of onEnable(); these always take the null-hook branch.

    @Test
    void isInFactionIsAlwaysFalseNowThatTheFactionsHookIsNeverRegistered() {
        assertFalse(plugin.isInFaction("SomeFaction", new Location(world, 0, 64, 0)));
    }

    @Test
    void getFactionIdIsAlwaysNullNowThatTheFactionsHookIsNeverRegistered() {
        assertNull(plugin.getFactionId("SomeFaction"));
    }

    @Test
    void isInTownIsAlwaysFalseNowThatTheTownyHookIsNeverRegistered() {
        assertFalse(plugin.isInTown("SomeTown", new Location(world, 0, 64, 0)));
    }

    @Test
    void getTownIdIsAlwaysNullNowThatTheTownyHookIsNeverRegistered() {
        assertNull(plugin.getTownId("SomeTown"));
    }

    // ---- getIslandId() ----

    @Test
    void getIslandIdReturnsTheOfflinePlayersUuidAsAString() {
        PlayerMock player = server.addPlayer("IslandOwner");

        assertEquals(player.getUniqueId().toString(), plugin.getIslandId("IslandOwner"));
    }

    // ---- getLevelFromItem() ----

    @Test
    void getLevelFromItemParsesTheLevelNumberOutOfAColonFormattedDisplayName() {
        ELevelManager levelManager = (ELevelManager) plugin.getLevelManager();
        levelManager.clear();
        levelManager.addLevel(1, 10, 100, 5, 1, false, false, new java.util.ArrayList<>());
        levelManager.addLevel(2, 20, 200, 5, 1, false, false, new java.util.ArrayList<>());

        ItemStack item = new ItemStack(Material.HOPPER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("2:Some Hopper Name");
        item.setItemMeta(meta);

        Level level = plugin.getLevelFromItem(item);

        assertEquals(2, level.getLevel());
    }

    @Test
    void getLevelFromItemFallsBackToTheLowestLevelWithoutAColonInTheDisplayName() {
        ELevelManager levelManager = (ELevelManager) plugin.getLevelManager();
        levelManager.clear();
        levelManager.addLevel(1, 10, 100, 5, 1, false, false, new java.util.ArrayList<>());
        levelManager.addLevel(2, 20, 200, 5, 1, false, false, new java.util.ArrayList<>());

        ItemStack item = new ItemStack(Material.HOPPER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("Plain Hopper");
        item.setItemMeta(meta);

        Level level = plugin.getLevelFromItem(item);

        assertEquals(1, level.getLevel());
    }

    // ---- register() / registerProtectionHook() ----

    @Test
    void registerDelegatesToRegisterProtectionHook() {
        StubHook hook = new StubHook(plugin, true);

        plugin.register(() -> hook);

        assertTrue(plugin.canBuild(server.addPlayer(), new Location(world, 0, 64, 0)));
    }

    @Test
    void registerProtectionHookRejectsANullHook() {
        assertThrows(NullPointerException.class, () -> plugin.registerProtectionHook(null));
    }

    @Test
    void registerProtectionHookRejectsAHookWhosePluginIsNull() {
        assertThrows(NullPointerException.class,
                () -> plugin.registerProtectionHook(new StubHook(null, true)));
    }

    @Test
    void registerProtectionHookRejectsRegisteringTheSamePluginTwice() {
        plugin.registerProtectionHook(new StubHook(plugin, true));

        assertThrows(IllegalArgumentException.class,
                () -> plugin.registerProtectionHook(new StubHook(plugin, true)));
    }

    @Test
    void registerProtectionHookAllowsASecondHookForADifferentPlugin() {
        // The duplicate-plugin check loops over every already-registered
        // hook; with one existing hook already registered for a *different*
        // plugin, the loop must complete that non-matching comparison (and
        // fall out of the loop normally) rather than throwing, then still
        // register the new hook.
        org.mockbukkit.mockbukkit.plugin.PluginMock otherPlugin = MockBukkit.createMockPlugin("OtherPlugin");
        plugin.registerProtectionHook(new StubHook(plugin, true));

        plugin.registerProtectionHook(new StubHook(otherPlugin, false));

        assertFalse(plugin.canBuild(server.addPlayer(), new Location(world, 0, 64, 0)),
                "both hooks must now be registered and consulted, so the second "
                        + "(refusing) hook must veto canBuild()");
    }

    @Test
    void registerProtectionHookSkipsAddingAHookThatIsDisabledInHooksYml() {
        // hooks.yml's "hooks.<pluginName>" key lets a server owner disable a
        // specific protection hook without removing the softdepend plugin
        // entirely; registerProtectionHook() must honor that and never add
        // the hook to the active list, even though it still validates the
        // hook and its plugin first.
        String hookPluginName = plugin.getDescription().getName();
        plugin.getHooksFile().getConfig().set("hooks." + hookPluginName, false);

        plugin.registerProtectionHook(new StubHook(plugin, false));

        assertTrue(plugin.canBuild(server.addPlayer(), new Location(world, 0, 64, 0)),
                "a disabled hook must never be added to the active protection-hook list, "
                        + "so canBuild() has nothing to consult and defaults to true");
    }

    // ---- onDisable() / saveToFile() ----

    @Test
    void onDisableSkipsSavingAHopperWithNoLevelInsteadOfThrowing() {
        // saveToFile() (run on every onDisable() and on a repeating scheduled
        // task) iterates every known hopper and reads hopper.getLevel().getLevel()
        // to persist it; a hopper with no level at all (e.g. one left in a
        // half-initialized state) must be skipped via its own guard clause
        // rather than NPE-ing and aborting the save of every other hopper.
        // If that guard clause were ever removed or broken, this call would
        // throw a NullPointerException instead of completing normally.
        EHopper incomplete = new EHopper(new Location(world, 1, 64, 1), null, null, null,
                null, new EFilter(), TeleportTrigger.DISABLED, null);
        plugin.getHopperManager().addHopper(new Location(world, 1, 64, 1), incomplete);

        assertDoesNotThrow(plugin::onDisable);
    }
}
