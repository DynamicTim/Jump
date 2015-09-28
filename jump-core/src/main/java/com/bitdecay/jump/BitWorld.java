package com.bitdecay.jump;

import com.bitdecay.jump.collision.SATStrategy;
import com.bitdecay.jump.geom.*;
import com.bitdecay.jump.level.Level;
import com.bitdecay.jump.level.TileObject;
import com.bitdecay.jump.listener.BitCollisionListener;

import java.util.*;

/**
 * A Pseudo-Physics simulation world. Will step according to all body's
 * properties, but properties are publicly accessible to allow for total
 * control.
 * 
 * @author Monday
 *
 */
public class BitWorld {
	public static final String VERSION = "0.1.2";
	public static final float STEP_SIZE = 1 / 120f;
	/**
	 * Holds left-over time when there isn't enough time for a full
	 * {@link #STEP_SIZE}
	 */
	private float extraStepTime = 0;

	private int tileSize = 32;
	private BitPointInt gridOffset = new BitPointInt(0, 0);
	private BitBody[][] gridObjects = new BitBody[0][0];
	private List<BitBody> bodies = new ArrayList<>();

	/**
	 * A map of x to y to an occupying body.
	 */
	private Map<Integer, Map<Integer, Set<BitBody>>> occupiedSpaces;
	private Map<BitBody, BitResolution> pendingResolutions;

	public static BitPoint gravity = new BitPoint(0, 0);
	public static BitPoint maxSpeed = new BitPoint(2000, 2000);

	private List<BitBody> pendingAdds;
	private List<BitBody> pendingRemoves;
	public final List<BitRectangle> resolvedCollisions = new ArrayList<BitRectangle>();
	public final List<BitRectangle> unresolvedCollisions = new ArrayList<BitRectangle>();

    private List<BitBody> aGroupCollisions = new ArrayList<>();
    private List<BitBody> bGroupCollisions = new ArrayList<>();
    private List<BitCollisionListener> listeners = new ArrayList<>();

	public static final BitBody LEVEL_BODY = new BitBody();
	static {
		LEVEL_BODY.bodyType = BodyType.STATIC;
	}

	public BitWorld() {
		bodies = new ArrayList<BitBody>();
		pendingAdds = new ArrayList<BitBody>();
		pendingRemoves = new ArrayList<BitBody>();
		occupiedSpaces = new HashMap<Integer, Map<Integer, Set<BitBody>>>();
		pendingResolutions = new HashMap<BitBody, BitResolution>();
	}

	public BitPoint getGravity() {
		return gravity;
	}

	public void setGravity(float x, float y) {
		this.gravity.x = x;
		this.gravity.y = y;
	}

	public void addBody(BitBody body) {
		pendingAdds.add(body);
	}

	public void removeBody(BitBody body) {
		pendingRemoves.add(body);
	}

	/**
	 * steps the physics world in {@link BitWorld#STEP_SIZE} time steps. Any
	 * left over will be rolled over in to the next call to this method.
	 * 
	 * @param delta
	 * @return true if the world stepped, false otherwise
	 */
	public boolean step(float delta) {
		//		delta *= .05f;
		boolean stepped = false;
		//add any left over time from last call to step();
		delta += extraStepTime;
		while (delta > STEP_SIZE) {
			stepped = true;
			resolvedCollisions.clear();
			unresolvedCollisions.clear();
			internalStep(STEP_SIZE);
			delta -= STEP_SIZE;
		}
		// store off our leftover so it can be added in next time
		extraStepTime = delta;
		return stepped;
	}

	public void nonStep(float delta) {
		doAddRemoves();
	}

