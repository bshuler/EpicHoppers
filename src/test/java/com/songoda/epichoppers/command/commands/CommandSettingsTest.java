package com.songoda.epichoppers.command.commands;

import com.songoda.epichoppers.EpicHoppersPlugin;
import com.songoda.epichoppers.command.AbstractCommand;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandSettingsTest {

    private ServerMock server;
    private EpicHoppersPlugin plugin;
    private CommandSettings command;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(EpicHoppersPlugin.class);
        command = new CommandSettings(null);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void opensTheSettingsManagerInventoryForThePlayer() {
        PlayerMock player = server.addPlayer();

        AbstractCommand.ReturnType result = command.runCommand(plugin, player, "settings");

        assertEquals(AbstractCommand.ReturnType.SUCCESS, result);
        assertTrue(player.getOpenInventory().getTopInventory().getViewers().contains(player));
        assertEquals("EpicHoppers Settings Manager", player.getOpenInventory().getTitle());
    }

    @Test
    void isMarkedNoConsoleSinceRunCommandCastsSenderToPlayer() {
        // CommandManager#processRequirements rejects non-Player senders before
        // ever calling runCommand when isNoConsole() is true (see
        // CommandManager.java line 61), so the direct
        // "(Player) sender" cast in CommandSettings#runCommand is never
        // actually reachable with a console sender in real production use -
        // isNoConsole() is what makes that cast safe.
        assertTrue(command.isNoConsole());
    }

    @Test
    void permissionNodeSyntaxAndDescriptionAreStable() {
        assertEquals("epichoppers.admin", command.getPermissionNode());
        assertNotNull(command.getSyntax());
        assertNotNull(command.getDescription());
    }
}
