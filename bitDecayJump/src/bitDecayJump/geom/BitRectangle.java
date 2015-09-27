package bitDecayJump.geom;

public class BitRectangle implements Projectable{
	public BitPoint xy;
	public float width;
	public float height;

	public BitRectangle() {
		// here for JSON
	}

	public BitRectangle(BitRectangle other) {
		this(other.xy.x, other.xy.y, other.width, other.height);
	}

	public BitRectangle(float x, float y, float width, float height) {
		xy = new BitPoint(x, y);
		this.width = width;
		this.height = height;

		if (width < 0) {
			xy.x += width;
			this.width *= -1;
		}

		if (height < 0) {
			xy.y += height;
			this.height *= -1;
		}
	}

	public BitRectangle(BitPointInt startPoint, BitPointInt endPoint) {
		this(startPoint.x, startPoint.y, endPoint.x - startPoint.x, endPoint.y - startPoint.y);
	}

	public float getWidth() {
		return width;
	}

	public float getHeight() {
		return height;
	}

	public void translate(BitPoint point) {
		translate(point.x, point.y);
	}

	public void translate(float x, float y) {
		xy.x += x;
		xy.y += y;
	}

	public BitPoint center() {
		return new BitPoint(xy.x + width / 2, xy.y + height / 2);
	}

	public boolean contains(BitPointInt point) {
		return contains(point.x, point.y);
	}

	public boolean contains(int x, int y) {
		return x >= xy.x && x <= xy.x + width && y >= xy.y && y <= xy.y + height;
	}

	public boolean contains(BitRectangle other) {
		return other.xy.x >= this.xy.x && other.xy.y >= this.xy.y && other.xy.x + other.width <= this.xy.x + this.width
				&& other.xy.y + other.height <= this.xy.y + this.height;
	}

	public boolean containsInternal(BitPointInt point) {
		int x = point.x;
		int y = point.y;
		return x > xy.x && x < xy.x + width && y > xy.y && y < xy.y + height;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		BitRectangle other = (BitRectangle) obj;
		if (height != other.height) {
			return false;
		}
		if (width != other.width) {
			return false;
		}
		if (xy == null) {
			if (other.xy != null) {
				return false;
			}
		} else if (!xy.looseEquals(other.xy)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "(x: " + xy.x + ", y: " + xy.y + " - w: " + width + ", h: " + height + ")";
	}

	@Override
	public BitPoint[] getProjectionPoints() {
		return new BitPoint[] {xy,  xy.plus(width, 0), xy.plus(width, height), xy.plus(0, height)};
	}
}
