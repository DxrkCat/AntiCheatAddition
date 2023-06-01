package de.photon.anticheataddition.modules.checks.inventory;

import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.util.violationlevels.Flag;
import de.photon.anticheataddition.util.violationlevels.ViolationLevelManagement;
import de.photon.anticheataddition.util.violationlevels.ViolationManagement;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

public final class InventoryHit extends ViolationModule implements Listener
{
    public static final InventoryHit INSTANCE = new InventoryHit();

    private final int cancelVl = loadInt(".cancel_vl", 60);

    private InventoryHit()
    {
        super("Inventory.parts.Hit");
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event)
    {
        // Is a hit-attack
        if (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK &&
            event.getDamager() instanceof Player player &&
            Inventory.hasMinTPS())
        {
            final var user = User.getUser(player.getUniqueId());
            if (User.isUserInvalid(user, this)) return;

            // Is in Inventory (Detection)
            if (user.hasOpenInventory() &&
                // Have the inventory opened for some time
                user.notRecentlyOpenedInventory(1000))
            {
                this.getManagement().flag(Flag.of(user)
                                              .setAddedVl(20)
                                              .setCancelAction(cancelVl, () -> event.setCancelled(true))
                                              .setDebug(() -> "Inventory-Debug | Player: " + user.getPlayer().getName() + " hit an entity while having an open inventory."));
            }
        }
    }

    @Override
    protected ViolationManagement createViolationManagement()
    {
        return ViolationLevelManagement.builder(this)
                                       .emptyThresholdManagement()
                                       .withDecay(160, 2).build();
    }
}
