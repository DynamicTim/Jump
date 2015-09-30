package com.bitdecay.jump.level;

import com.bitdecay.jump.BitBody;
import com.bitdecay.jump.BodyType;
import com.bitdecay.jump.geom.BitRectangle;
import com.bitdecay.jump.geom.BitTriangle;

public class TileSlopeObject extends TileObject {
	public TileSlopeObject() {
		// TODO Here for JSON
	}

	public TileSlopeObject(BitRectangle rect) {
		super(rect);
	}

	@Override
	public BitBody getBody() {
		BitBody body = super.getBody();
		body.refined = new BitTriangle(rect.xy.x, rect.xy.y, rect.width, rect.height);
		return body;
	}

	/*
	NOTES:

	We need a way to specify NON- rectangular shapes for bodies now. Need
	generic polygon (though we will probably only use squares and triangles
	for the foreseeable future)

	Currently ALL bodies are rectangles courtesy of  the aabb property on
	BitBody. We should genericize this so we can have some 'Shape'.
	Perhaps have a standard 'aabb' but also have an additional 'more
	specific' shape that can be used if it exists.
	 */
}
