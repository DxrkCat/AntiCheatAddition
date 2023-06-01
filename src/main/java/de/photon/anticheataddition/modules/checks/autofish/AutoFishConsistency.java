package de.photon.anticheataddition.modules.checks.autofish;

import de.photon.anticheataddition.AntiCheatAddition;
import de.photon.anticheataddition.ServerVersion;
import de.photon.anticheataddition.modules.ModuleLoader;
import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.user.data.TimeKey;
import de.photon.anticheataddition.util.mathematics.MathUtil;
import de.photon.anticheataddition.util.violationlevels.Flag;
import de.photon.anticheataddition.util.violationlevels.ViolationLevelManagement;
import de.photon.anticheataddition.util.violationlevels.ViolationManagement;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;

public final class AutoFishConsistency extends ViolationModule implements Listener
{
    public static final AutoFishConsistency INSTANCE = new AutoFishConsistency();

    private final int cancelVl = AntiCheatAddition.getInstance().getConfig().getInt("AutoFish.cancel_vl");

    private static final int FISHING_ATTEMPTS_TO_CHECK = 5;
    private static final int MIN_HUMAN_VARIATION = 50;

    private AutoFishConsistency()
    {
        super("AutoFish.parts.consistency");
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerFish(final PlayerFishEvent event)
    {
        final var user = de.photon.anticheataddition.user.User.getUser(event.getPlayer());
        if (User.isUserInvalid(user, this)) return;

        switch (event.getState()) {
            case FISHING -> {
                // Not too many failed attempts in between (afk fish farm false positives)
                // Negative maximum_fails indicate not allowing afk fishing farms.
                if (user.getData().counter.autofishFailed.smallerThanThreshold() &&
                    // If the last attempt was a fail do not check (false positives)
                    user.getTimeMap().at(TimeKey.AUTOFISH_DETECTION).getTime() != 0)
                {
                    // Buffer the data.
                    final var consistencyData = user.getData().object.autoFishConsistencyData;
                    consistencyData.accept(user.getTimeMap().at(TimeKey.AUTOFISH_DETECTION).passedTime());

                    // Check that we have enough data.
                    if (consistencyData.getCount() < FISHING_ATTEMPTS_TO_CHECK) return;

                    // Calculate the maximum offset.
                    final double maxOffset = Math.max(MathUtil.absDiff(consistencyData.getMin(), consistencyData.getAverage()), MathUtil.absDiff(consistencyData.getMax(), consistencyData.getAverage()));

                    if (MIN_HUMAN_VARIATION > (maxOffset + 1)) {
                        // (maxOffset / minVariation) will be at most 1 and at least 0
                        final double flagOffset = 160 - (159 * (maxOffset / MIN_HUMAN_VARIATION));

                        this.getManagement().flag(Flag.of(event.getPlayer())
                                                      .setAddedVl((int) flagOffset)
                                                      .setDebug(() -> "AutoFish-Debug | Player %s failed consistency | average time: %f | maximum offset: %f | flag offset: %f"
                                                              .formatted(user.getPlayer().getName(),
                                                                         consistencyData.getAverage(),
                                                                         maxOffset,
                                                                         flagOffset))
                                                      .setCancelAction(cancelVl, () -> event.setCancelled(true)));
                    }

                    // Reset the statistics.
                    consistencyData.reset();
                }

                // Reset the fail counter as just now there was a fishing success.
                user.getData().counter.autofishFailed.setToZero();
            }
            // No consistency when not fishing / failed fishing
            case IN_GROUND, FAILED_ATTEMPT -> {
                user.getTimeMap().at(TimeKey.AUTOFISH_DETECTION).setToZero();
                user.getData().counter.autofishFailed.increment();
            }
            // CAUGHT_FISH covers all forms of items from the water.
            case CAUGHT_FISH -> user.getTimeMap().at(TimeKey.AUTOFISH_DETECTION).update();
        }
    }

    @Override
    protected ModuleLoader createModuleLoader()
    {
        return ModuleLoader.builder(this)
                           .setAllowedServerVersions(ServerVersion.NON_188_VERSIONS)
                           .build();
    }

    @Override
    protected ViolationManagement createViolationManagement()
    {
        return ViolationLevelManagement.builder(this)
                                       .emptyThresholdManagement()
                                       .withDecay(800, 3).build();
    }
}