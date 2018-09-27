package com.songoda.epichoppers.storage;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

public class StorageItem {

    private String key = null;

    private final Object object;

    public StorageItem(Object object) {
        this.object = object;
    }

    public StorageItem(String key, Object object) {
        this.key = key;
        this.object = object;
    }

    public StorageItem(String key, List<Material> material) {
        String object = "";
        for (Material m : material) {
            object += m.name() + ";";
        }
        this.key = key;
        this.object = object;
    }

    public String getKey() {
        return key;
    }

    public String asString() {
        if (object == null) return null;
        return (String)object;
    }

    public boolean asBoolean() {
        if (object == null) return false;
        return (boolean)object;
    }

    public int asInt() {
        if (object == null) return 0;
        return (int)object;
    }

    public Object asObject() {
        return object;
    }

    public List<Material> asMaterialList() {
        List<Material> list = new ArrayList<>();
        if (object == null) return list;
        String[] stack = ((String)object).split(";");
        for (String item : stack) {
            if (item.equals("")) continue;
            list.add(Material.valueOf(item));
        }
        return list;
    }
}
