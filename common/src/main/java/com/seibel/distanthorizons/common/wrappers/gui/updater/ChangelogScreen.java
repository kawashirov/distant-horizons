package com.seibel.distanthorizons.common.wrappers.gui.updater;

import com.mojang.blaze3d.vertex.PoseStack;
import com.seibel.distanthorizons.common.wrappers.gui.DhScreen;
import com.seibel.distanthorizons.core.dependencyInjection.SingletonInjector;
import com.seibel.distanthorizons.core.wrapperInterfaces.IVersionConstants;
import com.seibel.distanthorizons.coreapi.ModInfo;
import com.seibel.distanthorizons.core.jar.installer.MarkdownFormatter;
import com.seibel.distanthorizons.core.jar.installer.ModrinthGetter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

#if POST_MC_1_17_1
import net.minecraft.client.gui.narration.NarratableEntry;
#endif

#if PRE_MC_1_20_1
import net.minecraft.client.gui.GuiComponent;
#else
import net.minecraft.client.gui.GuiGraphics;
#endif


import static com.seibel.distanthorizons.common.wrappers.gui.GuiHelper.*;

import java.util.*;

/**
 * The screen that pops up if the mod has an update.
 *
 * @author coolGi
 */
// TODO: After finishing the config, rewrite this in openGL as well
// TODO: Make this
public class ChangelogScreen extends DhScreen
{
	private Screen parent;
	private String versionID;
	private List<String> changelog;
	private TextArea changelogArea;
	
	public boolean usable = false;
	
	public ChangelogScreen(Screen parent)
	{
		this(parent, null);
		
		if (!ModrinthGetter.initted) // Make sure the modrinth stuff is initted
			ModrinthGetter.init();
		if (!ModrinthGetter.initted) // If its not initted, then this isnt usable
			return;
		
		if (!ModrinthGetter.mcVersions.contains(SingletonInjector.INSTANCE.get(IVersionConstants.class).getMinecraftVersion()))
			return;
		
		String versionID = ModrinthGetter.getLatestIDForVersion(SingletonInjector.INSTANCE.get(IVersionConstants.class).getMinecraftVersion());
		if (versionID == null)
			return;
		try
		{
			setupChangelog(versionID);
			usable = true;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public ChangelogScreen(Screen parent, String versionID)
	{
		super(Translatable(ModInfo.ID + ".updater.title"));
		this.parent = parent;
		this.versionID = versionID;
		
		
		if (versionID == null)
			return;
		try
		{
			setupChangelog(versionID);
			usable = true;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	private void setupChangelog(String versionID)
	{
		this.changelog = new ArrayList<>();
		
		// Put the new version name at the very top of the change log
		this.changelog.add("§lChangelog for " + ModrinthGetter.releaseNames.get(versionID) + "§r");
		this.changelog.add("");
		this.changelog.add("");
		
		// Get the release changelog and split it by the new lines
		String[] unwrappedChangelog = // Arrays.asList could be used if a list object is desired here vs List.of which is only available for Java 9+
				new MarkdownFormatter.MinecraftFormat().convertTo( // This formats markdown to minecraft's "§" characters
						ModrinthGetter.changeLogs.get(versionID)
				).split("\\n");
		// Makes the words wrap around to not go off the screen
		for (String str : unwrappedChangelog)
		{
			this.changelog.addAll(
					MarkdownFormatter.splitString(str, 75)
			);
		}
		// Debugging
//        System.out.println(this.changelog);
	}
	
	
	
	@Override
	protected void init()
	{
		super.init();
		if (!usable)
			return;
		
		
		this.addBtn( // Close
				MakeBtn(Translatable(ModInfo.ID + ".general.back"), 5, this.height - 25, 100, 20, (btn) -> {
					this.onClose();
				})
		);
		
		
		this.changelogArea = new TextArea(this.minecraft, this.width * 2, this.height, 32, this.height - 32);
		for (int i = 0; i < changelog.size(); i++)
		{
			this.changelogArea.addButton(TextOrLiteral(changelog.get(i)));
//            drawString(matrices, this.font, changelog.get(i), this.width / 2 - 175, this.height / 2 - 100 + i*10, 0xFFFFFF);
		}
		
	}
	
	@Override
    #if PRE_MC_1_20_1
	public void render(PoseStack matrices, int mouseX, int mouseY, float delta)
    #else
	public void render(GuiGraphics matrices, int mouseX, int mouseY, float delta)
    #endif
	{
		#if PRE_MC_1_20_2
		this.renderBackground(matrices); // Render background
		#else
		this.renderBackground(matrices, mouseX, mouseY, delta); // Render background
		#endif
		if (!usable)
			return;
		
		// Set the scroll position to the mouse height relative to the screen
		// This is a bit of a hack as we cannot scroll on this area
		double scrollAmount = ((double) mouseY) / ((double) this.height) * 1.1 * this.changelogArea.getMaxScroll();
	    #if MC_1_16_5 || MC_1_17_1
		this.changelogArea.setScrollAmount(scrollAmount);
		#else
		this.changelogArea.scrollAmount = scrollAmount;
		#endif
		
		
		this.changelogArea.render(matrices, mouseX, mouseY, delta); // Render the changelog
		
		super.render(matrices, mouseX, mouseY, delta); // Render the buttons
		
		DhDrawCenteredString(matrices, font, title, width / 2, 15, 0xFFFFFF); // Render title
	}
	
	@Override
	public void onClose()
	{
		Objects.requireNonNull(minecraft).setScreen(this.parent); // Goto the parent screen
	}
	
	public static class TextArea extends ContainerObjectSelectionList<ButtonEntry>
	{
		Font textRenderer;
		
		public TextArea(Minecraft minecraftClient, int i, int j, int k, int l)
		{
			#if PRE_MC_1_20_4
			super(minecraftClient, i, j, k, l, 10);
			#else
			super(minecraftClient, i, j, k, l);
			#endif
			this.centerListVertically = false;
			textRenderer = minecraftClient.font;
		}
		
		public void addButton(Component text)
		{
			this.addEntry(ButtonEntry.create(text));
		}
		
		@Override
		public int getRowWidth()
		{
			return 10000;
		}
		
	}
	
	public static class ButtonEntry extends ContainerObjectSelectionList.Entry<ButtonEntry>
	{
		private static final Font textRenderer = Minecraft.getInstance().font;
		private final Component text;
		private final List<AbstractWidget> children = new ArrayList<>();
		
		private ButtonEntry(Component text)
		{
			this.text = text;
		}
		
		public static ButtonEntry create(Component text)
		{
			return new ButtonEntry(text);
		}
		
		#if PRE_MC_1_20_1
		@Override
		public void render(PoseStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta)
		{
			GuiComponent.drawString(matrices, textRenderer, text, 12, y + 5, 0xFFFFFF);
		}
		#else
		@Override
		public void render(GuiGraphics matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta)
		{
			matrices.drawString(textRenderer, text, 12, y + 5, 0xFFFFFF);
		}
        #endif
		
		@Override
		public List<? extends GuiEventListener> children()
		{
			return children;
		}
		#if POST_MC_1_17_1
		@Override
		public List<? extends NarratableEntry> narratables()
		{
			return children;
		}
		#endif
	}
	
}