package com.seibel.lod.common.wrappers.config;

// Uses https://github.com/mwanji/toml4j for toml
import com.moandjiezana.toml.Toml;
// TomlWriter is threadsave while Writer is not
import com.moandjiezana.toml.TomlWriter;
import com.mojang.blaze3d.vertex.PoseStack;
import com.seibel.lod.common.LodCommonMain;
import com.seibel.lod.core.ModInfo;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
/**
 * Based upon TinyConfig
 * https://github.com/Minenash/TinyConfig
 *
 * Credits to Motschen
 *
 * @author coolGi2007
 * @version 12-09-2021
 */
// Everything required is packed into 1 class, so it is easier to copy
// This config should work for both Fabric and Forge as long as you use Mojang mappings
@SuppressWarnings("unchecked")
public abstract class ConfigGui {
    /*
            TODO list

        Make wiki
        Make it so you can enable and disable buttons from showing
        Make min and max not final
        Move the ConfigScreenConfigs class to the config class that extends this
     */
    /*
            List of hacky things that are done that should be done properly

        The buttons that dont show are still loded but just not rendered
        The screen with is set to double so the scroll bar dosnt show
     */
    private static final Pattern INTEGER_ONLY = Pattern.compile("(-?[0-9]*)");
    private static final Pattern DECIMAL_ONLY = Pattern.compile("-?([\\d]+\\.?[\\d]*|[\\d]*\\.?[\\d]+|\\.)");

    private static final List<EntryInfo> entries = new ArrayList<>();

    private static TomlWriter tomlWriter = new TomlWriter();

    private static class ConfigScreenConfigs {
        // This contains all the configs for the configs
        public static final int SpaceFromRightScreen = 10;
        public static final int ButtonWidthSpacing = 5;
        public static final int ResetButtonWidth = 40;
    }

    protected static class EntryInfo {
        Field field;
        Object widget;
        int width = 0;
        int max;
        Map.Entry<EditBox, Component> error;
        Object defaultValue;
        Object value;
        String tempValue;
        boolean inLimits = true;
        String id;              // ModID
        TranslatableComponent name;
        int index;
        boolean hideOption = false; // Hides the button
        boolean button = false; // This asks if it is a button to goto a new screen
        String gotoScreen = ""; // This is only called if button is true
        String category;
    }

    public static final Map<String, Class<?>> configClass = new HashMap<>();
//    public static List<String> nestedClasses = new ArrayList<>();
    private static Path path;

