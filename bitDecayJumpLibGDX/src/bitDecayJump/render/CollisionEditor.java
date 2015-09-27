package bitDecayJump.render;

import bitDecayJump.*;
import bitDecayJump.geom.*;

import bitDecayJump.collision.SATCollisions;
import bitDecayJump.collision.SATResolution;
import com.badlogic.gdx.*;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.glutils.*;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Vector3;

public class CollisionEditor extends InputAdapter implements Screen {

    private static final int CAM_SPEED = 5;
    public static final double PI_OVER_TWO = Math.PI / 2;

    public BitmapFont font = new BitmapFont(Gdx.files.internal("fonts/test2.fnt"), Gdx.files.internal("fonts/test2.png"), false);

    private String jumpVersion = "Jump v" + BitWorld.VERSION;
    private String renderVersion = "Render v" + BitWorld.VERSION;
    private BitPoint jumpSize = new BitPoint(font.getBounds(jumpVersion).width, font.getBounds(jumpVersion).height);
    private BitPoint renderSize = new BitPoint(font.getBounds(renderVersion).width, font.getBounds(renderVersion).height);

    public SpriteBatch spriteBatch;
    public SpriteBatch uiBatch;

    public BitPointInt mouseDown;
    public BitPointInt mouseRelease;


    private OrthographicCamera camera;
    private ShapeRenderer shaper;

    BitSATRectangle r1;
    BitSATRectangle r2;

    BitTriangle t1;
    BitTriangle t2;

    public CollisionEditor() {
        spriteBatch = new SpriteBatch();
        uiBatch = new SpriteBatch();
        shaper = new ShapeRenderer();

        font.setColor(Color.YELLOW);

        camera = new OrthographicCamera(1600, 900);
        setCamToOrigin();

        r1 = new BitSATRectangle(-4.95f, 0, 5, 5);
        r2 = new BitSATRectangle(10, -15, 10, 10);
        t1 = new BitTriangle(20,-5, 20, 5);
        t2 = new BitTriangle(20,-5, -5, 20);
    }

    private void setCamToOrigin() {
        camera.position.set(0, 0, 0);
        camera.zoom = .1f;
        camera.update();
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        handleInput();

        camera.update();
        spriteBatch.setProjectionMatrix(camera.combined);
        shaper.setProjectionMatrix(camera.combined);

        drawGrid();
        debugRender();
        drawObjects();
        drawOrigin();

        spriteBatch.begin();
        shaper.begin(ShapeType.Line);
        spriteBatch.end();
        shaper.end();

        uiBatch.begin();
        renderHotkeys();
        renderMouseCoords();
        renderVersion();
        uiBatch.end();

        renderSpecial();
    }

    private void renderSpecial() {
    }

    private void debugRender() {
    }

    private void renderHotkeys() {
    }

    private void renderMouseCoords() {
        font.draw(uiBatch, getMouseCoords().toString(), 20, 20);
    }

    private void renderVersion() {
        font.draw(uiBatch, jumpVersion, Gdx.graphics.getWidth() - jumpSize.x, jumpSize.y);
        font.draw(uiBatch, renderVersion, Gdx.graphics.getWidth() - renderSize.x, jumpSize.y + renderSize.y);
    }

    private void drawObjects() {
        spriteBatch.begin();
        spriteBatch.setColor(1, 1, 1, .3f);
        spriteBatch.end();

        shaper.setColor(Color.GREEN);
        SATResolution reso = SATCollisions.getCollision(r1, t1);
        if (reso != null) {
            resolve(reso);
            shaper.setColor(Color.RED);
        }

        reso = SATCollisions.getCollision(r1, t2);
        if (reso != null) {
            resolve(reso);
            shaper.setColor(Color.RED);
        }

        reso = SATCollisions.getCollision(r1, r2);
        if (reso != null) {
            resolve(reso);
            shaper.setColor(Color.RED);
        }




        shaper.begin(ShapeType.Line);
        shaper.rect(r1.xy.x - r1.halfWidth, r1.xy.y - r1.halfHeight, r1.halfWidth * 2, r1.halfHeight * 2);
        shaper.rect(r2.xy.x - r2.halfWidth, r2.xy.y - r2.halfHeight, r2.halfWidth * 2, r2.halfHeight * 2);
        shaper.triangle(t1.rightAngle.x, t1.rightAngle.y, t1.rightAngle.x, t1.rightAngle.y + t1.height, t1.rightAngle.x + t1.width, t1.rightAngle.y);
        shaper.triangle(t2.rightAngle.x, t2.rightAngle.y, t2.rightAngle.x, t2.rightAngle.y + t2.height, t2.rightAngle.x + t2.width, t2.rightAngle.y);
        shaper.end();
    }

