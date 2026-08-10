package com.songoda.epichoppers.hopper.levels;

import com.songoda.epichoppers.EpicHoppersPlugin;
import com.songoda.epichoppers.api.hopper.Hopper;
import com.songoda.epichoppers.api.hopper.levels.Level;
import com.songoda.epichoppers.api.hopper.levels.modules.Module;
import org.bukkit.Material;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ELevel's constructor calls EpicHoppersPlugin.getInstance().getLocale() to
 * build its description list, so every test here needs a fully loaded
 * plugin (not just a bare MockBukkit.mock() server).
 */
class ELevelManagerTest {

    private static final Module NO_OP_MODULE = new Module() {
        @Override public String getName() { return "stub"; }
        @Override public void run(Hopper hopper) { }
        @Override public List<Material> getBlockedItems(Hopper hopper) { return Collections.emptyList(); }
        @Override public String getDescription() { return "Stub module description"; }
    };

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
        MockBukkit.load(EpicHoppersPlugin.class);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void isLevelFalseWhenNotRegistered() {
        ELevelManager manager = new ELevelManager();
        assertFalse(manager.isLevel(1));
        assertNull(manager.getLevel(1));
    }

    @Test
    void addLevelRegistersItAndComputesADescription() {
        ELevelManager manager = new ELevelManager();
        manager.addLevel(1, 100, 50, 5, 2, true, true, new ArrayList<>());

        assertTrue(manager.isLevel(1));
        Level level = manager.getLevel(1);
        assertEquals(1, level.getLevel());
        assertEquals(100, level.getCostExperience());
        assertEquals(50, level.getCostEconomy());
        assertEquals(5, level.getRange());
        assertEquals(2, level.getAmount());
        assertTrue(level.isFilter());
        assertTrue(level.isTeleport());
        // range + amount + filter(true) + teleport(true) = 4 description lines.
        assertEquals(4, level.getDescription().size());
    }

    @Test
    void descriptionOmitsFilterAndTeleportLinesWhenBothDisabled() {
        ELevelManager manager = new ELevelManager();
        manager.addLevel(1, 0, 0, 1, 1, false, false, new ArrayList<>());

        Level level = manager.getLevel(1);
        // range + amount only = 2 description lines.
        assertEquals(2, level.getDescription().size());
    }

    @Test
    void descriptionIncludesOneLinePerRegisteredModule() {
        ELevelManager manager = new ELevelManager();
        ArrayList<Module> modules = new ArrayList<>();
        modules.add(NO_OP_MODULE);
        manager.addLevel(1, 0, 0, 1, 1, false, false, modules);

        Level level = manager.getLevel(1);
        assertEquals(3, level.getDescription().size());
        assertTrue(level.getDescription().contains("Stub module description"));
    }

    @Test
    void getDescriptionReturnsADefensiveCopy() {
        ELevelManager manager = new ELevelManager();
        manager.addLevel(1, 0, 0, 1, 1, false, false, new ArrayList<>());
        Level level = manager.getLevel(1);

        List<String> a = level.getDescription();
        List<String> b = level.getDescription();
        assertEquals(a, b);
        org.junit.jupiter.api.Assertions.assertNotSame(a, b);
    }

    @Test
    void getRegisteredModulesReturnsADefensiveCopy() {
        ELevelManager manager = new ELevelManager();
        ArrayList<Module> modules = new ArrayList<>();
        modules.add(NO_OP_MODULE);
        manager.addLevel(1, 0, 0, 1, 1, false, false, modules);
        Level level = manager.getLevel(1);

        List<Module> copy = level.getRegisteredModules();
        assertEquals(1, copy.size());
        copy.clear();
        assertEquals(1, level.getRegisteredModules().size());
    }

    @Test
    void addModuleAppendsToTheLiveRegisteredModulesList() {
        ELevelManager manager = new ELevelManager();
        manager.addLevel(1, 0, 0, 1, 1, false, false, new ArrayList<>());
        Level level = manager.getLevel(1);

        level.addModule(NO_OP_MODULE);
        assertEquals(1, level.getRegisteredModules().size());
    }

    @Test
    void getLowestAndHighestLevelReflectRegisteredKeys() {
        ELevelManager manager = new ELevelManager();
        manager.addLevel(1, 0, 0, 1, 1, false, false, new ArrayList<>());
        manager.addLevel(5, 0, 0, 1, 1, false, false, new ArrayList<>());
        manager.addLevel(3, 0, 0, 1, 1, false, false, new ArrayList<>());

        assertEquals(1, manager.getLowestLevel().getLevel());
        assertEquals(5, manager.getHighestLevel().getLevel());
    }

    @Test
    void getLowestLevelThrowsWhenNoLevelsAreRegistered() {
        ELevelManager manager = new ELevelManager();
        assertThrows(NullPointerException.class, manager::getLowestLevel);
    }

    @Test
    void getLevelsIsUnmodifiableAndReflectsAllRegistrations() {
        ELevelManager manager = new ELevelManager();
        manager.addLevel(1, 0, 0, 1, 1, false, false, new ArrayList<>());
        manager.addLevel(2, 0, 0, 1, 1, false, false, new ArrayList<>());

        assertEquals(2, manager.getLevels().size());
        assertThrows(UnsupportedOperationException.class,
                () -> manager.getLevels().put(3, manager.getLevel(1)));
    }

    @Test
    void clearRemovesAllRegisteredLevels() {
        ELevelManager manager = new ELevelManager();
        manager.addLevel(1, 0, 0, 1, 1, false, false, new ArrayList<>());
        manager.clear();

        assertFalse(manager.isLevel(1));
        assertTrue(manager.getLevels().isEmpty());
    }
}
