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

class CommandGiveTest {

    private ServerMock server;
    private EpicHoppersPlugin plugin;
    private CommandGive command;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(EpicHoppersPlugin.class);
        command = new CommandGive(null);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void tooFewArgumentsIsASyntaxError() {
        assertEquals(AbstractCommand.ReturnType.SYNTAX_ERROR, command.runCommand(plugin, server.addPlayer()));
    }

    @Test
    void unknownPlayerNameFails() {
        PlayerMock sender = server.addPlayer();
        AbstractCommand.ReturnType result = command.runCommand(plugin, sender, "give", "NoSuchPlayer", "1");
        assertEquals(AbstractCommand.ReturnType.FAILURE, result);
        assertNotNull(sender.nextMessage());
    }

    @Test
    void invalidLevelFails() {
        PlayerMock target = server.addPlayer("Target");

        AbstractCommand.ReturnType result = command.runCommand(plugin, target, "give", "Target", "9999");

        assertEquals(AbstractCommand.ReturnType.FAILURE, result);
    }

    @Test
    void validGiveAddsAHopperItemAtTheRequestedLevel() {
        PlayerMock target = server.addPlayer("Target");

        AbstractCommand.ReturnType result = command.runCommand(plugin, target, "give", "Target", "1");

        assertEquals(AbstractCommand.ReturnType.SUCCESS, result);
        boolean foundHopper = false;
        for (org.bukkit.inventory.ItemStack item : target.getInventory().getContents()) {
            if (item != null && item.getType() == Material.HOPPER) {
                foundHopper = true;
            }
        }
        org.junit.jupiter.api.Assertions.assertTrue(foundHopper);
    }

    @Test
    void singleArgumentIsASyntaxErrorBeforeTheSelfGiveBranchIsEverReached() {
        // args.length <= 2 short-circuits to SYNTAX_ERROR before the
        // "args.length == 1 -> give to self" branch further down is ever
        // reached, making that later branch genuinely unreachable dead code
        // as currently written - not a regression introduced by this port,
        // just documented here since it surfaced while writing this test.
        ConsoleCommandSenderMock console = server.getConsoleSender();
        AbstractCommand.ReturnType result = command.runCommand(plugin, console, "give");
        assertEquals(AbstractCommand.ReturnType.SYNTAX_ERROR, result);
    }

    @Test
    void permissionNodeSyntaxAndDescriptionAreStable() {
        assertEquals("epichoppers.admin", command.getPermissionNode());
        assertEquals("/eh give [player] [level]", command.getSyntax());
        assertNotNull(command.getDescription());
    }
}
