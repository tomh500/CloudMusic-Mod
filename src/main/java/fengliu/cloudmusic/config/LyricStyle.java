package fengliu.cloudmusic.config;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import net.minecraft.network.chat.Component;

/**
 * 歌词显示样式: default=游戏内面板, actionbar=原版 ActionBar 居中显示, off=关闭
 */
public enum LyricStyle implements IConfigOptionListEntry {
    DEFAULT("cloudmusic.lyric.style.default", "default"),
    ACTIONBAR("cloudmusic.lyric.style.actionbar", "actionbar"),
    OFF("cloudmusic.lyric.style.off", "off");

    private final String translationKey;
    private final String style;

    LyricStyle(String translationKey, String style) {
        this.translationKey = translationKey;
        this.style = style;
    }

    @Override
    public String getStringValue() {
        return this.style;
    }

    @Override
    public String getDisplayName() {
        return Component.translatable(this.translationKey).getString();
    }

    @Override
    public IConfigOptionListEntry cycle(boolean forward) {
        int id = this.ordinal();

        if (forward) {
            if (++id >= values().length) {
                id = 0;
            }
        } else {
            if (--id < 0) {
                id = values().length - 1;
            }
        }

        return values()[id % values().length];
    }

    @Override
    public IConfigOptionListEntry fromString(String value) {
        for (LyricStyle style : LyricStyle.values()) {
            if (style.getStringValue().equals(value)) {
                return style;
            }
        }

        return LyricStyle.DEFAULT;
    }
}
