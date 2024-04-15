package net.bezeram.manhuntmod.enums;

public enum DimensionID {
    OVERWORLD(0), NETHER(1), END(2), NULL(-1);

    DimensionID(int index) { this.index = index; }
    public final int index;
}
