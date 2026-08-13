package fengliu.cloudmusic.render;

import com.google.gson.JsonElement;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import fengliu.cloudmusic.command.MusicCommand;
import fengliu.cloudmusic.config.Configs;
import fengliu.cloudmusic.music163.IMusic;
import fengliu.cloudmusic.music163.data.DjMusic;
import fengliu.cloudmusic.music163.data.Music;
import fengliu.cloudmusic.util.MusicPlayer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import org.joml.Matrix3x2fStack;

/**
 * 26.2 使用 Fabric API 的 HudElement 渲染 HUD, 替代旧版的 HudRenderCallback/InGameHudMixin
 * 打开聊天栏时, 可以用鼠标拖动歌词和歌曲信息面板, 拖动后的位置会保存到 malilib 配置文件
 */
public class MusicHudRenderer implements HudElement {
    public static final Identifier ID = Identifier.fromNamespaceAndPath("cloudmusic", "music_hud");
    private final Minecraft client = Minecraft.getInstance();

    private enum DragTarget {
        NONE, MUSIC_INFO, LYRIC
    }

    private DragTarget dragTarget = DragTarget.NONE;
    private double dragGrabOffsetX;
    private double dragGrabOffsetY;
    private int dragEffectOffsetX;
    private int dragEffectOffsetY;

    public static void register() {
        HudElementRegistry.addLast(ID, new MusicHudRenderer());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, DeltaTracker deltaTracker) {
        this.handleDrag();
        this.renderLoginQrCode(extractor);

        MusicPlayer player = MusicCommand.getPlayer();
        IMusic playingMusic = player.getPlayingMusic();
        if (playingMusic == null) {
            return;
        }

        if (Configs.GUI.STOP_PLAY_SHOW_UI.getBooleanValue() && !player.isPlaying()) {
            return;
        }

        this.renderLyric(extractor, player);
        this.renderDragHints(extractor);

        if (!Configs.GUI.MUSIC_INFO.getBooleanValue()) {
            return;
        }

        int width = this.client.getWindow().getGuiScaledWidth();

        int[] pos = this.getMusicInfoPos();
        int y = pos[0];
        int x = pos[1];

        extractor.fill(width - 175 - x, y, width - x, 48 + y, Configs.GUI.MUSIC_INFO_COLOR.getIntegerValue());
        extractor.fill(width - 145 - x, 40 + y, width - 30 - x, 43 + y, Configs.GUI.MUSIC_PROGRESS_BAR_COLOR.getIntegerValue());
        int progress = Math.round((115 / (float) playingMusic.getDurationSecond()) * player.getPlayingProgressSecond());
        if (progress > 115) {
            progress = 115;
        }
        extractor.fill(width - 145 - x, 40 + y, width - 145 + progress - x, 43 + y, Configs.GUI.MUSIC_PLAYED_PROGRESS_BAR_COLOR.getIntegerValue());
        extractor.blit(MusicIconTexture.MUSIC_ICON_ID, width - 172 - x, (int) (2.5f + y), width - 172 - x + 32, (int) (2.5f + y) + 32, 0.0f, 1.0f, 0.0f, 1.0f);
        extractor.text(this.client.font, playingMusic.getName().length() > 16 ? playingMusic.getName().substring(0, 16) + "..." : playingMusic.getName(), width - 135 - x, 4 + y, forceOpaqueColor(Configs.GUI.MUSIC_INFO_TITLE_FONT_COLOR.getIntegerValue()), true);

        int progressFontColor = forceOpaqueColor(Configs.GUI.MUSIC_PROGRESS_FONT_COLOR.getIntegerValue());
        extractor.text(this.client.font, player.getPlayingProgressToString(), width - 172 - x, 38 + y, progressFontColor, true);
        extractor.text(this.client.font, playingMusic.getDurationToString(), width - 28 - x, 38 + y, progressFontColor, true);
        int musicFontColor = forceOpaqueColor(Configs.GUI.MUSIC_INFO_FONT_COLOR.getIntegerValue());
        if (playingMusic instanceof DjMusic music) {
            extractor.text(this.client.font, Component.translatable("cloudmusic.info.dj.creator", music.dj.get("nickname").getAsString()), width - 135 - x, 14 + y, musicFontColor, true);
            extractor.text(this.client.font, Component.translatable("cloudmusic.info.dj.music.count", music.listenerCount, music.likedCount), width - 135 - x, 24 + y, musicFontColor, true);
            return;
        }

        Music music = (Music) playingMusic;
        if (!music.aliasName.isEmpty()) {
            extractor.text(this.client.font, music.aliasName.length() > 16 ? music.aliasName.substring(0, 16) + "..." : music.aliasName, width - 135 - x, 14 + y, musicFontColor, true);
        } else {
            String album = music.album.get("name").getAsString();
            extractor.text(this.client.font, album.length() > 16 ? album.substring(0, 16) + "..." : album, width - 135 - x, 14 + y, musicFontColor, true);
        }

        StringBuilder artist = new StringBuilder();
        for (JsonElement artistData : music.artists.asList()) {
            artist.append(artistData.getAsJsonObject().get("name").getAsString()).append("/");
        }
        artist = new StringBuilder(artist.substring(0, artist.length() - 1));
        extractor.text(this.client.font, artist.length() > 16 ? artist.substring(0, 16) + "..." : artist.toString(), width - 135 - x, 24 + y, musicFontColor, true);

        if (music.freeTrialInfo == null) {
            return;
        }

        int freeTrialEndProgress = Math.round((115 / (float) playingMusic.getDurationSecond()) * music.freeTrialInfo.get("end").getAsInt());
        extractor.fill(width - 145 + freeTrialEndProgress - 1 - x, 40 + y, width - 145 + freeTrialEndProgress + 1 - x, 43 + y, Configs.GUI.MUSIC_PLAYED_PROGRESS_BAR_COLOR.getIntegerValue());
    }

