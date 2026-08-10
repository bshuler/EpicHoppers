package com.songoda.epichoppers.utils;

import com.songoda.epichoppers.EpicHoppersPlugin;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SettingsManager's onInventoryClick/onChat handlers are plain public
 * @EventHandler methods, so they are exercised here by constructing real
 * InventoryClickEvent/AsyncPlayerChatEvent instances and calling them
 * directly rather than by round-tripping through the plugin manager's event
 * bus - the assertions are on the manager's real side effects either way
 * (config mutation, event cancellation, GUI state), not on dispatch
 * plumbing.
 */
class SettingsManagerTest {

    private ServerMock server;
    private EpicHoppersPlugin plugin;
    private SettingsManager settingsManager;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(EpicHoppersPlugin.class);
        settingsManager = plugin.getSettingsManager();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void updateSettingsPopulatesConfigDefaultsForEveryDeclaredSetting() {
        // Already invoked once by EpicHoppersPlugin#onEnable(); calling it
        // again must be idempotent and is itself part of the public contract.
        settingsManager.updateSettings();
        FileConfiguration config = plugin.getConfig();

        assertEquals(true, config.getDefaults().get("Main.Allow hopper Upgrading"));
        assertEquals("WITCH", config.getDefaults().get("Main.Upgrade Particle Type"));
        assertEquals(8L, config.getDefaults().get("Main.Amount of Ticks Between Hops"));
        assertEquals(-1, config.getDefaults().get("Main.Max Hoppers Per Chunk"));
        assertEquals("SUNFLOWER", config.getDefaults().get("Interfaces.Economy Icon"));
        assertEquals(false, config.getDefaults().get("Database.Activate Mysql Support"));
        assertEquals("en_US", config.getDefaults().get("System.Language Mode"));
    }

    @Test
    void openSettingsManagerCreatesOneCategoryItemPerTopLevelSection() {
        PlayerMock player = server.addPlayer();

        settingsManager.openSettingsManager(player);

        org.bukkit.inventory.Inventory inv = player.getOpenInventory().getTopInventory();
        assertEquals(27, inv.getSize());
        // Main, Interfaces, Database, System - one WHITE_WOOL category icon each, starting at slot 10.
        assertEquals(Material.WHITE_WOOL, inv.getItem(10).getType());
        assertEquals(Material.WHITE_WOOL, inv.getItem(11).getType());
        assertEquals(Material.WHITE_WOOL, inv.getItem(12).getType());
        assertEquals(Material.WHITE_WOOL, inv.getItem(13).getType());
        assertEquals("Main", org.bukkit.ChatColor.stripColor(inv.getItem(10).getItemMeta().getDisplayName()));
    }

    @Test
    void clickingACategoryInTheManagerOpensTheEditorForThatCategory() {
        PlayerMock player = server.addPlayer();
        settingsManager.openSettingsManager(player);
        InventoryView managerView = player.getOpenInventory();

        InventoryClickEvent event = new InventoryClickEvent(managerView, InventoryType.SlotType.CONTAINER, 10,
                ClickType.LEFT, InventoryAction.PICKUP_ALL);
        settingsManager.onInventoryClick(event);

        assertTrue(event.isCancelled());
        org.bukkit.inventory.Inventory editor = player.getOpenInventory().getTopInventory();
        assertEquals(54, editor.getSize());
        assertEquals("EpicHoppers Settings Editor", player.getOpenInventory().getTitle());
    }

    @Test
    void clickingAStainedGlassPlaceholderInTheManagerDoesNothing() {
        PlayerMock player = server.addPlayer();
        settingsManager.openSettingsManager(player);
        InventoryView managerView = player.getOpenInventory();

        // Slot 0 is background glass, not a category item.
        InventoryClickEvent event = new InventoryClickEvent(managerView, InventoryType.SlotType.CONTAINER, 0,
                ClickType.LEFT, InventoryAction.PICKUP_ALL);
        settingsManager.onInventoryClick(event);

        assertTrue(event.isCancelled());
        assertEquals(27, player.getOpenInventory().getTopInventory().getSize());
    }

