package dev.quarkos.emotes.config.ui;

import dev.quarkos.emotes.config.ConfigLayout.Box;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class Ui {
    public static final int SCRIM = 0xe6232323;
    public static final int GROUND = 0xff232323;
    public static final int WELL = 0xff272727;
    public static final int PANEL = 0xff2b2b2b;
    public static final int CONTROL = 0xff333333;
    public static final int RAISED = 0xff3a3a3a;
    public static final int BORDER = 0xff454545;
    public static final int HOVER = 0xff575757;
    public static final int TEXT = 0xffededed;
    public static final int MUTED = 0xffa0a0a0;
    public static final int ACCENT = 0xff4c9aff;
    public static final int SELECTED = 0xff30445c;

    private Ui() {}

    public static Component text(String key, Object... args) {
        return Component.translatable("text.quarkos_emotes." + key, args);
    }

    public static void fill(GuiGraphics g, Box box, int color) {
        if (box.w() > 0 && box.h() > 0) g.fill(box.x(), box.y(), box.right(), box.bottom(), color);
    }

    public static void softRect(GuiGraphics g, Box box, int color) {
        if (box.w() < 3 || box.h() < 3) {
            fill(g, box, color);
            return;
        }
        g.fill(box.x() + 1, box.y(), box.right() - 1, box.bottom(), color);
        g.fill(box.x(), box.y() + 1, box.x() + 1, box.bottom() - 1, color);
        g.fill(box.right() - 1, box.y() + 1, box.right(), box.bottom() - 1, color);
    }

    public static void panel(GuiGraphics g, Box box, int background, int border) {
        softRect(g, box, border);
        softRect(g, box.inset(1), background);
    }

    public static String trim(Font font, Component label, int width) {
        return TextFit.ellipsize(label.getString(), width, font::width);
    }

    public static void center(GuiGraphics g, Font font, Component label, Box box, int color) {
        String text = trim(font, label, box.w());
        g.drawString(font, text, box.x() + (box.w() - font.width(text)) / 2,
                box.y() + (box.h() - font.lineHeight) / 2, color, false);
    }

    public static void label(GuiGraphics g, Font font, Component label, Box box, int color) {
        g.drawString(font, trim(font, label, box.w()), box.x(), box.y() + (box.h() - font.lineHeight) / 2, color, false);
    }

    public static void heading(GuiGraphics g, Font font, Component label, Box box, float scale, boolean centered) {
        int maxWidth = (int) (box.w() / scale);
        String text = trim(font, label, maxWidth);
        float x = centered ? box.x() + (box.w() - font.width(text) * scale) / 2 : box.x();
        push(g, x, box.y() + (box.h() - font.lineHeight * scale) / 2, scale);
        g.drawString(font, text, 0, 0, TEXT, false);
        pop(g);
    }

    public static void push(GuiGraphics g, float x, float y, float scale) {
        var pose = g.pose();
        //? if >=1.21.11 {
        pose.pushMatrix();
        pose.translate(x, y);
        pose.scale(scale, scale);
        //?} else {
        /*pose.pushPose();
        pose.translate(x, y, 0);
        pose.scale(scale, scale, 1);
        *///?}
    }

    public static void pop(GuiGraphics g) {
        //? if >=1.21.11 {
        g.pose().popMatrix();
        //?} else {
        /*g.pose().popPose();
        *///?}
    }

    public static void layer(GuiGraphics g) {
        //? if >=1.21.11 {
        g.nextStratum();
        //?}
    }

    public static void clip(GuiGraphics g, Box box) {
        g.enableScissor(box.x(), box.y(), box.right(), box.bottom());
    }
}