    private void renderLyric(GuiGraphicsExtractor extractor, MusicPlayer player) {
        if (!Configs.GUI.LYRIC.getBooleanValue()) {
            return;
        }

        String[] lyrics = player.getLyric();
        if (lyrics.length == 0) {
            return;
        }

        float lyricScale = (float) Configs.GUI.LYRIC_SCALE.getDoubleValue();
        int lyricY = Configs.GUI.LYRIC_Y.getIntegerValue();
        int lyricX = Configs.GUI.LYRIC_X.getIntegerValue();
        int lyricColor = forceOpaqueColor(Configs.GUI.LYRIC_COLOR.getIntegerValue());
        Matrix3x2fStack pose = extractor.pose();
        pose.pushMatrix();
        pose.scale(lyricScale, lyricScale);
        for (String lyric : lyrics) {
            extractor.text(this.client.font, lyric, lyricX, lyricY, lyricColor, true);
            lyricY += 10;
        }
        pose.popMatrix();
    }

    private void renderLoginQrCode(GuiGraphicsExtractor extractor) {
        if (!MusicCommand.loadQRCode) {
            return;
        }
        extractor.blit(MusicIconTexture.QR_CODE_ID, 5, 10, 5 + 64, 10 + 64, 0.0f, 1.0f, 0.0f, 1.0f);
    }

