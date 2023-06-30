package com.seibel.distanthorizons.common.wrappers.gui;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

#if PRE_MC_1_19_2
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
#endif

public class GuiHelper {
    /**
     * Helper static methods for versional compat
     */
    public static Button MakeBtn(Component base, int a, int b, int c, int d, Button.OnPress action) {
        #if PRE_MC_1_19_4
        return new Button(a, b, c, d, base, action);
        #else
        return Button.builder(base, action).bounds(a,b,c,d).build();
        #endif
    }

    public static MutableComponent TextOrLiteral(String text) {
        #if PRE_MC_1_19_2
        return new TextComponent(text);
        #else
        return Component.literal(text);
        #endif
    }

    public static MutableComponent TextOrTranslatable(String text) {
        #if PRE_MC_1_19_2
        return new TextComponent(text);
        #else
        return Component.translatable(text);
        #endif
    }

    public static MutableComponent Translatable(String text, Object... args) {
        #if PRE_MC_1_19_2
        return new TranslatableComponent(text, args);
        #else
        return Component.translatable(text, args);
        #endif
    }

    public static void SetX(AbstractWidget w, int x) {
        #if PRE_MC_1_19_4
        w.x = x;
        #else
        w.setX(x);
        #endif
    }

    public static void SetY(AbstractWidget w, int y) {
        #if PRE_MC_1_19_4
        w.y = y;
        #else
        w.setY(y);
        #endif
    }

}
