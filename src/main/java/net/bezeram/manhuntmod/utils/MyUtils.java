package net.bezeram.manhuntmod.utils;

import static java.lang.Math.floor;

public class MyUtils {
    public static double fractional(double x) {
        return x - floor(x);
    }
    public static float fractional(float x) {
        return x - (float)floor(x);
    }
}
