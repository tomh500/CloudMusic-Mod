package fengliu.cloudmusic.util;

import fengliu.cloudmusic.config.Configs;
import net.minecraft.network.chat.*;
import net.minecraft.ChatFormatting;

import java.util.function.Function;

public class TextClickItem {
    public final MutableComponent show;
    protected MutableComponent text;
    public final String commandSuggest;

    public TextClickItem(MutableComponent text, MutableComponent show, String commandSuggest) {
        this.text = text;
        this.show = show;
        this.commandSuggest = commandSuggest;
    }

    public TextClickItem(MutableComponent text, String commandSuggest) {
        this(text, Component.translatable(IdUtil.getOptionsShow("default")), commandSuggest);
    }

    public TextClickItem(String textId, String showId, String commandSuggest) {
        this(Component.translatable(textId), Component.translatable(showId), commandSuggest);
    }

    public TextClickItem(String textId, String commandSuggest) {
        this(IdUtil.getOptions(textId), IdUtil.getOptionsShow(textId), commandSuggest);
    }

    public static MutableComponent combine(TextClickItem... items) {
        return combine(" ", mutableText -> mutableText.setStyle(mutableText.getStyle().withBold(true).withColor(ChatFormatting.RED)), items);
    }

    public TextClickItem appendToStarts(MutableComponent text) {
        this.text = text.append(this.text);
        return this;
    }

    public TextClickItem appendToStarts(String textId) {
        return this.appendToStarts(Component.translatable(textId));
    }

    public TextClickItem append(MutableComponent text) {
        this.text.append(text);
        return this;
    }

    public TextClickItem append(String text) {
        this.text.append(text);
        return this;
    }

    public ClickEvent getAction() {
        if (Configs.COMMAND.CLICK_RUN_COMMAND.getBooleanValue() && this.commandSuggest.startsWith("/")) {
            return new ClickEvent.RunCommand(this.commandSuggest);
        }
        return new ClickEvent.SuggestCommand(this.commandSuggest);
    }

    public static MutableComponent combine(String sign, Function<MutableComponent, MutableComponent> setText, TextClickItem... items) {
        MutableComponent text = Component.empty();
        for (int index = 0; index < items.length; index++) {
            text.append(setText.apply(items[index].build()));
            if (index + 1 != items.length) {
                text.append(sign);
            }
        }
        return text;
    }

    public MutableComponent build() {
        return this.text.setStyle(Style.EMPTY
                .withClickEvent(this.getAction())
                .withHoverEvent(new HoverEvent.ShowText(this.show)));
    }
}
