package com.songoda.epichoppers.storage;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StorageRowTest {

    @Test
    void getKeyReturnsConstructorKey() {
        StorageRow row = new StorageRow("row-key", new HashMap<>());
        assertEquals("row-key", row.getKey());
    }

    @Test
    void getReturnsTheStoredItemWhenPresent() {
        Map<String, StorageItem> items = new HashMap<>();
        StorageItem item = new StorageItem("value");
        items.put("field", item);
        StorageRow row = new StorageRow("row-key", items);

        assertSame(item, row.get("field"));
    }

    @Test
    void getReturnsANullSentinelItemWhenKeyIsMissing() {
        StorageRow row = new StorageRow("row-key", new HashMap<>());

        StorageItem sentinel = row.get("missing");
        assertNull(sentinel.asString());
        assertNull(sentinel.asObject());
    }

    @Test
    void getItemsIsUnmodifiable() {
        Map<String, StorageItem> items = new HashMap<>();
        items.put("field", new StorageItem("value"));
        StorageRow row = new StorageRow("row-key", items);

        Map<String, StorageItem> exposed = row.getItems();
        assertEquals(1, exposed.size());
        assertThrows(UnsupportedOperationException.class, () -> exposed.put("other", new StorageItem("x")));
    }
}
