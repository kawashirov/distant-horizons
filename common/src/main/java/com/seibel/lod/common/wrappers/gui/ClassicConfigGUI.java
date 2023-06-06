package com.seibel.lod.common.wrappers.gui;

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

// Logger (for debug stuff)

import com.seibel.lod.common.wrappers.gui.updater.ChangelogScreen;
import com.seibel.lod.core.config.types.AbstractConfigType;
import com.seibel.lod.core.config.types.ConfigCategory;
import com.seibel.lod.core.config.types.ConfigEntry;

// Uses https://github.com/TheElectronWill/night-config for toml (only for Fabric since Forge already includes this)

// Gets info from our own mod

import com.seibel.lod.core.config.*;

// Minecraft imports

import com.mojang.blaze3d.vertex.PoseStack;
import com.seibel.lod.core.config.types.ConfigUIComment;
import com.seibel.lod.core.dependencyInjection.SingletonInjector;
import com.seibel.lod.core.jar.installer.ModrinthGetter;
import com.seibel.lod.core.jar.updater.SelfUpdater;
import com.seibel.lod.core.wrapperInterfaces.IVersionConstants;
import com.seibel.lod.coreapi.ModInfo;
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
import net.minecraft.client.resources.language.I18n;	// translation
#if POST_MC_1_17_1
import net.minecraft.client.gui.narration.NarratableEntry;
#endif
#if PRE_MC_1_19
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
#endif

/**
 * Based upon TinyConfig but is highly modified
 * https://github.com/Minenash/TinyConfig
 *
 * Credits to Motschen
 *
 * @author coolGi
 * @version 5-21-2022
 */
// FLOATS DONT WORK WITH THIS
/** This file is going to be removed sometime soon, please dont hook onto anything within this file until the new UI is compleated */
@SuppressWarnings("unchecked")
public class ClassicConfigGUI {
	/*
	    This would be removed later on as it is going to be re-written in java swing
	 */


    //==============//
    // Initializers //
    //==============//

    // Some regexes to check if an input is valid
    private static final Pattern INTEGER_ONLY_REGEX = Pattern.compile("(-?[0-9]*)");
    private static final Pattern DECIMAL_ONLY_REGEX = Pattern.compile("-?([\\d]+\\.?[\\d]*|[\\d]*\\.?[\\d]+|\\.)");

    private static class ConfigScreenConfigs {
        // This contains all the configs for the configs
        public static final int SpaceFromRightScreen = 10;
        public static final int ButtonWidthSpacing = 5;
        public static final int ResetButtonWidth = 40;
    }

    /**
     * The terribly coded old stuff
     */
    public static class EntryInfo {
        Object widget;
        Map.Entry<EditBox, Component> error;
        String tempValue;
        int index;
    }



