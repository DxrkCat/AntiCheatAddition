package de.photon.anticheataddition.user.data.batch;

import com.google.common.eventbus.EventBus;
import de.photon.anticheataddition.ServerVersion;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.util.datastructure.batch.Batch;
import de.photon.anticheataddition.util.mathematics.MathUtil;
import de.photon.anticheataddition.util.minecraft.world.InternalPotion;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.NotNull;

public final class ScaffoldBatch extends Batch<ScaffoldBatch.ScaffoldBlockPlace>
{
    public static final EventBus SCAFFOLD_BATCH_EVENTBUS = new EventBus();

    public ScaffoldBatch(@NotNull User user)
    {
        super(SCAFFOLD_BATCH_EVENTBUS, user, 6, new ScaffoldBlockPlace(0, null, null, new Location(null, 0, 0, 0), true, 0, 1));
    }

    public record ScaffoldBlockPlace(long time, Block block, BlockFace blockFace, Location location, boolean sneaked, int swiftSneakLevel, double speedModifier)
    {
        public ScaffoldBlockPlace(Block block, BlockFace blockFace, User user)
        {
            this(System.currentTimeMillis(),
                 block,
                 blockFace,
                 user.getPlayer().getLocation(),
                 user.hasSneakedRecently(175),
                 swiftSneakLevel(user.getPlayer()),
                 InternalPotion.SPEED.getPotionEffect(user.getPlayer()).map(ScaffoldBlockPlace::calcSpeedModifier).orElse(1.0D));
        }

        private static double calcSpeedModifier(@NotNull PotionEffect potionEffect)
        {
            return switch (potionEffect.getAmplifier()) {
                case 0 -> 1.03D;
                case 1 -> 1.25D;
                case 2 -> 1.15D;
                case 3 -> 1.2D;
                case 4 -> 1.5D;
                case 5, 6 -> 1.65D;
                case 7 -> 2D;
                case 8 -> 2.2D;
                // Everything above 8 should have a speed_modifier of 3
                default -> {
                    if (potionEffect.getAmplifier() <= 17) {
                        yield 2.5;
                    } else if (potionEffect.getAmplifier() <= 50) {
                        yield 4;
                    } else yield 4.5;
                }
            };
        }

        private static int swiftSneakLevel(Player player)
        {
            // SwiftSneak is only available in 1.19+
            if (ServerVersion.MC118.activeIsEarlierOrEqual()) return 0;

            var equip = player.getEquipment();
            if (equip == null) return 0;
            var leggings = equip.getLeggings();
            if (leggings == null) return 0;
            var enchantments = leggings.getEnchantments();
            return enchantments.getOrDefault(Enchantment.SWIFT_SNEAK, 0);
        }

        public long timeOffset(@NotNull ScaffoldBlockPlace other)
        {
            return MathUtil.absDiff(time, other.time);
        }
    }
}
