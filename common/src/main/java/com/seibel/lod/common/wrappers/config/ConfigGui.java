package com.seibel.lod.common.wrappers.config;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

// Logger (for debug stuff)

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// Uses https://github.com/TheElectronWill/night-config for toml (only for Fabric since Forge already includes this)

import com.electronwill.nightconfig.core.file.CommentedFileConfig;

// Gets info from our own mod

import com.seibel.lod.common.LodCommonMain;
import com.seibel.lod.core.ModInfo;
import com.seibel.lod.core.config.*;

// Minecraft imports

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.client.resources.language.I18n;	// translation
import net.minecraft.client.gui.narration.NarratableEntry; // Remove in 1.16

/**
 * Based upon TinyConfig
 * https://github.com/Minenash/TinyConfig
 *
 * This config should work for both Fabric and Forge as long as you use Mojang mappings
 *
 * Credits to Motschen
 *
 * @author coolGi2007
 * @version 1-14-2022
 */
@SuppressWarnings("unchecked")
public abstract class ConfigGui
{
	/*
	        TODO list

		Fix floats not working
        Make a wiki
        Make it so you can enable and disable buttons from showing
        Make min and max not final
        Move the ConfigScreenConfigs class to the config class that extends this
     */
	/*
		List of hacky things that are done that should be done properly

		The buttons that don't show are still loaded but just not rendered
		The screen with is set to double so the scroll bar doesn't show
	 */


	private static final Pattern INTEGER_ONLY_REGEX = Pattern.compile("(-?[0-9]*)");
	private static final Pattern DECIMAL_ONLY_REGEX = Pattern.compile("-?([\\d]+\\.?[\\d]*|[\\d]*\\.?[\\d]+|\\.)");

	private static final List<EntryInfo> entries = new ArrayList<>();
	public static final Map<String,EntryInfo> entryMap = new HashMap<>();

	// Change these to your own mod
	private static final String MOD_NAME = ModInfo.NAME;					// For file saving and identifying
	private static final String MOD_NAME_READABLE = ModInfo.READABLE_NAME;	// For logs
	//	private static final Logger LOGGER = ClientApi.LOGGER;						// For logs
	private static final Logger LOGGER = LogManager.getLogger(ModInfo.NAME);		// For logs (this inits before ClientAPI so this is a temp fix)




	//==============//
	// Initializers //
	//==============//

	private static class ConfigScreenConfigs
	{
		// This contains all the configs for the configs
		public static final int SpaceFromRightScreen = 10;
		public static final int ButtonWidthSpacing = 5;
		public static final int ResetButtonWidth = 40;
	}

	public static class EntryInfo<T>
	{
		Field field;
		Object widget;
		int width = 0;
		int max;
		Map.Entry<EditBox, Component> error;
		Object defaultValue;
		Object value;
		String tempValue;
		boolean inLimits = true;
		TranslatableComponent name;
		int index;
		/** Hides the button */
		boolean hideOption = false;
		/** This asks if it is a button to goto a new screen */
		boolean screenButton = false;
		/** This is only called if button is true */
		String gotoScreen = "";
		String category;
		Class<T> varClass;


		@Deprecated
		boolean fileComment = false;
	}

	private static Path configFilePath;



	public static void init(Class<?> config)
	{
		Minecraft mc =  Minecraft.getInstance();
		configFilePath = mc.gameDirectory.toPath().resolve("config").resolve(MOD_NAME + ".toml");

		initNestedClass(config, "");

		for (EntryInfo info : entries) {
			if (info.field.isAnnotationPresent(ConfigAnnotations.Entry.class)) {
				try {
					info.value = info.field.get(null);
					info.tempValue = info.value.toString();
				} catch (IllegalAccessException ignored) {
				}
			}
		}

		loadFromFile();
	}

