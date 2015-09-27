package bitDecayJump.collision;

import bitDecayJump.*;
import bitDecayJump.geom.BitRectangle;
import bitDecayJump.geom.MathUtils;

/**
 * Created by Monday on 9/27/2015.
 */
public class SATStrategy  extends BitResolution {

    public SATStrategy(BitBody body) {
        super(body);
    }

    @Override
    public void satisfy() {
        for (BitCollision collision : collisions) {
            SATResolution satRes = SATCollisions.getCollision(resolvedPosition, collision.otherBody.aabb);
            if (satRes != null) {
                satResolve(resolvedPosition, satRes);
                postResolve(body, collision.otherBody, satRes);
            }
        }
    }

    private void satResolve(BitRectangle resolvedPosition, SATResolution satRes) {
        if (satRes.axis.x != 0 && satRes.axis.y > 0) {
            // this is logic to make it so the player doesn't move slower when running uphill. Likewise, we will need logic to 'glue' the player to the ground when running downhill.
            // atan is our angle of resolution
            double atan = Math.atan(satRes.axis.y / satRes.axis.x);

            if (Math.abs(atan - MathUtils.PI_OVER_TWO) <= Math.toRadians(30)) {
                // currently we impose a hard limit of 30 degree angle 'walkability'
                if (atan > 0) {
                    double angleToUpright;
                    angleToUpright = MathUtils.PI_OVER_TWO - atan;
                    double straightUp = satRes.distance / Math.cos(angleToUpright);
                    resolvedPosition.xy.add(0, (float) straightUp);
                } else {
                    double angleToUpright;
                    angleToUpright = -MathUtils.PI_OVER_TWO - atan;
                    double straightUp = satRes.distance / Math.cos(angleToUpright);
                    resolvedPosition.xy.add(0, (float) straightUp);
                }
                return;
            }
        }

        resolvedPosition.xy.add(satRes.axis.x * satRes.distance, satRes.axis.y * satRes.distance);

    }


    private void postResolve(BitBody body, BitBody otherBody, SATResolution satRes) {
        if (satRes.axis.len() != 0 && BodyType.KINETIC.equals(otherBody.props.bodyType)) {
            if (body.parent == null) {
                // Attach as child if we were resolved by the kinetic object in the direction it is moving
                if (satRes.axis.dot(otherBody.lastAttempt.x, otherBody.lastAttempt.y) > 0) {
                    body.parent = otherBody;
                    otherBody.children.add(body);
                }
                // attach if we were resolved against gravity (aka we are standing on it)
                if (satRes.axis.dot(BitWorld.gravity.x, BitWorld.gravity.y) < 0) {
                    body.parent = otherBody;
                    otherBody.children.add(body);
                }
            }
        }
    }



}
