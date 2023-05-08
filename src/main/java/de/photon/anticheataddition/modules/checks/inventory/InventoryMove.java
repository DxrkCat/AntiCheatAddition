package de.photon.anticheataddition.modules.checks.inventory;

import de.photon.anticheataddition.AntiCheatAddition;
import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.user.data.TimeKey;
import de.photon.anticheataddition.util.messaging.Log;
import de.photon.anticheataddition.util.minecraft.entity.EntityUtil;
import de.photon.anticheataddition.util.minecraft.tps.TPSProvider;
import de.photon.anticheataddition.util.minecraft.world.InternalPotion;
import de.photon.anticheataddition.util.minecraft.world.MaterialUtil;
import de.photon.anticheataddition.util.minecraft.world.WorldUtil;
import de.photon.anticheataddition.util.violationlevels.Flag;
import de.photon.anticheataddition.util.violationlevels.ViolationLevelManagement;
import de.photon.anticheataddition.util.violationlevels.ViolationManagement;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.potion.PotionEffect;

import java.util.Set;
import java.util.stream.Stream;

public final class InventoryMove extends ViolationModule implements Listener
{
    public static final InventoryMove INSTANCE = new InventoryMove();
    public static final double STANDING_STILL_THRESHOLD = 0.005;

    private final int cancelVl = loadInt(".cancel_vl", 60);
    private final int lenienceMillis = loadInt(".lenience_millis", 0);
    private final int teleportBypassTime = loadInt(".teleport_bypass_time", 900);
    private final int worldChangeBypassTime = loadInt(".world_change_bypass_time", 2000);

    private InventoryMove()
    {
        super("Inventory.parts.Move");
    }

    private static void cancelAction(User user, PlayerMoveEvent event)
    {
        // Not many blocks moved to prevent exploits and world change problems.
        if (WorldUtil.INSTANCE.inSameWorld(event.getFrom(), event.getTo()) && event.getFrom().distanceSquared(event.getTo()) < 4) {
            // Teleport back the next tick.
            Bukkit.getScheduler().runTask(AntiCheatAddition.getInstance(), () -> user.getPlayer().teleport(event.getFrom(), PlayerTeleportEvent.TeleportCause.UNKNOWN));
        }
    }

    /**
     * This checks all 9 blocks centered on where the player stands as well as the 9 blocks below to reliably check for materials like slabs.
     * Checking all those blocks is required because stepping up a slab does not mean the player's block-location is already the slab.
     */
    private static boolean checkGroundMaterial(Location location, Set<Material> materials)
    {
        return Stream.concat(WorldUtil.INSTANCE.getBlocksAround(location.getBlock(), WorldUtil.HORIZONTAL_FACES, Set.of()).stream(),
                             WorldUtil.INSTANCE.getBlocksAround(location.getBlock().getRelative(BlockFace.DOWN), WorldUtil.HORIZONTAL_FACES, Set.of()).stream())
                     .map(Block::getType)
                     .anyMatch(materials::contains);
    }

