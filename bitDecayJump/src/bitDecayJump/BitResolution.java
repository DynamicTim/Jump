package bitDecayJump;

import java.util.PriorityQueue;

import bitDecayJump.collision.SATCollisions;
import bitDecayJump.collision.SATResolution;
import bitDecayJump.geom.*;
import bitDecayJump.level.*;

/**
 * A class to contain all logic and information about a single resolution plan.
 * Each instance of this class will resolve collisions against a single body.
 *
 * @author Monday
 *
 */
public abstract class BitResolution {
	/**
	 * A priority queue that resolves the LARGEST collisions first. Larger
	 * collisions (by area) are easier to determine accurate resolutions for.
	 */
	protected PriorityQueue<BitCollision> collisions = new PriorityQueue<BitCollision>();
	protected BitRectangle resolvedPosition;
	protected BitPoint resolution = new BitPoint(0, 0);
    protected boolean haltX;
    protected boolean haltY;
    protected BitBody body;

	public BitResolution(BitBody body) {
		this.body = body;
		resolvedPosition = new BitRectangle(body.aabb);
	}

    public void resolve() {
        satisfy();

        // set final resolution values
        resolution.x = resolvedPosition.xy.x - body.aabb.xy.x;
        resolution.y = resolvedPosition.xy.y - body.aabb.xy.y;
        if (resolution.y != 0) {
            haltY = true;
        }
        if (resolution.x != 0) {
            haltX = true;
        }
    }

	/**
	 * Let our strategy resolve the collision and set the resolved position
	 */
	public abstract void satisfy();
}