	private void internalStep(final float delta) {
		if (delta <= 0) {
			return;
		}
		// make sure world contains everything it should
		doAddRemoves();

		occupiedSpaces.clear();

		// first, move everything
		bodies.parallelStream().forEach(body -> {
			if (body.active) {
				// apply gravity to DYNAMIC bodies
				if (BodyType.DYNAMIC == body.bodyType) {
					if (body.gravitational) {
						body.velocity.add(gravity.scale(delta));
					}
				}
				// then let controller handle the body
				if (body.controller != null) {
					body.controller.update(delta, body);
				}

				// cap body at max speed
				body.velocity.x = Math.min(Math.abs(body.velocity.x), maxSpeed.x) * (body.velocity.x < 0 ? -1 : 1);
				body.velocity.y = Math.min(Math.abs(body.velocity.y), maxSpeed.y) * (body.velocity.y < 0 ? -1 : 1);

				// then move all of our non-static bodies
				if (BodyType.STATIC != body.bodyType) {
					body.lastAttempt = body.velocity.scale(delta);
					body.aabb.translate(body.lastAttempt);
					if (BodyType.KINETIC == body.bodyType) {
						for (BitBody child : body.children) {
							/*
							 * we make sure to move the child just slightly less
							 * than the parent to guarantee that it still
							 * collides if nothing else influences it's motion
							 */
							BitPoint influence = body.lastAttempt.shrink(MathUtils.FLOAT_PRECISION);
							child.aabb.translate(influence);
							// the child did attempt to move this additional amount according to our engine
							child.lastAttempt.add(influence);
						}
						body.children.clear();
					}
				}
				// all bodies are assumed to be not grounded unless a collision happens this step.
				body.grounded = false;
				// all bodies assumed to be independent unless a lineage collision happens this step.
				body.parent = null;
			}
		});

		// resolve collisions for DYNAMIC bodies against Level bodies-
		bodies.stream().filter(body -> body.active && BodyType.DYNAMIC == body.bodyType).forEach(body -> buildLevelCollisions(body));
		//		applyPendingResolutions();
		bodies.stream().filter(body -> body.active && BodyType.KINETIC == body.bodyType).forEach(body -> buildKineticCollections(body));
		resolveAndApplyPendingResolutions();

		bodies.parallelStream().filter(body -> body.active && body.stateWatcher != null).forEach(body -> body.stateWatcher.update(body));
	}

	private void doAddRemoves() {
		bodies.removeAll(pendingRemoves);
		pendingRemoves.clear();

		bodies.addAll(pendingAdds);
		pendingAdds.clear();
	}

	private void resolveAndApplyPendingResolutions() {
		for (BitBody body : pendingResolutions.keySet()) {
			pendingResolutions.get(body).resolve(this);
			applyResolution(body, pendingResolutions.get(body));
		}
		pendingResolutions.clear();


        // notify listeners after resolution is reached for all the bodies
        for (int i = 0; i < aGroupCollisions.size(); i++) {
            final int index = i;
            listeners.stream().forEach(listener -> listener.collision(aGroupCollisions.get(index), bGroupCollisions.get(index)));
        }
        aGroupCollisions.clear();
        bGroupCollisions.clear();
	}

	private void buildKineticCollections(BitBody kineticBody) {
		// 1. determine tile that x,y lives in
		BitPoint startCell = kineticBody.aabb.xy.floorDivideBy(tileSize, tileSize).minus(gridOffset);

		// 2. determine width/height in tiles
		int endX = (int) (startCell.x + Math.ceil(1.0 * kineticBody.aabb.width / tileSize));
		int endY = (int) (startCell.y + Math.ceil(1.0 * kineticBody.aabb.height / tileSize));

		for (int x = (int) startCell.x; x <= endX; x++) {
			if (!occupiedSpaces.containsKey(x)) {
				continue;
			}
			for (int y = (int) startCell.y; y <= endY; y++) {
				if (!occupiedSpaces.get(x).containsKey(y)) {
					continue;
				}
				for (BitBody otherBody : occupiedSpaces.get(x).get(y)) {
					checkForNewCollision(otherBody, kineticBody);
				}
			}
		}
	}

