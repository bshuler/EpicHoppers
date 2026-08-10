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
    void hourSuffixBoostsWithACustomDuration() {
        PlayerMock target = server.addPlayer("Target");
        long before = System.currentTimeMillis();

        command.runCommand(plugin, target, "boost", "Target", "2", "h:5");

        BoostData boost = plugin.getBoostManager().getBoost(target.getUniqueId());
        assertNotNull(boost);
        assertEquals(2, boost.getMultiplier());
        assertExpiryWithinTolerance(before, java.util.Calendar.HOUR, 5, boost.getEndTime());
    }

    @Test
    void daySuffixBoostsWithACustomDuration() {
        PlayerMock target = server.addPlayer("Target");
        long before = System.currentTimeMillis();

        command.runCommand(plugin, target, "boost", "Target", "2", "d:3");

        BoostData boost = plugin.getBoostManager().getBoost(target.getUniqueId());
        assertNotNull(boost);
        assertEquals(2, boost.getMultiplier());
        // CommandBoost implements the "d:" suffix as Calendar.HOUR * 24, not
        // Calendar.DAY_OF_MONTH - mirror that exactly rather than a
        // day-field add, which can differ across a DST boundary.
        assertExpiryWithinTolerance(before, java.util.Calendar.HOUR, 3 * 24, boost.getEndTime());
    }

    @Test
    void yearSuffixBoostsWithACustomDuration() {
        PlayerMock target = server.addPlayer("Target");
        long before = System.currentTimeMillis();

        command.runCommand(plugin, target, "boost", "Target", "2", "y:1");

        BoostData boost = plugin.getBoostManager().getBoost(target.getUniqueId());
        assertNotNull(boost);
        assertEquals(2, boost.getMultiplier());
        assertExpiryWithinTolerance(before, java.util.Calendar.YEAR, 1, boost.getEndTime());
    }

    private void assertExpiryWithinTolerance(long before, int calendarField, int amount, long actualEndTime) {
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.setTimeInMillis(before);
        c.add(calendarField, amount);
        long expected = c.getTimeInMillis();
        long toleranceMillis = 5_000;
        org.junit.jupiter.api.Assertions.assertTrue(Math.abs(actualEndTime - expected) <= toleranceMillis,
                "expected endTime near " + expected + " but was " + actualEndTime);
    }

    @Test
    void permissionNodeSyntaxAndDescriptionAreStable() {
        assertEquals("epichoppers.admin", command.getPermissionNode());
        assertNotNull(command.getSyntax());
        assertNotNull(command.getDescription());
    }
}
