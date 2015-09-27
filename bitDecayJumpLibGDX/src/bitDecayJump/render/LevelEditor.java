package bitDecayJump.render;

import java.util.*;

import javax.swing.JDialog;

import bitDecayJump.*;
import bitDecayJump.geom.*;
import bitDecayJump.input.PlayerInputHandler;
import bitDecayJump.level.*;
import bitDecayJump.render.mouse.*;
import bitDecayJump.setup.*;
import bitDecayJump.ui.*;

import com.badlogic.gdx.*;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.glutils.*;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Vector3;

public class LevelEditor extends InputAdapter implements Screen, OptionsUICallback, PropModUICallback {

	public static final String EDITOR_ASSETS_FOLDER = "editorAssets";

	private static final int CAM_SPEED = 5;

	public BitmapFont font = new BitmapFont(Gdx.files.internal("fonts/test2.fnt"), Gdx.files.internal("fonts/test2.png"), false);

	private String jumpVersion = "Jump v" + BitWorld.VERSION;
	private String renderVersion = "Render v" + BitWorld.VERSION;
	private BitPoint jumpSize = new BitPoint(font.getBounds(jumpVersion).width, font.getBounds(jumpVersion).height);
	private BitPoint renderSize = new BitPoint(font.getBounds(renderVersion).width, font.getBounds(renderVersion).height);

	public SpriteBatch spriteBatch;
	public SpriteBatch uiBatch;

	public BitPointInt mouseDown;
	public BitPointInt mouseRelease;

	public LevelBuilder curLevelBuilder;
	public String savedLevel;

	private OrthographicCamera camera;
	private ShapeRenderer shaper;
	private Map<OptionsMode, MouseMode> mouseModes;
	private MouseMode mouseMode;

	private Map<Integer, JDialog> uiKeys;
	private PropModUI propUI;

	private JumperProps playerProps;

	private Map<String, TextureRegion[]> materialMap;
	private TextureRegion[] fallbackTiles;
	private TextureRegion fullSet;

	private BitWorld world;
	private LibGDXWorldRenderer worldRenderer;

	private LevelBuilderListener levelListener = new LevelBuilderListener() {

		@Override
		public void levelChanged(Level level) {
			world.setLevel(level);
			BitBody player = maybeGetPlayer();
			if (player != null) {
				world.addBody(player);
			}
		}

		@Override
		public void updateGrid(BitPointInt gridOffset, TileObject[][] grid, Collection<LevelObject> otherObjects) {
			world.setGridOffset(gridOffset);
			world.setGrid(grid);
			BitBody player = maybeGetPlayer();
			world.setObjects(buildBodies(otherObjects));
			if (player != null) {
				world.addBody(player);
			}
		}
	};

	private PlayerInputHandler playerController;

	private EditorToolbox toolBox = new EditorToolbox();

	// a flags to control whether we are moving the world forward or not
	private boolean stepWorld = true;
	private boolean singleStep = false;

