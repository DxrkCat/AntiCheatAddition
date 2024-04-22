package de.photon.anticheataddition.util.mathematics;

import lombok.experimental.UtilityClass;
import org.bukkit.Location;
import org.bukkit.util.Vector;

@UtilityClass
public final class RotationUtil
{

    private static final float FIX_CONVERT_FACTOR = 256.0F / 360.0F;
    private static final float FIX_INVERSE_CONVERT_FACTOR = 360.0F / 256.0F;

    /**
     * Fixes the rotation for the ClientsideLivingEntities
     */
    public static byte getFixRotation(final float yawpitch)
    {
        return (byte) (yawpitch * FIX_CONVERT_FACTOR);
    }

    /**
     * Reconverts rotation values returned by {@link #getFixRotation(float)}
     */
    public static float convertFixedRotation(final byte fixedRotation)
    {
        return fixedRotation * FIX_INVERSE_CONVERT_FACTOR;
    }

    /**
     * Generates the direction - vector from yaw and pitch, basically a copy of {@link Location#getDirection()}
     */
    @SuppressWarnings("RedundantCast")
    public static Vector getDirection(final float yaw, final float pitch)
    {
        final double yawRadians = Math.toRadians((double) yaw);
        final double pitchRadians = Math.toRadians((double) pitch);

        final var vector = new Vector();

        vector.setY(-Math.sin(pitchRadians));

        final double xz = Math.cos(pitchRadians);

        vector.setX(-xz * Math.sin(yawRadians));
        vector.setZ(xz * Math.cos(yawRadians));

        return vector;
    }

    /**
     * Calculates the angle between two rotations using {@link Vector}s.
     *
     * @return The angle between the two rotations in degrees.
     */
    public static float getAngleBetweenRotations(final float firstYaw, final float firstPitch, final float secondYaw, final float secondPitch)
    {
        final Vector first = getDirection(firstYaw, firstPitch);
        final Vector second = getDirection(secondYaw, secondPitch);

        return (float) Math.toDegrees(first.angle(second));
    }
}
