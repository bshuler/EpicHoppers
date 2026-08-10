package com.songoda.epichoppers.utils;

import com.songoda.epichoppers.EpicHoppersPlugin;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by songoda on 2/24/2017.
 *
 * Formatting/GUI/serialization helpers formerly provided by the Arconix
 * library. Hand-written, Bukkit-native replacements; see CLAUDE.md for why
 * Arconix was removed.
 */
public class Methods {

    public static boolean isSync(Player p) {
        try {
            ItemStack hand = p.getInventory().getItemInMainHand();
            if (hand.hasItemMeta()
                    && hand.getType() != Material.AIR
                    && hand.getItemMeta().hasLore()) {
                for (String str : hand.getItemMeta().getLore()) {
                    if (str.equals(formatText("&7Sync Touch")) || str.equals(formatText("&aSync Touch"))) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            Debugger.runReport(e);
        }
        return false;
    }

    public static ItemStack getGlass() {
        try {
            EpicHoppersPlugin instance = EpicHoppersPlugin.getInstance();
            return getGlassPane(instance.getConfig().getBoolean("Interfaces.Replace Glass Type 1 With Rainbow Glass"), instance.getConfig().getInt("Interfaces.Glass Type 1"));
        } catch (Exception e) {
            Debugger.runReport(e);
        }
        return null;
    }

    public static ItemStack getBackgroundGlass(boolean type) {
        try {
            EpicHoppersPlugin instance = EpicHoppersPlugin.getInstance();
            if (type)
                return getGlassPane(false, instance.getConfig().getInt("Interfaces.Glass Type 2"));
            else
                return getGlassPane(false, instance.getConfig().getInt("Interfaces.Glass Type 3"));
        } catch (Exception e) {
            Debugger.runReport(e);
        }
        return null;
    }

    private static ItemStack getGlassPane(boolean rainbow, int dyeData) {
        DyeColor color = rainbow
                ? DyeColor.values()[(int) (Math.random() * DyeColor.values().length)]
                : DyeColor.getByWoolData((byte) dyeData);
        if (color == null) color = DyeColor.WHITE;
        Material mat = Material.matchMaterial(color.name() + "_STAINED_GLASS_PANE");
        if (mat == null) mat = Material.WHITE_STAINED_GLASS_PANE;
        ItemStack glass = new ItemStack(mat);
        ItemMeta meta = glass.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            glass.setItemMeta(meta);
        }
        return glass;
    }

    public static String formatName(int level, boolean full) {
        try {
            EpicHoppersPlugin instance = EpicHoppersPlugin.getInstance();
            String name = instance.getLocale().getMessage("general.nametag.nameformat", level);

            String info = "";
            if (full) {
                info += toHiddenString(level + ":");
            }

            return info + formatText(name);
        } catch (Exception e) {
            Debugger.runReport(e);
        }
        return null;
    }

    /**
     * Bukkit-native replacement for Arconix's {@code TextComponent.formatText}:
     * translates {@code &}-prefixed color codes.
     */
    public static String formatText(String text) {
        if (text == null) return null;
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', text);
    }

    /**
     * Bukkit-native replacement for Arconix's
     * {@code format().convertToInvisibleString}: hides a string inside a
     * chat-color escape sequence so it can be smuggled into item lore
     * without being visibly rendered.
     */
    public static String toHiddenString(String text) {
        if (text == null) return "";
        StringBuilder builder = new StringBuilder();
        for (char c : text.toCharArray()) {
            builder.append(org.bukkit.ChatColor.COLOR_CHAR).append(c);
        }
        return builder.toString();
    }

    /**
     * Bukkit-native replacement for Arconix's
     * {@code serialize().serializeLocation}.
     */
    public static String serializeLocation(org.bukkit.block.Block block) {
        return serializeLocation(block.getLocation());
    }

    public static String serializeLocation(Location location) {
        if (location == null || location.getWorld() == null) return "";
        return location.getWorld().getName() + ";"
                + location.getBlockX() + ";"
                + location.getBlockY() + ";"
                + location.getBlockZ();
    }

    /**
     * Bukkit-native replacement for Arconix's
     * {@code serialize().unserializeLocation}. Accepts the
     * {@code world;x;y;z} format produced by {@link #serializeLocation}.
     */
    public static Location unserializeLocation(String serialized) {
        try {
            if (serialized == null || serialized.isEmpty()) return null;
            String[] parts = serialized.split(";");
            if (parts.length < 4) return null;
            World world = org.bukkit.Bukkit.getWorld(parts[0]);
            if (world == null) return null;
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            int z = Integer.parseInt(parts[3]);
            return new Location(world, x, y, z);
        } catch (Exception e) {
            Debugger.runReport(e);
        }
        return null;
    }

    /**
     * Bukkit-native replacement for Arconix's {@code format().formatEconomy}:
     * renders a currency amount with thousands separators and two decimal
     * places.
     */
    public static String formatEconomy(double amount) {
        return String.format("%,.2f", amount);
    }

    /**
     * Bukkit-native replacement for Arconix's {@code TimeComponent.makeReadable}:
     * renders a millisecond duration as {@code Hh Mm Ss}.
     */
    public static String makeReadable(long millis) {
        if (millis < 0) millis = 0;
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;
        StringBuilder builder = new StringBuilder();
        if (hours > 0) builder.append(hours).append("h ");
        if (hours > 0 || minutes > 0) builder.append(minutes).append("m ");
        builder.append(seconds).append("s");
        return builder.toString();
    }

    public static void doParticles(Player p, Location location) {
        try {
            EpicHoppersPlugin instance = EpicHoppersPlugin.getInstance();
            location.setX(location.getX() + .5);
            location.setY(location.getY() + .5);
            location.setZ(location.getZ() + .5);
            p.getWorld().spawnParticle(resolveParticle(instance.getConfig().getString("Main.Upgrade Particle Type")), location, 200, .5, .5, .5);
        } catch (Exception e) {
            Debugger.runReport(e);
        }
    }

    /**
     * Bukkit-native replacement for Arconix's
     * {@code packetLibrary.getParticleManager().broadcastParticle(...)}.
     * Note the count-last parameter order, preserved from the original
     * Arconix call sites in ModuleSuction/ModuleBlockBreak.
     */
    public static void broadcastParticle(Location location, double offsetX, double offsetY, double offsetZ, double speed, String particleType, int count) {
        try {
            if (location == null || location.getWorld() == null || particleType == null) return;
            Particle particle = resolveParticle(particleType);
            location.getWorld().spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, speed);
        } catch (Exception e) {
            Debugger.runReport(e);
        }
    }