	private static void initNestedClass(Class<?> config, String category)
	{
		for (Field field : config.getFields())
		{
			EntryInfo info = new EntryInfo();
			if (field.isAnnotationPresent(ConfigAnnotations.Entry.class) || field.isAnnotationPresent(ConfigAnnotations.Comment.class) || field.isAnnotationPresent(ConfigAnnotations.ScreenEntry.class))
			{
				// If putting in your own mod then put your own check for server sided
				info.category = category;
				if (!LodCommonMain.serverSided)
					initClient(field, info, category);
			}

			if (field.isAnnotationPresent(ConfigAnnotations.Entry.class))
			{
				entryMap.put((!category.isEmpty() ? category + "." : "") + field.getName(), info);
				info.varClass = field.getType();
				try
				{
					info.defaultValue = field.get(null);
				}
				catch (IllegalAccessException ignored) {}
			}

			if (field.isAnnotationPresent(ConfigAnnotations.ScreenEntry.class))
				initNestedClass(field.getType(), (!category.isEmpty() ? category + "." : "") + field.getName());

			// File comment (WILL BE REMOVED SOON)
			if (field.isAnnotationPresent(ConfigAnnotations.FileComment.class)) {
				entryMap.put((!category.isEmpty() ? category + "." : "") + field.getName(), info);
				info.fileComment = true;
				try
				{
					info.value = info.defaultValue = field.get(null);
				}
				catch (IllegalAccessException ignored) {}
			}

			info.field = field;
		}
	}

	/** This adds the buttons to the queue to be rendered */
	private static void initClient(Field field, EntryInfo info, String category)
	{
		Class<?> fieldClass = field.getType();
		ConfigAnnotations.Entry entry = field.getAnnotation(ConfigAnnotations.Entry.class);
		ConfigAnnotations.ScreenEntry screenEntry = field.getAnnotation(ConfigAnnotations.ScreenEntry.class);

		if (entry != null)
			info.width = entry.width();
		else if (screenEntry != null)
			info.width = screenEntry.width();

		if (entry != null)
		{
			if (!entry.name().equals(""))
				info.name = new TranslatableComponent(entry.name());


			if (fieldClass == int.class)
			{
				// For int
				textField(info, Integer::parseInt, INTEGER_ONLY_REGEX, entry.minValue(), entry.maxValue(), true);
			}
			else if (fieldClass == double.class)
			{
				// For double
				textField(info, Double::parseDouble, DECIMAL_ONLY_REGEX, entry.minValue(), entry.maxValue(), false);
			}
			else if (fieldClass == String.class || fieldClass == List.class)
			{
				// For string or list
				info.max = entry.maxValue() == Double.MAX_VALUE ? Integer.MAX_VALUE : (int) entry.maxValue();
				textField(info, String::length, null, Math.min(entry.minValue(), 0), Math.max(entry.maxValue(), 1), true);
			}
			else if (fieldClass == boolean.class)
			{
				// For boolean
				Function<Object, Component> func = value -> new TextComponent((Boolean) value ? "True" : "False").withStyle((Boolean) value ? ChatFormatting.GREEN : ChatFormatting.RED);
				info.widget = new AbstractMap.SimpleEntry<Button.OnPress, Function<Object, Component>>(button -> {
					info.value = !(Boolean) info.value;
					button.setMessage(func.apply(info.value));
				}, func);
			}
			else if (fieldClass.isEnum())
			{
				// For enum
				List<?> values = Arrays.asList(field.getType().getEnumConstants());
				Function<Object, Component> func = value -> new TranslatableComponent(MOD_NAME + ".config." + "enum." + fieldClass.getSimpleName() + "." + info.value.toString());
				info.widget = new AbstractMap.SimpleEntry<Button.OnPress, Function<Object, Component>>(button -> {
					int index = values.indexOf(info.value) + 1;
					info.value = values.get(index >= values.size() ? 0 : index);
					button.setMessage(func.apply(info.value));
				}, func);
			}
		}
		else if (screenEntry != null)
		{
			if (!screenEntry.name().equals(""))
				info.name = new TranslatableComponent(screenEntry.name());

			info.screenButton = true;
			info.gotoScreen = (!info.category.isEmpty() ? info.category + "." : "") + field.getName();
		}
		entries.add(info);
	}