    /**
     * creates a text field
     */
    private static void textField(AbstractConfigType info, Function<String, Number> func, Pattern pattern, boolean cast) {
        boolean isNumber = pattern != null;
        ((EntryInfo) info.guiValue).widget = (BiFunction<EditBox, Button, Predicate<String>>) (editBox, button) -> stringValue ->
        {
            stringValue = stringValue.trim();
            if (!(stringValue.isEmpty() || !isNumber || pattern.matcher(stringValue).matches()))
                return false;

            Number value = 0;
            ((EntryInfo) info.guiValue).error = null;
            if (isNumber && !stringValue.isEmpty() && !stringValue.equals("-") && !stringValue.equals(".")) {
                value = func.apply(stringValue);
                #if PRE_MC_1_19
                ((EntryInfo) info.guiValue).error = ((ConfigEntry) info).isValid(value) == 0 ? null : new AbstractMap.SimpleEntry<>(editBox, new TextComponent(((ConfigEntry) info).isValid(value) == -1 ?
                #else
                ((EntryInfo) info.guiValue).error = ((ConfigEntry) info).isValidMemoryAddress(value) == 0 ? null : new AbstractMap.SimpleEntry<>(editBox, Component.translatable(((ConfigEntry) info).isValidMemoryAddress(value) == -1 ?
                #endif
                        "§cMinimum " + "length" + (cast ? " is " + (int) ((ConfigEntry) info).getMin() : " is " + ((ConfigEntry) info).getMin()) :
                        "§cMaximum " + "length" + (cast ? " is " + (int) ((ConfigEntry) info).getMax() : " is " + ((ConfigEntry) info).getMax())));
            }

            ((EntryInfo) info.guiValue).tempValue = stringValue;
            editBox.setTextColor(((ConfigEntry) info).isValid(value) == 0 ? 0xFFFFFFFF : 0xFFFF7777);
//            button.active = entries.stream().allMatch(e -> e.inLimits);


            if (((ConfigEntry) info).isValid(value) == 0 && info.getType() != List.class) {
                if (!cast)
                    ((ConfigEntry) info).setWithoutSaving(value);
                else
                    ((ConfigEntry) info).setWithoutSaving(value.intValue());
            }
//            else if (((ConfigEntry) info).isValidMemoryAddress() == 0)
//            {
//                if (((List<String>) info.get()).size() == ((EntryInfo) info.guiValue).index)
//                    info.set(((List<String>) info.get()).add(""));
//                info.set(((List<String>) info.get()).set(((EntryInfo) info.guiValue).index, Arrays.stream(((EntryInfo) info.guiValue).tempValue.replace("[", "").replace("]", "").split(", ")).collect(Collectors.toList()).get(0)));
//            }

            return true;
        };
    }

    //==============//
    // GUI handling //
    //==============//

    /**
     * if you want to get this config gui's screen call this
     */
    public static Screen getScreen(ConfigBase configBase, Screen parent, String category) {
        return new ConfigScreen(configBase, parent, category);
    }

    /**
     * Pain
     */
    private static class ConfigScreen extends Screen {
        protected ConfigScreen(ConfigBase configBase, Screen parent, String category) {
            #if PRE_MC_1_19
            super(new TranslatableComponent(
            #else
            super(Component.translatable(
            #endif
                    I18n.exists(configBase.modID + ".config" + (category.isEmpty() ? "." + category : "") + ".title") ?
                            configBase.modID + ".config.title" :
                            configBase.modID + ".config" + (category.isEmpty() ? "" : "." + category) + ".title")
            );
            this.configBase = configBase;
            this.parent = parent;
            this.category = category;
            this.translationPrefix = configBase.modID + ".config.";
        }
        private final ConfigBase configBase;

        private final String translationPrefix;
        private final Screen parent;
        private final String category;
        private ConfigListWidget list;
        private boolean reload = false;

        // Real Time config update //
        @Override
        public void tick() {
            super.tick();
        }


        /**
         * When you close it, it goes to the previous screen and saves
         */
        @Override
        public void onClose() {
            ConfigBase.INSTANCE.configFileINSTANCE.saveToFile();
            Objects.requireNonNull(minecraft).setScreen(this.parent);
        }

        // addRenderableWidget in 1.17 and over
        // addButton in 1.16 and below
        private Button addBtn(Button button) {
			#if PRE_MC_1_17_1
            this.addButton(button);
			#else
            this.addRenderableWidget(button);
			#endif
            return button;
        }

        @Override
        protected void init() {
            super.init();
            if (!reload)
                ConfigBase.INSTANCE.configFileINSTANCE.loadFromFile();

            // Changelog button
            if (Config.Client.AutoUpdater.enableAutoUpdater.get()) {
                this.addBtn(new TexturedButtonWidget(
                        // Where the button is on the screen
                        this.width - 28, this.height - 28,
                        // Width and height of the button
                        20, 20,
                        // Offset
                        0, 0,
                        // Some textuary stuff
                        0, new ResourceLocation(ModInfo.ID, "textures/gui/changelog.png"), 20, 20,
                        // Create the button and tell it where to go
                        (buttonWidget) -> Objects.requireNonNull(minecraft).setScreen(new ChangelogScreen(this)),
                        // Add a title to the button
                        #if PRE_MC_1_19
                        new TranslatableComponent(ModInfo.ID + ".updater.title")
                        #else
                        Component.translatable(ModInfo.ID + ".updater.title")
                        #endif
                ));
            }

            addBtn(new Button(this.width / 2 - 154, this.height - 28, 150, 20, CommonComponents.GUI_CANCEL, button -> {
                ConfigBase.INSTANCE.configFileINSTANCE.loadFromFile();
                Objects.requireNonNull(minecraft).setScreen(parent);
            }));
            Button done = addBtn(new Button(this.width / 2 + 4, this.height - 28, 150, 20, CommonComponents.GUI_DONE, (button) -> {
                ConfigBase.INSTANCE.configFileINSTANCE.saveToFile();
                Objects.requireNonNull(minecraft).setScreen(parent);
            }));

            this.list = new ConfigListWidget(this.minecraft, this.width * 2, this.height, 32, this.height - 32, 25);
            if (this.minecraft != null && this.minecraft.level != null)
                this.list.setRenderBackground(false);
            this.addWidget(this.list);
            for (AbstractConfigType info : ConfigBase.INSTANCE.entries) {
                try {
                    if (info.getCategory().matches(category) && info.getAppearance().showInGui) {
                        initEntry(info, this.translationPrefix);
                        #if PRE_MC_1_19
                        TranslatableComponent name = new TranslatableComponent(translationPrefix + info.getNameWCategory());
                        #else
                        Component name = Component.translatable(translationPrefix + info.getNameWCategory());
                        #endif
                        if (ConfigEntry.class.isAssignableFrom(info.getClass())) {
                            #if PRE_MC_1_19
                            Button resetButton = new Button(this.width - ConfigScreenConfigs.SpaceFromRightScreen - 150 - ConfigScreenConfigs.ButtonWidthSpacing - ConfigScreenConfigs.ResetButtonWidth, 0, ConfigScreenConfigs.ResetButtonWidth, 20, new TextComponent("Reset").withStyle(ChatFormatting.RED), (button -> {
                            #else
                            Button resetButton = new Button(this.width - ConfigScreenConfigs.SpaceFromRightScreen - 150 - ConfigScreenConfigs.ButtonWidthSpacing - ConfigScreenConfigs.ResetButtonWidth, 0, ConfigScreenConfigs.ResetButtonWidth, 20, Component.translatable("Reset").withStyle(ChatFormatting.RED), (button -> {
                            #endif
                                ((ConfigEntry) info).setWithoutSaving(((ConfigEntry) info).getDefaultValue());
                                ((EntryInfo) info.guiValue).index = 0;
                                this.reload = true;
                                Objects.requireNonNull(minecraft).setScreen(this);
                            }));

                            if (((EntryInfo) info.guiValue).widget instanceof Map.Entry) {
                                Map.Entry<Button.OnPress, Function<Object, Component>> widget = (Map.Entry<Button.OnPress, Function<Object, Component>>) ((EntryInfo) info.guiValue).widget;
                                if (info.getType().isEnum())
                                    #if PRE_MC_1_19
                                    widget.setValue(value -> new TranslatableComponent(translationPrefix + "enum." + info.getType().getSimpleName() + "." + info.get().toString()));
                                    #else
                                    widget.setValue(value -> Component.translatable(translationPrefix + "enum." + info.getType().getSimpleName() + "." + info.get().toString()));
                                    #endif
                                this.list.addButton(new Button(this.width - 150 - ConfigScreenConfigs.SpaceFromRightScreen, 0, 150, 20, widget.getValue().apply(info.get()), widget.getKey()), resetButton, null, name);
                            } else if (((EntryInfo) info.guiValue).widget != null) {
                                EditBox widget = new EditBox(font, this.width - 150 - ConfigScreenConfigs.SpaceFromRightScreen + 2, 0, 150 - 4, 20, null);
                                widget.setMaxLength(150);
                                widget.insertText(String.valueOf(info.get()));
                                Predicate<String> processor = ((BiFunction<EditBox, Button, Predicate<String>>) ((EntryInfo) info.guiValue).widget).apply(widget, done);
                                widget.setFilter(processor);
                                this.list.addButton(widget, resetButton, null, name);
                            }
                        } else if (ConfigCategory.class.isAssignableFrom(info.getClass())) {
                            Button widget = new Button(this.width / 2 - 100, this.height - 28, 100 * 2, 20, name, (button -> {
                                ConfigBase.INSTANCE.configFileINSTANCE.saveToFile();
                                Objects.requireNonNull(minecraft).setScreen(ClassicConfigGUI.getScreen(this.configBase, this, ((ConfigCategory) info).getDestination()));
                            }));
                            this.list.addButton(widget, null, null, null);
                        } else if (ConfigUIComment.class.isAssignableFrom(info.getClass())) {
                            this.list.addButton(null, null, null, name);
                        }
                    }
                } catch (Exception e) {
                    System.out.println("ERROR: Failed to show ["+info.getNameWCategory()+"]");
                    if (info.get() != null)
                        System.out.print(" with the value ["+info.get()+"] with type ["+info.getType()+"]");
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void render(PoseStack matrices, int mouseX, int mouseY, float delta) {
            this.renderBackground(matrices); // Renders background
            this.list.render(matrices, mouseX, mouseY, delta); // Render buttons
            drawCenteredString(matrices, font, title, width / 2, 15, 0xFFFFFF); // Render title

            // If the update is pending, display this message to inform the user that it will apply when the game restarts
            if (SelfUpdater.deleteOldOnClose)
                drawString(matrices, font, new TranslatableComponent("lod.updater.waitingForClose"), 4, height-38, 0xFFFFFF);


            // Render the tooltip only if it can find a tooltip in the language file
            for (AbstractConfigType info : ConfigBase.INSTANCE.entries) {
                if (info.getCategory().matches(category) && info.getAppearance().showInGui) {
                    if (list.getHoveredButton(mouseX, mouseY).isPresent()) {
                        AbstractWidget buttonWidget = list.getHoveredButton(mouseX, mouseY).get();
                        Component text = ButtonEntry.buttonsWithText.get(buttonWidget);
                        #if PRE_MC_1_19
                        TranslatableComponent name = new TranslatableComponent(this.translationPrefix + (info.category.isEmpty() ? "" : info.category + ".") + info.getName());
                        #else
                        Component name = Component.translatable(this.translationPrefix + (info.category.isEmpty() ? "" : info.category + ".") + info.getName());
                        #endif
                        String key = translationPrefix + (info.category.isEmpty() ? "" : info.category + ".") + info.getName() + ".@tooltip";

                        if (((EntryInfo) info.guiValue).error != null && text.equals(name))
                            renderTooltip(matrices, (Component) ((EntryInfo) info.guiValue).error.getValue(), mouseX, mouseY);
                        else if (I18n.exists(key) && (text != null && text.equals(name))) {
                            List<Component> list = new ArrayList<>();
                            for (String str : I18n.get(key).split("\n"))
                                #if PRE_MC_1_19
                                list.add(new TextComponent(str));
                                #else
                                list.add(Component.translatable(str));
                                #endif
                            renderComponentTooltip(matrices, list, mouseX, mouseY);
                        }
                    }
                }
            }
            super.render(matrices, mouseX, mouseY, delta);
        }
    }





    private static void initEntry(AbstractConfigType info, String translationPrefix) {
        info.guiValue = new EntryInfo();
        Class<?> fieldClass = info.getType();

        if (ConfigEntry.class.isAssignableFrom(info.getClass())) {
            if (fieldClass == Integer.class) {
                // For int
                textField(info, Integer::parseInt, INTEGER_ONLY_REGEX, true);
            } else if (fieldClass == Double.class) {
                // For double
                textField(info, Double::parseDouble, DECIMAL_ONLY_REGEX, false);
            } else if (fieldClass == String.class || fieldClass == List.class) {
                // For string or list
                textField(info, String::length, null, true);
            } else if (fieldClass == Boolean.class) {
                // For boolean
                #if PRE_MC_1_19
                Function<Object, Component> func = value -> new TextComponent((Boolean) value ? "True" : "False").withStyle((Boolean) value ? ChatFormatting.GREEN : ChatFormatting.RED);
                #else
                Function<Object, Component> func = value -> Component.translatable((Boolean) value ? "True" : "False").withStyle((Boolean) value ? ChatFormatting.GREEN : ChatFormatting.RED);
                #endif
                ((EntryInfo) info.guiValue).widget = new AbstractMap.SimpleEntry<Button.OnPress, Function<Object, Component>>(button -> {
                    ((ConfigEntry) info).setWithoutSaving(!(Boolean) info.get());
                    button.setMessage(func.apply(info.get()));
                }, func);
            }
            else if (fieldClass.isEnum())
            {
                // For enum
                List<?> values = Arrays.asList(info.getType().getEnumConstants());
                #if PRE_MC_1_19
                Function<Object, Component> func = value -> new TranslatableComponent(translationPrefix + "enum." + fieldClass.getSimpleName() + "." + info.get().toString());
                #else
                Function<Object, Component> func = value -> Component.translatable(translationPrefix + "enum." + fieldClass.getSimpleName() + "." + info.get().toString());
                #endif
                ((EntryInfo) info.guiValue).widget = new AbstractMap.SimpleEntry<Button.OnPress, Function<Object, Component>>(button -> {
                    int index = values.indexOf(info.get()) + 1;
                    info.set(values.get(index >= values.size() ? 0 : index));
                    button.setMessage(func.apply(info.get()));
                }, func);
            }
        } else if (ConfigCategory.class.isAssignableFrom(info.getClass())) {
//            if (!info.info.getName().equals(""))
//                info.name = new TranslatableComponent(info.info.getName());
        }
//        return info;
    }




    public static class ConfigListWidget extends ContainerObjectSelectionList<ButtonEntry> {
        Font textRenderer;

        public ConfigListWidget(Minecraft minecraftClient, int i, int j, int k, int l, int m) {
            super(minecraftClient, i, j, k, l, m);
            this.centerListVertically = false;
            textRenderer = minecraftClient.font;
        }

        public void addButton(AbstractWidget button, AbstractWidget resetButton, AbstractWidget indexButton, Component text) {
            this.addEntry(ButtonEntry.create(button, text, resetButton, indexButton));
        }

        @Override
        public int getRowWidth() {
            return 10000;
        }

        public Optional<AbstractWidget> getHoveredButton(double mouseX, double mouseY) {
            for (ButtonEntry buttonEntry : this.children()) {
                if (buttonEntry.button != null && buttonEntry.button.isMouseOver(mouseX, mouseY)) {
                    return Optional.of(buttonEntry.button);
                }
            }
            return Optional.empty();
        }
    }


    public static class ButtonEntry extends ContainerObjectSelectionList.Entry<ButtonEntry> {
        private static final Font textRenderer = Minecraft.getInstance().font;
        public final AbstractWidget button;
        private final AbstractWidget resetButton;
        private final AbstractWidget indexButton;
        private final Component text;
        private final List<AbstractWidget> children = new ArrayList<>();
        public static final Map<AbstractWidget, Component> buttonsWithText = new HashMap<>();

        private ButtonEntry(AbstractWidget button, Component text, AbstractWidget resetButton, AbstractWidget indexButton) {
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

        public static ButtonEntry create(AbstractWidget button, Component text, AbstractWidget resetButton, AbstractWidget indexButton) {
            return new ButtonEntry(button, text, resetButton, indexButton);
        }

        @Override
        public void render(PoseStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            if (button != null) {
                button.y = y;
                button.render(matrices, mouseX, mouseY, tickDelta);
            }
            if (resetButton != null) {
                resetButton.y = y;
                resetButton.render(matrices, mouseX, mouseY, tickDelta);
            }
            if (indexButton != null) {
                indexButton.y = y;
                indexButton.render(matrices, mouseX, mouseY, tickDelta);
            }
            if (text != null && (!text.getString().contains("spacer") || button != null))
                GuiComponent.drawString(matrices, textRenderer, text, 12, y + 5, 0xFFFFFF);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return children;
        }

        // Only for 1.17 and over
        // Remove in 1.16 and below
		#if POST_MC_1_17_1
        @Override
        public List<? extends NarratableEntry> narratables() {
            return children;
        }
		#endif
    }
}