    private void handleDrag() {
        if (!(this.client.gui.screen() instanceof ChatScreen)) {
            if (this.dragTarget != DragTarget.NONE) {
                Configs.INSTANCE.save();
                this.dragTarget = DragTarget.NONE;
            }
            return;
        }

        double mouseX = this.client.mouseHandler.getScaledXPos(this.client.getWindow());
        double mouseY = this.client.mouseHandler.getScaledYPos(this.client.getWindow());

        if (this.dragTarget == DragTarget.NONE) {
            if (!this.client.mouseHandler.isLeftPressed()) {
                return;
            }

            DragTarget target = this.getDragTargetAt(mouseX, mouseY);
            if (target == DragTarget.NONE) {
                return;
            }

            this.dragTarget = target;
            this.dragGrabOffsetX = 0;
            this.dragGrabOffsetY = 0;
            this.dragEffectOffsetX = 0;
            this.dragEffectOffsetY = 0;

            if (target == DragTarget.MUSIC_INFO) {
                int width = this.client.getWindow().getGuiScaledWidth();
                int[] pos = this.getMusicInfoPos();
                int configX = Configs.GUI.MUSIC_INFO_X.getIntegerValue();
                int configY = Configs.GUI.MUSIC_INFO_Y.getIntegerValue();
                this.dragGrabOffsetX = mouseX - (width - 175 - pos[1]);
                this.dragGrabOffsetY = mouseY - pos[0];
                this.dragEffectOffsetX = pos[1] - configX;
                this.dragEffectOffsetY = pos[0] - configY;
            } else {
                float scale = (float) Configs.GUI.LYRIC_SCALE.getDoubleValue();
                this.dragGrabOffsetX = mouseX - (Configs.GUI.LYRIC_X.getIntegerValue() * scale);
                this.dragGrabOffsetY = mouseY - (Configs.GUI.LYRIC_Y.getIntegerValue() * scale);
            }
            return;
        }

        if (!this.client.mouseHandler.isLeftPressed()) {
            Configs.INSTANCE.save();
            this.dragTarget = DragTarget.NONE;
            return;
        }

        if (this.dragTarget == DragTarget.MUSIC_INFO) {
            int width = this.client.getWindow().getGuiScaledWidth();
            int newX = clamp((int) Math.round(width - 175 - (mouseX - this.dragGrabOffsetX) - this.dragEffectOffsetX), Configs.GUI.MUSIC_INFO_X.getMinIntegerValue(), Configs.GUI.MUSIC_INFO_X.getMaxIntegerValue());
            int newY = clamp((int) Math.round(mouseY - this.dragGrabOffsetY - this.dragEffectOffsetY), Configs.GUI.MUSIC_INFO_Y.getMinIntegerValue(), Configs.GUI.MUSIC_INFO_Y.getMaxIntegerValue());
            Configs.GUI.MUSIC_INFO_X.setIntegerValue(newX);
            Configs.GUI.MUSIC_INFO_Y.setIntegerValue(newY);
        } else {
            float scale = (float) Configs.GUI.LYRIC_SCALE.getDoubleValue();
            int newX = clamp(Math.round((float) (mouseX - this.dragGrabOffsetX) / scale), Configs.GUI.LYRIC_X.getMinIntegerValue(), Configs.GUI.LYRIC_X.getMaxIntegerValue());
            int newY = clamp(Math.round((float) (mouseY - this.dragGrabOffsetY) / scale), Configs.GUI.LYRIC_Y.getMinIntegerValue(), Configs.GUI.LYRIC_Y.getMaxIntegerValue());
            Configs.GUI.LYRIC_X.setIntegerValue(newX);
            Configs.GUI.LYRIC_Y.setIntegerValue(newY);
        }
    }

    /**
     * 聊天栏打开时, 鼠标悬停在可拖动控件上就画一个边框并显示移动光标
     */
    private void renderDragHints(GuiGraphicsExtractor extractor) {
        if (!(this.client.gui.screen() instanceof ChatScreen)) {
            return;
        }

        double mouseX = this.client.mouseHandler.getScaledXPos(this.client.getWindow());
        double mouseY = this.client.mouseHandler.getScaledYPos(this.client.getWindow());

        int[] rect = this.getMusicInfoRect();
        if (rect != null && this.isInRect(mouseX, mouseY, rect)) {
            this.drawRectBorder(extractor, rect, 0xAAFFFFFF);
            extractor.requestCursor(CursorTypes.RESIZE_ALL);
            return;
        }

        rect = this.getLyricRect();
        if (rect != null && this.isInRect(mouseX, mouseY, rect)) {
            this.drawRectBorder(extractor, rect, 0xAAFFFFFF);
            extractor.requestCursor(CursorTypes.RESIZE_ALL);
        }
    }

