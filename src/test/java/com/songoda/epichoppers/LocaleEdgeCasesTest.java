package com.songoda.epichoppers;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Locale's static fields (LOCALES, plugin, localeFolder) live for the whole
 * JVM/test run, not per-test - the en_US Locale object every other test in
 * the suite reads through {@code plugin.getLocale()} is the SAME instance
 * across every test class, and localeFolder is only ever set once (guarded
 * by a null check in {@code init}). Every test below is written to either
 * (a) touch only brand-new locale tags nobody else reads, or (b) fully
 * snapshot and restore whatever shared file/state it touches before it
 * returns, so it cannot leak a broken state into a sibling test class.
 * {@code Locale}'s constructor is private, so all of this still has to go
 * through the public static surface (searchForLocales/saveDefaultLocale)
 * rather than constructing instances directly.
 */
class LocaleEdgeCasesTest {

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
    void searchForLocalesDiscoversAndLoadsANewlyDroppedLangFile() throws Exception {
        File localeDir = plugin.getLocale().getFile().getParentFile();
        File dropped = new File(localeDir, "de_DE.lang");
        Files.writeString(dropped.toPath(), "greeting.hello = \"Hallo\"\n");

        Locale.searchForLocales();

        assertTrue(Locale.localeExists("de_DE"));
        assertEquals("Hallo", Locale.getLocale("de_DE").getMessageOrDefault("greeting.hello", null));
    }

    @Test
    void saveDefaultLocaleCopiesASuppliedInputStreamForABrandNewLocale() {
        InputStream in = new ByteArrayInputStream(
                "test.node = \"Bonjour\"\n".getBytes(StandardCharsets.UTF_8));

        boolean saved = Locale.saveDefaultLocale(in, "fr_FR.lang");

        assertTrue(saved);
        assertTrue(Locale.localeExists("fr_FR"));
        assertEquals("Bonjour", Locale.getLocale("fr_FR").getMessageOrDefault("test.node", null));
    }

    @Test
    void saveDefaultLocaleReturnsFalseWhenTheFileNameIsNotLanguageRegionShaped() {
        InputStream in = new ByteArrayInputStream("x = \"y\"\n".getBytes(StandardCharsets.UTF_8));

        // "invalidname" has no "_" to split into exactly [language, region],
        // so the post-copy shape check must reject it even though the copy
        // itself succeeded.
        assertFalse(Locale.saveDefaultLocale(in, "invalidname.lang"));
    }

