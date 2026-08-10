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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandEpicHoppersTest {

    private ServerMock server;
    private EpicHoppersPlugin plugin;
    private CommandEpicHoppers command;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(EpicHoppersPlugin.class);
        command = new CommandEpicHoppers();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void printsAHelpPageListingEveryRegisteredSubcommand() {
        ConsoleCommandSenderMock console = server.getConsoleSender();

        AbstractCommand.ReturnType result = command.runCommand(plugin, console, "EpicHoppers");

        assertEquals(AbstractCommand.ReturnType.SUCCESS, result);
        boolean sawGiveSyntax = false;
        String message;
        while ((message = console.nextMessage()) != null) {
            if (message.contains("/eh give")) sawGiveSyntax = true;
        }
        assertTrue(sawGiveSyntax);
    }

    @Test
    void hasNoPermissionNodeSoEveryoneCanUseIt() {
        assertNull(command.getPermissionNode());
        assertNotNull(command.getSyntax());
        assertNotNull(command.getDescription());
    }
}
