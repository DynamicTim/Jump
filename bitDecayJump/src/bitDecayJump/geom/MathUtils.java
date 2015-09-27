package bitDecayJump.geom;

public class MathUtils {
	public static final float FLOAT_PRECISION = .0001f;
	public static final float COLLISION_PRECISION = .02f;
	public static final float PI_OVER_TWO = (float) (Math.PI / 2);

	public static BitPoint normalize(BitPoint xy) {
		return xy.dividedBy((float) Math.sqrt(Math.pow(xy.x, 2) + Math.pow(xy.y, 2)));
	}

	public static boolean close(float f1, float f2) {
		return within(f1, f2, FLOAT_PRECISION);
	}

	public static boolean within(float f1, float f2, float precision) {
		return Math.abs(f1 - f2) <= precision;
	}
}
