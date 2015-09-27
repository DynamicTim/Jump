package bitDecayJump.collision;

import bitDecayJump.*;
import bitDecayJump.geom.BitPoint;
import bitDecayJump.geom.BitRectangle;
import bitDecayJump.geom.GeomUtils;
import bitDecayJump.geom.MathUtils;
import bitDecayJump.level.Direction;
import bitDecayJump.level.TileBodyProps;

/**
 * Created by Monday on 9/27/2015.
 */
public class ClassicStrategy extends BitResolution{

    // Some simple state stuff for tracking what direction we've been resolved in
    int leftRight = Direction.NONE;
    int upDown = Direction.NONE;

    public ClassicStrategy(BitBody body) {
        super(body);
    }

    @Override
    public void satisfy() {
        // use a temp BitPoint to hold resolution values for each collision
        BitPoint tempResolution = new BitPoint(0, 0);
        BitPoint cumulativeResolution = new BitPoint(0, 0);
        for (BitCollision collision : collisions) {
            if (GeomUtils.intersection(resolvedPosition, collision.collisionZone) != null) {
                // only deal with this if we are still needing to resolved
                int resoDirection = resolve(tempResolution, cumulativeResolution, body, collision.otherBody);
                if (resoDirection != 0 && BodyType.KINETIC.equals(collision.otherBody.props.bodyType)) {
                    // only attach as child if we were resolved by the kinetic object in the direction it is moving
                    boolean attach = false;
                    switch (resoDirection) {
                        case Direction.UP:
                            if (BitWorld.gravity.y < 0 || collision.otherBody.lastAttempt.y > 0) {
                                attach = true;
                            }
                            break;
                        case Direction.DOWN:
                            if (BitWorld.gravity.y > 0 || collision.otherBody.lastAttempt.y < 0) {
                                attach = true;
                            }
                            break;
                        case Direction.LEFT:
                            if (BitWorld.gravity.x > 0 || collision.otherBody.lastAttempt.x < 0) {
                                attach = true;
                            }
                            break;
                        case Direction.RIGHT:
                            if (BitWorld.gravity.x < 0 || collision.otherBody.lastAttempt.x > 0) {
                                attach = true;
                            }
                            break;
                        default:

                    }
                    if (attach && body.parent == null) {
                        body.parent = collision.otherBody;
                        collision.otherBody.children.add(body);
                    }
                }

                // Some simple state stuff to find when we are resolved in conflicting directions
                if ((resoDirection & Direction.SIDES) != 0) {
                    if (leftRight != 0 && resoDirection != leftRight) {
                        // resolved left and right in same resolution phase
                        body.active = false;
                    }
                    leftRight = resoDirection;
                }
                if ((resoDirection & Direction.UPDOWN) != 0) {
                    if (upDown != 0 && resoDirection != upDown) {
                        // resolved up and own in same resolution phase
                        body.active = false;
                    }
                    upDown = resoDirection;
                }
                resolvedPosition.xy.x += tempResolution.x;
                resolvedPosition.xy.y += tempResolution.y;
                cumulativeResolution.x += tempResolution.x;
                cumulativeResolution.y += tempResolution.y;

                tempResolution.x = 0;
                tempResolution.y = 0;

                BitWorld.resolvedCollisions.add(collision.collisionZone);
            } else {
                BitWorld.unresolvedCollisions.add(collision.collisionZone);
            }
        }
    }

    private int resolve(BitPoint resolution, BitPoint cumulativeResolution, BitBody body, BitBody against) {
        BitRectangle insec = GeomUtils.intersection(body.aabb, against.aabb);
        if (insec != null) {
            if (insec.width < MathUtils.COLLISION_PRECISION && insec.height < MathUtils.COLLISION_PRECISION) {
                return Direction.NONE;
            }
            int nValue = 0;
            if (against.props instanceof TileBodyProps) {
                nValue = ((TileBodyProps) against.props).nValue;
            }
			/*
			 * Take the cumulative resolution into account because the body has
			 * effectively moved this much while resolving the position
			 */
            BitPoint relativeVelocity = body.lastAttempt;
            if (body.parent != null) {
                relativeVelocity.add(body.parent.lastAttempt);
            }
            //			BitPoint relativeVelocity = body.lastAttempt
            //					.plus(cumulativeResolution);
            if (against.lastAttempt.x != 0 || against.lastAttempt.y != 0) {
                // adjust this to work with kinetic bodies
                relativeVelocity = relativeVelocity.plus(body.lastResolution).minus(against.lastAttempt);
            }
            final boolean xSpeedDominant = Math.abs(relativeVelocity.x) > Math.abs(relativeVelocity.y);

            if (insec.equals(body.aabb)) {
                // body is entirely inside another object
                return resolveBodyInside(resolution, body, against, nValue, insec, xSpeedDominant);
            } else if (insec.equals(against)) {
                // against is entirely inside the body
                return resolveBodyInside(resolution, body, against, nValue, insec, xSpeedDominant);
            }

            boolean validLeftCollision = validBodyLeftCollision(body, nValue, insec, relativeVelocity);
            boolean validRightCollision = validBodyRightCollision(body, nValue, insec, relativeVelocity);
            boolean validTopCollision = validBodyTopCollisionLoose(body, nValue, insec, relativeVelocity);
            boolean validBottomCollision = validBodyBottomCollisionLoose(body, nValue, insec, relativeVelocity);
            if (validBottomCollision && body.props.velocity.y <= 0 && body.grounded) {
                // body collided on its bottom side
                resolution.y = Math.max(resolution.y, insec.height);
                return Direction.UP;
            } else if (validLeftCollision) {
                // body collided on its left side
                resolution.x = Math.max(resolution.x, insec.width);
                return Direction.RIGHT;
            } else if (validRightCollision) {
                // body collided on its right side
                resolution.x = Math.min(resolution.x, -insec.width);
                return Direction.LEFT;
            } else if (validTopCollision) {
                // body collided on its top side
                resolution.y = Math.min(resolution.y, -insec.height);
                return Direction.DOWN;
            } else if (validBottomCollision) {
                // body collided on its bottom side
                resolution.y = Math.max(resolution.y, insec.height);
                return Direction.UP;
            } else {
                // handle where the body is partially inside a box that was not resolvable with the basic checks
                if (insec.height == body.aabb.height) {
                    if (body.props.velocity.y <= 0) {
                        return resolveUp(resolution, against, insec);
                    } else {
                        return resolveDown(resolution, against, insec);
                    }
                } else if (insec.width == body.aabb.width) {
                    if (body.props.velocity.x < 0) {
                        return resolveRight(resolution, against, insec);
                    } else if (body.props.velocity.x > 0) {
                        return resolveLeft(resolution, against, insec);
                    }
                }
            }
            return Direction.NONE;
        }
        return Direction.NONE;
    }