    private DragTarget getDragTargetAt(double mouseX, double mouseY) {
        int[] rect = this.getMusicInfoRect();
        if (rect != null && this.isInRect(mouseX, mouseY, rect)) {
            return DragTarget.MUSIC_INFO;
        }

        rect = this.getLyricRect();
        if (rect != null && this.isInRect(mouseX, mouseY, rect)) {
            return DragTarget.LYRIC;
        }

        return DragTarget.NONE;
    }

    /**
     * 歌曲信息面板的屏幕区域, 不可见时返回 null
     */
    private int[] getMusicInfoRect() {
        MusicPlayer player = MusicCommand.getPlayer();
        if (player.getPlayingMusic() == null) {
            return null;
        }
        if (Configs.GUI.STOP_PLAY_SHOW_UI.getBooleanValue() && !player.isPlaying()) {
            return null;
        }
        if (!Configs.GUI.MUSIC_INFO.getBooleanValue()) {
            return null;
        }

        int width = this.client.getWindow().getGuiScaledWidth();
        int[] pos = this.getMusicInfoPos();
        return new int[]{width - 175 - pos[1], pos[0], width - pos[1], pos[0] + 48};
    }

    /**
     * 歌词的屏幕区域(按缩放计算), 没有歌词或关闭时返回 null
     */
    private int[] getLyricRect() {
        if (!Configs.GUI.LYRIC.getBooleanValue()) {
            return null;
        }

        String[] lyrics = MusicCommand.getPlayer().getLyric();
        if (lyrics.length == 0) {
            return null;
        }

        float scale = (float) Configs.GUI.LYRIC_SCALE.getDoubleValue();
        int maxWidth = 0;
        for (String line : lyrics) {
            maxWidth = Math.max(maxWidth, this.client.font.width(line));
        }

        int x0 = Math.round(Configs.GUI.LYRIC_X.getIntegerValue() * scale);
        int y0 = Math.round(Configs.GUI.LYRIC_Y.getIntegerValue() * scale);
        int x1 = x0 + Math.round(maxWidth * scale) + 4;
        int y1 = y0 + Math.round(lyrics.length * 10 * scale) + 4;
        return new int[]{x0, y0, x1, y1};
    }

    private boolean isInRect(double mouseX, double mouseY, int[] rect) {
        return mouseX >= rect[0] && mouseX <= rect[2] && mouseY >= rect[1] && mouseY <= rect[3];
    }

    private void drawRectBorder(GuiGraphicsExtractor extractor, int[] rect, int color) {
        extractor.fill(rect[0], rect[1], rect[2], rect[1] + 1, color);
        extractor.fill(rect[0], rect[3] - 1, rect[2], rect[3], color);
        extractor.fill(rect[0], rect[1], rect[0] + 1, rect[3], color);
        extractor.fill(rect[2] - 1, rect[1], rect[2], rect[3], color);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int forceOpaqueColor(int color) {
        return (color & 0xFF000000) == 0 ? (color | 0xFF000000) : color;
    }

    private int[] getMusicInfoPos() {
        int y = Configs.GUI.MUSIC_INFO_Y.getIntegerValue();
        int x = Configs.GUI.MUSIC_INFO_X.getIntegerValue();
        if (this.client.player == null || !Configs.GUI.MUSIC_INFO_EFFECT_OFFSET.getBooleanValue()) {
            return new int[]{y, x};
        }

        int offset = 0;
        for (MobEffectInstance statusEffect : this.client.player.getActiveEffects()) {
            if (statusEffect.getEffect().value().isBeneficial()) {
                offset = 1;
            } else {
                offset = 2;
                break;
            }
        }

        if (offset == 0) {
            return new int[]{y, x};
        }
        return new int[]{y + Configs.GUI.MUSIC_INFO_EFFECT_OFFSET_Y.getIntegerValue() * offset, x + Configs.GUI.MUSIC_INFO_EFFECT_OFFSET_X.getIntegerValue()};
    }
}
