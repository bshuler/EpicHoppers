package com.songoda.epichoppers.command.commands;

import com.songoda.arconix.api.methods.formatting.TextComponent;
import com.songoda.epichoppers.EpicHoppersPlugin;
import com.songoda.epichoppers.command.AbstractCommand;
import org.bukkit.command.CommandSender;

public class CommandEpicHoppers extends AbstractCommand {

    public CommandEpicHoppers() {
        super("EpicHoppers", null, false);
    }

    @Override
    protected ReturnType runCommand(EpicHoppersPlugin instance, CommandSender sender, String... args) {
        sender.sendMessage("");
        sender.sendMessage(TextComponent.formatText(instance.references.getPrefix() + "&7Version " + instance.getDescription().getVersion() + " Created with <3 by &5&l&oBrianna"));

        for (AbstractCommand command : instance.getCommandManager().getCommands()) {
            if (command.getPermissionNode() == null || sender.hasPermission(command.getPermissionNode())) {
                sender.sendMessage(TextComponent.formatText("&8 - &a" + command.getSyntax() + "&7 - " + command.getDescription()));
            }
        }
        sender.sendMessage("");

        return ReturnType.SUCCESS;
    }

    @Override
    public String getPermissionNode() {
        return null;
    }

    @Override
    public String getSyntax() {
        return "/EpicHoppers";
    }

    @Override
    public String getDescription() {
        return "Displays this page.";
    }
}
