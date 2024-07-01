package de.photon.anticheataddition.commands.subcommands.internaltestcommands;

import de.photon.anticheataddition.InternalPermission;
import de.photon.anticheataddition.ServerVersion;
import de.photon.anticheataddition.commands.CommandAttributes;
import de.photon.anticheataddition.commands.InternalCommand;
import de.photon.anticheataddition.commands.TabCompleteSupplier;
import de.photon.anticheataddition.user.data.DataUpdaterEvents;
import de.photon.anticheataddition.util.inventoryview.InventoryViewUtil;
import de.photon.anticheataddition.util.log.Log;
import de.photon.anticheataddition.util.messaging.ChatMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.event.inventory.InventoryOpenEvent;

import java.util.Queue;

public class TestInventoryOpen extends InternalCommand
{
    public TestInventoryOpen()
    {
        super("inventoryopen", CommandAttributes.builder()
                                                .addCommandHelp("This command sets the internal state of a player as if they had opened an inventory.",
                                                                "Syntax: /anticheataddition internaltest inventoryopen <player>")
                                                .exactArguments(1)
                                                .setPermission(InternalPermission.INTERNALTEST).build(), TabCompleteSupplier.builder().allPlayers());
    }

    @Override
    protected void execute(CommandSender sender, Queue<String> arguments)
    {
        final var user = parseUser(sender, arguments.peek());
        if (user == null) return;

        if (ServerVersion.ACTIVE == ServerVersion.MC120) {
            ChatMessage.sendMessage(sender, "Due to various API changes this command is not available on Minecraft 1.20.");
            return;
        }

        final var view = InventoryViewUtil.INSTANCE.createTestView(user);
        DataUpdaterEvents.INSTANCE.onInventoryOpen(new InventoryOpenEvent(view));

        Log.fine(() -> "Executed internal inventory open test command affecting player " + user.getPlayer().getName() + " requested by " + sender.getName());
        ChatMessage.sendMessage(sender, "Internal opening for player " + user.getPlayer().getName() + " executed.");
    }
}