	public LevelEditor() {
		spriteBatch = new SpriteBatch();
		uiBatch = new SpriteBatch();
		shaper = new ShapeRenderer();

		font.setColor(Color.YELLOW);

		camera = new OrthographicCamera(1600, 900);
		setCamToOrigin();

		curLevelBuilder = new LevelBuilder(16);
		curLevelBuilder.addListener(levelListener);

		world = new BitWorld();
		world.setGravity(0, -900);

		world.setGridOffset(curLevelBuilder.gridOffset);
		world.setTileSize(curLevelBuilder.tileSize);
		world.setGrid(curLevelBuilder.grid);

		worldRenderer = new LibGDXWorldRenderer(world, camera);

		playerProps = new JumperProps();
		playerProps.bodyType = BodyType.DYNAMIC;

		mouseModes = new HashMap<OptionsMode, MouseMode>();
		mouseModes.put(OptionsMode.SELECT, new SelectMouseMode(curLevelBuilder));
		mouseModes.put(OptionsMode.CREATE, new CreateMouseMode(curLevelBuilder));
		mouseModes.put(OptionsMode.MOVING_PLATFORM, new MovingPlatformMouseMode(curLevelBuilder));
		mouseModes.put(OptionsMode.DELETE, new DeleteMouseMode(curLevelBuilder));
		mouseModes.put(OptionsMode.SET_SPAWN, new SpawnMouseMode(curLevelBuilder));

		playerController = new PlayerInputHandler();
		mouseModes.put(OptionsMode.SET_TEST_PLAYER, new SetPlayerMouseMode(curLevelBuilder, world, playerController, playerProps));
		mouseMode = mouseModes.get(OptionsMode.SELECT);

		uiKeys = new HashMap<Integer, JDialog>();

		JDialog buttonsDialog = new OptionsUI(this, toolBox);
		buttonsDialog.setTitle("Tools");

		uiKeys.put(Keys.T, buttonsDialog);

		JDialog playerTweakDialog = new PropModUI(this, playerProps);
		playerTweakDialog.setTitle("Player Props");

		uiKeys.put(Keys.P, playerTweakDialog);

		materialMap = new HashMap<String, TextureRegion[]>();

		//		FileHandle editorAssets = Gdx.files.internal(EDITOR_ASSETS_FOLDER);
		//		if (!editorAssets.exists()) {
		//			throw new RuntimeException("/" + EDITOR_ASSETS_FOLDER
		//					+ " directory not found in assets folder. Please copy this out of the bitDecayJumpLibGDX project into your assets folder");
		//		}
		fullSet = new TextureRegion(new Texture(Gdx.files.internal(EDITOR_ASSETS_FOLDER + "/fallbacktileset.png")));

		fallbackTiles = fullSet.split(16, 16)[0];
	}

	private Collection<BitBody> buildBodies(Collection<LevelObject> otherObjects) {
		ArrayList<BitBody> bodies = new ArrayList<BitBody>();
		for (LevelObject levelObject : otherObjects) {
			bodies.add(levelObject.getBody());
		}
		return bodies;
	}

	private void setCamToOrigin() {
		camera.position.set(camera.viewportWidth / 2f, camera.viewportHeight / 2f, 0);
		camera.update();
	}

	@Override
	public void render(float delta) {
		Gdx.gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		handleInput();
		playerController.update();

		camera.update();
		spriteBatch.setProjectionMatrix(camera.combined);
		shaper.setProjectionMatrix(camera.combined);

		drawGrid();
		debugRender();
		if (stepWorld || singleStep) {
			singleStep = false;
			world.step(delta);
		} else {
			world.nonStep(delta);
		}
		worldRenderer.render();
		drawLevelEdit();
		drawOrigin();

		spriteBatch.begin();
		shaper.begin(ShapeType.Line);
		mouseMode.render(shaper, spriteBatch);
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
		shaper.setProjectionMatrix(uiBatch.getProjectionMatrix());
		shaper.setColor(Color.WHITE);
		if (stepWorld) {
			shaper.begin(ShapeType.Line);
			shaper.polygon(new float[] { 20, 20, 70, 45, 20, 70, 20, 20 });
		} else {
			shaper.begin(ShapeType.Filled);
			shaper.rect(20, 20, 20, 50);
			shaper.rect(50, 20, 20, 50);
		}
		shaper.end();
	}

	private void debugRender() {
		shaper.begin(ShapeType.Line);
		shaper.setColor(Color.OLIVE);
		shaper.rect(curLevelBuilder.gridOffset.x * curLevelBuilder.tileSize, curLevelBuilder.gridOffset.y * curLevelBuilder.tileSize,
				curLevelBuilder.grid.length * curLevelBuilder.tileSize, curLevelBuilder.grid[0].length * curLevelBuilder.tileSize);
		shaper.end();
	}

