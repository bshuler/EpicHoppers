package com.songoda.epichoppers.storage;

import com.songoda.epichoppers.EpicHoppersPlugin;
import com.songoda.epichoppers.utils.YamlDataFile;

import java.util.List;

public abstract class Storage {

    protected final EpicHoppersPlugin instance;
    protected final YamlDataFile dataFile;

    public Storage(EpicHoppersPlugin instance) {
        this.instance = instance;
        this.dataFile = new YamlDataFile(instance, "data.yml");
        this.dataFile.getConfig().options().copyDefaults(true);
        this.dataFile.saveConfig();
    }

    public abstract boolean containsGroup(String group);

    public abstract List<StorageRow> getRowsByGroup(String group);

    public abstract void clearFile();

    public abstract void saveItem(String group, StorageItem... items);

    public abstract void closeConnection();

}
