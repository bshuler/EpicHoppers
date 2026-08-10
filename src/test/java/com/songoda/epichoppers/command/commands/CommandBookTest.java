package com.songoda.epichoppers.command.commands;

import com.songoda.epichoppers.EpicHoppersPlugin;
import com.songoda.epichoppers.command.AbstractCommand;
import org.bukkit.Material;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.command.ConsoleCommandSenderMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandBookTest {

    private ServerMock server;
    private EpicHoppersPlugin plugin;
    private CommandBook command;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(EpicHoppersPlugin.class);
        command = new CommandBook(null);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private boolean hasEnchantedBook(PlayerMock player) {
        for (org.bukkit.inventory.ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.ENCHANTED_BOOK) return true;
        }
        return false;
    }

    @Test
    void aPlayerWithNoTargetGivesTheBookToThemself() {
        PlayerMock player = server.addPlayer();
        AbstractCommand.ReturnType result = command.runCommand(plugin, player, "book");
        assertEquals(AbstractCommand.ReturnType.SUCCESS, result);
        assertTrue(hasEnchantedBook(player));
    }

    @Test
    void consoleWithNoTargetFailsSinceItIsNotAPlayer() {
        ConsoleCommandSenderMock console = server.getConsoleSender();
        AbstractCommand.ReturnType result = command.runCommand(plugin, console, "book");
        assertEquals(AbstractCommand.ReturnType.FAILURE, result);
    }

    @Test
    void unknownTargetPlayerFails() {
        PlayerMock sender = server.addPlayer();
        AbstractCommand.ReturnType result = command.runCommand(plugin, sender, "book", "NoSuchPlayer");
        assertEquals(AbstractCommand.ReturnType.FAILURE, result);
        assertNotNull(sender.nextMessage());
    }

    @Test
    void validTargetPlayerReceivesTheBook() {
        PlayerMock sender = server.addPlayer();
        PlayerMock target = server.addPlayer("Target");

        AbstractCommand.ReturnType result = command.runCommand(plugin, sender, "book", "Target");

        assertEquals(AbstractCommand.ReturnType.SUCCESS, result);
        assertTrue(hasEnchantedBook(target));
    }

    @Test
    void permissionNodeSyntaxAndDescriptionAreStable() {
        assertEquals("epichoppers.admin", command.getPermissionNode());
        assertNotNull(command.getSyntax());
        assertNotNull(command.getDescription());
    }
}
