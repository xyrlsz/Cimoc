package com.xyrlsz.xcimocob.utils;

import java.util.Random;

public class RandomUtils {
    public static int randomInt(int min, int max) {
        return new Random().nextInt(max - min + 1) + min;
    }

}
