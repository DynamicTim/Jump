package com.bitdecay.jump;

import com.bitdecay.jump.controller.BitBodyController;
import com.bitdecay.jump.exception.BitBodySerializeException;
import com.bitdecay.jump.geom.BitPoint;
import com.bitdecay.jump.geom.BitRectangle;
import com.bitdecay.jump.geom.Projectable;
import com.bitdecay.jump.state.BitBodyStateWatcher;

import java.util.HashSet;
import java.util.Set;

public class BitBody {
    /**
     * Contains the position and size data for this body
     */
	public BitRectangle aabb = new BitRectangle(1, 1, 1, 1);

    /**
     * Contains a more specific shape we can use for the body. This shape should
     * be contained within the aabb.
     */
    public Projectable refined = null;

    /**
     * Reference to the optional parent object
     */
	public BitBody parent = null;

	/**
	 * Holds children bodies of this body. Allows for 'attaching' bodies to each
	 * other
	 */
	public Set<BitBody> children = new HashSet<>();

	/**
	 * Simple flag to determine if a body is grounded. Specific to platformer
	 * style physics/movement
	 */
	public boolean grounded = false;

	/**
	 * Simple flag to distinguish which direction the body is facing. This does
	 * not necessarily correlate with what direction the body is <i>moving</i>.
	 */
	public Facing facing = Facing.RIGHT;

	/**
	 * Optional controller to control movement/behavior of the body
	 */
	public BitBodyController controller = null;

	/**
	 * Optional state manager to automatically handle what state an object is in
	 * relative to the physics world
	 */
	public BitBodyStateWatcher stateWatcher = null;

	/**
	 * Tracks the <b>last</b> movement the body <b>tried</b> to make. This does
	 * not mean the body did move.
	 */
	public BitPoint lastAttempt = new BitPoint(0, 0);

	/**
	 * Tracks what the physics world <b>last</b> did to resolve any collisions
	 * that may have happened
	 */
	public BitPoint lastResolution = new BitPoint(0, 0);

	/**
	 * Only active bodies are updated in the world
	 */
	public boolean active = true;

    /**
     * Determines how the body acts in the physics world
     */
    public BodyType bodyType = BodyType.STATIC;

    /**
     * The velocity of the body
     */
    public BitPoint velocity = new BitPoint(0, 0);

    /**
     * The acceleration of the body
     */
    public BitPoint acceleration = new BitPoint(0, 0);

    /**
     * The max speed of the body
     */
    public BitPoint maxSpeed = new BitPoint(0, 0);

    /**
     * A flag for whether or not gravity should affect this body
     */
    public boolean gravitational = true;

    public BitBody(){}

    /**
     * Copy constructor
     * @param other the other body to make a copy of
     */
    public BitBody(BitBody other) {
        this.aabb = new BitRectangle(other.aabb);
        this.parent = other.parent;
        this.children = new HashSet<>(other.children);
        this.grounded = other.grounded;
        this.facing = other.facing;
        this.controller = null; // this can't be easily copied from one body to another
        this.stateWatcher = null; // this can't be easily copied from one body to another
        this.lastAttempt = new BitPoint(other.lastAttempt);
        this.lastResolution = new BitPoint(other.lastResolution);
        this.active = other.active;
        this.bodyType = other.bodyType;
        this.velocity = new BitPoint(other.velocity);
        this.acceleration = new BitPoint(other.acceleration);
        this.maxSpeed = new BitPoint(other.maxSpeed);
        this.gravitational = other.gravitational;
    }

    public void set(String prop, Object value) throws BitBodySerializeException {
        try {
            this.getClass().getDeclaredField(prop).set(this, value);
        } catch (NoSuchFieldException nsfe){
            throw new BitBodySerializeException("There was no such field with the name: " + prop, nsfe);
        } catch (IllegalAccessException iae){
            throw new BitBodySerializeException("The property: " + prop + " is not marked as public", iae);
        }
    }
}
