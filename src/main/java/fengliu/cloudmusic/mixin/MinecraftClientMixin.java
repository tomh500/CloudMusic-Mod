package fengliu.cloudmusic.mixin;

import fengliu.cloudmusic.command.MusicCommand;
import fengliu.cloudmusic.config.Configs;
import fengliu.cloudmusic.util.MusicPlayer;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftClientMixin {

    /**
     * 退出游戏/断开连接的时候停止播放音乐
     */
    @Inject(method = "disconnect(Lnet/minecraft/client/gui/screens/Screen;ZZ)V", at = @At("HEAD"))
    private void disconnect(CallbackInfo ci) {
        if(!Configs.PLAY.NOT_PLAY_GAME_MUSIC.getBooleanValue()){
            return;
        }

        MusicPlayer player = MusicCommand.getPlayer();
        if (!player.isPlaying()) {
            return;
        }

        player.stop();
    }
}