	/** creates a text field */
	private static void textField(EntryInfo info, Function<String, Number> func, Pattern pattern, double minValue, double maxValue, boolean cast)
	{
		boolean isNumber = pattern != null;
		info.widget = (BiFunction<EditBox, Button, Predicate<String>>) (editBox, button) -> stringValue ->
		{
			stringValue = stringValue.trim();
			if (!(stringValue.isEmpty() || !isNumber || pattern.matcher(stringValue).matches()))
				return false;

			Number value = 0;
			boolean inLimits = false;
			info.error = null;
			if (isNumber && !stringValue.isEmpty() && !stringValue.equals("-") && !stringValue.equals("."))
			{
				value = func.apply(stringValue);
				inLimits = value.doubleValue() >= minValue && value.doubleValue() <= maxValue;
				info.error = inLimits ? null : new AbstractMap.SimpleEntry<>(editBox, new TextComponent(value.doubleValue() < minValue ?
						"§cMinimum " + "length" + (cast ? " is " + (int) minValue : " is " + minValue) :
						"§cMaximum " + "length" + (cast ? " is " + (int) maxValue : " is " + maxValue)));
			}

			info.tempValue = stringValue;
			editBox.setTextColor(inLimits ? 0xFFFFFFFF : 0xFFFF7777);
			info.inLimits = inLimits;
			button.active = entries.stream().allMatch(e -> e.inLimits);


			if (inLimits && info.field.getType() != List.class)
			{
				info.value = value;
			}
			else if (inLimits)
			{
				if (((List<String>) info.value).size() == info.index)
					((List<String>) info.value).add("");
				((List<String>) info.value).set(info.index, Arrays.stream(info.tempValue.replace("[", "").replace("]", "").split(", ")).collect(Collectors.toList()).get(0));
			}

			return true;
		};
	}




	//===============//
	// File Handling //
	//===============//

	/** Grabs what is in the config and puts it in modid.toml */
	public static void saveToFile()
	{
		CommentedFileConfig config = CommentedFileConfig.builder(configFilePath.toFile()).build();

		// First try to create a config file
		try {
			if (!Files.exists(configFilePath))
				Files.createFile(configFilePath);
		}
		catch (Exception e) {
			LOGGER.info("Failed creating config file for " + MOD_NAME_READABLE + " at the path [" + configFilePath.toString() + "].");
			e.printStackTrace();
		}

		loadFileWithErrorCheck(config);

		for (EntryInfo info : entries) {
			if (info.field.isAnnotationPresent(ConfigAnnotations.Entry.class)) {
				editSingleOption.saveOption(info, config);

				if (editSingleOption.getEntry((info.category.isEmpty() ? "" : info.category + ".") + "_" + info.field.getName()) != null)
					config.setComment((info.category.isEmpty() ? "" : info.category + ".") + info.field.getName(), String.valueOf(editSingleOption.getEntry((info.category.isEmpty() ? "" : info.category + ".") + "_" + info.field.getName()).defaultValue));
			}
		}

		config.save();
		config.close();
	}

	/**
	 * Grabs what is in modid.toml and puts it into the config
	 * If the file doesn't exist then it runs saveToFile
	 */
	public static void loadFromFile()
	{
		CommentedFileConfig config = CommentedFileConfig.builder(configFilePath.toFile()).autosave().build();

		// First checks if the config file was already made
		if (!Files.exists(configFilePath)) {
			LOGGER.info("Config file not found for " + MOD_NAME_READABLE + ". Creating config...");
			saveToFile();
			return;
		}

		loadFileWithErrorCheck(config);

		// Puts everything into its variable
		for (EntryInfo info : entries) {
			if (info.field.isAnnotationPresent(ConfigAnnotations.Entry.class)) {
				editSingleOption.loadOption(info, config);

				// File comments (WILL REMOVE SOON)
				if (editSingleOption.getEntry((info.category.isEmpty() ? "" : info.category + ".") + "_" + info.field.getName()) != null)
					config.setComment((info.category.isEmpty() ? "" : info.category + ".") + info.field.getName(), String.valueOf(editSingleOption.getEntry((info.category.isEmpty() ? "" : info.category + ".") + "_" + info.field.getName()).defaultValue));
			}
		}

		config.close();
	}

	public static class editSingleOption {
		/** Get the entry info of an item using its string name */
		public static EntryInfo getEntry(String name)
		{
			return entryMap.get(name);
		}

		/** Save a single item using its string name */
		public static void saveOption(String name)
		{
			saveOption(entryMap.get(name));
		}