	private void renderHotkeys() {
		int menuIndex = 0;
		int spacing = 200;
		int row = 0;
		int rowSpacing = 200;
		uiBatch.setColor(Color.BLUE);
		for (Integer menuKey : uiKeys.keySet()) {
			JDialog menu = uiKeys.get(menuKey);
			int x = menuIndex++ * spacing;
			if (x > camera.viewportWidth) {
				menuIndex = 0;
				row++;
			}
			font.draw(uiBatch, "(" + Keys.toString(menuKey) + ") " + menu.getTitle(), x, Gdx.graphics.getHeight() - row * rowSpacing);
		}
	}

	private void renderMouseCoords() {
		font.draw(uiBatch, getMouseCoords().toString(), 20, 20);
	}

	private void renderVersion() {
		font.draw(uiBatch, jumpVersion, Gdx.graphics.getWidth() - jumpSize.x, jumpSize.y);
		font.draw(uiBatch, renderVersion, Gdx.graphics.getWidth() - renderSize.x, jumpSize.y + renderSize.y);
	}

	private void drawLevelEdit() {
		spriteBatch.begin();
		spriteBatch.setColor(1, 1, 1, .3f);
		for (TileObject obj : curLevelBuilder.tileObjects) {
			spriteBatch.draw(getMaterial(obj.material)[obj.nValue], obj.rect.xy.x, obj.rect.xy.y, obj.rect.width, obj.rect.height);
		}
		spriteBatch.end();

		shaper.begin(ShapeType.Line);
		shaper.setColor(Color.GREEN);
		for (LevelObject obj : curLevelBuilder.selection) {
			shaper.rect(obj.rect.xy.x, obj.rect.xy.y, obj.rect.width, obj.rect.height);
		}
		shaper.end();
	}

	private TextureRegion[] getMaterial(int material) {
		//		String matLoc = curLevelBuilder.level.materials.get(material);
		//		if (matLoc != null && !matLoc.isEmpty()) {
		//			TextureRegion[] materialImages = materialMap.get(curLevelBuilder.level.baseMaterialDir + matLoc);
		//			if (materialImages != null) {
		//				return materialImages;
		//			}
		//		}
		return fallbackTiles;
	}

	private void drawGrid() {
		shaper.begin(ShapeType.Line);
		shaper.setColor(.1f, .1f, .1f, 1f);
		Vector3 topLeft = camera.unproject(new Vector3(-curLevelBuilder.tileSize, -curLevelBuilder.tileSize, 0));
		BitPointInt snapTopLeft = GeomUtils.snap(new BitPointInt((int) topLeft.x, (int) topLeft.y), curLevelBuilder.tileSize);
		Vector3 bottomRight = camera.unproject(new Vector3(Gdx.graphics.getWidth(), Gdx.graphics.getHeight() + 2 * curLevelBuilder.tileSize, 0));
		BitPointInt snapBottomRight = GeomUtils.snap(new BitPointInt((int) bottomRight.x, (int) bottomRight.y), curLevelBuilder.tileSize);

		for (float x = snapTopLeft.x; x <= snapBottomRight.x; x += curLevelBuilder.tileSize) {
			shaper.line(x, snapTopLeft.y, x, snapBottomRight.y);
		}
		for (float y = snapBottomRight.y; y <= snapTopLeft.y; y += curLevelBuilder.tileSize) {
			shaper.line(snapTopLeft.x, y, snapBottomRight.x, y);
		}
		shaper.end();
	}

	private void drawOrigin() {
		shaper.setColor(Color.MAROON);
		shaper.begin(ShapeType.Filled);
		shaper.circle(0, 0, camera.zoom * (curLevelBuilder.tileSize / 3));
		shaper.end();
	}

	public BitPointInt unproject(int x, int y) {
		Vector3 unproj = camera.unproject(new Vector3(x, y, 0));
		return new BitPointInt((int) unproj.x, (int) unproj.y);
	}

