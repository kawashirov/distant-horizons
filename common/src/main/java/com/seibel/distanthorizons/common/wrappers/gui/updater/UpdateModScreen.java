package com.seibel.distanthorizons.common.wrappers.gui.updater;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.seibel.distanthorizons.api.enums.config.EUpdateBranch;
import com.seibel.distanthorizons.common.wrappers.gui.DhScreen;
import com.seibel.distanthorizons.common.wrappers.gui.TexturedButtonWidget;
import com.seibel.distanthorizons.core.jar.ModGitInfo;
import com.seibel.distanthorizons.core.jar.installer.GitlabGetter;
import com.seibel.distanthorizons.coreapi.ModInfo;
import com.seibel.distanthorizons.core.config.Config;
import com.seibel.distanthorizons.core.jar.JarUtils;
import com.seibel.distanthorizons.core.jar.installer.ModrinthGetter;
import com.seibel.distanthorizons.core.jar.updater.SelfUpdater;
import net.minecraft.client.Minecraft;
#if POST_MC_1_20_1
import net.minecraft.client.gui.GuiGraphics;
#endif
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import static com.seibel.distanthorizons.common.wrappers.gui.GuiHelper.*;

import java.util.*;

/**
 * The screen that pops up if the mod has an update.
 *
 * @author coolGi
 */
// TODO: After finishing the config, rewrite this in Java Swing as well
// and also maybe add this suggestion https://discord.com/channels/881614130614767666/1035863487110467625/1035949054485594192
public class UpdateModScreen extends DhScreen
{
	private Screen parent;
	private String newVersionID;
	
	private String currentVer;
	private String nextVer;
	
	
	public UpdateModScreen(Screen parent, String newVersionID)
	{
		super(Translatable(ModInfo.ID + ".updater.title"));
		this.parent = parent;
		this.newVersionID = newVersionID;

        switch (Config.Client.Advanced.AutoUpdater.updateBranch.get()) {
	        case STABLE:
                currentVer = ModInfo.VERSION;
                nextVer = ModrinthGetter.releaseNames.get(this.newVersionID);
				break;
	        case NIGHTLY:
                currentVer = ModGitInfo.Git_Main_Commit.substring(0,7);
                nextVer = this.newVersionID.substring(0,7);
				break;
        }
	}
	
	@Override
	protected void init()
	{
		super.init();
		
		
		try
		{
			// We cannot get assets from the root of the mod so we use this hack
			// TODO: Load the icon.png and logo.png in the mod initialise rather than here
			ResourceLocation logoLocation = new ResourceLocation(ModInfo.ID, "logo.png");
			Minecraft.getInstance().getTextureManager().register(
					logoLocation,
					new DynamicTexture(NativeImage.read(JarUtils.accessFile("logo.png")))
			);
			
			
			// Logo image
			this.addBtn(new TexturedButtonWidget(
					// Where the button is on the screen
					this.width / 2 - 65, this.height / 2 - 110,
					// Width and height of the button
					130, 65,
					// Offset
					0, 0,
					// Some textuary stuff
					0, logoLocation, 130, 65,
					// Create the button and tell it where to go
					// For now it goes to the client option by default
					(buttonWidget) -> System.out.println("Nice, you found an easter egg :)"), // TODO: Add a proper easter egg to pressing the logo (maybe with confetti)
					// Add a title to the button
					Translatable(ModInfo.ID + ".updater.title"),
					// Dont render the background of the button
					false
			));
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		if (Config.Client.Advanced.AutoUpdater.updateBranch.get() == EUpdateBranch.STABLE)
		{
			this.addBtn(new TexturedButtonWidget(
					// Where the button is on the screen
					this.width / 2 - 97, this.height / 2 + 8,
					// Width and height of the button
					20, 20,
					// Offset
					0, 0,
					// Some textuary stuff
					0, new ResourceLocation(ModInfo.ID, "textures/gui/changelog.png"), 20, 20,
					// Create the button and tell it where to go
					(buttonWidget) -> Objects.requireNonNull(minecraft).setScreen(new ChangelogScreen(this, this.newVersionID)), // TODO: Add a proper easter egg to pressing the logo (maybe with confetti)
					// Add a title to the button
					Translatable(ModInfo.ID + ".updater.title")
			));
		}
		
		
		this.addBtn( // Update
				MakeBtn(Translatable(ModInfo.ID + ".updater.update"), this.width / 2 - 75, this.height / 2 + 8, 150, 20, (btn) -> {
					SelfUpdater.updateMod();
					this.onClose();
				})
		);
		this.addBtn( // Silent update
				MakeBtn(Translatable(ModInfo.ID + ".updater.silent"), this.width / 2 - 75, this.height / 2 + 30, 150, 20, (btn) -> {
					Config.Client.Advanced.AutoUpdater.enableSilentUpdates.set(true);
					SelfUpdater.updateMod();
					this.onClose();
				})
		);
		this.addBtn( // Later (not now)
				MakeBtn(Translatable(ModInfo.ID + ".updater.later"), this.width / 2 + 2, this.height / 2 + 70, 100, 20, (btn) -> {
					this.onClose();
				})
		);
		this.addBtn( // Never
				MakeBtn(Translatable(ModInfo.ID + ".updater.never"), this.width / 2 - 102, this.height / 2 + 70, 100, 20, (btn) -> {
					Config.Client.Advanced.AutoUpdater.enableAutoUpdater.set(false);
					this.onClose();
				})
		);
		
	}
	
	@Override
    #if PRE_MC_1_20_1
	public void render(PoseStack matrices, int mouseX, int mouseY, float delta)
    #else
	public void render(GuiGraphics matrices, int mouseX, int mouseY, float delta)
    #endif
	{
		#if MC_1_20_2
		this.renderBackground(matrices, mouseX, mouseY, delta); // Render background
		#else
		this.renderBackground(matrices); // Render background
		#endif
		
		
		// Render the text's
		DhDrawCenteredString(matrices, this.font, Translatable(ModInfo.ID + ".updater.text1"), this.width / 2, this.height / 2 - 35, 0xFFFFFF);
		DhDrawCenteredString(matrices, this.font, 
				Translatable(ModInfo.ID + ".updater.text2", currentVer, nextVer), 
				this.width / 2, this.height / 2 - 20, 0x52FD52);
		
		// TODO: add the tooltips for the buttons
		super.render(matrices, mouseX, mouseY, delta); // Render the buttons
		
		// TODO: Add tooltips
	}
	
	@Override
	public void onClose()
	{
		Objects.requireNonNull(minecraft).setScreen(this.parent); // Goto the parent screen
	}
	
}