		/** Saves a single item using entry info */
		public static void saveOption(EntryInfo info)
		{
			CommentedFileConfig config = CommentedFileConfig.builder(configFilePath.toFile()).autosave().build();

			loadFileWithErrorCheck(config);

			saveOption(info, config);

			config.close();
		}

		/** Saves a single item using its entry info and its config builder */
		public static void saveOption(EntryInfo info, CommentedFileConfig config)
		{
			config.set((info.category.isEmpty() ? "" : info.category + ".") + info.field.getName(), info.value);
		}

		/** Load a single item using its string name */
		public static void loadOption(String name)
		{
			loadOption(entryMap.get(name));
		}

		/** Load a single item using entry info */
		public static void loadOption(EntryInfo info)
		{
			CommentedFileConfig config = CommentedFileConfig.builder(configFilePath.toFile()).autosave().build();

			loadFileWithErrorCheck(config);

			loadOption(info, config);

			config.close();
		}

		/** Loads a single item using its entry info and its config builder */
		public static void loadOption(EntryInfo info, CommentedFileConfig config)
		{
			String itemPath = (info.category.isEmpty() ? "" : info.category + ".") + info.field.getName();
			if (config.contains(itemPath)) {
				if (info.field.getType().isEnum())
					info.value = config.getEnum(itemPath, info.varClass);
				else
					info.value = config.get(itemPath);
			} else
				config.set(itemPath, info.value);

			try {
				info.field.set(null, info.value);
			} catch (IllegalAccessException ignored) {
			}
		}
	}

	/** Dose config.load(); but with error checking to avoid crashes */
	public static void loadFileWithErrorCheck(CommentedFileConfig config) {
		try {
			config.load();
		} catch (Exception e) {
			LOGGER.info("Error loading config for " + MOD_NAME_READABLE + " at the path [" + configFilePath.toString() + "].");
			LOGGER.info("Creating a new config...");
			try {
				Files.deleteIfExists(configFilePath);
				saveToFile();
			} catch (Exception f) {
				LOGGER.info("Failed creating config file for " + MOD_NAME_READABLE + " at the path [" + configFilePath.toString() + "].");
				f.printStackTrace();
			}
		}
	}




	//==============//
	// GUI handling //
	//==============//

	public static Screen getScreen(Screen parent, String category)
	{
		return new ConfigScreen(parent, category);
	}

	private static class ConfigScreen extends Screen
	{
		protected ConfigScreen(Screen parent, String category)
		{
			super(new TranslatableComponent(
					I18n.exists(MOD_NAME + ".config" + (category.isEmpty()? "." + category : "") + ".title") ?
							MOD_NAME + ".config.title" :
							MOD_NAME + ".config" + (category.isEmpty() ? "" : "." + category) + ".title")
			);
			this.parent = parent;
			this.category = category;
			this.translationPrefix = MOD_NAME + ".config.";
		}

		private final String translationPrefix;
		private final Screen parent;
		private final String category;
		private ConfigListWidget list;
		private boolean reload = false;

		// Real Time config update //
		@Override
		public void tick()
		{
			super.tick();
		}


		/** When you close it, it goes to the previous screen and saves */
		@Override
		public void onClose()
		{
			saveToFile();
			Objects.requireNonNull(minecraft).setScreen(this.parent);
		}

