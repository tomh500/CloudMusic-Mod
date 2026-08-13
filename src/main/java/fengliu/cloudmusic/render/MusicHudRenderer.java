package fengliu.cloudmusic.render;

import com.google.gson.JsonElement;
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
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import org.joml.Matrix3x2fStack;

/**
 * 26.2 使用 Fabric API 的 HudElement 渲染 HUD, 替代旧版的 HudRenderCallback/InGameHudMixin
 */
public class MusicHudRenderer implements HudElement {
    public static final Identifier ID = Identifier.fromNamespaceAndPath("cloudmusic", "music_hud");
    private final Minecraft client = Minecraft.getInstance();

    public static void register() {
        HudElementRegistry.addLast(ID, new MusicHudRenderer());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, DeltaTracker deltaTracker) {
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
        float lyricScale = (float) Configs.GUI.LYRIC_SCALE.getDoubleValue();
        int lyricY = Configs.GUI.LYRIC_Y.getIntegerValue();
        int lyricX = Configs.GUI.LYRIC_X.getIntegerValue();
        int lyricColor = forceOpaqueColor(Configs.GUI.LYRIC_COLOR.getIntegerValue());
        Matrix3x2fStack pose = extractor.pose();
        pose.pushMatrix();
        pose.scale(lyricScale, lyricScale);
        for (String lyric : player.getLyric()) {
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