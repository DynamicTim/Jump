package com.bitdecay.jump.leveleditor.ui;

public enum OptionsMode {
    SELECT("Select", ModeType.MOUSE, 0),
    CREATE("Create", ModeType.MOUSE, 0),
    ANGLE("Angle", ModeType.MOUSE, 0),
    DELETE("Delete", ModeType.MOUSE, 0),

    SET_TEST_PLAYER("Set Test Player", ModeType.MOUSE, 1),
    SAVE_PLAYER("Save Player Props", ModeType.ACTION, 1),
    LOAD_PLAYER("Load Player Props", ModeType.ACTION, 1),

    SET_SPAWN("Set Spawn Point", ModeType.MOUSE, 2),
    SET_MAT_DIR("Set Material Directory", ModeType.ACTION, 2),

    MOVING_PLATFORM("Moving Platform", ModeType.MOUSE, 3),
    UP("Up", ModeType.ACTION, 2),
    DOWN("Down", ModeType.ACTION, 2),
    LEFT("Left", ModeType.ACTION, 2),
    RIGHT("Right", ModeType.ACTION, 2),

    CREATE_TEST_CASE("Create Test Case", ModeType.ACTION, 4),
    LOAD_TEST_CASE("Load Test Case", ModeType.ACTION, 4),

    SAVE_LEVEL("Save Level", ModeType.ACTION, -1),
    LOAD_LEVEL("Load Level", ModeType.ACTION, -1);

    public final String label;
    public final ModeType type;
    public final int group;

    private OptionsMode(String label, ModeType type, int group) {
        this.label = label;
        this.type = type;
        this.group = group;
    }
}