	private void handleInput() {
		if (Gdx.input.isKeyJustPressed(Keys.GRAVE)) {
			stepWorld = !stepWorld;
		}
		if (!stepWorld && Gdx.input.isKeyJustPressed(Keys.PLUS)) {
			singleStep = true;
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
			} else if (camera.zoom > .001f) {
				camera.zoom -= .001;
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

		if (Gdx.input.isKeyPressed(Keys.DEL) || Gdx.input.isKeyPressed(Keys.BACKSPACE)) {
			curLevelBuilder.deleteSelected();
		}

		for (Integer uiKey : uiKeys.keySet()) {
			if (Gdx.input.isKeyPressed(uiKey)) {
				JDialog dialog = uiKeys.get(uiKey);
				if (!dialog.isShowing()) {
					dialog.setVisible(true);
				}
			}
		}
	}

	private BitBody maybeGetPlayer() {
		return ((SetPlayerMouseMode) mouseModes.get(OptionsMode.SET_TEST_PLAYER)).lastPlayer;
	}

	@Override
	public boolean touchDown(int screenX, int screenY, int pointer, int button) {
		mouseMode.mouseDown(getMouseCoords());
		return super.touchDown(screenX, screenY, pointer, button);
	}

	@Override
	public boolean touchUp(int screenX, int screenY, int pointer, int button) {
		// mouseRelease = GeomUtils.snap(getMouseCoords(), curLevelBuilder.level.tileSize);
		mouseMode.mouseUp(getMouseCoords());
		// createObject();
		// mouseDown = null;
		// mouseRelease = null;
		return super.touchUp(screenX, screenY, pointer, button);
	}

	@Override
	public boolean touchDragged(int screenX, int screenY, int pointer) {
		mouseMode.mouseDragged(getMouseCoords());
		return super.mouseMoved(screenX, screenY);
	}

	@Override
	public boolean mouseMoved(int screenX, int screenY) {
		mouseMode.mouseMoved(getMouseCoords());
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

	@Override
	public void setMode(OptionsMode mode) {
		if (mouseModes.containsKey(mode)) {
			mouseMode = mouseModes.get(mode);
		} else if (OptionsMode.UP.equals(mode)) {
			MovingPlatformMouseMode.direction = Direction.UP;
		} else if (OptionsMode.DOWN.equals(mode)) {
			MovingPlatformMouseMode.direction = Direction.DOWN;
		} else if (OptionsMode.LEFT.equals(mode)) {
			MovingPlatformMouseMode.direction = Direction.LEFT;
		} else if (OptionsMode.RIGHT.equals(mode)) {
			MovingPlatformMouseMode.direction = Direction.RIGHT;
		} else if (OptionsMode.SET_MAT_DIR.equals(mode)) {
			// set base directory. Allow textures to be loaded from it.
		} else if (OptionsMode.SAVE_PLAYER.equals(mode)) {
			saveProps();
		} else if (OptionsMode.LOAD_PLAYER.equals(mode)) {
			loadProps();
		} else if (OptionsMode.SAVE_LEVEL.equals(mode)) {
			setLevelBuilder(LevelUtilities.saveLevel(curLevelBuilder));
		} else if (OptionsMode.LOAD_LEVEL.equals(mode)) {
			Level loadLevel = LevelUtilities.loadLevel();
			if (loadLevel != null) {
				setLevelBuilder(loadLevel);
				setCamToOrigin();
			}
		}
	}

	private void saveProps() {
		BitBody player = maybeGetPlayer();
		if (player != null) {
			FileUtils.saveToFile(player.props);
		}
	}

	private void loadProps() {
		BitBody player = maybeGetPlayer();
		if (player != null) {
			player.props = FileUtils.loadFileAs(JumperProps.class);
			propUI.setProperties(player.props);
		}
	}

	private void setLevelBuilder(Level level) {
		if (level == null) {
			curLevelBuilder.newLevel(16);
		} else {
			curLevelBuilder.setLevel(level);
		}
	}

	@Override
	public void propertyChanged(String prop, Object value) {
		System.out.println(prop + " = " + value);
		BitBody player = maybeGetPlayer();
		if (player != null) {
			try {
				player.props.set(prop, value);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