    private int findSlotOfType(org.bukkit.inventory.Inventory inv, Material type) {
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && item.getType() == type) return i;
        }
        throw new AssertionError("No item of type " + type + " found in inventory");
    }

    @Test
    void clickingABooleanEntryInTheEditorTogglesItAndReopensTheEditor() {
        PlayerMock player = server.addPlayer();
        settingsManager.openSettingsManager(player);
        settingsManager.onInventoryClick(new InventoryClickEvent(player.getOpenInventory(),
                InventoryType.SlotType.CONTAINER, 10, ClickType.LEFT, InventoryAction.PICKUP_ALL));

        org.bukkit.inventory.Inventory editor = player.getOpenInventory().getTopInventory();
        int leverSlot = findSlotOfType(editor, Material.LEVER);
        boolean before = plugin.getConfig().getBoolean("Main.Allow hopper Upgrading");

        InventoryClickEvent event = new InventoryClickEvent(player.getOpenInventory(), InventoryType.SlotType.CONTAINER,
                leverSlot, ClickType.LEFT, InventoryAction.PICKUP_ALL);
        settingsManager.onInventoryClick(event);

        assertTrue(event.isCancelled());
        assertEquals(!before, plugin.getConfig().getBoolean("Main.Allow hopper Upgrading"));
        assertEquals("EpicHoppers Settings Editor", player.getOpenInventory().getTitle());
    }

    @Test
    void clickingAStringEntryInTheEditorPromptsForChatInputThenOnChatAppliesIt() {
        PlayerMock player = server.addPlayer();
        settingsManager.openSettingsManager(player);
        settingsManager.onInventoryClick(new InventoryClickEvent(player.getOpenInventory(),
                InventoryType.SlotType.CONTAINER, 10, ClickType.LEFT, InventoryAction.PICKUP_ALL));

        org.bukkit.inventory.Inventory editor = player.getOpenInventory().getTopInventory();
        int paperSlot = findSlotOfType(editor, Material.PAPER);

        InventoryClickEvent click = new InventoryClickEvent(player.getOpenInventory(), InventoryType.SlotType.CONTAINER,
                paperSlot, ClickType.LEFT, InventoryAction.PICKUP_ALL);
        settingsManager.onInventoryClick(click);

        assertTrue(click.isCancelled());
        assertNotNull(player.nextMessage()); // "Please enter a value for..."

        AsyncPlayerChatEvent chat = new AsyncPlayerChatEvent(false, player, "EFFECT", Collections.emptySet());
        settingsManager.onChat(chat);

        assertTrue(chat.isCancelled());
        assertEquals("EFFECT", plugin.getConfig().getString("Main.Upgrade Particle Type"));
    }

    @Test
    void clickingAnIntEntryInTheEditorPromptsForChatInputThenOnChatAppliesIt() {
        PlayerMock player = server.addPlayer();
        settingsManager.openSettingsManager(player);
        settingsManager.onInventoryClick(new InventoryClickEvent(player.getOpenInventory(),
                InventoryType.SlotType.CONTAINER, 11, ClickType.LEFT, InventoryAction.PICKUP_ALL)); // "Interfaces"

        org.bukkit.inventory.Inventory editor = player.getOpenInventory().getTopInventory();
        int clockSlot = findSlotOfType(editor, Material.CLOCK);

        settingsManager.onInventoryClick(new InventoryClickEvent(player.getOpenInventory(), InventoryType.SlotType.CONTAINER,
                clockSlot, ClickType.LEFT, InventoryAction.PICKUP_ALL));

        AsyncPlayerChatEvent chat = new AsyncPlayerChatEvent(false, player, "42", Collections.emptySet());
        settingsManager.onChat(chat);

        assertEquals(42, plugin.getConfig().getInt("Interfaces.Glass Type 1"));
    }

    @Test
    void onChatIsANoOpWhenThePlayerIsNotCurrentlyEditingAnything() {
        PlayerMock player = server.addPlayer();
        AsyncPlayerChatEvent chat = new AsyncPlayerChatEvent(false, player, "hello", Collections.emptySet());

        settingsManager.onChat(chat);

        assertFalse(chat.isCancelled());
    }

    @Test
    void onInventoryClickIgnoresClicksInUnrelatedInventories() {
        PlayerMock player = server.addPlayer();
        org.bukkit.inventory.Inventory random = server.createInventory(null, 9, "Some Other Screen");
        player.openInventory(random);
        random.setItem(0, new ItemStack(Material.DIAMOND));

        InventoryClickEvent event = new InventoryClickEvent(player.getOpenInventory(), InventoryType.SlotType.CONTAINER,
                0, ClickType.LEFT, InventoryAction.PICKUP_ALL);
        settingsManager.onInventoryClick(event);

        assertFalse(event.isCancelled());
    }

    @Test
    void editObjectClosesInventoryAndPromptsWithNumericHintForIntSettings() {
        PlayerMock player = server.addPlayer();
        player.openInventory(server.createInventory(null, 9));

        settingsManager.editObject(player, "Interfaces.Glass Type 1");

        player.nextMessage(); // blank line
        String prompt = player.nextMessage();
        assertTrue(prompt.contains("Glass Type 1"));
        assertNotNull(player.nextMessage()); // "Use only numbers."
    }

    @Test
    void clickingAStainedGlassPlaceholderInTheEditorDoesNothing() {
        // openEditor() itself never places glass (unlike openSettingsManager),
        // but onInventoryClick's Editor branch defensively re-checks for it -
        // exercise that guard directly against the live open inventory.
        PlayerMock player = server.addPlayer();
        settingsManager.openSettingsManager(player);
        settingsManager.onInventoryClick(new InventoryClickEvent(player.getOpenInventory(),
                InventoryType.SlotType.CONTAINER, 10, ClickType.LEFT, InventoryAction.PICKUP_ALL));

        org.bukkit.inventory.Inventory editor = player.getOpenInventory().getTopInventory();
        int emptySlot = -1;
        for (int i = 0; i < editor.getSize(); i++) {
            if (editor.getItem(i) == null) {
                emptySlot = i;
                break;
            }
        }
        assertTrue(emptySlot >= 0, "Expected at least one unused slot in the editor inventory");
        editor.setItem(emptySlot, Methods.getGlass());
        boolean before = plugin.getConfig().getBoolean("Main.Allow hopper Upgrading");

        InventoryClickEvent event = new InventoryClickEvent(player.getOpenInventory(), InventoryType.SlotType.CONTAINER,
                emptySlot, ClickType.LEFT, InventoryAction.PICKUP_ALL);
        settingsManager.onInventoryClick(event);

        assertTrue(event.isCancelled());
        assertEquals(before, plugin.getConfig().getBoolean("Main.Allow hopper Upgrading"));
        assertEquals("EpicHoppers Settings Editor", player.getOpenInventory().getTitle());
    }

    @Test
    void onChatParsesADoubleValuedSettingUsingIsDouble() {
        // No shipped Setting is double-typed today, but onChat's isDouble
        // branch must still work correctly for a config value an admin (or a
        // future setting) stores as a real double - exercise it against a
        // key set directly to a double default, the same way editObject()
        // would have set up "current" for any other type.
        PlayerMock player = server.addPlayer();
        plugin.getConfig().set("Main.Double Setting", 1.5);
        // finishEditing() unconditionally reopens the editor for whatever
        // category is on record for the player, so establish "Main" as that
        // category the same way a real click would first.
        settingsManager.openSettingsManager(player);
        settingsManager.onInventoryClick(new InventoryClickEvent(player.getOpenInventory(),
                InventoryType.SlotType.CONTAINER, 10, ClickType.LEFT, InventoryAction.PICKUP_ALL));

        settingsManager.editObject(player, "Main.Double Setting");
        AsyncPlayerChatEvent chat = new AsyncPlayerChatEvent(false, player, "3.5", Collections.emptySet());
        settingsManager.onChat(chat);

        assertTrue(chat.isCancelled());
        assertEquals(3.5, plugin.getConfig().getDouble("Main.Double Setting"));
    }

    @Test
    void openEditorAddsWrappedDescriptionLoreWhenSettingDefinitionsHasAMatchingEntry() {
        // SettingDefinitions.yml now carries a real entry keyed
        // "Main.Allow hopper Upgrading" - openEditor's description lookup
        // (defs.getConfig().contains(fKey)/getString(fKey)) should find it
        // and append the word-wrapped description as extra lore lines below
        // the plain true/false line.
        PlayerMock player = server.addPlayer();
        settingsManager.openSettingsManager(player);
        settingsManager.onInventoryClick(new InventoryClickEvent(player.getOpenInventory(),
                InventoryType.SlotType.CONTAINER, 10, ClickType.LEFT, InventoryAction.PICKUP_ALL));

        org.bukkit.inventory.Inventory editor = player.getOpenInventory().getTopInventory();
        int leverSlot = findSlotOfType(editor, Material.LEVER);
        java.util.List<String> lore = editor.getItem(leverSlot).getItemMeta().getLore();

        assertTrue(lore.size() > 1, "Expected description lore lines beyond the plain true/false line");
        String joined = org.bukkit.ChatColor.stripColor(String.join(" ", lore.subList(1, lore.size())))
                .replaceAll("\\s+", " ").trim();
        assertTrue(joined.contains("access the upgrade window"), "Unexpected lore text: " + joined);
    }

    @Test
    void finishEditingRemovesTheInProgressEditAndReopensTheCategoryEditor() {
        PlayerMock player = server.addPlayer();
        settingsManager.openSettingsManager(player);
        settingsManager.onInventoryClick(new InventoryClickEvent(player.getOpenInventory(),
                InventoryType.SlotType.CONTAINER, 10, ClickType.LEFT, InventoryAction.PICKUP_ALL));
        settingsManager.editObject(player, "Main.Upgrade Particle Type");

        settingsManager.finishEditing(player);

        assertEquals("EpicHoppers Settings Editor", player.getOpenInventory().getTitle());
    }
}
