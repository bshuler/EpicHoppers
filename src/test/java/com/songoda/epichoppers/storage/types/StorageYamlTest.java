package com.songoda.epichoppers.storage.types;

import com.songoda.epichoppers.EpicHoppersPlugin;
import com.songoda.epichoppers.storage.StorageItem;
import com.songoda.epichoppers.storage.StorageRow;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

// NOTE: StorageRow's lookup method is named get(String), and it never
// returns a Java null for a missing key - it returns a StorageItem wrapping
// a null object instead (see StorageRow#get). Tests below assert against
// asObject()/asInt()/asString() rather than expecting a null StorageItem.

class StorageYamlTest {

    private ServerMock server;
    private EpicHoppersPlugin plugin;
    private File dataFile;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(EpicHoppersPlugin.class);
        dataFile = new File(plugin.getDataFolder(), "data.yml");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void containsGroupReturnsFalseForAGroupThatWasNeverSaved() {
        StorageYaml storage = new StorageYaml(plugin);
        assertFalse(storage.containsGroup("sync"));
    }

    @Test
    void containsGroupReturnsTrueAfterAnItemIsSavedIntoThatGroup() {
        StorageYaml storage = new StorageYaml(plugin);
        storage.saveItem("sync", new StorageItem("location", "world;0;0;0"),
                new StorageItem("level", 1));

        assertTrue(storage.containsGroup("sync"));
    }

    @Test
    void saveItemSkipsNullItemsAndNullValuedItems() {
        StorageYaml storage = new StorageYaml(plugin);
        storage.saveItem("sync",
                new StorageItem("location", "world;0;0;0"),
                null,
                new StorageItem("block", (Object) null),
                new StorageItem("level", 5));

        List<StorageRow> rows = storage.getRowsByGroup("sync");
        assertEquals(1, rows.size());
        StorageRow row = rows.get(0);
        // Only "location" (the row key itself) and "level" were ever written;
        // the null entry and the null-valued item must be entirely absent
        // from the row rather than stored as literal nulls.
        assertEquals(5, row.get("level").asInt());
        assertNull(row.get("block").asObject());
    }

    @Test
    void getRowsByGroupRoundTripsEverySavedFieldForItsRow() {
        StorageYaml storage = new StorageYaml(plugin);
        storage.saveItem("sync",
                new StorageItem("location", "world;1;2;3"),
                new StorageItem("level", 4),
                new StorageItem("placedby", "some-uuid"));

        List<StorageRow> rows = storage.getRowsByGroup("sync");
        assertEquals(1, rows.size());
        StorageRow row = rows.get(0);
        assertEquals("world;1;2;3", row.getKey());
        assertEquals(4, row.get("level").asInt());
        assertEquals("some-uuid", row.get("placedby").asString());
    }

    @Test
    void getRowsByGroupSkipsARowThatEndsUpWithNoItems() throws Exception {
        // A row key whose section has no children at all (e.g. a stray
        // empty mapping left behind by manual editing or a partial write)
        // means the inner key2 loop never runs and "items" stays empty -
        // getRowsByGroup's "items.isEmpty() -> continue" guard must skip
        // that row rather than emitting an empty StorageRow.
        YamlConfiguration raw = new YamlConfiguration();
        raw.createSection("data.sync.world;9;9;9");
        raw.save(dataFile);

        StorageYaml storage = new StorageYaml(plugin);
        assertTrue(storage.containsGroup("sync"));
        assertTrue(storage.getRowsByGroup("sync").isEmpty());
    }

    @Test
    void getRowsByGroupConvertsANestedSectionIntoAnInlineListString() throws Exception {
        // A hand-written data.yml (e.g. from manual editing, or an older
        // on-disk format) can have a nested mapping at a field's path
        // instead of a flat scalar; getConfig().get(path) then returns a
        // MemorySection rather than a String, and getRowsByGroup must
        // convert it to the "key:value;" inline form instead of storing the
        // MemorySection object itself.
        YamlConfiguration raw = new YamlConfiguration();
        raw.set("data.sync.world;5;5;5.whitelist.STONE", 1);
        raw.set("data.sync.world;5;5;5.whitelist.DIRT", 2);
        raw.save(dataFile);

        StorageYaml storage = new StorageYaml(plugin);
        List<StorageRow> rows = storage.getRowsByGroup("sync");

        assertEquals(1, rows.size());
        String inline = rows.get(0).get("whitelist").asString();
        assertTrue(inline.contains("STONE:1;"));
        assertTrue(inline.contains("DIRT:2;"));
    }

    @Test
    void clearFileRemovesEveryPreviouslySavedGroup() {
        StorageYaml storage = new StorageYaml(plugin);
        storage.saveItem("sync", new StorageItem("location", "world;0;0;0"),
                new StorageItem("level", 1));
        assertTrue(storage.containsGroup("sync"));

        storage.clearFile();

        assertFalse(storage.containsGroup("sync"));
    }

    @Test
    void closeConnectionPersistsSavedDataToDisk() {
        StorageYaml storage = new StorageYaml(plugin);
        storage.saveItem("sync", new StorageItem("location", "world;7;7;7"),
                new StorageItem("level", 2));

        storage.closeConnection();

        YamlConfiguration onDisk = YamlConfiguration.loadConfiguration(dataFile);
        assertEquals(2, onDisk.getInt("data.sync.world;7;7;7.level"));
    }
}