		@Override
		protected void init()
		{
			super.init();
			if (!reload)
				loadFromFile();

			// addRenderableWidget in 1.17 and over
			// addButton in 1.16 and below
			this.addRenderableWidget(new Button(this.width / 2 - 154, this.height - 28, 150, 20, CommonComponents.GUI_CANCEL, button -> {
				loadFromFile();
				Objects.requireNonNull(minecraft).setScreen(parent);
			}));

			Button done = this.addRenderableWidget(new Button(this.width / 2 + 4, this.height - 28, 150, 20, CommonComponents.GUI_DONE, (button) -> {
				saveToFile();
				Objects.requireNonNull(minecraft).setScreen(parent);
			}));

			this.list = new ConfigListWidget(this.minecraft, this.width * 2, this.height, 32, this.height - 32, 25);
			if (this.minecraft != null && this.minecraft.level != null)
				this.list.setRenderBackground(false);
			this.addWidget(this.list);
			for (EntryInfo info : entries)
			{
				if (info.category.matches(category) && !info.hideOption)
				{
					TranslatableComponent name = (info.name == null ? new TranslatableComponent(translationPrefix + (!info.category.isEmpty() ? info.category + "." : "") + info.field.getName()) : info.name);
					Button resetButton = new Button(this.width - ConfigScreenConfigs.SpaceFromRightScreen - info.width - ConfigScreenConfigs.ButtonWidthSpacing - ConfigScreenConfigs.ResetButtonWidth, 0, ConfigScreenConfigs.ResetButtonWidth, 20, new TextComponent("Reset").withStyle(ChatFormatting.RED), (button -> {
						info.value = info.defaultValue;
						info.tempValue = info.defaultValue.toString();
						info.index = 0;
						this.reload = true;
						Objects.requireNonNull(minecraft).setScreen(this);
					}));

					if (info.widget instanceof Map.Entry)
					{
						Map.Entry<Button.OnPress, Function<Object, Component>> widget = (Map.Entry<Button.OnPress, Function<Object, Component>>) info.widget;
						if (info.field.getType().isEnum())
							widget.setValue(value -> new TranslatableComponent(translationPrefix + "enum." + info.field.getType().getSimpleName() + "." + info.value.toString()));
						this.list.addButton(new Button(this.width - info.width - ConfigScreenConfigs.SpaceFromRightScreen, 0, info.width, 20, widget.getValue().apply(info.value), widget.getKey()), resetButton, null, name);
					}
					else if (info.field.getType() == List.class)
					{
						if (!reload)
							info.index = 0;
						EditBox widget = new EditBox(font, this.width - info.width - ConfigScreenConfigs.SpaceFromRightScreen, 0, info.width, 20, null);
						widget.setMaxLength(info.width);
						if (info.index < ((List<String>) info.value).size())
							widget.insertText((String.valueOf(((List<String>) info.value).get(info.index))));
						else
							widget.insertText("");
						Predicate<String> processor = ((BiFunction<EditBox, Button, Predicate<String>>) info.widget).apply(widget, done);
						widget.setFilter(processor);
						resetButton.setWidth(20);
						resetButton.setMessage(new TextComponent("R").withStyle(ChatFormatting.RED));
						Button cycleButton = new Button(this.width - 185, 0, 20, 20, new TextComponent(String.valueOf(info.index)).withStyle(ChatFormatting.GOLD), (button -> {
							((List<String>) info.value).remove("");
							this.reload = true;
							info.index = info.index + 1;
							if (info.index > ((List<String>) info.value).size())
								info.index = 0;
							Objects.requireNonNull(minecraft).setScreen(this);
						}));
						this.list.addButton(widget, resetButton, cycleButton, name);
					}
					else if (info.widget != null)
					{
						EditBox widget = new EditBox(font, this.width - info.width - ConfigScreenConfigs.SpaceFromRightScreen + 2, 0, info.width - 4, 20, null);
						widget.setMaxLength(info.width);
						widget.insertText(String.valueOf(info.value));
						Predicate<String> processor = ((BiFunction<EditBox, Button, Predicate<String>>) info.widget).apply(widget, done);
						widget.setFilter(processor);
						this.list.addButton(widget, resetButton, null, name);
					}
					else if (info.screenButton)
					{
						Button widget = new Button(this.width / 2 - info.width, this.height - 28, info.width * 2, 20, name, (button -> {
							Objects.requireNonNull(minecraft).setScreen(ConfigGui.getScreen(this, info.gotoScreen));
						}));
						this.list.addButton(widget, null, null, null);
					}
					else if (!info.fileComment)
					{
						this.list.addButton(null, null, null, name);
					}
				}
			}

		}

