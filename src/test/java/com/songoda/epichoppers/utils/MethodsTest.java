package com.songoda.epichoppers.utils;

import com.songoda.epichoppers.EpicHoppersPlugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers every pure helper in {@link Methods}, plus its plugin-instance
 * dependent methods via a real {@code MockBukkit.load(EpicHoppersPlugin)}.
 * {@code resolveParticle} is exercised in both alias directions per the
 * modernization brief: a direct hit on the current-API name ({@code WITCH}/
 * {@code EFFECT}) and a fallback from the pre-1.20.5 legacy name
 * ({@code SPELL_WITCH}/{@code SPELL}), since this Paper target (see
 * build.gradle.kts's {@code testPaperApiVersion}) only defines the new
 * names.
 */
class MethodsTest {

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

    // -- formatText --------------------------------------------------------

    @Test
    void formatTextTranslatesColorCodes() {
        assertEquals(org.bukkit.ChatColor.GREEN + "hi", Methods.formatText("&ahi"));
    }

    @Test
    void formatTextNullIsNull() {
        assertNull(Methods.formatText(null));
    }

    // -- toHiddenString ------------------------------------------------------

    @Test
    void toHiddenStringInsertsColorCharBeforeEachCharacter() {
        String hidden = Methods.toHiddenString("ab");
        assertEquals(4, hidden.length());
        assertEquals(org.bukkit.ChatColor.COLOR_CHAR, hidden.charAt(0));
        assertEquals('a', hidden.charAt(1));
        assertEquals(org.bukkit.ChatColor.COLOR_CHAR, hidden.charAt(2));
        assertEquals('b', hidden.charAt(3));
    }

    @Test
    void toHiddenStringNullIsEmpty() {
        assertEquals("", Methods.toHiddenString(null));
    }

    // -- serializeLocation / unserializeLocation ----------------------------

    @Test
    void serializeThenUnserializeLocationRoundTrips() {
        World world = server.addSimpleWorld("test-world");
        Location location = new Location(world, 10, 20, 30);

        String serialized = Methods.serializeLocation(location);
        assertEquals("test-world;10;20;30", serialized);

        Location roundTripped = Methods.unserializeLocation(serialized);
        assertNotNull(roundTripped);
        assertEquals(world, roundTripped.getWorld());
        assertEquals(10, roundTripped.getBlockX());
        assertEquals(20, roundTripped.getBlockY());
        assertEquals(30, roundTripped.getBlockZ());
    }

    @Test
    void serializeLocationFromBlockDelegatesToLocationOverload() {
        World world = server.addSimpleWorld("block-world");
        org.bukkit.block.Block block = world.getBlockAt(1, 2, 3);
        assertEquals("block-world;1;2;3", Methods.serializeLocation(block));
    }

    @Test
    void serializeLocationNullOrNoWorldIsEmptyString() {
        assertEquals("", Methods.serializeLocation((Location) null));
    }

    @Test
    void unserializeLocationRejectsNullOrEmpty() {
        assertNull(Methods.unserializeLocation(null));
        assertNull(Methods.unserializeLocation(""));
    }

    @Test
    void unserializeLocationRejectsTooFewParts() {
        assertNull(Methods.unserializeLocation("world;1;2"));
    }

    @Test
    void unserializeLocationRejectsUnknownWorld() {
        assertNull(Methods.unserializeLocation("no-such-world;1;2;3"));
    }

    @Test
    void unserializeLocationRejectsNonNumericComponent() {
        World world = server.addSimpleWorld("bad-number-world");
        assertNull(Methods.unserializeLocation("bad-number-world;x;2;3"));
    }

    // -- formatEconomy -------------------------------------------------------

    @Test
    void formatEconomyAddsThousandsSeparatorAndTwoDecimals() {
        assertEquals("1,234.50", Methods.formatEconomy(1234.5));
        assertEquals("0.00", Methods.formatEconomy(0));
    }

    // -- makeReadable ---------------------------------------------------------

    @Test
    void makeReadableRendersSecondsOnlyBelowOneMinute() {
        assertEquals("5s", Methods.makeReadable(5_000));
    }

    @Test
    void makeReadableRendersMinutesAndSeconds() {
        assertEquals("2m 5s", Methods.makeReadable(125_000));
    }

    @Test
    void makeReadableRendersHoursMinutesAndSeconds() {
        assertEquals("1h 1m 1s", Methods.makeReadable(3_661_000));
    }

    @Test
    void makeReadableClampsNegativeToZero() {
        assertEquals("0s", Methods.makeReadable(-500));
    }

    // -- resolveParticle: both alias directions ------------------------------

    @Test
    void resolveParticleDirectMatchOnCurrentApiName() {
        // WITCH is the current (post-1.20.5) name and exists directly on
        // this Paper target - no alias fallback needed.
        assertEquals(Particle.WITCH, Methods.resolveParticle("WITCH"));
        assertEquals(Particle.EFFECT, Methods.resolveParticle("EFFECT"));
    }

    @Test
    void resolveParticleFallsBackFromLegacyWitchAlias() {
        // SPELL_WITCH does not exist on this Paper target; resolveParticle
        // must catch the IllegalArgumentException from the direct lookup
        // and fall back through PARTICLE_ALIASES to WITCH.
        assertEquals(Particle.WITCH, Methods.resolveParticle("SPELL_WITCH"));
    }

    @Test
    void resolveParticleFallsBackFromLegacySpellAlias() {
        // SPELL does not exist on this Paper target; falls back to EFFECT.
        assertEquals(Particle.EFFECT, Methods.resolveParticle("SPELL"));
    }

    @Test
    void resolveParticleDirectMatchForAnyOtherConstant() {
        assertEquals(Particle.LAVA, Methods.resolveParticle("LAVA"));
    }

    @Test
    void resolveParticleWithNoAliasAndNoDirectMatchThrows() {
        assertThrows(IllegalArgumentException.class, () -> Methods.resolveParticle("NOT_A_REAL_PARTICLE"));
    }

    // -- isSync ----------------------------------------------------------------

    @Test
    void isSyncFalseWhenHandIsEmpty() {
        PlayerMock player = server.addPlayer();
        assertFalse(Methods.isSync(player));
    }

    @Test
    void isSyncTrueWhenMainHandHasSyncTouchLore() {
        PlayerMock player = server.addPlayer();
        ItemStack book = new ItemStack(Material.BOOK);
        ItemMeta meta = book.getItemMeta();
        meta.setLore(List.of(Methods.formatText("&7Sync Touch")));
        book.setItemMeta(meta);
        player.getInventory().setItemInMainHand(book);

        assertTrue(Methods.isSync(player));
    }

    @Test
    void isSyncFalseWhenLorePresentButDoesNotMatch() {
        PlayerMock player = server.addPlayer();
        ItemStack book = new ItemStack(Material.BOOK);
        ItemMeta meta = book.getItemMeta();
        meta.setLore(Collections.singletonList("Just a regular book"));
        book.setItemMeta(meta);
        player.getInventory().setItemInMainHand(book);

        assertFalse(Methods.isSync(player));
    }

    // -- getGlass / getBackgroundGlass ------------------------------------------

    @Test
    void getGlassReturnsAStainedGlassPane() {
        ItemStack glass = Methods.getGlass();
        assertNotNull(glass);
        assertTrue(glass.getType().name().endsWith("_STAINED_GLASS_PANE"));
    }

    @Test
    void getBackgroundGlassReturnsAStainedGlassPaneForBothTypes() {
        ItemStack a = Methods.getBackgroundGlass(true);
        ItemStack b = Methods.getBackgroundGlass(false);
        assertNotNull(a);
        assertNotNull(b);
        assertTrue(a.getType().name().endsWith("_STAINED_GLASS_PANE"));
        assertTrue(b.getType().name().endsWith("_STAINED_GLASS_PANE"));
    }

    // -- formatName --------------------------------------------------------

    @Test
    void formatNameWithoutFullOmitsHiddenLevelPrefix() {
        String withoutFull = Methods.formatName(1, false);
        String withFull = Methods.formatName(1, true);
        assertNotNull(withoutFull);
        assertNotNull(withFull);
        // "full" prepends a hidden ("1:") tag ahead of the same formatted
        // name, so it must be strictly longer than the non-full variant by
        // exactly the hidden tag's length, and end with the same text.
        assertEquals(Methods.toHiddenString("1:").length() + withoutFull.length(), withFull.length());
        assertTrue(withFull.endsWith(withoutFull));
    }

    @Test
    void formatNameWithFullIncludesHiddenLevelPrefix() {
        String withFull = Methods.formatName(3, true);
        assertNotNull(withFull);
        assertTrue(withFull.startsWith(Methods.toHiddenString("3:")));
    }

    // -- doParticles / broadcastParticle -------------------------------------

    @Test
    void doParticlesDoesNotThrowForAnOnlinePlayer() {
        PlayerMock player = server.addPlayer();
        World world = player.getWorld();
        Methods.doParticles(player, new Location(world, 0, 65, 0));
        // No assertion beyond "did not throw" - MockBukkit's WorldMock
        // records spawnParticle calls but this plugin does not expose a
        // way to inspect them beyond not throwing.
    }

    @Test
    void broadcastParticleDoesNotThrowWithValidLocationAndType() {
        World world = server.addSimpleWorld("particle-world");
        Methods.broadcastParticle(new Location(world, 0, 65, 0), .5, .5, .5, .1, "WITCH", 10);
    }

    @Test
    void broadcastParticleNoOpsOnNullLocation() {
        Methods.broadcastParticle(null, 0, 0, 0, 0, "WITCH", 1);
    }

    @Test
    void broadcastParticleNoOpsOnNullParticleType() {
        World world = server.addSimpleWorld("particle-world-2");
        Methods.broadcastParticle(new Location(world, 0, 65, 0), 0, 0, 0, 0, null, 1);
    }
}