    /**
     * Known old-name/new-name pairs for {@link Particle} constants renamed
     * around the Minecraft 1.20.5 Bukkit API bump (e.g. {@code SPELL_WITCH}
     * -&gt; {@code WITCH}, {@code SPELL} -&gt; {@code EFFECT}). Kept as a
     * simple symmetric map so a lookup in either direction finds the other
     * name.
     */
    private static final Map<String, String> PARTICLE_ALIASES = new HashMap<>();

    static {
        PARTICLE_ALIASES.put("WITCH", "SPELL_WITCH");
        PARTICLE_ALIASES.put("SPELL_WITCH", "WITCH");
        PARTICLE_ALIASES.put("EFFECT", "SPELL");
        PARTICLE_ALIASES.put("SPELL", "EFFECT");
    }

    /**
     * Resolves a {@link Particle} by name, tolerating the ~1.20.5 Bukkit
     * {@code Particle} enum rename. Paper API versions before that rename
     * (1.18.2/1.19.4/1.20.1) only have the old names ({@code SPELL_WITCH},
     * {@code SPELL}); versions after it (1.21.11, 26.2) only have the new
     * names ({@code WITCH}, {@code EFFECT}). Tries the requested name first,
     * then falls back to its known alias, so a single config value (old or
     * new) resolves correctly regardless of which Paper API the server is
     * running. Pattern mirrors the {@code resolveParticle} helper in the
     * sibling Spigot-InvUnload plugin.
     */
    public static Particle resolveParticle(String name) {
        try {
            return Particle.valueOf(name);
        } catch (IllegalArgumentException ex) {
            String alias = PARTICLE_ALIASES.get(name);
            if (alias != null) {
                try {
                    return Particle.valueOf(alias);
                } catch (IllegalArgumentException ignored) {
                    // fall through to rethrow the original failure below
                }
            }
            throw ex;
        }
    }
}