		@Override
		public void render(PoseStack matrices, int mouseX, int mouseY, float delta)
		{
			this.renderBackground(matrices); // Renders background
			this.list.render(matrices, mouseX, mouseY, delta); // Render buttons
			drawCenteredString(matrices, font, title, width / 2, 15, 0xFFFFFF); // Render title

			// Render the tooltip only if it can find a tooltip in the language file
			for (EntryInfo info : entries) {
				if (info.category.matches(category) && !info.hideOption) {
					if (list.getHoveredButton(mouseX,mouseY).isPresent()) {
						AbstractWidget buttonWidget = list.getHoveredButton(mouseX,mouseY).get();
						Component text = ButtonEntry.buttonsWithText.get(buttonWidget);
						TranslatableComponent name = new TranslatableComponent(this.translationPrefix + (info.category.isEmpty() ? "" : info.category + ".") + info.field.getName());
						String key = translationPrefix + (info.category.isEmpty() ? "" : info.category + ".") + info.field.getName() + ".@tooltip";

						if (info.error != null && text.equals(name)) renderTooltip(matrices, (Component) info.error.getValue(), mouseX, mouseY);
						else if (I18n.exists(key) && (text != null && text.equals(name))) {
							List<Component> list = new ArrayList<>();
							for (String str : I18n.get(key).split("\n"))
								list.add(new TextComponent(str));
							renderComponentTooltip(matrices, list, mouseX, mouseY);
						}
					}
				}
			}
			super.render(matrices, mouseX, mouseY, delta);
		}
	}




	public static class ConfigListWidget extends ContainerObjectSelectionList<ButtonEntry>
	{
		Font textRenderer;

		public ConfigListWidget(Minecraft minecraftClient, int i, int j, int k, int l, int m)
		{
			super(minecraftClient, i, j, k, l, m);
			this.centerListVertically = false;
			textRenderer = minecraftClient.font;
		}

		public void addButton(AbstractWidget button, AbstractWidget resetButton, AbstractWidget indexButton, Component text)
		{
			this.addEntry(ButtonEntry.create(button, text, resetButton, indexButton));
		}

		@Override
		public int getRowWidth()
		{
			return 10000;
		}

		public Optional<AbstractWidget> getHoveredButton(double mouseX, double mouseY)
		{
			for (ButtonEntry buttonEntry : this.children())
			{
				if (buttonEntry.button != null && buttonEntry.button.isMouseOver(mouseX, mouseY))
				{
					return Optional.of(buttonEntry.button);
				}
			}
			return Optional.empty();
		}
	}





	public static class ButtonEntry extends ContainerObjectSelectionList.Entry<ButtonEntry>
	{
		private static final Font textRenderer = Minecraft.getInstance().font;
		public final AbstractWidget button;
		private final AbstractWidget resetButton;
		private final AbstractWidget indexButton;
		private final Component text;
		private final List<AbstractWidget> children = new ArrayList<>();
		public static final Map<AbstractWidget, Component> buttonsWithText = new HashMap<>();

		private ButtonEntry(AbstractWidget button, Component text, AbstractWidget resetButton, AbstractWidget indexButton)
		{
			buttonsWithText.put(button, text);
			this.button = button;
			this.resetButton = resetButton;
			this.text = text;
			this.indexButton = indexButton;
			if (button != null)
				children.add(button);
			if (resetButton != null)
				children.add(resetButton);
			if (indexButton != null)
				children.add(indexButton);
		}

		public static ButtonEntry create(AbstractWidget button, Component text, AbstractWidget resetButton, AbstractWidget indexButton)
		{
			return new ButtonEntry(button, text, resetButton, indexButton);
		}

		@Override
		public void render(PoseStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta)
		{
			if (button != null)
			{
				button.y = y;
				button.render(matrices, mouseX, mouseY, tickDelta);
			}
			if (resetButton != null)
			{
				resetButton.y = y;
				resetButton.render(matrices, mouseX, mouseY, tickDelta);
			}
			if (indexButton != null)
			{
				indexButton.y = y;
				indexButton.render(matrices, mouseX, mouseY, tickDelta);
			}
			if (text != null && (!text.getString().contains("spacer") || button != null))
				GuiComponent.drawString(matrices, textRenderer, text, 12, y + 5, 0xFFFFFF);
		}

		@Override
		public List<? extends GuiEventListener> children()
		{
			return children;
		}

		// Only for 1.17 and over
		// Remove in 1.16 and below
        @Override
        public List<? extends NarratableEntry> narratables()
        {
            return children;
        }
	}
}