    public static void init(String modid, Class<?> config) {
        path = Minecraft.getInstance().gameDirectory.toPath().resolve("config").resolve(modid + ".toml");

        // Goes through all the nested classes and normal classes and inits them
        initClass(modid, config, "");

        // Save and read the file
        try {
            new Toml().read(path.toFile()).to(config);
//            new Toml().read(path.toFile()).
        }   catch (Exception e) {
            createFile(modid);
        }

        for (EntryInfo info : entries) {
            if (info.field.isAnnotationPresent(Entry.class))
                try {
                    info.value = info.field.get(null);
                    info.tempValue = info.value.toString();
                } catch (IllegalAccessException ignored) {
                }
        }
    }
    private static void initClass(String modid, Class<?> config, String category) {
        String e = modid + (category != "" ? "." + category : "");
        configClass.put(e, config);
        for (Field field : config.getFields()) {
            EntryInfo info = new EntryInfo();
            if (field.isAnnotationPresent(Entry.class) || field.isAnnotationPresent(Comment.class) || field.isAnnotationPresent(ScreenEntry.class))
                // If putting in your own mod then put your own check for server sided
                if (!LodCommonMain.serverSided)
                    initClient(modid, field, info);
            if (field.isAnnotationPresent(Entry.class))
                try {
                    info.defaultValue = field.get(null);
                } catch (IllegalAccessException ignored) {}
            if (field.isAnnotationPresent(ScreenEntry.class)) {
                String c = field.getAnnotation(Category.class) != null ? field.getAnnotation(Category.class).value() : "";
                initClass(modid, field.getType(),
                        (c != "" ? c + "." : "")
                                + field.getName());
            }
        }
    }
    private static void initClient(String modid, Field field, EntryInfo info) {
        // This adds the buttons to the queue to be rendered
        Class<?> type = field.getType();
        Category c = field.getAnnotation(Category.class);
        Entry e = field.getAnnotation(Entry.class);
        ScreenEntry s = field.getAnnotation(ScreenEntry.class);
        if (e!=null)
            info.width = e.width();
        else if (s!=null)
            info.width = s.width();
        info.field = field;
        info.id = modid;
        info.category = c != null ? c.value() : "";

        if (e != null) {
            if (!e.name().equals(""))
                info.name = new TranslatableComponent(e.name());
            if (type == int.class) // For int
                textField(info, Integer::parseInt, INTEGER_ONLY, e.min(), e.max(), true);
            else if (type == double.class) // For double
                textField(info, Double::parseDouble, DECIMAL_ONLY, e.min(), e.max(), false);
            else if (type == String.class || type == List.class) {  // For string or list
                info.max = e.max() == Double.MAX_VALUE ? Integer.MAX_VALUE : (int) e.max();
                textField(info, String::length, null, Math.min(e.min(), 0), Math.max(e.max(), 1), true);
            } else if (type == boolean.class) { // For boolean
                Function<Object, Component> func = value -> new TextComponent((Boolean) value ? "True" : "False").withStyle((Boolean) value ? ChatFormatting.GREEN : ChatFormatting.RED);
                info.widget = new AbstractMap.SimpleEntry<Button.OnPress, Function<Object, Component>>(button -> {
                    info.value = !(Boolean) info.value;
                    button.setMessage(func.apply(info.value));
                }, func);
            } else if (type.isEnum()) { // For enum
                List<?> values = Arrays.asList(field.getType().getEnumConstants());
                Function<Object, Component> func = value -> new TranslatableComponent(modid + ".config." + "enum." + type.getSimpleName() + "." + info.value.toString());
                info.widget = new AbstractMap.SimpleEntry<Button.OnPress, Function<Object, Component>>(button -> {
                    int index = values.indexOf(info.value) + 1;
                    info.value = values.get(index >= values.size() ? 0 : index);
                    button.setMessage(func.apply(info.value));
                }, func);
            }
        } else if (s != null) {
            if (!s.name().equals(""))
                info.name = new TranslatableComponent(s.name());
            info.button = true;
            info.gotoScreen = (info.category != "" ? info.category + "." : "") + field.getName();
        }
        entries.add(info);
    }

    private static void textField(EntryInfo info, Function<String,Number> f, Pattern pattern, double min, double max, boolean cast) {
        boolean isNumber = pattern != null;
        info.widget = (BiFunction<EditBox, Button, Predicate<String>>) (t, b) -> s -> {
            s = s.trim();
            if (!(s.isEmpty() || !isNumber || pattern.matcher(s).matches())) return false;

            Number value = 0;
            boolean inLimits = false;
            info.error = null;
            if (!(isNumber && s.isEmpty()) && !s.equals("-") && !s.equals(".")) {
                value = f.apply(s);
                inLimits = value.doubleValue() >= min && value.doubleValue() <= max;
                info.error = inLimits? null : new AbstractMap.SimpleEntry<>(t, new TextComponent(value.doubleValue() < min ?
                        "§cMinimum " + (isNumber? "value" : "length") + (cast? " is " + (int)min : " is " + min) :
                        "§cMaximum " + (isNumber? "value" : "length") + (cast? " is " + (int)max : " is " + max)));
            }

            info.tempValue = s;
            t.setTextColor(inLimits? 0xFFFFFFFF : 0xFFFF7777);
            info.inLimits = inLimits;
            b.active = entries.stream().allMatch(e -> e.inLimits);

            if (inLimits && info.field.getType() != List.class)
                info.value = isNumber? value : s;
            else if (inLimits) {
                if (((List<String>) info.value).size() == info.index) ((List<String>) info.value).add("");
                ((List<String>) info.value).set(info.index, Arrays.stream(info.tempValue.replace("[", "").replace("]", "").split(", ")).toList().get(0));
            }

            return true;
        };
    }

