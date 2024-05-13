package de.photon.anticheataddition.modules.sentinel.mods;

import de.photon.anticheataddition.ServerVersion;
import de.photon.anticheataddition.modules.ModuleLoader;
import de.photon.anticheataddition.modules.sentinel.ParsedPluginMessageListener;
import de.photon.anticheataddition.modules.sentinel.SentinelModule;
import de.photon.anticheataddition.util.log.Log;
import de.photon.anticheataddition.util.pluginmessage.MessageChannel;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;

public final class VapeSentinel extends SentinelModule implements Listener, ParsedPluginMessageListener
{
    public static final VapeSentinel INSTANCE = new VapeSentinel();

    private static final MessageChannel VAPE_MESSAGE_CHANNEL = MessageChannel.ofLegacy("LOLIMAHCKER");

    private VapeSentinel()
    {
        super("Vape");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event)
    {
        event.getPlayer().sendMessage("§8 §8 §1 §3 §3 §7 §8 ");
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, @NotNull String message)
    {
        Log.fine(() -> "Player " + player.getName() + " joined with Vape | Data: " + message);
        detection(player);
    }

    @Override
    protected ModuleLoader createModuleLoader()
    {
        return ModuleLoader.builder(this)
                           .setAllowedServerVersions(ServerVersion.LEGACY_PLUGIN_MESSAGE_VERSIONS)
                           .addIncomingMessageChannel(VAPE_MESSAGE_CHANNEL)
                           .build();
    }
}
