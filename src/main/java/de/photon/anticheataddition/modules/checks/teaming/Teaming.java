package de.photon.anticheataddition.modules.checks.teaming;

import com.google.common.base.Preconditions;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import de.photon.anticheataddition.AntiCheatAddition;
import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.user.data.TimeKey;
import de.photon.anticheataddition.util.datastructure.quadtree.QuadTreeSet;
import de.photon.anticheataddition.util.mathematics.TimeUtil;
import de.photon.anticheataddition.util.messaging.Log;
import de.photon.anticheataddition.util.minecraft.world.Region;
import de.photon.anticheataddition.util.violationlevels.Flag;
import de.photon.anticheataddition.util.violationlevels.ViolationLevelManagement;
import de.photon.anticheataddition.util.violationlevels.ViolationManagement;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.HashSet;
import java.util.Set;

public final class Teaming extends ViolationModule implements Listener
{
    public static final Teaming INSTANCE = new Teaming();
    private static final long CHECK_INTERVAL = TimeUtil.toTicks(5000);

    private Teaming()
    {
        super("Teaming");
    }

    private Set<World> loadEnabledWorlds()
    {
        final Set<World> worlds = new HashSet<>();
        for (final String key : loadStringList(".enabled_worlds")) {
            final var world = Bukkit.getWorld(key);
            if (world == null) {
                Log.fine(() -> "Unable to load world \"" + key + "\" in teaming check.");
                continue;
            }
            worlds.add(world);
        }
        return Set.copyOf(worlds);
    }

    private Set<Region> loadSafeZones(Set<World> enabledWorlds)
    {
        final Set<Region> safeZones = new HashSet<>();
        for (final String s : loadStringList(".safe_zones")) {
            try {
                final var region = Region.parseRegion(s);
                if (enabledWorlds.contains(region.world())) safeZones.add(region);
            } catch (NullPointerException e) {
                Log.severe(() -> "Unable to load safe zone \"" + s + "\" in teaming check, is the world correct?");
            } catch (ArrayIndexOutOfBoundsException e) {
                Log.severe(() -> "Unable to load safe zone \"" + s + "\" in teaming check, are all coordinates present?");
            }
        }
        return Set.copyOf(safeZones);
    }

    private Set<Region> loadWorldGuardSafeZones(Set<World> enabledWorlds)
    {
        final Set<Region> regions = new HashSet<>();
        final var regionContainer = WorldGuard.getInstance().getPlatform().getRegionContainer();

        for (World world : enabledWorlds) {
            final var regionManager = regionContainer.get(BukkitAdapter.adapt(world));
            if (regionManager == null) continue;

            for (var region : regionManager.getRegions().values()) {
                final StateFlag.State pvpFlag = region.getFlag(Flags.PVP);
                if (pvpFlag == null || pvpFlag == StateFlag.State.DENY) {
                    final var min = region.getMinimumPoint();
                    final var max = region.getMaximumPoint();
                    regions.add(new Region(world, min.getX(), min.getZ(), max.getX(), max.getZ()));
                }
            }
        }

        return regions;
    }

    @Override
    public void enable()
    {
        final var enabledWorlds = loadEnabledWorlds();

        final var safeZonesLoading = new HashSet<>(loadSafeZones(enabledWorlds));
        if (loadBoolean(".worldguard", false)) safeZonesLoading.addAll(loadWorldGuardSafeZones(enabledWorlds));
        final var safeZones = Set.copyOf(safeZonesLoading);

        final double proximityRange = loadDouble(".proximity_range", 4.5);
        final double proximityRangeSquared = proximityRange * proximityRange;

        final int noPvpTime = loadInt(".no_pvp_time", 6000);

        final int allowedSize = loadInt(".allowed_size", 1);
        Preconditions.checkArgument(allowedSize > 0, "The Teaming allowed_size must be greater than 0.");

        Bukkit.getScheduler().scheduleSyncRepeatingTask(AntiCheatAddition.getInstance(), () -> {
            // Set for fast removeAll calls.
            final var quadTree = new QuadTreeSet<Player>();

            for (World world : enabledWorlds) {
                for (Player player : world.getPlayers()) {
                    final User user = User.getUser(player);
                    if (!User.isUserInvalid(user, Teaming.INSTANCE)
                        // Correct game modes.
                        && user.inAdventureOrSurvivalMode()
                        // Not engaged in pvp.
                        && user.getTimeMap().at(TimeKey.COMBAT).notRecentlyUpdated(noPvpTime)) {
                        final var loc = player.getLocation();
                        // Not in a bypassed region.
                        if (safeZones.stream().noneMatch(safeZone -> safeZone.isInsideRegion(loc))) quadTree.add(loc.getX(), loc.getZ(), player);
                    }
                }

                while (!quadTree.isEmpty()) {
                    final var firstNode = quadTree.getAny();
                    final var teamNodes = quadTree.queryCircle(firstNode, proximityRange).stream()
                                                  // The queryCircle function does not check the y-coords of a player, we need to do that here manually.
                                                  .filter(node -> node.element().getLocation().distanceSquared(firstNode.element().getLocation()) <= proximityRangeSquared)
                                                  .toList();

                    quadTree.removeAll(teamNodes);

                    // Team is too big
                    final int vl = teamNodes.size() - allowedSize;
                    if (vl <= 0) continue;

                    for (final var node : teamNodes) this.getManagement().flag(Flag.of(node.element()).setAddedVl(vl));
                }
            }
        }, 1L, CHECK_INTERVAL);
    }

    @Override
    protected ViolationManagement createViolationManagement()
    {
        return ViolationLevelManagement.builder(this)
                                       .loadThresholdsToManagement()
                                       .withDecay(300, 1)
                                       .build();
    }
}
