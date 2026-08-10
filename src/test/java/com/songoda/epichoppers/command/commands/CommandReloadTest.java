package com.songoda.epichoppers.command.commands;

import com.songoda.epichoppers.EpicHoppersPlugin;
import com.songoda.epichoppers.command.AbstractCommand;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.command.ConsoleCommandSenderMock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandReloadTest {

    private ServerMock server;
    private EpicHoppersPlugin plugin;
    private CommandReload command;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(EpicHoppersPlugin.class);
        command = new CommandReload(null);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void reloadsTheConfigurationAndSendsAConfirmationMessage() {
        ConsoleCommandSenderMock console = server.getConsoleSender();

        AbstractCommand.ReturnType result = command.runCommand(plugin, console, "reload");

        assertEquals(AbstractCommand.ReturnType.SUCCESS, result);
        String message = console.nextMessage();
        assertNotNull(message);
        assertTrue(message.contains("reloaded"));
    }

    @Test
    void isNotNoConsoleSoItCanBeRunFromTheConsole() {
        assertFalse(command.isNoConsole());
    }

    @Test
    void permissionNodeSyntaxAndDescriptionAreStable() {
        assertEquals("epichoppers.admin", command.getPermissionNode());
        assertEquals("/eh reload", command.getSyntax());
        assertNotNull(command.getDescription());
    }
}
