package com.songoda.epichoppers;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Locale's constructor is private - the only way to obtain a real instance
 * is through the static init/saveDefaultLocale path that
 * EpicHoppersPlugin.onEnable() already drives, so these tests exercise the
 * locale the real plugin loads (en_US) rather than constructing one
 * directly.
 */
class LocaleTest {

    private EpicHoppersPlugin plugin;

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
        plugin = MockBukkit.load(EpicHoppersPlugin.class);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void getMessageTranslatesColorCodesForAKnownNode() {
        Locale locale = plugin.getLocale();
        String message = locale.getMessage("general.nametag.next");
        assertEquals(org.bukkit.ChatColor.BLUE + "Next", message);
    }

    @Test
    void getMessageReturnsTheNodeItselfWhenUnknown() {
        Locale locale = plugin.getLocale();
        assertEquals("no.such.node", locale.getMessage("no.such.node"));
    }

    @Test
    void getMessageWithArgsReplacesPlaceholdersInOrder() {
        Locale locale = plugin.getLocale();
        String message = locale.getMessage("interface.hopper.range", 7);
        assertTrue(message.contains("7"));
        assertFalse(message.contains("%range%"));
    }

    @Test
    void getMessageOrDefaultReturnsStoredValueWhenPresent() {
        Locale locale = plugin.getLocale();
        String stored = locale.getMessageOrDefault("general.nametag.next", "fallback");
        assertEquals("&9Next", stored);
    }

    @Test
    void getMessageOrDefaultReturnsSuppliedDefaultWhenMissing() {
        Locale locale = plugin.getLocale();
        assertEquals("fallback", locale.getMessageOrDefault("no.such.node", "fallback"));
    }

    @Test
    void getMessageNodeMapContainsLoadedNodes() {
        Locale locale = plugin.getLocale();
        assertTrue(locale.getMessageNodeMap().containsKey("general.nametag.next"));
        assertEquals("&9Next", locale.getMessageNodeMap().get("general.nametag.next"));
    }

    @Test
    void getMessageNodeMapIsImmutable() {
        Locale locale = plugin.getLocale();
        org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class,
                () -> locale.getMessageNodeMap().put("x", "y"));
    }

    @Test
    void reloadMessagesReturnsTrueAndKeepsKnownNodesForAnExistingFile() {
        Locale locale = plugin.getLocale();
        assertTrue(locale.reloadMessages());
        assertEquals("&9Next", locale.getMessageOrDefault("general.nametag.next", null));
    }

    @Test
    void getNameAndRegionAndLanguageTagAreDerivedFromEnUs() {
        Locale locale = plugin.getLocale();
        assertEquals("en", locale.getName());
        assertEquals("US", locale.getRegion());
        assertEquals("en_US", locale.getLanguageTag());
    }

    @Test
    void getFileReturnsTheBackingLangFile() {
        Locale locale = plugin.getLocale();
        assertNotNull(locale.getFile());
        assertTrue(locale.getFile().getName().endsWith(".lang"));
    }

    @Test
    void staticGetLocaleFindsItByFullTag() {
        assertEquals(plugin.getLocale(), Locale.getLocale("en_US"));
    }

    @Test
    void staticGetLocaleByNameFindsItByLanguageOnly() {
        assertEquals(plugin.getLocale(), Locale.getLocaleByName("en"));
    }

    @Test
    void staticGetLocaleByRegionFindsItByRegionOnly() {
        assertEquals(plugin.getLocale(), Locale.getLocaleByRegion("US"));
    }

    @Test
    void staticLocaleExistsIsTrueForALoadedTagAndFalseForAnUnknownOne() {
        assertTrue(Locale.localeExists("en_US"));
        assertFalse(Locale.localeExists("xx_YY"));
    }

    @Test
    void staticGetLocalesIncludesTheLoadedLocale() {
        assertTrue(Locale.getLocales().contains(plugin.getLocale()));
    }
}
