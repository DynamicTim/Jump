package com.bitdecay.jump.collision;

import com.bitdecay.jump.BitBody;
import com.bitdecay.jump.geom.BitPoint;
import com.bitdecay.jump.geom.GeomUtils;
import com.bitdecay.jump.geom.MathUtils;
import com.bitdecay.jump.level.Direction;
import com.bitdecay.jump.level.TileBody;

/**
 * Created by Monday on 6/16/2016.
 */
public class CollisionUtilities {
    public static BitPoint getRelativeMovement(BitBody body, BitBody otherBody, BitPoint cumulativeResolution) {
        // this line is just taking where the body tried to move and the partially resolved position into account to
        // figure out the relative momentum.
        BitPoint bodyAttempt = body.resolutionLocked ? new BitPoint(0, 0) : body.currentAttempt;
        BitPoint otherBodyAttempt = otherBody.resolutionLocked ? new BitPoint(0, 0) : otherBody.currentAttempt;
        return bodyAttempt.plus(cumulativeResolution).minus(otherBodyAttempt);
    }

    public static boolean isTileValidCollision(TileBody otherBody, Manifold manifold, float resolutionPosition, float lastPosition) {
        if (!axisValidForNValue(manifold, otherBody)) {
            return true;
        }

        if (MathUtils.opposing(resolutionPosition, lastPosition)) {
            // all collisions should push a body backwards according to the
            // relative movement. If it's not doing so, it's not a valid case.
            return true;
        }

        if (manifold.distance < 0 && (lastPosition > resolutionPosition)) {
            return true;
        }

        if (manifold.distance > 0 && lastPosition < resolutionPosition) {
            return true;
        }

        if (otherBody.collisionAxis != null) {
            if (manifold.axis.equals(otherBody.collisionAxis)) {
                if (manifold.distance < 0) {
                    return true;
                }
            } else if (manifold.axis.equals(otherBody.collisionAxis.scale(-1))) {
                if (manifold.distance > 0) {
                    return true;
                }
            } else {
                return true;
            }
        }
        return false;
    }

    /**
     * This method finds what axis should be used for resolution. This method takes into account the relative
     * speeds of the objects being resolved. A resolution will only be applied to an object that is AGAINST
     * the relative movement between it and what it has collided against.
     *
     * This logic is here to ensure corners feel crisp when a body is landing on the edge of a platform.
     *
     * <b>NOTE:</b> Due to logic to make sure the feel is correct, there are situations (such as one-way
     * platforms) that can result in NO axis (aka the Zero-axis) being set on the resolution.
     *
     *  @param body the body being resolved
     * @param otherBody the other body that participated in the collision
     * @param cumulativeResolution the current partially built resolved position
     */
    public static Manifold solve(ManifoldBundle bundle, BitBody body, BitBody otherBody, BitPoint cumulativeResolution) {
        bundle.getCandidates().sort((o1, o2) -> Float.compare(Math.abs(o1.distance), Math.abs(o2.distance)));

        BitPoint relativeMovement = CollisionUtilities.getRelativeMovement(body, otherBody, cumulativeResolution);

        float dotProd;
        for (Manifold manifold : bundle.getCandidates()) {
            dotProd = relativeMovement.dot(manifold.axis);
            if (dotProd != 0 && !MathUtils.sameSign(dotProd, manifold.distance)) {
                if (otherBody instanceof TileBody) {
                    // confirm that body came from past this thing
                    float resolutionPosition = body.aabb.xy.plus(cumulativeResolution).plus(manifold.result).dot(manifold.axis);
                    float lastPosition = body.lastPosition.dot(manifold.axis);

                    if (CollisionUtilities.isTileValidCollision((TileBody) otherBody, manifold, resolutionPosition, lastPosition)) {
                        continue;
                    }
                }
                return manifold;
            }
        }
        /*
         * This is a fallback, I can't think of a scenario where we would get here, but better to do
         * something than to leave ourselves open to an NPE
         */
        return GeomUtils.ZERO_MANIFOLD;
    }

    public static boolean axisValidForNValue(Manifold axisOver, TileBody body) {
        if (axisOver.axis.equals(GeomUtils.X_AXIS) && (body.nValue & Direction.RIGHT) == 0) {
            return true;
        } else if (axisOver.axis.equals(GeomUtils.NEG_X_AXIS) && (body.nValue & Direction.LEFT) == 0) {
            return true;
        } else if (axisOver.axis.equals(GeomUtils.Y_AXIS) && (body.nValue & Direction.UP) == 0) {
            return true;
        } else if (axisOver.axis.equals(GeomUtils.NEG_Y_AXIS) && (body.nValue & Direction.DOWN) == 0) {
            return true;
        } else {
            return false;
        }
    }

    public static Manifold getSolutionCandidate(BitBody body, BitBody against, BitPoint cumulativeResolution) {
        ManifoldBundle bundle = ProjectionUtilities.getBundle(body.aabb.copyOf().translate(cumulativeResolution), against.aabb);
        if (bundle == null) {
            return GeomUtils.ZERO_MANIFOLD;
        } else {
            return solve(bundle, body, against, cumulativeResolution);
        }
    }
}