    // Creates the modid.toml
    private static void createFile(String modid) {
        path = Minecraft.getInstance().gameDirectory.toPath().resolve("config").resolve(modid + ".toml");
        try {
            if (!Files.exists(path))
                Files.createFile(path);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Screen getScreen(Screen parent, String modid, String category) {
        return new ConfigScreen(parent, modid, category);
    }
    private static class ConfigScreen extends Screen {
        protected ConfigScreen(Screen parent, String modid, String category) {
            super(new TranslatableComponent(modid + ".config.title"));
            this.parent = parent;
            this.modid = modid;
            this.category = category;
            this.translationPrefix = modid + ".config.";
        }
        private final String translationPrefix;
        private final Screen parent;
        private final String modid;
        private String category;
        private ConfigListWidget list;
        private boolean reload = false;

        // Real Time config update //
        @Override
        public void tick() {
            super.tick();
            for (EntryInfo info : entries) {
                try {info.field.set(null, info.value);} catch (IllegalAccessException ignored) {}
            }
        }
        private void loadValues() {
            try {
                new Toml().read(path.toFile()).to(configClass.get(modid));
            } catch (Exception e) {
                createFile(modid);
            }

            for (EntryInfo info : entries) {
                if (info.field.isAnnotationPresent(Entry.class))
                    try {
                        info.value = info.field.get(null);
                        info.tempValue = info.value.toString();
                    } catch (IllegalAccessException ignored) {}
            }
        }
        @Override
        protected void init() {
            super.init();
            if (!reload) loadValues();

            this.addRenderableWidget(new Button(this.width / 2 - 154, this.height - 28, 150, 20, CommonComponents.GUI_CANCEL, button -> {
                loadValues();
                Objects.requireNonNull(minecraft).setScreen(parent);
            }));

            Button done = this.addRenderableWidget(new Button(this.width / 2 + 4, this.height - 28, 150, 20, CommonComponents.GUI_DONE, (button) -> {
                for (EntryInfo info : entries)
                    if (info.id.equals(modid)) {
                        try {
                            info.field.set(null, info.value);
                        } catch (IllegalAccessException ignored) {}
                    }
                createFile(modid);
                Objects.requireNonNull(minecraft).setScreen(parent);
            }));

            this.list = new ConfigListWidget(this.minecraft, this.width*2, this.height, 32, this.height - 32, 25);
            if (this.minecraft != null && this.minecraft.level != null) this.list.setRenderBackground(false);
            this.addWidget(this.list);
            for (EntryInfo info : entries) {
                if (info.id.equals(modid) && info.category.matches(category) && !info.hideOption) {
//                if (info.id.equals(modid) && !info.hideOption) {
                    TranslatableComponent name = Objects.requireNonNullElseGet(info.name, () -> new TranslatableComponent(translationPrefix + (info.category != "" ? info.category + "." : "") + info.field.getName()));
                    Button resetButton = new Button(this.width - ConfigScreenConfigs.SpaceFromRightScreen - info.width - ConfigScreenConfigs.ButtonWidthSpacing - ConfigScreenConfigs.ResetButtonWidth, 0, ConfigScreenConfigs.ResetButtonWidth, 20, new TextComponent("Reset").withStyle(ChatFormatting.RED), (button -> {
                        info.value = info.defaultValue;
                        info.tempValue = info.defaultValue.toString();
                        info.index = 0;
                        this.reload = true;
                        Objects.requireNonNull(minecraft).setScreen(this);
                    }));

                    if (info.widget instanceof Map.Entry) {
                        Map.Entry<Button.OnPress, Function<Object, Component>> widget = (Map.Entry<Button.OnPress, Function<Object, Component>>) info.widget;
                        if (info.field.getType().isEnum())
                            widget.setValue(value -> new TranslatableComponent(translationPrefix + "enum." + info.field.getType().getSimpleName() + "." + info.value.toString()));
                        this.list.addButton(new Button(this.width - info.width - ConfigScreenConfigs.SpaceFromRightScreen, 0, info.width, 20, widget.getValue().apply(info.value), widget.getKey()), resetButton, null, name);
                    } else if (info.field.getType() == List.class) {
                        if (!reload) info.index = 0;
                        EditBox widget = new EditBox(font, this.width- info.width - ConfigScreenConfigs.SpaceFromRightScreen, 0, info.width, 20, null);
                        widget.setMaxLength(info.width);
                        if (info.index < ((List<String>) info.value).size())
                            widget.insertText((String.valueOf(((List<String>) info.value).get(info.index))));
                        else widget.insertText("");
                        Predicate<String> processor = ((BiFunction<EditBox, Button, Predicate<String>>) info.widget).apply(widget, done);
                        widget.setFilter(processor);
                        resetButton.setWidth(20);
                        resetButton.setMessage(new TextComponent("R").withStyle(ChatFormatting.RED));
                        Button cycleButton = new Button(this.width - 185, 0, 20, 20, new TextComponent(String.valueOf(info.index)).withStyle(ChatFormatting.GOLD), (button -> {
                            ((List<String>) info.value).remove("");
                            this.reload = true;
                            info.index = info.index + 1;
                            if (info.index > ((List<String>) info.value).size()) info.index = 0;
                            Objects.requireNonNull(minecraft).setScreen(this);
                        }));
                        this.list.addButton(widget, resetButton, cycleButton, name);
                    } else if (info.widget != null) {
                        EditBox widget = new EditBox(font, this.width - info.width - ConfigScreenConfigs.SpaceFromRightScreen + 2, 0, info.width - 4, 20, null);
                        widget.setMaxLength(info.width);
                        widget.insertText(info.tempValue);
                        Predicate<String> processor = ((BiFunction<EditBox, Button, Predicate<String>>) info.widget).apply(widget, done);
                        widget.setFilter(processor);
                        this.list.addButton(widget, resetButton, null, name);
                    } else if (info.button) {
                        Button widget = new Button(this.width / 2 - info.width, this.height - 28, info.width*2, 20, name, (button -> {
                            Objects.requireNonNull(minecraft).setScreen(ConfigGui.getScreen(this, modid, info.gotoScreen));
                        }));
                        this.list.addButton(widget, null, null, null);
                    } else {
                        this.list.addButton(null, null, null, name);
                    }
                }
            }

        }
        @Override
        public void render(PoseStack matrices, int mouseX, int mouseY, float delta) {
            this.renderBackground(matrices);
            this.list.render(matrices, mouseX, mouseY, delta);
            // Render title
            drawCenteredString(matrices, font, title, width / 2, 15, 0xFFFFFF);


            // TODO[CONFIG]: Fix the tooltip
            /*
            for (EntryInfo info : entries) {
                if (info.id.equals(modid)) {
                    if (list.getHoveredButton(mouseX,mouseY).isPresent()) {
                        AbstractWidget buttonWidget = list.getHoveredButton(mouseX,mouseY).get();
                        Component text = ButtonEntry.buttonsWithText.get(buttonWidget);
                        TranslatableComponent name = new TranslatableComponent(this.translationPrefix + (info.category != "" ? info.category + "." : "") + info.field.getName());
                        String key = translationPrefix + (info.category != "" ? info.category + "." : "") + info.field.getName() + ".@tooltip";

                        if (info.error != null && text.equals(name)) renderTooltip(matrices, info.error.getValue(), mouseX, mouseY);
                        else if (I18n.exists(key) && text.equals(name)) {
                            List<Component> list = new ArrayList<>();
                            for (String str : I18n.get(key).split("\n"))
                                list.add(new TextComponent(str));
                            renderTooltip(matrices, (Component) list, mouseX, mouseY);
                        }
                    }
                }
            }
             */
            super.render(matrices,mouseX,mouseY,delta);
        }
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
        public int getRowWidth() { return 10000; }
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
            buttonsWithText.put(button,text);
            this.button = button;
            this.resetButton = resetButton;
            this.text = text;
            this.indexButton = indexButton;
            if (button != null) children.add(button);
            if (resetButton != null) children.add(resetButton);
            if (indexButton != null) children.add(indexButton);
        }
        public static ButtonEntry create(AbstractWidget button, Component text, AbstractWidget resetButton, AbstractWidget indexButton) {
            return new ButtonEntry(button, text, resetButton, indexButton);
        }
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
                GuiComponent.drawString(matrices,textRenderer, text,12,y+5,0xFFFFFF);
        }
        @Override
        public List<? extends GuiEventListener> children() {return children;}
        @Override
        public List<? extends NarratableEntry> narratables() {return children;}
    }

    // Where the @Entry is defined
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface Entry {
        String name() default "";
        int width() default 150;
        double min() default Double.MIN_NORMAL;
        double max() default Double.MAX_VALUE;
    }

    // Where the @ScreenEntry is defined
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface ScreenEntry {
        String name() default "";
        int width() default 100;
    }

    // Where the @Category is defined
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface Category {
        String value() default "";
    }

    // Where the @Comment is defined
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface Comment {}
}