	private void buildLevelCollisions(BitBody body) {
		// 1. determine tile that x,y lives in
		BitPoint startCell = body.aabb.xy.floorDivideBy(tileSize, tileSize).minus(gridOffset);

		// 2. determine width/height in tiles
		int endX = (int) (startCell.x + Math.ceil(1.0 * body.aabb.width / tileSize));
		int endY = (int) (startCell.y + Math.ceil(1.0 * body.aabb.height / tileSize));

		// 3. loop over those all occupied tiles
		for (int x = (int) startCell.x; x <= endX; x++) {
			if (!occupiedSpaces.containsKey(x)) {
				occupiedSpaces.put(x, new HashMap<Integer, Set<BitBody>>());
			}
			for (int y = (int) startCell.y; y <= endY; y++) {
				if (!occupiedSpaces.get(x).containsKey(y)) {
					occupiedSpaces.get(x).put(y, new HashSet<BitBody>());
				}
				// mark the body as occupying the current grid coordinate
				occupiedSpaces.get(x).get(y).add(body);
				// ensure valid cell
				if (ArrayUtilities.onGrid(gridObjects, x, y) && gridObjects[x][y] != null) {
					BitBody checkObj = gridObjects[x][y];
					checkForNewCollision(body, checkObj);
				}
			}
		}
	}

	private void applyResolution(BitBody body, BitResolution resolution) {
		if (resolution.resolution.x != 0 || resolution.resolution.y != 0) {
			body.aabb.translate(resolution.resolution);
			// CONSIDER: have grounded check based on gravity direction rather than just always assuming down
			if (Math.abs(gravity.y - resolution.resolution.y) > Math.abs(gravity.y)) {
				// if the body was resolved against the gravity's y, we assume grounded.
				// CONSIDER: 4-directional gravity might become a possibility.
				body.grounded = true;
			}
		}
		if (resolution.haltX) {
			body.velocity.x = 0;
		}
		if (resolution.haltY) {
			body.velocity.y = 0;
		}

		body.lastResolution = resolution.resolution;
	}

	/**
	 * A simple method that sees if there is a collision and adds it to the
	 * {@link BitResolution} as something that needs to be handled at the time
	 * of resolution.
	 * 
	 * @param body
	 * @param against
	 */
	private void checkForNewCollision(BitBody body, BitBody against) {
		BitRectangle insec = GeomUtils.intersection(body.aabb, against.aabb);
		if (insec != null) {
			if (!pendingResolutions.containsKey(body)) {
				pendingResolutions.put(body, new SATStrategy(body));
			}
			BitResolution resolution = pendingResolutions.get(body);
			//TODO: This can definitely be made more efficient via a hash map or something of the like
			for (BitCollision collision : resolution.collisions) {
				if (collision.otherBody == against) {
					return;
				}
			}
			resolution.collisions.add(new BitCollision(insec, against));
            aGroupCollisions.add(body);
            aGroupCollisions.add(against);
		}
	}

	public List<BitBody> getBodies() {
		return Collections.unmodifiableList(bodies);
	}

	public void setTileSize(int tileSize) {
		this.tileSize = tileSize;
	}

	public void setGridOffset(BitPointInt bodyOffset) {
		this.gridOffset = bodyOffset;
	}

	public void setLevel(Level level) {
		tileSize = level.tileSize;
		gridOffset = level.gridOffset;
		parseGrid(level.gridObjects);
	}

	public void setGrid(TileObject[][] grid) {
		parseGrid(grid);
	}

	private void parseGrid(TileObject[][] grid) {
		gridObjects = new BitBody[grid.length][grid[0].length];
		for (int x = 0; x < grid.length; x++) {
			for (int y = 0; y < grid[0].length; y++) {
				if (grid[x][y] != null) {
					gridObjects[x][y] = grid[x][y].getBody();
				}
			}
		}
	}

	public BitBody[][] getGrid() {
		return gridObjects;
	}

	public int getTileSize() {
		return tileSize;
	}

	public BitPointInt getBodyOffset() {
		return gridOffset;
	}

	public void setObjects(Collection<BitBody> otherObjects) {
		pendingRemoves.addAll(bodies);
		pendingAdds.addAll(otherObjects);
	}

	public void removeAllBodies() {
		pendingRemoves.addAll(bodies);
		pendingAdds.clear();
	}

    public void addCollisionListener(BitCollisionListener listener){
        listeners.add(listener);
    }

    public void removeCollisionListener(BitCollisionListener listener){
        listeners.remove(listener);
    }
}
