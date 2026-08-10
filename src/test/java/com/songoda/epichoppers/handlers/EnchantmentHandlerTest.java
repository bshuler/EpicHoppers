package com.songoda.epichoppers.handlers;

import com.songoda.epichoppers.EpicHoppersPlugin;
import com.songoda.epichoppers.utils.Methods;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnchantmentHandlerTest {

    private ServerMock server;
    private EnchantmentHandler handler;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        MockBukkit.load(EpicHoppersPlugin.class);
        handler = new EnchantmentHandler();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void createSyncTouchEncodesTheBlockLocationWhenABlockIsGiven() {
        WorldMock world = server.addSimpleWorld("world");
        Block block = world.getBlockAt(1, 2, 3);

        ItemStack item = handler.createSyncTouch(new ItemStack(Material.PAPER), block);

        assertNotNull(item);
        assertEquals(2, item.getItemMeta().getLore().size());
        assertEquals(org.bukkit.ChatColor.GREEN + "Sync Touch", item.getItemMeta().getLore().get(0));
    }

    @Test
    void createSyncTouchStoresTheLocationInThePersistentDataContainerUncorrupted() {
        // The old design smuggled the location into the second lore line via
        // a hidden-color-code trick, which modern Adventure-based ItemMeta
        // lore round-tripping silently corrupts (any character that is also
        // a valid legacy color code gets eaten as formatting). The location
        // must survive intact by reading it back from the PDC, not the lore.
        WorldMock world = server.addSimpleWorld("world");
        Block block = world.getBlockAt(1, 2, 3);

        ItemStack item = handler.createSyncTouch(new ItemStack(Material.PAPER), block);

        String encoded = item.getItemMeta().getPersistentDataContainer()
                .get(EnchantmentHandler.syncLocationKey(), PersistentDataType.STRING);
        assertNotNull(encoded);
        assertEquals(block.getLocation(), Methods.unserializeLocation(encoded));
    }

    @Test
    void createSyncTouchUsesTheUnboundVariantWhenNoBlockIsGiven() {
        ItemStack item = handler.createSyncTouch(new ItemStack(Material.PAPER), null);

        assertNotNull(item);
        assertEquals(1, item.getItemMeta().getLore().size());
        assertEquals(org.bukkit.ChatColor.GRAY + "Sync Touch", item.getItemMeta().getLore().get(0));
    }

    @Test
    void createSyncTouchReturnsNullWhenTheGivenItemHasNoItemMeta() {
        // Material.AIR items have no ItemMeta (getItemMeta() returns null on
        // real Bukkit/MockBukkit alike), so dereferencing it inside the
        // b != null branch is a genuine, reachable NPE - not a contrived one -
        // caught by createSyncTouch's own catch block.
        WorldMock world = server.addSimpleWorld("world");
        Block block = world.getBlockAt(1, 2, 3);

        ItemStack result = handler.createSyncTouch(new ItemStack(Material.AIR), block);

        assertNull(result);
    }

    @Test
    void getbookReturnsAnEnchantedBookWithSyncTouchLoreAndDisplayName() {
        ItemStack book = handler.getbook();

        assertNotNull(book);
        assertEquals(Material.ENCHANTED_BOOK, book.getType());
        assertEquals(org.bukkit.ChatColor.YELLOW + "Enchanted Book", book.getItemMeta().getDisplayName());
        assertTrue(book.getItemMeta().getLore().get(0).endsWith("Sync Touch"));
    }
}
