package bitDecayJump.geom;

public class BitPoint {
	public float x;
	public float y;

	public BitPoint(float x, float y) {
		this.x = x;
		this.y = y;
	}

	/**
	 * adds point's values to this object
	 * 
	 * @param point
	 */
	public void add(BitPoint point) {
		x += point.x;
		y += point.y;
	}

	/**
	 * returns the total of this plus point
	 * 
	 * @param point
	 * @return result
	 */
	public BitPoint plus(BitPoint point) {
		return new BitPoint(x + point.x, y + point.y);
	}

	public BitPoint getScaled(float scale) {
		return new BitPoint(x * scale, y * scale);
	}
}