    private void resolve(SATResolution reso) {
        System.out.println();
        System.out.println("reso: " + reso);

        if (reso.axis.x != 0 && reso.axis.y > 0) {
            // this is logic to make it so the player doesn't move slower when running uphill. Likewise, we will need logic to 'glue' the player to the ground when running downhill.
            // atan is our angle of resolution
            double atan = Math.atan(reso.axis.y / reso.axis.x);
            System.out.println("atan: " + atan);

            if (Math.abs(atan - PI_OVER_TWO) <= Math.toRadians(30)) {
                // currently we impose a hard limit of 30 degree angle 'walkability'
                if (atan > 0) {
                    double angleToUpright;
                    angleToUpright = PI_OVER_TWO - atan;
                    System.out.println("Ang to Up: " + angleToUpright);
                    double straightUp = reso.distance / Math.cos(angleToUpright);
                    r1.xy.add(0, (float) straightUp);
                } else {
                    double angleToUpright;
                    angleToUpright = -PI_OVER_TWO - atan;
                    System.out.println("Ang to Up: " + angleToUpright);
                    double straightUp = reso.distance / Math.cos(angleToUpright);
                    r1.xy.add(0, (float) straightUp);
                }
                return;
            }
        }

        r1.xy.add(reso.axis.x * reso.distance, reso.axis.y * reso.distance);

    }

    private void drawGrid() {
        shaper.begin(ShapeType.Line);
        shaper.setColor(.1f, .1f, .1f, 1f);
        Vector3 topLeft = camera.unproject(new Vector3(-10, -10, 0));
        BitPointInt snapTopLeft = GeomUtils.snap(new BitPointInt((int) topLeft.x, (int) topLeft.y), 10);
        Vector3 bottomRight = camera.unproject(new Vector3(Gdx.graphics.getWidth(), Gdx.graphics.getHeight() + 2 * 10, 0));
        BitPointInt snapBottomRight = GeomUtils.snap(new BitPointInt((int) bottomRight.x, (int) bottomRight.y), 10);

        for (float x = snapTopLeft.x; x <= snapBottomRight.x; x += 10) {
            shaper.line(x, snapTopLeft.y, x, snapBottomRight.y);
        }
        for (float y = snapBottomRight.y; y <= snapTopLeft.y; y += 10) {
            shaper.line(snapTopLeft.x, y, snapBottomRight.x, y);
        }
        shaper.end();
    }

    private void drawOrigin() {
        shaper.setColor(Color.MAROON);
        shaper.begin(ShapeType.Filled);
        shaper.circle(0, 0, 2);
        shaper.end();
    }

    public BitPointInt unproject(int x, int y) {
        Vector3 unproj = camera.unproject(new Vector3(x, y, 0));
        return new BitPointInt((int) unproj.x, (int) unproj.y);
    }

    private void handleInput() {
        if (Gdx.input.isKeyPressed(Keys.A)) {
            r1.xy.x -= .1f;
        } else if (Gdx.input.isKeyPressed(Keys.D)) {
            r1.xy.x += .1f;
        }
        if (Gdx.input.isKeyPressed(Keys.W)) {
            r1.xy.y += 5f;
        } else if (Gdx.input.isKeyPressed(Keys.S)) {
            r1.xy.y -= 5f;
        }

        if (Gdx.input.isKeyPressed(Keys.LEFT)) {
            camera.translate(-CAM_SPEED * camera.zoom, 0);
        } else if (Gdx.input.isKeyPressed(Keys.RIGHT)) {
            camera.translate(CAM_SPEED * camera.zoom, 0);
        }
        if (Gdx.input.isKeyPressed(Keys.UP)) {
            camera.translate(0, CAM_SPEED * camera.zoom);
        } else if (Gdx.input.isKeyPressed(Keys.DOWN)) {
            camera.translate(0, -CAM_SPEED * camera.zoom);
        }

        if (Gdx.input.isKeyPressed(Keys.NUM_2)) {
            if (camera.zoom > 5) {
                camera.zoom -= .2f;
            } else if (camera.zoom > .2) {
                camera.zoom -= .05f;
            }
        } else if (Gdx.input.isKeyPressed(Keys.NUM_1)) {
            if (camera.zoom < 5) {
                camera.zoom += .05f;
            } else if (camera.zoom < 20) {
                camera.zoom += .2f;
            }
        }

        if (Gdx.input.isKeyPressed(Keys.ESCAPE)) {
            Gdx.app.exit();
        }
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        return super.touchDown(screenX, screenY, pointer, button);
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        return super.touchUp(screenX, screenY, pointer, button);
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        return super.mouseMoved(screenX, screenY);
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return super.mouseMoved(screenX, screenY);
    }

    private BitPointInt getMouseCoords() {
        return unproject(Gdx.input.getX(), Gdx.input.getY());
    }

    @Override
    public void dispose() {
    }

    @Override
    public void hide() {
    }

    @Override
    public void pause() {
    }

    @Override
    public void resize(int arg0, int arg1) {
    }

    @Override
    public void resume() {
    }

    @Override
    public void show() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        Gdx.input.setInputProcessor(this);
    }
}
