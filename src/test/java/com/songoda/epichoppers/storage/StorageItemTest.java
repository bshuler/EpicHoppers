package com.songoda.epichoppers.storage;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StorageItemTest {

    @Test
    void keyIsNullWhenNotProvided() {
        StorageItem item = new StorageItem("value");
        assertNull(item.getKey());
    }

    @Test
    void keyIsReturnedWhenProvided() {
        StorageItem item = new StorageItem("mykey", "value");
        assertEquals("mykey", item.getKey());
    }

    @Test
    void asStringReturnsTheStringObject() {
        StorageItem item = new StorageItem("hello");
        assertEquals("hello", item.asString());
    }

    @Test
    void asStringReturnsNullWhenObjectIsNull() {
        StorageItem item = new StorageItem(null);
        assertNull(item.asString());
    }

    @Test
    void asBooleanReturnsTheBooleanObject() {
        assertTrue(new StorageItem(true).asBoolean());
        assertFalse(new StorageItem(false).asBoolean());
    }

    @Test
    void asBooleanReturnsFalseWhenObjectIsNull() {
        assertFalse(new StorageItem(null).asBoolean());
    }

    @Test
    void asIntReturnsTheIntObject() {
        assertEquals(42, new StorageItem(42).asInt());
    }

    @Test
    void asIntReturnsZeroWhenObjectIsNull() {
        assertEquals(0, new StorageItem(null).asInt());
    }

    @Test
    void asObjectReturnsTheRawObjectRegardlessOfType() {
        Object o = new Object();
        assertEquals(o, new StorageItem(o).asObject());
    }

    @Test
    void materialListConstructorSerializesToSemicolonJoinedNames() {
        StorageItem item = new StorageItem("key", Arrays.asList(Material.DIRT, Material.STONE));
        assertEquals("DIRT;STONE;", item.asString());
        assertEquals("key", item.getKey());
    }

    @Test
    void asMaterialListRoundTripsTheSerializedForm() {
        StorageItem item = new StorageItem("key", Arrays.asList(Material.DIRT, Material.STONE, Material.LAVA_BUCKET));
        List<Material> roundTripped = item.asMaterialList();
        assertEquals(Arrays.asList(Material.DIRT, Material.STONE, Material.LAVA_BUCKET), roundTripped);
    }

    @Test
    void asMaterialListOfEmptySourceListIsEmpty() {
        StorageItem item = new StorageItem("key", Collections.emptyList());
        assertTrue(item.asMaterialList().isEmpty());
    }

    @Test
    void asMaterialListReturnsEmptyListWhenObjectIsNull() {
        StorageItem item = new StorageItem(null);
        assertTrue(item.asMaterialList().isEmpty());
    }
}
