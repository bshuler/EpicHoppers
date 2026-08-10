package com.songoda.epichoppers.handlers;

import com.songoda.epichoppers.EpicHoppersPlugin;
import com.songoda.epichoppers.utils.Debugger;
import com.songoda.epichoppers.utils.Methods;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;

/**
 * Created by songoda on 3/22/2017.
 */
public class EnchantmentHandler {

    /**
     * Where the synced-block's location is actually stored on a "Sync Touch"
     * item. The location used to be smuggled into the second lore line via
     * {@link Methods#toHiddenString(String)} (a §-before-every-character
     * trick), which relied on ItemMeta lore staying a raw, untouched string.
     * Modern Paper/MockBukkit round-trips {@code setLore}/{@code getLore}
     * through Adventure legacy-component (de)serialization, which silently
     * *consumes* any character that happens to also be a valid legacy color
     * code (0-9, a-f, k-o, r) instead of preserving it as text - and a
     * serialized "world;x;y;z" string is virtually guaranteed to contain
     * several of those. That corrupted the stored location on every modern
     * target (confirmed via a failing test: a real serialized location came
     * back mangled and un-parseable). The lore line is still written for
     * backwards-appearance/the "exactly 2 lore lines" checks elsewhere, but
     * the location itself now lives in the PersistentDataContainer, which
     * stores the raw string in NBT and is not subject to that round-trip.
     * Computed lazily (not a cached static field) so it always reflects
     * whichever {@code EpicHoppersPlugin} instance is currently live - a
     * cached {@code static final} would either NPE if this class loads
     * before the plugin enables, or (worse) permanently poison the class
     * with an {@code ExceptionInInitializerError} for the rest of the JVM.
     */
    public static NamespacedKey syncLocationKey() {
        return new NamespacedKey(EpicHoppersPlugin.getInstance(), "sync-touch-location");
    }

    public ItemStack createSyncTouch(ItemStack item, Block b) {
        try {
            ItemMeta itemmeta = item.getItemMeta();
            ArrayList<String> lore = new ArrayList<String>();
            if (b != null) {
                lore.add(Methods.formatText("&aSync Touch"));
                lore.add(Methods.toHiddenString(Methods.serializeLocation(b)));
                itemmeta.getPersistentDataContainer().set(syncLocationKey(), PersistentDataType.STRING,
                        Methods.serializeLocation(b));
            } else {
                lore.add(Methods.formatText("&7Sync Touch"));
            }
            itemmeta.setLore(lore);
            item.setItemMeta(itemmeta);
            return item;
        } catch (Exception e) {
            Debugger.runReport(e);
        }
        return null;
    }

    public ItemStack getbook() {
        try {
            ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
            ItemMeta meta = book.getItemMeta();
            meta.setDisplayName(Methods.formatText("&eEnchanted Book"));

            ArrayList<String> lore = new ArrayList<>();
            lore.add(Methods.formatText("&7Sync Touch"));
            meta.setLore(lore);
            book.setItemMeta(meta);
            return book;
        } catch (Exception e) {
            Debugger.runReport(e);
        }
        return null;
    }
}
