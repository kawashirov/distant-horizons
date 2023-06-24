package com.seibel.distanthorizons.common.wrappers.gui;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class DhScreen extends Screen {

    protected DhScreen(Component $$0) {
        super($$0);
    }

    // addRenderableWidget in 1.17 and over
    // addButton in 1.16 and below
    protected void addBtn(Button button) {
		#if PRE_MC_1_17_1
        this.addButton(button);
		#else
        this.addRenderableWidget(button);
		#endif
    }
}
