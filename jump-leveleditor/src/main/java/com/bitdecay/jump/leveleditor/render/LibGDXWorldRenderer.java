package com.bitdecay.jump.leveleditor.render;

import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.glutils.*;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.bitdecay.jump.BitBody;
import com.bitdecay.jump.BitWorld;
import com.bitdecay.jump.geom.BitPoint;
import com.bitdecay.jump.geom.BitRectangle;
import com.bitdecay.jump.geom.GeomUtils;
import com.bitdecay.jump.level.Direction;
import com.bitdecay.jump.level.TileBody;

public class LibGDXWorldRenderer implements BitWorldRenderer {
    private BitWorld world;
    private OrthographicCamera cam;
    private ShapeRenderer renderer;

    public LibGDXWorldRenderer(BitWorld world, OrthographicCamera cam) {
        this.world = world;
        this.cam = cam;
        renderer = new ShapeRenderer();
    }

    @Override
    public void render() {
        BitRectangle view = new BitRectangle(cam.position.x, cam.position.y, cam.viewportWidth * cam.zoom, cam.viewportHeight * cam.zoom);
        view.translate(-view.width / 2, -view.height / 2);
        renderer.setProjectionMatrix(cam.combined);
        renderer.begin(ShapeType.Line);
        renderer.setColor(Color.WHITE);
        BitBody[][] gridObjects = world.getGrid();
        for (int x = 0; x < gridObjects.length; x++) {
            BitBody[] column = gridObjects[x];
            for (int y = 0; y < column.length; y++) {
                if (column[y] != null) {
                    BitBody levelObject = column[y];
                    if (GeomUtils.intersection(view, levelObject.aabb) == null) {
                        // don't even attempt to draw if not on camera
                        continue;
                    }
                    if (levelObject.refined != null) {
                        BitPoint[] projectionPoints = levelObject.refined.getProjectionPoints();
                        float[] floats = new float[projectionPoints.length * 2];
                        for (int i = 0; i < projectionPoints.length; i++) {
                            floats[i*2] = projectionPoints[i].x;
                            floats[i*2+1] = projectionPoints[i].y;
                        }
                        renderer.polygon(floats);
                        continue;
                    }
                    float leftX = levelObject.aabb.xy.x;
                    float rightX = levelObject.aabb.xy.x + levelObject.aabb.width;
                    float bottomY = levelObject.aabb.xy.y;
                    float topY = levelObject.aabb.xy.y + levelObject.aabb.height;
                    int nValue = 0;
                    if (levelObject instanceof TileBody) {
                        nValue = ((TileBody) levelObject).nValue;
                    }
                    if ((nValue & Direction.UP) == 0) {
                        renderer.setColor(Color.WHITE);
                        renderer.line(leftX, topY, rightX, topY);
                    }
                    if ((nValue & Direction.DOWN) == 0) {
                        renderer.setColor(Color.WHITE);
                        renderer.line(leftX, bottomY, rightX, bottomY);
                    }
                    if ((nValue & Direction.LEFT) == 0) {
                        renderer.setColor(Color.WHITE);
                        renderer.line(leftX, bottomY, leftX, topY);
                    }
                    if ((nValue & Direction.RIGHT) == 0) {
                        renderer.setColor(Color.WHITE);
                        renderer.line(rightX, bottomY, rightX, topY);
                    }
                }
            }
        }
        for (BitBody body : world.getBodies()) {
            if (!body.active) {
                renderer.setColor(Color.GRAY);
            } else {
                switch (body.bodyType) {
                    case DYNAMIC:
                        renderer.setColor(Color.CYAN);
                        break;
                    case KINETIC:
                        renderer.setColor(Color.BLUE);
                        break;
                    case STATIC:
                        renderer.setColor(Color.TEAL);
                        break;
                    default:
                        renderer.setColor(Color.WHITE);
                        break;
                }
            }
            if (body.parent != null) {
                renderer.setColor(Color.MAGENTA);
            } else if (body.children.size() > 0) {
                renderer.setColor(Color.PURPLE);
            }
            renderer.rect(body.aabb.xy.x, body.aabb.xy.y, body.aabb.width, body.aabb.height);
            if (body.velocity.x != 0 || body.velocity.y != 0) {
                float x = body.aabb.xy.x + body.aabb.width / 2;
                float y = body.aabb.xy.y + body.aabb.height / 2;
                renderer.setColor(Color.PINK);
                renderer.line(x, y, x + body.velocity.x, y + body.velocity.y);
            }
        }

        renderer.setColor(Color.YELLOW);
        for (BitRectangle col : world.unresolvedCollisions) {
            renderer.rect(col.xy.x, col.xy.y, col.width, col.height);
        }
        renderer.setColor(Color.RED);
        for (BitRectangle col : world.resolvedCollisions) {
            renderer.rect(col.xy.x, col.xy.y, col.width, col.height);
        }
        renderer.end();
    }
}
