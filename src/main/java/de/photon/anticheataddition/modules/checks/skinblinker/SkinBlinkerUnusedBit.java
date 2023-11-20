package de.photon.anticheataddition.modules.checks.skinblinker;

import com.comphenix.protocol.PacketType;
import de.photon.anticheataddition.ServerVersion;
import de.photon.anticheataddition.modules.ModuleLoader;
import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.protocol.PacketAdapterBuilder;
import de.photon.anticheataddition.util.violationlevels.Flag;
import de.photon.anticheataddition.util.violationlevels.ViolationLevelManagement;
import de.photon.anticheataddition.util.violationlevels.ViolationManagement;

public final class SkinBlinkerUnusedBit extends ViolationModule
{
    public static final SkinBlinkerUnusedBit INSTANCE = new SkinBlinkerUnusedBit();

    private SkinBlinkerUnusedBit()
    {
        super("Skinblinker.parts.UnusedBit");
    }

    @Override
    protected ModuleLoader createModuleLoader()
    {
        return ModuleLoader.builder(this)
                           .setAllowedServerVersions(ServerVersion.MC119.getSupVersionsTo())
                           .addPacketListeners(PacketAdapterBuilder.of(this, PacketType.Play.Client.SETTINGS).onReceiving((event, user) -> {
                               /*
                                * Check for the special 0x80 bit in the skin packet that is officially unused by the protocol and set to 0 in vanilla clients.
                                * Some custom clients like LabyMod use that bit for their cosmetics.
                                */
                               final int newSkinComponents = event.getPacket().getIntegers().readSafely(1);

                               // Unused skin bit used (detection)
                               if ((newSkinComponents & 0x80) != 0) getManagement().flag(Flag.of(user).setAddedVl(100));
                           }).build())
                           .build();
    }

    @Override
    protected ViolationManagement createViolationManagement()
    {
        return ViolationLevelManagement.builder(this)
                                       .emptyThresholdManagement()
                                       .withDecay(200, 50).build();
    }
}