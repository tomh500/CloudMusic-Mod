package fengliu.cloudmusic.render;

import fengliu.cloudmusic.CloudMusicClient;
import fengliu.cloudmusic.music163.IMusic;
import fengliu.cloudmusic.util.HttpClient;
import fengliu.cloudmusic.util.PNGConverter;
import fengliu.cloudmusic.util.QRCode;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.io.File;
import java.io.FileInputStream;

public class MusicIconTexture {
    private static final Minecraft client = Minecraft.getInstance();
    public static Identifier MUSIC_ICON_ID = Identifier.fromNamespaceAndPath(CloudMusicClient.MOD_ID, "texture/music_icon.png");
    public static Identifier QR_CODE_ID = Identifier.fromNamespaceAndPath(CloudMusicClient.MOD_ID, "qr.code");
    private static boolean canUseIcon = false;

    /**
     * 获取封面并注册材质
     *
     * @param music 歌曲
     */
    public static void getMusicIcon(IMusic music) {
        Thread commandThread = new Thread(() -> {
            NativeImage img;
            try {
                img = NativeImage.read(PNGConverter.convertJPEGtoPNG(HttpClient.downloadStream(music.getPicUrl() + "?param=128y128")));
            } catch (Exception err) {
                err.printStackTrace();
                return;
            }

            client.execute(() -> {
                DynamicTexture imageTexture = new DynamicTexture(() -> "cloudmusic texture", img);
                client.getTextureManager().register(MUSIC_ICON_ID, imageTexture);
                canUseIcon = true;
            });
        });
        commandThread.setDaemon(true);
        commandThread.setName("CloudMusic getMusicIcon Thread");
        commandThread.start();
    }

    public static void getQRCode(String QRCodeData) {
        NativeImage img;
        try {
            File QRCodeFile = QRCode.generateQRCode(QRCodeData, CloudMusicClient.MC_PATH + "/cloud_music_qrcode.png", 128, 128, "png");
            img = NativeImage.read(new FileInputStream(QRCodeFile));
        } catch (Exception err) {
            err.printStackTrace();
            return;
        }

        client.execute(() -> {
            DynamicTexture imageTexture = new DynamicTexture(() -> "cloudmusic texture", img);
            client.getTextureManager().register(QR_CODE_ID, imageTexture);
        });
    }

    public static boolean canUseIcon() {
        return canUseIcon;
    }
}