    @Test
    void copySwallowsAnIOExceptionFromABrokenSourceStreamAndStillReturnsTrue() {
        InputStream broken = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("simulated read failure");
            }
        };

        // copy()'s IOException catch is a silent swallow (stack trace only) -
        // the destination file still gets created (empty) and the locale
        // still gets registered, so saveDefaultLocale itself reports success.
        boolean saved = assertDoesNotThrow(() -> Locale.saveDefaultLocale(broken, "bz_BZ.lang"));
        assertTrue(saved);
        assertTrue(Locale.localeExists("bz_BZ"));
    }

    @Test
    void saveDefaultLocaleViaCompareFilesReturnsFalseWhenNoDefaultResourceOrStreamIsAvailable() throws Exception {
        File localeDir = plugin.getLocale().getFile().getParentFile();
        File existing = new File(localeDir, "qq_QQ.lang");
        Files.writeString(existing.toPath(), "already.here = \"yes\"\n");

        // Destination already exists, so saveDefaultLocale takes the
        // compareFiles path; passing in=null with a file name that isn't a
        // bundled plugin resource means compareFiles' own "no default at
        // all" fallback (plugin.getResource) also comes back null.
        assertFalse(Locale.saveDefaultLocale(null, "qq_QQ.lang"));
    }

    @Test
    void saveDefaultLocaleViaCompareFilesAppendsAMissingBundledMessageAndReturnsTrue() throws Exception {
        Locale enUs = plugin.getLocale();
        File file = enUs.getFile();
        String original = Files.readString(file.toPath());
        try {
            String withoutOneLine = original.replaceFirst(
                    "(?m)^general\\.nametag\\.next\\s*=.*$\\R?", "");
            Files.writeString(file.toPath(), withoutOneLine);

            boolean changed = Locale.saveDefaultLocale(null, "en_US.lang");

            assertTrue(changed);
            String updated = Files.readString(file.toPath());
            assertTrue(updated.contains("general.nametag.next"));
        } finally {
            Files.writeString(file.toPath(), original);
            enUs.reloadMessages();
        }
    }

    @Test
    void reloadMessagesReturnsFalseAndWarnsWhenTheBackingFileIsMissing() throws Exception {
        Locale enUs = plugin.getLocale();
        File file = enUs.getFile();
        File movedAway = new File(file.getParentFile(), "en_US.lang.movedaway");
        Files.move(file.toPath(), movedAway.toPath());
        try {
            assertFalse(enUs.reloadMessages());
        } finally {
            Files.move(movedAway.toPath(), file.toPath());
            enUs.reloadMessages();
        }
    }

    @Test
    void reloadMessagesSkipsAnInvalidSyntaxLineButKeepsTheValidOnes() throws Exception {
        Locale enUs = plugin.getLocale();
        File file = enUs.getFile();
        String original = Files.readString(file.toPath());
        try {
            Files.writeString(file.toPath(), original + "\nthis line has no equals-quote shape at all\n");

            assertTrue(enUs.reloadMessages());
            assertEquals("&9Next", enUs.getMessageOrDefault("general.nametag.next", null));
        } finally {
            Files.writeString(file.toPath(), original);
            enUs.reloadMessages();
        }
    }

    @Test
    void reloadMessagesReturnsFalseWhenTheBackingPathBecomesUnreadable() throws Exception {
        Locale enUs = plugin.getLocale();
        File file = enUs.getFile();
        String original = Files.readString(file.toPath());
        try {
            // File#exists() is true for a directory too, so replacing the
            // backing file with a same-named directory clears the
            // exists()-guard but makes the FileReader construction inside
            // the try-with-resources fail with a (caught) IOException.
            assertTrue(file.delete());
            assertTrue(file.mkdir());

            assertFalse(enUs.reloadMessages());
        } finally {
            file.delete();
            Files.writeString(file.toPath(), original);
            assertTrue(enUs.reloadMessages());
        }
    }

    @Test
    void staticLookupsReturnNullForTagsNameAndRegionThatAreNotLoaded() {
        assertNull(Locale.getLocale("xx_YY"));
        assertNull(Locale.getLocaleByName("xx"));
        assertNull(Locale.getLocaleByRegion("YY"));
    }

    @Test
    void saveDefaultLocaleReturnsFalseWhenTheDestinationCannotBeCreated() throws Exception {
        File localeDir = plugin.getLocale().getFile().getParentFile();
        // A brand-new file name (nothing named this exists yet) forces
        // saveDefaultLocale down the "copy a new file" branch rather than
        // compareFiles; making the parent directory non-writable means the
        // `new FileOutputStream(destinationFile)` inside that branch's own
        // try block fails, exercising its own IOException catch/return false
        // (distinct from copy()'s internal, silently-swallowed catch).
        assertTrue(localeDir.setWritable(false));
        try {
            InputStream in = new ByteArrayInputStream("x = \"y\"\n".getBytes(StandardCharsets.UTF_8));
            assertFalse(Locale.saveDefaultLocale(in, "ss_SS.lang"));
        } finally {
            localeDir.setWritable(true);
        }
    }

    @Test
    void compareFilesReturnsFalseWhenTheExistingFileCannotBeWrittenTo() throws Exception {
        Locale enUs = plugin.getLocale();
        File file = enUs.getFile();
        String original = Files.readString(file.toPath());
        try {
            String withoutOneLine = original.replaceFirst(
                    "(?m)^general\\.nametag\\.next\\s*=.*$\\R?", "");
            Files.writeString(file.toPath(), withoutOneLine);
            assertTrue(file.setWritable(false));

            // The bundled en_US.lang resource still has the line the
            // on-disk file is now missing, so compareFiles wants to append
            // it - but the existing file can't be opened for writing,
            // exercising compareFiles' own IOException catch/return false.
            assertFalse(Locale.saveDefaultLocale(null, "en_US.lang"));
        } finally {
            file.setWritable(true);
            Files.writeString(file.toPath(), original);
            enUs.reloadMessages();
        }
    }

    @Test
    void clearLocaleDataEmptiesTheRegistryAndEachLocalesNodes() {
        Locale enUs = plugin.getLocale();
        assertFalse(enUs.getMessageNodeMap().isEmpty());

        Locale.clearLocaleData();

        assertTrue(Locale.getLocales().isEmpty());
        assertFalse(Locale.localeExists("en_US"));
        assertTrue(enUs.getMessageNodeMap().isEmpty());
        // No manual restore needed: the next test's MockBukkit.load() ->
        // onEnable() -> Locale.init() -> searchForLocales() call finds
        // en_US.lang still on disk and reconstructs+re-registers it, since
        // this test never touched the file itself - only the in-memory
        // registry/node cache.
    }
}
