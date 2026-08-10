package com.songoda.epichoppers.command.commands;

import com.songoda.epichoppers.EpicHoppersPlugin;
import com.songoda.epichoppers.boost.BoostData;
import com.songoda.epichoppers.command.AbstractCommand;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class CommandBoostTest {

    private ServerMock server;
    private EpicHoppersPlugin plugin;
    private CommandBoost command;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(EpicHoppersPlugin.class);
        command = new CommandBoost(null);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void tooFewArgumentsIsASyntaxError() {
        assertEquals(AbstractCommand.ReturnType.SYNTAX_ERROR,
                command.runCommand(plugin, server.addPlayer(), "boost", "Target"));
    }

    @Test
    void unknownPlayerFails() {
        PlayerMock sender = server.addPlayer();
        AbstractCommand.ReturnType result = command.runCommand(plugin, sender, "boost", "NoSuchPlayer", "2");
        assertEquals(AbstractCommand.ReturnType.FAILURE, result);
        assertNotNull(sender.nextMessage());
    }

    @Test
    void nonNumericMultiplierFails() {
        PlayerMock target = server.addPlayer("Target");
        AbstractCommand.ReturnType result = command.runCommand(plugin, target, "boost", "Target", "notanumber");
        assertEquals(AbstractCommand.ReturnType.FAILURE, result);
    }

    @Test
    void unrecognisedTimeSuffixSendsAMessageAndReturnsSuccessWithoutBoosting() {
        PlayerMock target = server.addPlayer("Target");
        AbstractCommand.ReturnType result = command.runCommand(plugin, target, "boost", "Target", "2", "bogus");
        assertEquals(AbstractCommand.ReturnType.SUCCESS, result);
        assertNull(plugin.getBoostManager().getBoost(target.getUniqueId()));
    }

    @Test
    void defaultDurationBoostsThePlayerAndReturnsFailureDespiteSucceeding() {
        // The final "return ReturnType.FAILURE" at the end of the success
        // path looks like a copy-paste bug (every other success path in this
        // command class returns SUCCESS), but the boost itself is genuinely
        // applied - documented rather than "fixed" since CommandManager only
        // acts on SYNTAX_ERROR, so this has no observable effect on players.
        PlayerMock target = server.addPlayer("Target");

        AbstractCommand.ReturnType result = command.runCommand(plugin, target, "boost", "Target", "3");

        assertEquals(AbstractCommand.ReturnType.FAILURE, result);
        BoostData boost = plugin.getBoostManager().getBoost(target.getUniqueId());
        assertNotNull(boost);
        assertEquals(3, boost.getMultiplier());
    }

    @Test
    void minuteSuffixBoostsWithACustomDuration() {
        PlayerMock target = server.addPlayer("Target");

        command.runCommand(plugin, target, "boost", "Target", "2", "m:5");

        BoostData boost = plugin.getBoostManager().getBoost(target.getUniqueId());
        assertNotNull(boost);
        assertEquals(2, boost.getMultiplier());
    }

    @Test
    void permissionNodeSyntaxAndDescriptionAreStable() {
        assertEquals("epichoppers.admin", command.getPermissionNode());
        assertNotNull(command.getSyntax());
        assertNotNull(command.getDescription());
    }
}
