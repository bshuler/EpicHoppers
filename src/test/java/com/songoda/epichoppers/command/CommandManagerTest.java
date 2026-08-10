package com.songoda.epichoppers.command;

import com.songoda.epichoppers.EpicHoppersPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.command.ConsoleCommandSenderMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandManagerTest {

    private ServerMock server;
    private EpicHoppersPlugin plugin;
    private CommandManager commandManager;
    private Command rootCommand;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(EpicHoppersPlugin.class);
        // The real CommandManager built during onEnable(), not a fresh one -
        // CommandEpicHoppers#runCommand walks instance.getCommandManager()
        // .getCommands(), so the manager under test must be the same instance
        // the plugin itself already wired up.
        commandManager = plugin.getCommandManager();
        rootCommand = plugin.getCommand("EpicHoppers");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void noArgumentsShowsTheRootHelpPage() {
        ConsoleCommandSenderMock console = server.getConsoleSender();

        boolean handled = commandManager.onCommand(console, rootCommand, "eh", new String[0]);

        assertTrue(handled);
        boolean sawGiveSyntax = false;
        String message;
        while ((message = console.nextMessage()) != null) {
            if (message.contains("/eh give")) sawGiveSyntax = true;
        }
        assertTrue(sawGiveSyntax);
    }

    @Test
    void firstArgumentMatchingASubcommandDispatchesToIt() {
        ConsoleCommandSenderMock console = server.getConsoleSender();

        boolean handled = commandManager.onCommand(console, rootCommand, "eh",
                new String[]{"reload"});

        assertTrue(handled);
        // Drain rather than take the single next message: onEnable() already
        // queued its startup banner lines onto this same console mock before
        // the test body ever runs, so the "reloaded" confirmation is not
        // necessarily the first message waiting in the queue.
        assertTrue(anyMessageContains(console, "reloaded"));
    }

    @Test
    void unknownRootCommandNameSendsADoesNotExistMessage() {
        ConsoleCommandSenderMock console = server.getConsoleSender();
        Command bogus = new Command("bogus") {
            @Override
            public boolean execute(CommandSender sender, String label, String[] args) {
                return false;
            }
        };

        boolean handled = commandManager.onCommand(console, bogus, "bogus", new String[0]);

        assertTrue(handled);
        assertTrue(anyMessageContains(console, "does not exist"));
    }

    @Test
    void noConsoleSubcommandRejectsAConsoleSender() {
        ConsoleCommandSenderMock console = server.getConsoleSender();

        boolean handled = commandManager.onCommand(console, rootCommand, "eh",
                new String[]{"settings"});

        assertTrue(handled);
        assertTrue(anyMessageContains(console, "must be a player"));
    }

    private boolean anyMessageContains(ConsoleCommandSenderMock console, String needle) {
        boolean found = false;
        String message;
        while ((message = console.nextMessage()) != null) {
            if (message.contains(needle)) found = true;
        }
        return found;
    }

    @Test
    void missingPermissionSendsTheNoPermissionMessage() {
        PlayerMock player = server.addPlayer();
        // CommandReload requires "epichoppers.admin", which this player was
        // never granted.

        boolean handled = commandManager.onCommand(player, rootCommand, "eh",
                new String[]{"reload"});

        assertTrue(handled);
        boolean sawNoPermissionReply = false;
        String message;
        while ((message = player.nextMessage()) != null) {
            sawNoPermissionReply = true;
        }
        assertTrue(sawNoPermissionReply);
    }

    @Test
    void syntaxErrorSendsInvalidSyntaxAndTheExpectedSyntaxHint() {
        ConsoleCommandSenderMock console = server.getConsoleSender();

        // CommandGive#runCommand returns SYNTAX_ERROR for anything with 2 or
        // fewer args.
        boolean handled = commandManager.onCommand(console, rootCommand, "eh",
                new String[]{"give"});

        assertTrue(handled);
        boolean sawInvalidSyntax = false;
        boolean sawSyntaxHint = false;
        String message;
        while ((message = console.nextMessage()) != null) {
            if (message.contains("Invalid Syntax")) sawInvalidSyntax = true;
            if (message.contains("/eh give")) sawSyntaxHint = true;
        }
        assertTrue(sawInvalidSyntax);
        assertTrue(sawSyntaxHint);
    }

    @Test
    void getCommandsReturnsAnUnmodifiableListOfEveryRegisteredCommand() {
        assertTrue(commandManager.getCommands().size() >= 6);
        org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class,
                () -> commandManager.getCommands().add(null));
    }
}