    private void resolveBodySurround() {
        // TODO Auto-generated method stub
    }

    private int resolveBodyInside(BitPoint resolution, BitBody body, BitBody against, int nValue, BitRectangle insec, boolean xSpeedDominant) {
        // handle where a body is entirely inside another object
        // check speed and move the character straight out based on the dominant vector axis
        if (xSpeedDominant && body.props.velocity.x <= 0 && (nValue & Direction.RIGHT) == 0) {
            //push them out to the right
            return resolveRight(resolution, against, insec);
        } else if (xSpeedDominant && body.props.velocity.x > 0 && (nValue & Direction.LEFT) == 0) {
            // push them out to the left
            return resolveLeft(resolution, against, insec);
        } else if (!xSpeedDominant && body.props.velocity.y <= 0 && (nValue & Direction.UP) == 0) {
            // push them out to the top
            return resolveUp(resolution, against, insec);
        } else if (!xSpeedDominant && body.props.velocity.y > 0 && (nValue & Direction.DOWN) == 0) {
            // push them out to the bottom
            return resolveDown(resolution, against, insec);
        }
        return Direction.NONE;
    }

    private int resolveLeft(BitPoint resolution, BitBody against, BitRectangle insec) {
        resolution.x = Math.min(resolution.x, against.aabb.xy.x - (insec.xy.x + insec.width));
        return Direction.LEFT;
    }

    private int resolveDown(BitPoint resolution, BitBody against, BitRectangle insec) {
        resolution.y = Math.min(resolution.y, against.aabb.xy.y - (insec.xy.y + insec.height));
        return Direction.DOWN;
    }

    private int resolveUp(BitPoint resolution, BitBody against, BitRectangle insec) {
        resolution.y = Math.max(resolution.y, against.aabb.xy.y + against.aabb.height - insec.xy.y);
        return Direction.UP;
    }

    private int resolveRight(BitPoint resolution, BitBody against, BitRectangle insec) {
        resolution.x = Math.max(resolution.x, against.aabb.xy.x + against.aabb.width - insec.xy.x);
        return Direction.RIGHT;
    }

    // top/bottom collisions are slightly more lenient on velocity restrictions (velocity.y can = 0 compared to velocity.x != 0 for left/right)
    private boolean validBodyBottomCollisionLoose(BitBody body, int nValue, BitRectangle insec, BitPoint relativeVelocity) {
        return (nValue & Direction.UP) == 0 && !MathUtils.close(insec.height, body.aabb.height) && MathUtils.close(insec.xy.y, body.aabb.xy.y)
                && (relativeVelocity.y <= 0 || body.lastResolution.y > MathUtils.FLOAT_PRECISION);
    }

    private boolean validBodyTopCollisionLoose(BitBody body, int nValue, BitRectangle insec, BitPoint relativeVelocity) {
        return (nValue & Direction.DOWN) == 0 && !MathUtils.close(insec.height, body.aabb.height)
                && MathUtils.close(insec.xy.y + insec.height, body.aabb.xy.y + body.aabb.height)
                && (relativeVelocity.y >= 0 || body.lastResolution.y < -MathUtils.FLOAT_PRECISION);
    }

    private boolean validBodyRightCollision(BitBody body, int nValue, BitRectangle insec, BitPoint relativeVelocity) {
        return (nValue & Direction.LEFT) == 0 && !MathUtils.close(insec.width, body.aabb.width)
                && MathUtils.close(insec.xy.x + insec.width, body.aabb.xy.x + body.aabb.width) && (insec.width < insec.height || noUpDownSpace(nValue))
                && (relativeVelocity.x > MathUtils.FLOAT_PRECISION || body.lastResolution.x < -MathUtils.FLOAT_PRECISION);
    }

    private boolean validBodyLeftCollision(BitBody body, int nValue, BitRectangle insec, BitPoint relativeVelocity) {
        return (nValue & Direction.RIGHT) == 0 && !MathUtils.close(insec.width, body.aabb.width) && MathUtils.close(insec.xy.x, body.aabb.xy.x)
                && (insec.width < insec.height || noUpDownSpace(nValue))
                && (relativeVelocity.x < -MathUtils.FLOAT_PRECISION || body.lastResolution.x > MathUtils.FLOAT_PRECISION);
    }

    private boolean noUpDownSpace(int nValue) {
        return (nValue & Direction.UPDOWN) == Direction.UPDOWN;
    }

    private boolean noLeftRightSpace(int nValue) {
        return (nValue & Direction.SIDES) == Direction.SIDES;
    }
}
