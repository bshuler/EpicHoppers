package com.songoda.epichoppers.utils;

import com.songoda.epichoppers.EpicHoppersPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YamlDataFileTest {

    private ServerMock server;
    private EpicHoppersPlugin plugin;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(EpicHoppersPlugin.class);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void getConfigReturnsAConfigBackedByARealFileOnDisk() {
        YamlDataFile dataFile = new YamlDataFile(plugin, "real-data.yml");

        assertNotNull(dataFile.getConfig());
        assertTrue(new File(plugin.getDataFolder(), "real-data.yml").exists());
    }

    @Test
    void constructorSwallowsAnIOExceptionWhenTheFileCannotBeCreated() throws IOException {
        // Pre-create "blocked" as a plain FILE (not a directory) inside the
        // data folder. The constructor's file.getParentFile().mkdirs() then
        // fails (a regular file already occupies that path segment), so the
        // subsequent createNewFile() throws a genuine IOException - real
        // filesystem contention, not a reflection hack - which the
        // constructor's own catch block must swallow rather than propagate.
        File blockedParent = new File(plugin.getDataFolder(), "blocked");
        plugin.getDataFolder().mkdirs();
        assertTrue(blockedParent.createNewFile());

        YamlDataFile dataFile = assertDoesNotThrow(() -> new YamlDataFile(plugin, "blocked/data.yml"));

        // The file was never actually created, but getConfig() still returns
        // a usable (empty) configuration rather than null.
        assertNotNull(dataFile.getConfig());
    }

    @Test
    void saveConfigSwallowsAnIOExceptionWhenTheFileCannotBeWritten() {
        YamlDataFile dataFile = new YamlDataFile(plugin, "unwritable.yml");
        File target = new File(plugin.getDataFolder(), "unwritable.yml");

        // Replace the real file on disk with a directory of the same name so
        // config.save(file) genuinely fails to open it for writing.
        assertTrue(target.delete());
        assertTrue(target.mkdirs());

        assertDoesNotThrow(dataFile::saveConfig);
    }
}