    private static long breakingTime(User user)
    {
        // 300 is the vanilla breaking time without a speed effect (derived from testing, especially slab jumping)
        return 300L + InternalPotion.SPEED.getPotionEffect(user.getPlayer())
                                          .map(PotionEffect::getAmplifier)
                                          // If a speed effect exists calculate the speed millis, otherwise the speedMillis are 0.
                                          .map(amplifier -> Math.max(100, amplifier + 1) * 50L)
                                          .orElse(0L);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerMove(PlayerMoveEvent event)
    {
        final var user = User.getUser(event.getPlayer());
        if (User.isUserInvalid(user, this) || event.getTo() == null ||
            // Check that the player has actually moved.
            // Do this here to prevent bypasses with setting allowedToJump to true
            event.getTo().distanceSquared(event.getFrom()) <= STANDING_STILL_THRESHOLD) return;

        // Not inside a vehicle
        if (user.getPlayer().isInsideVehicle() ||
            // Not flying (vanilla or elytra) as it may trigger some fps
            user.getPlayer().isFlying() ||
            EntityUtil.INSTANCE.isFlyingWithElytra(user.getPlayer()) ||
            // Player must be in an inventory
            !user.hasOpenInventory() ||
            // After being hit a player moves due to knock-back, so recent hits can cause false positives.
            user.getPlayer().getNoDamageTicks() != 0 ||
            // Recent teleports can cause bugs
            user.hasTeleportedRecently(teleportBypassTime) ||
            user.hasChangedWorldsRecently(worldChangeBypassTime) ||
            // The player is currently not in a liquid (liquids push)
            // This would need to check for async chunk loads if done in packets (see history)
            user.isInLiquids() ||
            // Auto-Disable if TPS are too low
            !TPSProvider.INSTANCE.atLeastTPS(Inventory.INSTANCE.getMinTps()))
        {
            user.getData().bool.allowedToJump = true;
            return;
        }

        final boolean movingUpwards = event.getFrom().getY() < event.getTo().getY();
        final boolean noYMovement = event.getFrom().getY() == event.getTo().getY();

        if (movingUpwards != user.getData().bool.movingUpwards) {
            handleJump(user, event, movingUpwards, noYMovement);
            return;
        }

        final double yMovement = event.getPlayer().getVelocity().getY();

        Log.finer(() -> "Inventory-Debug | Player " + user.getPlayer().getName() + " checking for falling: " + user.getTimeMap().at(TimeKey.VELOCITY_CHANGE_NO_EXTERNAL_CAUSES).passedTime() +
                        " | y-velocity: " + yMovement);

        // This bypasses players during a long fall from hundreds of blocks.
        if (yMovement < -2.0 ||
            // Bypass a falling player after a normal jump in which they opened an inventory.
            user.hasJumpedRecently(1850) &&
            // If the y-movement is 0, the falling process is finished and there is no need for bypassing anymore.
            !noYMovement)
        {
            user.getTimeMap().at(TimeKey.INVENTORY_MOVE_JUMP_END).update();
            return;
        }

        Log.finer(() -> "Inventory-Debug | Player " + user.getPlayer().getName() + " is not fall-bypassed with y-movement: " + yMovement);

        final long totalBreakingTime = breakingTime(user) + lenienceMillis;

        Log.finer(() -> "Inventory-Debug | Player " + user.getPlayer().getName() + " breaking time: " + totalBreakingTime +
                        " | inventory open passed time: " + user.getTimeMap().at(TimeKey.INVENTORY_OPENED).passedTime() +
                        " | jump end passed time: " + user.getTimeMap().at(TimeKey.INVENTORY_MOVE_JUMP_END).passedTime());

        // The breaking is no longer affecting the user as they have opened their inventory long enough ago.
        if (user.notRecentlyOpenedInventory(totalBreakingTime) &&
            // If the player jumped, we need to check the breaking time after the jump ended.
            user.getTimeMap().at(TimeKey.INVENTORY_MOVE_JUMP_END).notRecentlyUpdated(totalBreakingTime) &&
            // Do the entity pushing stuff here (performance impact)
            // No nearby entities that could push the player
            WorldUtil.INSTANCE.getLivingEntitiesAroundEntity(user.getPlayer(), user.getHitboxLocation().hitbox(), 0.1D).isEmpty())
        {
            getManagement().flag(Flag.of(user)
                                     .setAddedVl(5)
                                     .setCancelAction(cancelVl, () -> cancelAction(user, event))
                                     .setDebug(() -> "Inventory-Debug | Player: " + user.getPlayer().getName() + " moved while having an open inventory."));
        }
    }

    private void handleJump(User user, PlayerMoveEvent event, boolean positiveVelocity, boolean noYMovement)
    {
        Log.finer(() -> "Inventory-Debug | Player " + user.getPlayer().getName() + " detected a jump.");

        // A player is only allowed to jump once.
        if (user.getData().bool.allowedToJump) {
            user.getData().bool.allowedToJump = false;
            return;
        }

        Log.finer(() -> "Inventory-Debug | Player " + user.getPlayer().getName() + " is not jump-bypassed.");

        // Bouncing can lead to false positives.
        if (checkGroundMaterial(event.getFrom(), MaterialUtil.BOUNCE_MATERIALS)) return;

        Log.finer(() -> "Inventory-Debug | Player " + user.getPlayer().getName() + " no bounce materials detected.");

        if ((positiveVelocity || noYMovement) &&
            // Jumping onto a stair or slabs false positive
            checkGroundMaterial(event.getFrom(), MaterialUtil.AUTO_STEP_MATERIALS)) return;

        getManagement().flag(Flag.of(user)
                                 .setAddedVl(25)
                                 .setCancelAction(cancelVl, () -> cancelAction(user, event))
                                 .setDebug(() -> "Inventory-Debug | Player: " + user.getPlayer().getName() + " jumped while having an open inventory."));
    }

    @Override
    protected ViolationManagement createViolationManagement()
    {
        return ViolationLevelManagement.builder(this)
                                       .emptyThresholdManagement()
                                       .withDecay(100, 2).build();
    }
}

