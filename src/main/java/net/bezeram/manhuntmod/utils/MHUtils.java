package net.bezeram.manhuntmod.utils;

import static java.lang.Math.floor;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class MHUtils {
    public static double fractional(double x) {
        return x - floor(x);
    }
    public static float fractional(float x) {
        return x - (float)floor(x);
    }

    public static void displaySimpleClientMessage(final Player player, final String message,
                                                  ChatFormatting style, boolean actionBar) {
        player.displayClientMessage(Component.literal(message).withStyle(style), actionBar);
    }
}
